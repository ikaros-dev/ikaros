<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import PageCard from "@/views/console/PageCard.vue";
import {
  addFavorite,
  addResourceTag,
  archiveResource,
  deleteResourceTag,
  getFavorite,
  getResource,
  listResourceAttachments,
  listResourceMetadata,
  listResourceRelations,
  listResourceTags,
  purgeResource,
  removeFavorite,
  restoreAutomaticMetadata,
  restoreResource,
  setResourceMetadata,
  trashResource,
  updateResource,
  type Resource,
  type ResourceAttachment,
  type ResourceMetadata,
  type ResourceRelation,
  type ResourceTag
} from "@/api/resource";
import { getHttpErrorMessage } from "@/utils/http";
import { useMultiTagsStoreHook } from "@/store/modules/multiTags";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const resourceId = computed(() => String(route.params.resourceId ?? ""));

const loading = ref(false);
const mutating = ref(false);
const resource = ref<Resource | null>(null);
const attachments = ref<ResourceAttachment[]>([]);
const tags = ref<ResourceTag[]>([]);
const metadata = ref<ResourceMetadata[]>([]);
const relations = ref<ResourceRelation[]>([]);
const favorite = ref(false);

const editVisible = ref(false);
const editForm = reactive({ primary_title: "", summary: "" });
const tagInput = ref("");
const metadataVisible = ref(false);
const metadataForm = reactive({ fieldKey: "", value: "" });

const loadResource = async () => {
  loading.value = true;
  try {
    const detail = await getResource(resourceId.value);
    resource.value = detail;
    const [attachmentList, tagList, metaList, relationList, favoriteState] = await Promise.all([
      listResourceAttachments(resourceId.value),
      listResourceTags(resourceId.value),
      listResourceMetadata(resourceId.value),
      listResourceRelations(resourceId.value),
      getFavorite(resourceId.value)
    ]);
    attachments.value = attachmentList;
    tags.value = tagList;
    metadata.value = metaList;
    relations.value = relationList;
    favorite.value = favoriteState.favorite;
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("resourceDetail.loadFailed")));
  } finally {
    loading.value = false;
  }
};

const runMutation = async (action: () => Promise<void>) => {
  mutating.value = true;
  try {
    await action();
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("resourceDetail.actionFailed")));
  } finally {
    mutating.value = false;
  }
};

const openEdit = () => {
  if (!resource.value) return;
  Object.assign(editForm, {
    primary_title: resource.value.primaryTitle ?? "",
    summary: resource.value.summary ?? ""
  });
  editVisible.value = true;
};

const submitEdit = async () => {
  const current = resource.value;
  if (!current) return;
  editVisible.value = false;
  await runMutation(async () => {
    resource.value = await updateResource(current.id, {
      primary_title: editForm.primary_title.trim(),
      summary: editForm.summary
    }, current.version);
    ElMessage.success(t("resourceDetail.updateSuccess"));
  });
};

const addTag = async () => {
  const current = resource.value;
  const name = tagInput.value.trim();
  if (!current || !name) return;
  await runMutation(async () => {
    await addResourceTag(current.id, { name });
    tagInput.value = "";
    tags.value = await listResourceTags(current.id);
    ElMessage.success(t("resourceDetail.tagAdded"));
  });
};

const removeTag = async (tag: ResourceTag) => {
  const current = resource.value;
  if (!current) return;
  await runMutation(async () => {
    await deleteResourceTag(current.id, tag.id);
    tags.value = await listResourceTags(current.id);
    ElMessage.success(t("resourceDetail.tagRemoved"));
  });
};

const toggleFavorite = async () => {
  const current = resource.value;
  if (!current) return;
  await runMutation(async () => {
    if (favorite.value) {
      await removeFavorite(current.id);
      favorite.value = false;
    } else {
      favorite.value = (await addFavorite(current.id)).favorite;
    }
  });
};

const changeLifecycle = async (action: "archive" | "restore" | "trash" | "purge") => {
  const current = resource.value;
  if (!current) return;
  if (action === "trash" || action === "purge") {
    try {
      await ElMessageBox.confirm(
        t(`resourceDetail.${action}Confirm`, { name: current.primaryTitle ?? current.id }),
        t(`resourceDetail.${action}Title`),
        { type: "warning", confirmButtonText: t("buttons.pureConfirm"), cancelButtonText: t("buttons.pureClose") }
      );
    } catch {
      return;
    }
  }
  await runMutation(async () => {
    if (action === "archive") resource.value = await archiveResource(current.id, current.version);
    else if (action === "restore") resource.value = await restoreResource(current.id, current.version);
    else if (action === "trash") await trashResource(current.id, current.version);
    else await purgeResource(current.id, current.version);
    ElMessage.success(t(`resourceDetail.${action}Success`));
    await loadResource();
  });
};

const openMetadata = (row?: ResourceMetadata) => {
  Object.assign(metadataForm, {
    fieldKey: row?.fieldKey ?? "",
    value: row?.value ?? ""
  });
  metadataVisible.value = true;
};

const submitMetadata = async () => {
  const current = resource.value;
  if (!current || !metadataForm.fieldKey.trim()) return;
  metadataVisible.value = false;
  await runMutation(async () => {
    await setResourceMetadata(current.id, metadataForm.fieldKey.trim(), metadataForm.value);
    metadata.value = await listResourceMetadata(current.id);
    ElMessage.success(t("resourceDetail.metadataSaved"));
  });
};

const restoreMetadata = async (row: ResourceMetadata) => {
  const current = resource.value;
  if (!current) return;
  await runMutation(async () => {
    await restoreAutomaticMetadata(current.id, row.fieldKey);
    metadata.value = await listResourceMetadata(current.id);
    ElMessage.success(t("resourceDetail.metadataRestored"));
  });
};

const back = () => void router.push("/resources/library");

const openAttachmentDetail = (attachment: ResourceAttachment) => {
  void router.push({
    name: "StorageAttachmentDetail",
    params: { attachmentId: attachment.id }
  });
};

watch(
  resourceId,
  id => {
    if (!id) return;
    const tags = useMultiTagsStoreHook();
    const routeName = String(route.name ?? "ResourceDetail");
    tags.multiTags = tags.multiTags.filter(tag =>
      tag.name !== routeName || tag.params?.resourceId !== undefined
    );
    tags.handleTags("push", {
      path: route.path,
      name: routeName,
      params: { ...route.params },
      meta: {
        ...route.meta,
        tabTitle: `${t("menus.resourceDetail")}-${id.slice(0, 8)}`
      }
    });
    void loadResource();
  },
  { immediate: true }
);
</script>

<template>
  <PageCard>
    <div class="mt-6 mb-4 flex items-center justify-between">
      <el-button @click="back">{{ t("resourceDetail.back") }}</el-button>
      <div class="flex gap-2">
        <el-button :loading="loading" @click="loadResource">{{ t("resourceDetail.refresh") }}</el-button>
        <el-button :disabled="!resource" @click="openEdit">{{ t("resourceDetail.edit") }}</el-button>
        <el-button :disabled="!resource" :loading="mutating" @click="toggleFavorite">
          {{ favorite ? t("resourceDetail.unfavorite") : t("resourceDetail.favorite") }}
        </el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="resource">
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('resourceDetail.id')">{{ resource.id }}</el-descriptions-item>
        <el-descriptions-item :label="t('resourceDetail.type')">{{ resource.type }}</el-descriptions-item>
        <el-descriptions-item :label="t('resourceDetail.primaryTitle')">{{ resource.primaryTitle ?? "-" }}</el-descriptions-item>
        <el-descriptions-item :label="t('resourceDetail.classification')">{{ resource.dataClassification }}</el-descriptions-item>
        <el-descriptions-item :label="t('resourceDetail.lifecycle')">
          <el-tag :type="resource.lifecycle === 'ACTIVE' ? 'success' : 'info'">{{ resource.lifecycle }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('resourceDetail.version')">{{ resource.version }}</el-descriptions-item>
        <el-descriptions-item :label="t('resourceDetail.createdAt')">{{ new Date(resource.createdAt).toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item :label="t('resourceDetail.updatedAt')">{{ new Date(resource.updatedAt).toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item :label="t('resourceDetail.summary')" :span="2">{{ resource.summary ?? "-" }}</el-descriptions-item>
      </el-descriptions>

      <div class="mt-4 flex flex-wrap gap-2">
        <el-button v-if="resource.lifecycle === 'ACTIVE'" :loading="mutating" @click="changeLifecycle('archive')">
          {{ t("resourceDetail.archive") }}
        </el-button>
        <el-button v-if="resource.lifecycle === 'ARCHIVED' || resource.lifecycle === 'TRASHED'" :loading="mutating" @click="changeLifecycle('restore')">
          {{ t("resourceDetail.restore") }}
        </el-button>
        <el-button v-if="resource.lifecycle === 'ACTIVE' || resource.lifecycle === 'ARCHIVED'" type="warning" :loading="mutating" @click="changeLifecycle('trash')">
          {{ t("resourceDetail.trash") }}
        </el-button>
        <el-button v-if="resource.lifecycle === 'TRASHED'" type="danger" :loading="mutating" @click="changeLifecycle('purge')">
          {{ t("resourceDetail.purge") }}
        </el-button>
      </div>

      <el-divider content-position="left">{{ t("resourceDetail.attachments") }}</el-divider>
      <el-table :data="attachments" row-key="id" border>
        <el-table-column prop="fileName" :label="t('resourceDetail.fileName')" min-width="220" />
        <el-table-column prop="kind" :label="t('resourceDetail.kind')" width="130" />
        <el-table-column prop="mediaType" :label="t('resourceDetail.mediaType')" min-width="180" />
        <el-table-column prop="sizeBytes" :label="t('resourceDetail.fileSize')" width="130" />
        <el-table-column prop="availability" :label="t('resourceDetail.availability')" width="160" />
        <el-table-column :label="t('resourceDetail.actions')" width="100" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openAttachmentDetail(scope.row)">
              {{ t('attachmentManagement.details') }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty :description="t('resourceDetail.attachmentsEmpty')" /></template>
      </el-table>

      <el-divider content-position="left">{{ t("resourceDetail.tags") }}</el-divider>
      <div class="mb-2 flex flex-wrap items-center gap-2">
        <el-tag v-for="tag in tags" :key="tag.id" :color="tag.color ?? undefined" closable @close="removeTag(tag)">
          {{ tag.name }}
        </el-tag>
        <el-input v-model="tagInput" :placeholder="t('resourceDetail.tagPlaceholder')" style="width: 180px" @keyup.enter="addTag" />
        <el-button :disabled="!tagInput.trim()" @click="addTag">{{ t("resourceDetail.tagAdd") }}</el-button>
      </div>

      <el-divider content-position="left">{{ t("resourceDetail.titles") }}</el-divider>
      <el-table :data="resource.titles" row-key="id" border>
        <el-table-column prop="locale" :label="t('resourceDetail.locale')" width="140" />
        <el-table-column prop="value" :label="t('resourceDetail.value')" min-width="220" />
        <el-table-column :label="t('resourceDetail.primary')" width="100">
          <template #default="scope">{{ scope.row.primary ? t("resourceDetail.yes") : t("resourceDetail.no") }}</template>
        </el-table-column>
        <el-table-column prop="kind" :label="t('resourceDetail.kind')" width="120" />
        <template #empty><el-empty :description="t('resourceDetail.titlesEmpty')" /></template>
      </el-table>

      <el-divider content-position="left">{{ t("resourceDetail.identities") }}</el-divider>
      <el-table :data="resource.externalIdentities" row-key="id" border>
        <el-table-column prop="provider" label="Provider" width="180" />
        <el-table-column prop="type" :label="t('resourceDetail.externalType')" width="180" />
        <el-table-column prop="value" :label="t('resourceDetail.externalValue')" min-width="200" />
        <template #empty><el-empty :description="t('resourceDetail.identitiesEmpty')" /></template>
      </el-table>

      <el-divider content-position="left">{{ t("resourceDetail.metadata") }}</el-divider>
      <div class="mb-2 flex justify-end">
        <el-button @click="openMetadata()">{{ t("resourceDetail.metadataSet") }}</el-button>
      </div>
      <el-table :data="metadata" row-key="id" border>
        <el-table-column prop="fieldKey" :label="t('resourceDetail.fieldKey')" min-width="180" />
        <el-table-column prop="value" :label="t('resourceDetail.value')" min-width="200" />
        <el-table-column prop="source" :label="t('resourceDetail.source')" width="140" />
        <el-table-column :label="t('resourceDetail.manuallyLocked')" width="130">
          <template #default="scope">{{ scope.row.manuallyLocked ? t("resourceDetail.yes") : t("resourceDetail.no") }}</template>
        </el-table-column>
        <el-table-column :label="t('resourceDetail.actions')" width="170" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openMetadata(scope.row)">{{ t("resourceDetail.edit") }}</el-button>
            <el-button link type="primary" @click="restoreMetadata(scope.row)">{{ t("resourceDetail.metadataRestore") }}</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty :description="t('resourceDetail.metadataEmpty')" /></template>
      </el-table>

      <el-divider content-position="left">{{ t("resourceDetail.relations") }}</el-divider>
      <el-table :data="relations" row-key="id" border>
        <el-table-column prop="targetResourceId" :label="t('resourceDetail.target')" min-width="260" />
        <el-table-column prop="type" :label="t('resourceDetail.relationType')" width="180" />
        <el-table-column prop="position" :label="t('resourceDetail.position')" width="100" />
        <template #empty><el-empty :description="t('resourceDetail.relationsEmpty')" /></template>
      </el-table>
    </template>

    <el-dialog v-model="editVisible" :title="t('resourceDetail.editTitle')" width="560px">
      <el-form :model="editForm" label-width="120px">
        <el-form-item :label="t('resourceDetail.primaryTitle')">
          <el-input v-model="editForm.primary_title" maxlength="512" />
        </el-form-item>
        <el-form-item :label="t('resourceDetail.summary')">
          <el-input v-model="editForm.summary" type="textarea" :rows="4" maxlength="4000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button type="primary" :loading="mutating" @click="submitEdit">{{ t("buttons.pureConfirm") }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="metadataVisible" :title="t('resourceDetail.metadataSetTitle')" width="560px">
      <el-form :model="metadataForm" label-width="120px">
        <el-form-item :label="t('resourceDetail.fieldKey')" required>
          <el-input v-model="metadataForm.fieldKey" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('resourceDetail.value')" required>
          <el-input v-model="metadataForm.value" type="textarea" :rows="3" maxlength="10000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metadataVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button type="primary" :loading="mutating" @click="submitMetadata">{{ t("buttons.pureConfirm") }}</el-button>
      </template>
    </el-dialog>
  </PageCard>
</template>
