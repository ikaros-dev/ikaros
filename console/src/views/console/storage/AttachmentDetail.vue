<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import PageCard from "@/views/console/PageCard.vue";
import { getAttachment, listAllManagedAttachments, type Attachment } from "@/api/attachment";
import { hasPerms } from "@/utils/auth";
import { getHttpErrorMessage } from "@/utils/http";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const attachmentId = computed(() => String(route.params.attachmentId ?? ""));
const loading = ref(false);
const attachment = ref<Attachment | null>(null);

const load = async () => {
  loading.value = true;
  attachment.value = null;
  try {
    if (hasPerms("storage.attachment.manage")) {
      const result = await listAllManagedAttachments({ page: 0, size: 100, query: attachmentId.value });
      const managed = result.items.find(item => item.id === attachmentId.value);
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
      attachment.value = await getAttachment(attachmentId.value);
    }
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("attachmentManagement.detailFailed")));
  } finally {
    loading.value = false;
  }
};

watch(attachmentId, () => void load(), { immediate: true });
</script>

<template>
  <PageCard>
    <div class="mt-6">
      <el-button class="mb-4" @click="router.push('/storage/attachments')">
        {{ t("attachmentManagement.backToList") }}
      </el-button>

      <el-skeleton v-if="loading" :rows="8" animated />
      <el-empty v-else-if="!attachment" :description="t('attachmentManagement.notFound')" />
      <template v-else>
        <el-card shadow="never" class="mb-5">
          <template #header>
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div class="text-lg font-semibold">{{ t("attachmentManagement.basicInfo") }}</div>
                <div class="mt-1 break-all text-sm text-[var(--el-text-color-secondary)]">{{ attachment.fileName }}</div>
              </div>
              <el-tag :type="attachment.availability === 'READY' ? 'success' : 'warning'">
                {{ attachment.availability }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="t('attachmentManagement.kind')">{{ attachment.kind }}</el-descriptions-item>
            <el-descriptions-item :label="t('attachmentManagement.size')">
              {{ t('attachmentManagement.bytes', { value: attachment.sizeBytes.toLocaleString() }) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('attachmentManagement.mediaType')" :span="2">
              {{ attachment.mediaType }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="text-lg font-semibold">{{ t("attachmentManagement.otherInfo") }}</div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="t('attachmentManagement.attachmentId')">
              <span class="break-all">{{ attachment.id }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('attachmentManagement.resourceId')">
              <span class="break-all">{{ attachment.resourceId }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('attachmentManagement.sha256')">
              <span class="break-all font-mono">{{ attachment.sha256 }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </template>
    </div>
  </PageCard>
</template>
