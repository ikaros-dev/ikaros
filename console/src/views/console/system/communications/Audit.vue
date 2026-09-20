<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import { getAuditEvent, listAuditEvents, type AuditEvent } from "@/api/audit";
import { getHttpErrorMessage } from "@/utils/http";
import PageCard from "@/views/console/PageCard.vue";

const { t, locale } = useI18n();
const loading = ref(false);
const events = ref<AuditEvent[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const detailVisible = ref(false);
const detailLoading = ref(false);
const selectedEvent = ref<AuditEvent | null>(null);
const form = reactive({
  actorId: "",
  requestId: "",
  timeRange: [] as string[]
});

const formatDate = (value: string | null) => {
  if (!value) return t("auditLog.empty");
  return new Intl.DateTimeFormat(locale.value === "zh" ? "zh-CN" : "en-US", {
    dateStyle: "medium",
    timeStyle: "medium"
  }).format(new Date(value));
};

const loadEvents = async () => {
  loading.value = true;
  try {
    const [from, to] = form.timeRange;
    const result = await listAuditEvents({
      actor_id: form.actorId.trim() || undefined,
      request_id: form.requestId.trim() || undefined,
      from: from || undefined,
      to: to || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    });
    events.value = result.items;
    total.value = result.total;
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("auditLog.loadFailed")));
  } finally {
    loading.value = false;
  }
};

const search = () => {
  currentPage.value = 1;
  void loadEvents();
};

const reset = () => {
  form.actorId = "";
  form.requestId = "";
  form.timeRange = [];
  search();
};

const openDetail = async (event: AuditEvent) => {
  selectedEvent.value = event;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    selectedEvent.value = await getAuditEvent(event.id);
  } catch (error) {
    detailVisible.value = false;
    ElMessage.error(getHttpErrorMessage(error, t("auditLog.detailFailed")));
  } finally {
    detailLoading.value = false;
  }
};

onMounted(loadEvents);
</script>

<template>
  <PageCard>
    <el-form :inline="true" class="mt-6" @submit.prevent="search">
      <el-form-item :label="t('auditLog.actorId')">
        <el-input
          v-model="form.actorId"
          clearable
          :placeholder="t('auditLog.actorIdPlaceholder')"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item :label="t('auditLog.requestId')">
        <el-input
          v-model="form.requestId"
          clearable
          :placeholder="t('auditLog.requestIdPlaceholder')"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item :label="t('auditLog.timeRange')">
        <el-date-picker
          v-model="form.timeRange"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ssZ"
          :start-placeholder="t('auditLog.startTime')"
          :end-placeholder="t('auditLog.endTime')"
          :range-separator="t('auditLog.to')"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">{{
          t("auditLog.search")
        }}</el-button>
        <el-button @click="reset">{{ t("auditLog.reset") }}</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4 flex items-center justify-between">
      <span class="text-sm text-[var(--el-text-color-secondary)]">{{
        t("auditLog.resultHint")
      }}</span>
      <el-button :loading="loading" @click="loadEvents">{{
        t("auditLog.refresh")
      }}</el-button>
    </div>

    <el-table v-loading="loading" :data="events" border>
      <el-table-column
        prop="occurredAt"
        :label="t('auditLog.occurredAt')"
        min-width="190"
      >
        <template #default="{ row }">{{ formatDate(row.occurredAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('auditLog.actor')" min-width="190">
        <template #default="{ row }">
          <div>{{ row.actorType }}</div>
          <div class="text-xs text-[var(--el-text-color-secondary)]">
            {{ row.actorId || t("auditLog.systemActor") }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="action"
        :label="t('auditLog.action')"
        min-width="190"
      />
      <el-table-column :label="t('auditLog.target')" min-width="190">
        <template #default="{ row }">
          <div>{{ row.targetType }}</div>
          <div class="text-xs text-[var(--el-text-color-secondary)]">
            {{ row.targetId || t("auditLog.empty") }}
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('auditLog.result')" width="110">
        <template #default="{ row }"
          ><el-tag>{{ row.result }}</el-tag></template
        >
      </el-table-column>
      <el-table-column :label="t('auditLog.riskLevel')" width="110">
        <template #default="{ row }"
          ><el-tag type="warning">{{ row.riskLevel }}</el-tag></template
        >
      </el-table-column>
      <el-table-column fixed="right" :label="t('auditLog.actions')" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{
            t("auditLog.detail")
          }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="mt-4 flex justify-end">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadEvents"
        @size-change="
          () => {
            currentPage = 1;
            void loadEvents();
          }
        "
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      :title="t('auditLog.detailTitle')"
      size="520px"
    >
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="selectedEvent">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="t('auditLog.occurredAt')">{{
            formatDate(selectedEvent.occurredAt)
          }}</el-descriptions-item>
          <el-descriptions-item :label="t('auditLog.actor')"
            >{{ selectedEvent.actorType }} /
            {{
              selectedEvent.actorId || t("auditLog.systemActor")
            }}</el-descriptions-item
          >
          <el-descriptions-item :label="t('auditLog.action')">{{
            selectedEvent.action
          }}</el-descriptions-item>
          <el-descriptions-item :label="t('auditLog.target')"
            >{{ selectedEvent.targetType }} /
            {{
              selectedEvent.targetId || t("auditLog.empty")
            }}</el-descriptions-item
          >
          <el-descriptions-item :label="t('auditLog.requestId')">{{
            selectedEvent.requestId || t("auditLog.empty")
          }}</el-descriptions-item>
          <el-descriptions-item :label="t('auditLog.correlationId')">{{
            selectedEvent.correlationId || t("auditLog.empty")
          }}</el-descriptions-item>
          <el-descriptions-item :label="t('auditLog.result')">{{
            selectedEvent.result
          }}</el-descriptions-item>
          <el-descriptions-item :label="t('auditLog.riskLevel')">{{
            selectedEvent.riskLevel
          }}</el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <div class="mb-2 font-medium">{{ t("auditLog.details") }}</div>
        <pre class="audit-details">{{
          selectedEvent.details || t("auditLog.empty")
        }}</pre>
      </template>
    </el-drawer>
  </PageCard>
</template>

<style scoped>
.audit-details {
  max-height: 360px;
  padding: 12px;
  overflow: auto;
  word-break: break-word;
  white-space: pre-wrap;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
