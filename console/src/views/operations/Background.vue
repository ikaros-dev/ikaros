<script setup lang="ts">
import { computed, ref } from "vue";
import { http } from "@/utils/http";
type Task = Record<string, any>;
const rows = ref<Task[]>([]);
const loading = ref(false);
const error = ref("");
const status = ref("");
const type = ref("");
const detail = ref<any>(false);
const submitDialog = ref(false);
const submitting = ref(false);
const submitType = ref("");
const submitPayload = ref("{}");
const idempotencyKey = ref("");
const counts = computed(() => ({
  running: rows.value.filter(
    row => String(row.status).toUpperCase() === "RUNNING"
  ).length,
  queued: rows.value.filter(
    row => String(row.status).toUpperCase() === "PENDING"
  ).length,
  failed: rows.value.filter(row =>
    ["FAILED", "TIMED_OUT"].includes(String(row.status).toUpperCase())
  ).length
}));
async function load() {
  loading.value = true;
  try {
    const result: any = await http.get("/background-tasks");
    const values = Array.isArray(result)
      ? result
      : result?.content || result?.items || [];
    rows.value = values.filter(
      (row: Task) =>
        (!status.value || String(row.status).toUpperCase() === status.value) &&
        (!type.value ||
          String(row.task_type || row.taskType || "")
            .toLowerCase()
            .includes(type.value.toLowerCase()))
    );
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "后台任务加载失败";
  } finally {
    loading.value = false;
  }
}
async function submitTask() {
  if (!submitType.value.trim()) {
    error.value = "任务类型不能为空";
    return;
  }
  let payload: Record<string, unknown>;
  try {
    payload = JSON.parse(submitPayload.value || "{}");
    if (!payload || Array.isArray(payload) || typeof payload !== "object") {
      throw new Error("payload 必须是 JSON 对象");
    }
  } catch (e: any) {
    error.value = e?.message || "任务参数必须是合法 JSON 对象";
    return;
  }
  submitting.value = true;
  error.value = "";
  try {
    await http.post("/background-tasks", {
      data: { type: submitType.value.trim(), payload },
      headers: idempotencyKey.value.trim()
        ? { "Idempotency-Key": idempotencyKey.value.trim() }
        : undefined
    });
    submitDialog.value = false;
    submitType.value = "";
    submitPayload.value = "{}";
    idempotencyKey.value = "";
    await load();
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "后台任务提交失败";
  } finally {
    submitting.value = false;
  }
}
async function openDetail(row: Task) {
  if (!row.id) return;
  detail.value = row;
  try {
    detail.value = await http.get(`/background-tasks/${row.id}`);
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "任务详情加载失败";
  }
}
function progressPercent(row: Task) {
  const progress = row?.progress || {};
  const value = Number(progress.percent ?? progress.percentage);
  if (Number.isFinite(value)) return Math.max(0, Math.min(100, value));
  const completed = Number(progress.completed ?? progress.completedCount);
  const total = Number(progress.total ?? progress.totalCount);
  if (Number.isFinite(completed) && Number.isFinite(total) && total > 0) {
    return Math.round((completed / total) * 100);
  }
  return null;
}
function progressSummary(row: Task) {
  const progress = row?.progress || {};
  const percent = progressPercent(row);
  if (percent !== null) return `${percent}%`;
  const completed = progress.completed ?? progress.completedCount;
  const total = progress.total ?? progress.totalCount;
  if (completed != null && total != null) return `${completed} / ${total}`;
  return Object.keys(progress).length ? JSON.stringify(progress) : "—";
}
async function cancelTask(row: Task) {
  if (!row.id || !["PENDING", "RUNNING"].includes(String(row.status).toUpperCase())) return;
  if (!window.confirm("确认取消这个后台任务吗？已完成的部分不会回滚。")) return;
  try {
    await http.post(`/background-tasks/${row.id}/actions/cancel`);
    await load();
    if (detail.value?.id === row.id) await openDetail(row);
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "任务取消失败";
  }
}
load();
</script>
<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6">
      <div>
        <h1 class="text-2xl font-semibold">后台任务</h1>
        <p class="mt-1 text-[var(--el-text-color-secondary)]">
          统一查看导入、备份、恢复、迁移和其他异步任务。
        </p>
      </div>
      <div class="flex gap-2">
        <el-button type="primary" @click="submitDialog = true">提交任务</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="mb-4"
    />
    <div class="grid grid-cols-1 md:grid-cols-5 gap-4 mb-5">
      <el-card shadow="never"
        ><div class="text-sm">运行中</div>
        <div class="text-2xl font-semibold mt-2">
          {{ counts.running }}
        </div></el-card
      ><el-card shadow="never"
        ><div class="text-sm">排队中</div>
        <div class="text-2xl font-semibold mt-2">
          {{ counts.queued }}
        </div></el-card
      ><el-card shadow="never"
        ><div class="text-sm">失败</div>
        <div class="text-2xl font-semibold mt-2">
          {{ counts.failed }}
        </div></el-card
      ><el-card shadow="never"
        ><div class="text-sm">已完成</div>
        <div class="text-2xl font-semibold mt-2">
          {{ rows.filter(row => row.status === "SUCCEEDED").length }}
        </div></el-card
      ><el-card shadow="never"
        ><div class="text-sm">长时间运行</div>
        <div class="text-2xl font-semibold mt-2">—</div></el-card
      >
    </div>
    <div class="flex gap-3 mb-4">
      <el-select v-model="status" clearable placeholder="状态" @change="load"
        ><el-option label="排队中" value="PENDING" /><el-option
          label="运行中"
          value="RUNNING" /><el-option
          label="成功"
          value="SUCCEEDED" /><el-option
          label="失败"
          value="FAILED" /></el-select
      ><el-input
        v-model="type"
        clearable
        placeholder="任务类型"
        class="max-w-xs"
        @keyup.enter="load"
      />
    </div>
    <el-skeleton v-if="loading" :rows="9" animated /><el-card
      v-else
      shadow="never"
      ><el-empty v-if="!rows.length" description="暂无后台任务" /><el-table
        v-else
        :data="rows"
        stripe
        ><el-table-column
          prop="id"
          label="Task ID"
          min-width="240"
        /><el-table-column label="任务类型" width="160"
          ><template #default="{ row }">{{
            row.task_type || row.taskType
          }}</template></el-table-column
        ><el-table-column
          prop="status"
          label="状态"
          width="120"
        /><el-table-column label="进度" width="220"
          ><template #default="{ row }">
            <el-progress
              v-if="progressPercent(row) !== null"
              :percentage="progressPercent(row)"
              :status="row.status === 'FAILED' ? 'exception' : undefined"
            />
            <span v-else>{{ progressSummary(row) }}</span>
          </template></el-table-column
        ><el-table-column
          prop="created_at"
          label="创建时间"
          min-width="180"
        /><el-table-column label="操作" width="150"
          ><template #default="{ row }"
            ><el-button link @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="['PENDING', 'RUNNING'].includes(String(row.status).toUpperCase())"
              link
              type="danger"
              @click="cancelTask(row)"
              >取消</el-button
            ></template
          ></el-table-column
        ></el-table
      ></el-card
    ><el-drawer v-model="detail" title="任务检查器" size="480px"
      ><el-descriptions v-if="detail" :column="1" border
        ><el-descriptions-item label="Task ID">{{
          detail.id
        }}</el-descriptions-item
        ><el-descriptions-item label="任务类型">{{
          detail.task_type || detail.taskType || "—"
        }}</el-descriptions-item
        ><el-descriptions-item label="状态">{{
          detail.status || "—"
        }}</el-descriptions-item
        ><el-descriptions-item label="进度">
          <el-progress
            v-if="progressPercent(detail) !== null"
            :percentage="progressPercent(detail)"
            :status="detail.status === 'FAILED' ? 'exception' : undefined"
          />
          <span v-else>{{ progressSummary(detail) }}</span>
        </el-descriptions-item></el-descriptions
      ><el-button class="mt-4" @click="openDetail(detail)">刷新详情</el-button
      ><el-steps direction="vertical" class="mt-5"
        ><el-step title="排队" /><el-step title="执行" /><el-step
          title="完成 / 失败" /></el-steps
    ></el-drawer>
    <el-dialog v-model="submitDialog" title="提交后台任务" width="560px">
      <el-form label-position="top">
        <el-form-item label="任务类型" required>
          <el-input v-model="submitType" placeholder="例如 resource.rebuild" />
        </el-form-item>
        <el-form-item label="任务参数 JSON">
          <el-input
            v-model="submitPayload"
            type="textarea"
            :rows="7"
            placeholder='{"resource_id":"..."}'
          />
        </el-form-item>
        <el-form-item label="幂等键（可选）">
          <el-input v-model="idempotencyKey" placeholder="重复提交时保持相同" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTask">
          提交
        </el-button>
      </template>
    </el-dialog>
  </main>
</template>
