<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { http } from "@/utils/http";

type Activity = Record<string, any>;
const route = useRoute();
const router = useRouter();
const rows = ref<Activity[]>([]);
const selected = ref<Activity | null>(null);
const attempts = ref<Activity[]>([]);
const loading = ref(false);
const detailLoading = ref(false);
const attemptsLoading = ref(false);
const error = ref("");
const query = ref("");
const type = ref("");
const status = ref("");
const advanced = ref(false);
const statusOptions = [["排队中", "PENDING"], ["运行中", "RUNNING"], ["成功", "SUCCEEDED"], ["失败", "FAILED"], ["已取消", "CANCELLED"]];

function normalizedStatus(row: Activity) { return String(row.status ?? "UNKNOWN").toUpperCase(); }
function taskType(row: Activity) { return String(row.taskType ?? row.task_type ?? "background"); }
function actionLabel(row: Activity) { const payload = row.payload || {}; return String(row.action ?? payload.action ?? payload.operation ?? "后台处理"); }
function objectLabel(row: Activity) { const payload = row.payload || {}; return String(row.resourceTitle ?? row.resourceName ?? payload.resource_title ?? payload.resource_id ?? payload.attachment_id ?? "关联对象待定"); }
function updatedAt(row: Activity) { return row.updatedAt ?? row.updated_at ?? row.createdAt ?? row.created_at ?? "—"; }
function progressPercent(row: Activity) { const progress = row.progress || {}; const percent = Number(progress.percent ?? progress.percentage); if (Number.isFinite(percent)) return Math.max(0, Math.min(100, percent)); const completed = Number(progress.completed ?? progress.completedCount); const total = Number(progress.total ?? progress.totalCount); return Number.isFinite(completed) && Number.isFinite(total) && total > 0 ? Math.round((completed / total) * 100) : null; }
function progressLabel(row: Activity) { const progress = row.progress || {}; const percent = progressPercent(row); if (percent !== null) return `${percent}%`; const completed = progress.completed ?? progress.completedCount; const total = progress.total ?? progress.totalCount; return completed != null && total != null ? `${completed} / ${total}` : "进度未知"; }
function nextStep(row: Activity) { const current = normalizedStatus(row); if (["PENDING", "RUNNING"].includes(current)) return "等待后台执行完成"; if (["FAILED", "TIMED_OUT"].includes(current)) return "检查错误摘要后重试"; if (current === "CANCELLED") return "任务已取消"; if (current === "SUCCEEDED") return "查看关联对象"; return "状态待确认"; }
function canCancel(row: Activity) { return ["PENDING", "RUNNING"].includes(normalizedStatus(row)); }
function canRetry(row: Activity) { return ["FAILED", "TIMED_OUT"].includes(normalizedStatus(row)); }

const filteredRows = computed(() => rows.value.filter(row => {
  const text = JSON.stringify({ type: taskType(row), action: actionLabel(row), object: objectLabel(row), status: row.status, payload: row.payload }).toLowerCase();
  return (!type.value || taskType(row).toLowerCase().includes(type.value.toLowerCase())) && (!query.value || text.includes(query.value.toLowerCase()));
}));

async function load() {
  loading.value = true; error.value = "";
  try { const result: any = await http.get("/background-tasks", { params: status.value ? { status: status.value } : undefined }); const values = Array.isArray(result) ? result : result?.content || result?.items || []; rows.value = Array.isArray(values) ? values : []; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "后台工作加载失败"; }
  finally { loading.value = false; }
}
async function openDetail(row: Activity) {
  if (!row.id) return; selected.value = row; advanced.value = false; detailLoading.value = true; error.value = ""; router.push(`/activity/${row.id}`);
  try { selected.value = await http.get(`/background-tasks/${row.id}`); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "后台工作详情加载失败"; } finally { detailLoading.value = false; }
}
async function loadAttempts() {
  if (!selected.value?.id || !advanced.value) return; attemptsLoading.value = true;
  try { const result: any = await http.get(`/background-tasks/${selected.value.id}/attempts`); attempts.value = Array.isArray(result) ? result : result?.items || []; } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "执行尝试加载失败"; } finally { attemptsLoading.value = false; }
}
async function cancelTask(row: Activity) {
  if (!canCancel(row) || !window.confirm("确认取消这个后台工作吗？已完成的部分不会回滚。")) return;
  try { await http.post(`/background-tasks/${row.id}/actions/cancel`); await load(); if (selected.value?.id === row.id) await openDetail(row); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "后台工作取消失败"; }
}
async function retryTask(row: Activity) {
  if (!canRetry(row) || !window.confirm("确认重试这个后台工作吗？系统会创建新的执行尝试。")) return;
  try { await http.post(`/background-tasks/${row.id}/actions/retry`); await load(); if (selected.value?.id === row.id) await openDetail(row); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "后台工作重试失败"; }
}
function closeDetail() { selected.value = null; attempts.value = []; if (route.params.activityId) router.push("/activity"); }
onMounted(async () => { await load(); const id = String(route.params.activityId || ""); const row = rows.value.find(item => String(item.id) === id); if (row) await openDetail(row); });
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex flex-wrap items-start justify-between gap-4 mb-6"><div><h1 class="text-2xl font-semibold">Activity</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">统一观察导入、存储、元数据、备份、下载 / 缓存、AI 和自动化等后台工作。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never"><div class="flex flex-wrap gap-3 mb-4"><el-input v-model="query" clearable class="max-w-xs" placeholder="按动作或关联对象筛选" /><el-input v-model="type" clearable class="max-w-xs" placeholder="业务类型，如 import / backup" /><el-select v-model="status" clearable placeholder="状态" class="w-36" @change="load"><el-option v-for="option in statusOptions" :key="option[1]" :label="option[0]" :value="option[1]" /></el-select></div>
      <el-skeleton v-if="loading" :rows="8" animated /><el-empty v-else-if="!filteredRows.length" description="暂无符合条件的后台工作" /><el-table v-else :data="filteredRows" stripe><el-table-column label="业务类型" min-width="150"><template #default="{ row }">{{ taskType(row) }}</template></el-table-column><el-table-column label="动作" min-width="180"><template #default="{ row }">{{ actionLabel(row) }}</template></el-table-column><el-table-column label="关联对象" min-width="220"><template #default="{ row }">{{ objectLabel(row) }}</template></el-table-column><el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="normalizedStatus(row) === 'FAILED' ? 'danger' : normalizedStatus(row) === 'SUCCEEDED' ? 'success' : normalizedStatus(row) === 'RUNNING' ? 'primary' : 'info'">{{ normalizedStatus(row) }}</el-tag></template></el-table-column><el-table-column label="进度" width="180"><template #default="{ row }"><el-progress v-if="progressPercent(row) !== null" :percentage="progressPercent(row)" :status="normalizedStatus(row) === 'FAILED' ? 'exception' : undefined" /><span v-else>{{ progressLabel(row) }}</span></template></el-table-column><el-table-column label="更新时间" min-width="180"><template #default="{ row }">{{ updatedAt(row) }}</template></el-table-column><el-table-column label="下一步" min-width="170"><template #default="{ row }">{{ nextStep(row) }}</template></el-table-column><el-table-column label="操作" width="170" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">详情</el-button><el-button v-if="canCancel(row)" link type="danger" @click="cancelTask(row)">取消</el-button><el-button v-if="canRetry(row)" link type="warning" @click="retryTask(row)">重试</el-button></template></el-table-column></el-table>
    </el-card>
    <el-drawer :model-value="Boolean(selected)" title="Activity Detail" size="520px" @close="closeDetail"><el-skeleton v-if="detailLoading" :rows="8" animated /><template v-else-if="selected"><el-descriptions :column="1" border><el-descriptions-item label="业务类型">{{ taskType(selected) }}</el-descriptions-item><el-descriptions-item label="动作">{{ actionLabel(selected) }}</el-descriptions-item><el-descriptions-item label="关联对象">{{ objectLabel(selected) }}</el-descriptions-item><el-descriptions-item label="状态">{{ normalizedStatus(selected) }}</el-descriptions-item><el-descriptions-item label="进度"><el-progress v-if="progressPercent(selected) !== null" :percentage="progressPercent(selected)" /><span v-else>{{ progressLabel(selected) }}</span></el-descriptions-item><el-descriptions-item label="更新时间">{{ updatedAt(selected) }}</el-descriptions-item><el-descriptions-item label="下一步">{{ nextStep(selected) }}</el-descriptions-item><el-descriptions-item v-if="selected.result?.error_summary || selected.result?.errorMessage" label="错误摘要">{{ selected.result.error_summary || selected.result.errorMessage }}</el-descriptions-item></el-descriptions><div class="flex gap-2 mt-5"><el-button v-if="canCancel(selected)" type="danger" plain @click="cancelTask(selected)">取消工作</el-button><el-button v-if="canRetry(selected)" type="warning" plain @click="retryTask(selected)">重试工作</el-button><el-button @click="advanced = !advanced; loadAttempts()">{{ advanced ? "收起 Advanced" : "查看 Advanced" }}</el-button></div><el-card v-if="advanced" shadow="never" class="mt-5"><template #header>Advanced</template><el-descriptions :column="1" border><el-descriptions-item label="Task ID">{{ selected.id }}</el-descriptions-item><el-descriptions-item label="Attempt">{{ selected.attempt ?? "—" }}</el-descriptions-item><el-descriptions-item label="Worker">{{ selected.leaseOwner ?? selected.lease_owner ?? "—" }}</el-descriptions-item><el-descriptions-item label="Lease 到期">{{ selected.leaseExpiresAt ?? selected.lease_expires_at ?? "—" }}</el-descriptions-item><el-descriptions-item label="Raw Payload"><pre class="whitespace-pre-wrap break-all text-xs">{{ JSON.stringify(selected.payload || {}, null, 2) }}</pre></el-descriptions-item></el-descriptions><el-divider /><div class="font-medium mb-3">执行尝试历史</div><el-skeleton v-if="attemptsLoading" :rows="3" animated /><el-empty v-else-if="!attempts.length" description="暂无执行尝试" /><el-table v-else :data="attempts" size="small" stripe><el-table-column prop="attemptNo" label="#" width="55" /><el-table-column prop="status" label="状态" width="110" /><el-table-column prop="claimedBy" label="Worker" min-width="150" /><el-table-column prop="leaseExpiresAt" label="Lease 到期" min-width="180" /><el-table-column prop="startedAt" label="开始" min-width="180" /><el-table-column prop="endedAt" label="结束" min-width="180" /><el-table-column prop="errorSummary" label="错误摘要" min-width="180" /></el-table></el-card></template></el-drawer>
  </main>
</template>
