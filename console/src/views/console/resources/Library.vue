<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import axios from "axios";
import { createSHA256 } from "hash-wasm";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import PageCard from "@/views/console/PageCard.vue";
import {
  listCollections,
  listResources,
  listTagCatalog,
  beginAttachmentUpload,
  commitAttachmentUpload,
  createResource,
  resourceLifecycles,
  resourceTypes,
  type Collection,
  type Resource,
  type ResourceTag
} from "@/api/resource";
import { listStorageProviders, type StorageProvider } from "@/api/storageProvider";
import { getHttpErrorMessage } from "@/utils/http";

const { t } = useI18n();
const router = useRouter();
const loading = ref(false);
const resources = ref<Resource[]>([]);
const total = ref(0);
const collections = ref<Collection[]>([]);
const tags = ref<ResourceTag[]>([]);
const createPanelVisible = ref(false);
const creating = ref(false);
const uploadProgress = ref(0);
const providers = ref<StorageProvider[]>([]);
const selectedFile = ref<File | null>(null);
const createForm = reactive({
  title: "",
  type: "OTHER" as (typeof resourceTypes)[number],
  locale: "zh-CN",
  provider: "",
  path: ""
});
const writableProviders = computed(() =>
  providers.value.filter(provider => provider.enabled && provider.drain_status === "NORMAL")
);

const filters = reactive({
  query: "",
  type: "",
  lifecycle_status: "",
  collection_id: "",
  tag: "",
  source_provider: ""
});
const page = ref(0);
const size = ref(20);

const loadResources = async () => {
  loading.value = true;
  try {
    const result = await listResources({
      query: filters.query.trim() || undefined,
      type: filters.type || undefined,
      lifecycle_status: filters.lifecycle_status || undefined,
      collection_id: filters.collection_id || undefined,
      tag: filters.tag || undefined,
      source_provider: filters.source_provider.trim() || undefined,
      page: page.value,
      size: size.value
    });
    resources.value = result.items;
    total.value = result.total;
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("resourceLibrary.loadFailed")));
  } finally {
    loading.value = false;
  }
};

const loadOptions = async () => {
  try {
    collections.value = await listCollections();
  } catch {
    collections.value = [];
  }
  try {
    tags.value = (await listTagCatalog()).items;
  } catch {
    tags.value = [];
  }
};

const search = () => {
  page.value = 0;
  void loadResources();
};

const reset = () => {
  Object.assign(filters, {
    query: "",
    type: "",
    lifecycle_status: "",
    collection_id: "",
    tag: "",
    source_provider: ""
  });
  search();
};

const openDetail = (resource: Resource) => {
  void router.push(`/resources/library/${resource.id}`);
};

const openCreatePanel = async () => {
  createPanelVisible.value = true;
  if (providers.value.length) return;
  try {
    providers.value = await listStorageProviders();
    createForm.provider = writableProviders.value[0]?.provider_key ?? "";
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("resourceLibrary.providersLoadFailed")));
  }
};

const hashFile = async (file: File) => {
  const hasher = await createSHA256();
  const chunkSize = 2 * 1024 * 1024;
  for (let offset = 0; offset < file.size; offset += chunkSize) {
    hasher.update(new Uint8Array(await file.slice(offset, offset + chunkSize).arrayBuffer()));
  }
  return hasher.digest("hex") as string;
};

const submitCreate = async () => {
  const file = selectedFile.value;
  if (!file || !createForm.title.trim() || !createForm.provider || !createForm.path.trim()) return;

  creating.value = true;
  uploadProgress.value = 0;
  let createdResource: Resource | null = null;
  try {
    const sha256 = await hashFile(file);
    createdResource = await createResource(
      { type: createForm.type, title: createForm.title.trim(), locale: createForm.locale.trim() || "und" },
      crypto.randomUUID()
    );
    const intent = await beginAttachmentUpload(
      createdResource.id,
      {
        file_name: file.name,
        size_bytes: file.size,
        media_type: file.type || "application/octet-stream",
        provider: createForm.provider,
        object_key: createForm.path.trim(),
        sha256
      },
      crypto.randomUUID()
    );
    if (!intent.deduplicated) {
      if (!intent.url || intent.method.toUpperCase() !== "PUT") {
        throw new Error(t("resourceLibrary.unsupportedUploadMethod"));
      }
      const uploadHeaders = { ...intent.required_headers };
      const signedHeaders = new URL(intent.url).searchParams
        .get("X-Amz-SignedHeaders")
        ?.split(";")
        .map(header => header.toLowerCase()) ?? [];
      if (
        signedHeaders.includes("x-amz-checksum-sha256") &&
        !uploadHeaders["x-amz-checksum-sha256"]
      ) {
        const digestBytes = Uint8Array.from(
          sha256.match(/.{2}/g) ?? [],
          byte => Number.parseInt(byte, 16)
        );
        uploadHeaders["x-amz-checksum-sha256"] = btoa(
          String.fromCharCode(...digestBytes)
        );
      }
      uploadHeaders["content-type"] ??= file.type || "application/octet-stream";
      const missingSignedHeaders = signedHeaders.filter(
        header =>
          header !== "host" &&
          header !== "content-length" &&
          !Object.keys(uploadHeaders).some(key => key.toLowerCase() === header)
      );
      if (missingSignedHeaders.length > 0) {
        throw new Error(`上传签名头缺失：${missingSignedHeaders.join(", ")}`);
      }
      await axios.put(intent.url, file, {
        headers: {
          ...uploadHeaders,
          "Content-Type": (uploadHeaders["content-type"] ?? file.type) || "application/octet-stream"
        },
        onUploadProgress: event => {
          if (event.total) uploadProgress.value = Math.round((event.loaded / event.total) * 100);
        }
      });
    }
    await commitAttachmentUpload(createdResource.id, {
      sha256,
      upload_sha256: sha256,
      deduplicated: intent.deduplicated,
      size_bytes: file.size,
      media_type: file.type || "application/octet-stream",
      file_name: file.name,
      kind: "ORIGINAL",
      provider: intent.provider,
      tier: intent.tier,
      object_key: intent.object_key,
      idempotency_key: crypto.randomUUID()
    });
    createPanelVisible.value = false;
    ElMessage.success(t("resourceLibrary.createSuccess"));
    await loadResources();
    void openDetail(createdResource);
  } catch (error) {
    if (createdResource) {
      await loadResources();
      ElMessage.error(
        `${getHttpErrorMessage(error, t("resourceLibrary.createFailed"))} ${t("resourceLibrary.resourceCreatedWithoutAttachment")}`
      );
      void openDetail(createdResource);
    } else {
      ElMessage.error(getHttpErrorMessage(error, t("resourceLibrary.createFailed")));
    }
  } finally {
    creating.value = false;
  }
};

onMounted(async () => {
  await Promise.all([loadOptions(), loadResources()]);
});
</script>

<template>
  <PageCard>
    <el-form :inline="true" :model="filters" class="mt-6" @submit.prevent="search">
      <el-form-item :label="t('resourceLibrary.query')">
        <el-input v-model="filters.query" clearable style="width: 200px" @keyup.enter="search" />
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.type')">
        <el-select v-model="filters.type" clearable style="width: 160px">
          <el-option v-for="type in resourceTypes" :key="type" :label="type" :value="type" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.lifecycle')">
        <el-select v-model="filters.lifecycle_status" clearable style="width: 160px">
          <el-option v-for="item in resourceLifecycles" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.collection')">
        <el-select v-model="filters.collection_id" clearable filterable style="width: 200px">
          <el-option v-for="item in collections" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.tag')">
        <el-select v-model="filters.tag" clearable filterable style="width: 160px">
          <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.name" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.source')">
        <el-input v-model="filters.source_provider" clearable style="width: 160px" @keyup.enter="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">{{ t("resourceLibrary.search") }}</el-button>
        <el-button @click="reset">{{ t("resourceLibrary.reset") }}</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4 flex justify-between">
      <el-button type="primary" @click="openCreatePanel">{{ t("resourceLibrary.create") }}</el-button>
      <el-button :loading="loading" @click="loadResources">{{ t("resourceLibrary.refresh") }}</el-button>
    </div>

    <el-table v-loading="loading" :data="resources" row-key="id" border>
      <el-table-column :label="t('resourceLibrary.title')" min-width="220">
        <template #default="scope">{{ scope.row.primaryTitle ?? "-" }}</template>
      </el-table-column>
      <el-table-column prop="type" :label="t('resourceLibrary.type')" width="120" />
      <el-table-column prop="dataClassification" :label="t('resourceLibrary.classification')" width="140" />
      <el-table-column :label="t('resourceLibrary.lifecycle')" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.lifecycle === 'ACTIVE' ? 'success' : 'info'">
            {{ scope.row.lifecycle }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('resourceLibrary.updatedAt')" width="200">
        <template #default="scope">{{ new Date(scope.row.updatedAt).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column :label="t('resourceLibrary.actions')" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)">{{ t("resourceLibrary.details") }}</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty :description="t('resourceLibrary.empty')" /></template>
    </el-table>

    <div class="mt-4 flex justify-end">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadResources"
        @size-change="search"
      />
    </div>

    <el-drawer v-model="createPanelVisible" :title="t('resourceLibrary.createTitle')" size="520px" :before-close="(done: () => void) => !creating && done()">
      <el-form :model="createForm" label-position="top" @submit.prevent="submitCreate">
        <el-form-item :label="t('resourceLibrary.title')" required>
          <el-input v-model="createForm.title" maxlength="512" />
        </el-form-item>
        <el-form-item :label="t('resourceLibrary.type')" required>
          <el-select v-model="createForm.type" class="w-full">
            <el-option v-for="type in resourceTypes" :key="type" :value="type" :label="type" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('resourceLibrary.locale')" required>
          <el-input v-model="createForm.locale" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('resourceLibrary.storageProvider')" required>
          <el-select v-model="createForm.provider" class="w-full" :disabled="!writableProviders.length">
            <el-option v-for="provider in writableProviders" :key="provider.id" :value="provider.provider_key" :label="`${provider.display_name} (${provider.tier})`" />
          </el-select>
          <el-text v-if="!writableProviders.length" type="warning">{{ t("resourceLibrary.noWritableProviders") }}</el-text>
        </el-form-item>
        <el-form-item :label="t('resourceLibrary.storagePath')" required>
          <el-input v-model="createForm.path" maxlength="1024" :placeholder="t('resourceLibrary.storagePathHint')" />
        </el-form-item>
        <el-form-item :label="t('resourceLibrary.file')" required>
          <input type="file" class="block w-full" :disabled="creating" @change="selectedFile = ($event.target as HTMLInputElement).files?.[0] ?? null" />
          <span v-if="selectedFile" class="mt-1 text-sm text-[var(--el-text-color-secondary)]">{{ selectedFile.name }} · {{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB</span>
        </el-form-item>
        <el-progress v-if="creating" :percentage="uploadProgress" :status="uploadProgress === 100 ? 'success' : undefined" />
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button :disabled="creating" @click="createPanelVisible = false">{{ t("buttons.pureClose") }}</el-button>
          <el-button type="primary" :loading="creating" :disabled="!selectedFile || !createForm.title.trim() || !createForm.provider || !createForm.path.trim()" @click="submitCreate">{{ t("buttons.pureConfirm") }}</el-button>
        </div>
      </template>
    </el-drawer>
  </PageCard>
</template>
