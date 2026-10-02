<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import PageCard from "@/views/console/PageCard.vue";
import {
  getAttachment,
  getAttachmentPreviewUrl,
  listAllManagedAttachments,
  type Attachment,
  type AttachmentPreviewUrl
} from "@/api/attachment";
import { hasPerms } from "@/utils/auth";
import { getHttpErrorMessage } from "@/utils/http";
import { useMultiTagsStoreHook } from "@/store/modules/multiTags";

const { t } = useI18n();
const route = useRoute();
const attachmentId = computed(() => String(route.params.attachmentId ?? ""));
const loading = ref(false);
const attachment = ref<Attachment | null>(null);
const previewLoading = ref(false);
const previewError = ref("");
const preview = ref<AttachmentPreviewUrl | null>(null);
const selectedProviderKey = ref("");
let previewRequestId = 0;
const isImage = computed(
  () => attachment.value?.mediaType.startsWith("image/") ?? false
);
const isVideo = computed(
  () => attachment.value?.mediaType.startsWith("video/") ?? false
);
const isAudio = computed(
  () => attachment.value?.mediaType.startsWith("audio/") ?? false
);
const isPdf = computed(() => attachment.value?.mediaType === "application/pdf");
let attachmentRequestId = 0;

const loadPreview = async (providerKey?: string) => {
  const requestId = ++previewRequestId;
  previewLoading.value = true;
  previewError.value = "";
  preview.value = null;
  try {
    const result = await getAttachmentPreviewUrl(
      attachmentId.value,
      providerKey
    );
    if (requestId !== previewRequestId) return;
    preview.value = result;
    selectedProviderKey.value =
      result.selectedProvider?.deliveryProviderKey ?? "";
  } catch (error) {
    if (requestId === previewRequestId) {
      previewError.value = getHttpErrorMessage(
        error,
        t("attachmentManagement.previewFailed")
      );
    }
  } finally {
    if (requestId === previewRequestId) previewLoading.value = false;
  }
};

const load = async () => {
  const requestId = ++attachmentRequestId;
  previewRequestId++;
  previewLoading.value = false;
  loading.value = true;
  attachment.value = null;
  preview.value = null;
  previewError.value = "";
  const id = attachmentId.value;
  try {
    if (hasPerms("storage.attachment.manage")) {
      const result = await listAllManagedAttachments({
        page: 0,
        size: 100,
        query: id
      });
      if (requestId !== attachmentRequestId) return;
      const managed = result.items.find(item => item.id === id);
      if (!managed) throw new Error(t("attachmentManagement.notFound"));
      attachment.value = {
        id: managed.id,
        resourceId: managed.resource_id,
        fileName: managed.file_name,
        kind: managed.kind,
        sha256: managed.sha256,
        sizeBytes: managed.size_bytes,
        mediaType: managed.media_type,
        availability: managed.availability
      };
    } else {
      const result = await getAttachment(id);
      if (requestId !== attachmentRequestId) return;
      attachment.value = result;
    }
    if (requestId === attachmentRequestId && id === attachmentId.value)
      void loadPreview();
  } catch (error) {
    if (requestId === attachmentRequestId) {
      ElMessage.error(
        getHttpErrorMessage(error, t("attachmentManagement.detailFailed"))
      );
    }
  } finally {
    if (requestId === attachmentRequestId) loading.value = false;
  }
};

watch(
  attachmentId,
  id => {
    if (!id) return;
    const tags = useMultiTagsStoreHook();
    const routeName = String(route.name ?? "StorageAttachmentDetail");
    tags.multiTags = tags.multiTags.filter(
      tag =>
        tag.name !== routeName ||
        tag.params?.attachmentId !== undefined ||
        tag.path === route.path
    );
    tags.handleTags("push", {
      path: route.path,
      name: routeName,
      params: { ...route.params },
      meta: {
        ...route.meta,
        tabTitle: `${t("menus.attachmentManagement")}-${id.slice(0, 8)}`
      }
    });
    void load();
  },
  { immediate: true }
);
</script>

<template>
  <PageCard>
    <div class="mt-6">
      <el-skeleton v-if="loading" :rows="8" animated />
      <el-empty
        v-else-if="!attachment"
        :description="t('attachmentManagement.notFound')"
      />
      <template v-else>
        <el-card shadow="never" class="mb-5">
          <template #header>
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div class="text-lg font-semibold">
                  {{ t("attachmentManagement.information") }}
                </div>
                <div
                  class="mt-1 break-all text-sm text-[var(--el-text-color-secondary)]"
                >
                  {{ attachment.fileName }}
                </div>
              </div>
              <el-tag
                :type="
                  attachment.availability === 'READY' ? 'success' : 'warning'
                "
              >
                {{ attachment.availability }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item
              :label="t('attachmentManagement.attachmentId')"
            >
              <span class="break-all">{{ attachment.id }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('attachmentManagement.resourceId')">
              <span class="break-all">{{ attachment.resourceId }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('attachmentManagement.kind')">{{
              attachment.kind
            }}</el-descriptions-item>
            <el-descriptions-item :label="t('attachmentManagement.size')">
              {{
                t("attachmentManagement.bytes", {
                  value: attachment.sizeBytes.toLocaleString()
                })
              }}
            </el-descriptions-item>
            <el-descriptions-item
              :label="t('attachmentManagement.mediaType')"
              >{{ attachment.mediaType }}</el-descriptions-item
            >
            <el-descriptions-item :label="t('attachmentManagement.sha256')">
              <span class="break-all font-mono">{{ attachment.sha256 }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div class="text-lg font-semibold">
                {{ t("attachmentManagement.preview") }}
              </div>
              <el-select
                v-if="preview && preview.providers.length > 1"
                v-model="selectedProviderKey"
                class="!w-64"
                @change="loadPreview(String($event))"
              >
                <el-option
                  v-for="provider in preview.providers"
                  :key="provider.deliveryProviderKey"
                  :label="provider.displayName"
                  :value="provider.deliveryProviderKey"
                />
              </el-select>
            </div>
          </template>
          <div
            v-loading="previewLoading"
            class="flex min-h-64 items-center justify-center"
          >
            <el-alert
              v-if="previewError"
              :title="previewError"
              type="warning"
              :closable="false"
              show-icon
            />
            <el-image
              v-else-if="preview && isImage"
              :src="preview.url"
              :alt="attachment.fileName"
              fit="contain"
              class="max-h-[640px] w-full"
            />
            <video
              v-else-if="preview && isVideo"
              :src="preview.url"
              controls
              class="max-h-[640px] w-full"
            />
            <audio
              v-else-if="preview && isAudio"
              :src="preview.url"
              controls
              class="w-full"
            />
            <iframe
              v-else-if="preview && isPdf"
              :src="preview.url"
              :title="attachment.fileName"
              class="h-[640px] w-full border-0"
            />
            <el-empty
              v-else-if="preview"
              :description="t('attachmentManagement.previewUnsupported')"
            />
            <el-empty
              v-else-if="!previewLoading"
              :description="t('attachmentManagement.previewUnavailable')"
            />
          </div>
        </el-card>
      </template>
    </div>
  </PageCard>
</template>
