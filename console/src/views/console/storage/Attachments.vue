<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import PageCard from "@/views/console/PageCard.vue";
import {
  listAccessibleAttachments,
  listAllManagedAttachments,
  type Attachment
} from "@/api/attachment";
import { hasPerms } from "@/utils/auth";
import { getHttpErrorMessage } from "@/utils/http";

const { t } = useI18n();
const loading = ref(false);
const attachments = ref<Attachment[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(20);
const query = ref("");
const canManageAll = computed(() => hasPerms("storage.attachment.manage"));
const filteredAttachments = computed(() => {
  if (canManageAll.value) return attachments.value;
  const value = query.value.trim().toLowerCase();
  if (!value) return attachments.value;
  return attachments.value.filter(item =>
    item.fileName.toLowerCase().includes(value)
      || item.id.toLowerCase().includes(value)
      || item.resourceId.toLowerCase().includes(value)
  );
});

const load = async () => {
  loading.value = true;
  try {
    if (canManageAll.value) {
      const result = await listAllManagedAttachments({ page: page.value - 1, size: size.value,
        ...(query.value.trim() ? { query: query.value.trim() } : {}) });
      attachments.value = result.items.map(item => ({
        id: item.id,
        resourceId: item.resource_id,
        fileName: item.file_name,
        kind: item.kind,
        sha256: item.sha256,
        sizeBytes: item.size_bytes,
        mediaType: item.media_type,
        availability: item.availability
      }));
      total.value = result.total;
    } else {
      const result = await listAccessibleAttachments({ page: page.value - 1, size: size.value });
      attachments.value = result.items;
      total.value = result.total;
    }
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("attachmentManagement.loadFailed")));
  } finally {
    loading.value = false;
  }
};

const search = () => {
  page.value = 1;
  void load();
};

onMounted(load);
</script>

<template>
  <PageCard>
    <div class="mt-6 flex flex-wrap items-center justify-between gap-3">
      <el-alert
        class="min-w-[280px] flex-1"
        :title="canManageAll ? t('attachmentManagement.allUsersHint') : t('attachmentManagement.accessibleHint')"
        :type="canManageAll ? 'success' : 'info'"
        :closable="false"
        show-icon
      />
      <el-input
        v-model="query"
        clearable
        :placeholder="t(canManageAll ? 'attachmentManagement.searchPlaceholder' : 'attachmentManagement.searchCurrentPagePlaceholder')"
        class="!w-full sm:!w-72"
        @keyup.enter="search"
        @clear="search"
      >
        <template #append>
          <el-button @click="search">{{ t('attachmentManagement.search') }}</el-button>
        </template>
      </el-input>
    </div>

    <el-table v-loading="loading" :data="filteredAttachments" class="mt-4" row-key="id">
      <el-table-column prop="fileName" :label="t('attachmentManagement.fileName')" min-width="200" show-overflow-tooltip />
      <el-table-column prop="kind" :label="t('attachmentManagement.kind')" width="130" />
      <el-table-column prop="sizeBytes" :label="t('attachmentManagement.size')" width="130">
        <template #default="scope">{{ t('attachmentManagement.bytes', { value: scope.row.sizeBytes.toLocaleString() }) }}</template>
      </el-table-column>
      <el-table-column prop="mediaType" :label="t('attachmentManagement.mediaType')" min-width="170" show-overflow-tooltip />
      <el-table-column prop="availability" :label="t('attachmentManagement.availability')" width="160" />
      <el-table-column prop="id" :label="t('attachmentManagement.attachmentId')" min-width="230" show-overflow-tooltip />
      <el-table-column prop="resourceId" :label="t('attachmentManagement.resourceId')" min-width="230" show-overflow-tooltip />
    </el-table>

    <el-empty v-if="!loading && attachments.length === 0" :description="t('attachmentManagement.empty')" />
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      class="mt-4 justify-end"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="load"
      @size-change="search"
    />
  </PageCard>
</template>
