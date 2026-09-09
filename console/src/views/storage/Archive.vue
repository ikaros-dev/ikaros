<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, any>;
const tab = ref("restore");
const restores = ref<Row[]>([]);
const candidates = ref<Row[]>([]);
const loading = ref(false);
const submitting = ref(false);
const error = ref("");
const attachmentId = ref("");
const resourceId = ref("");
const targetTier = ref("HOT");
const budget = ref<Row | null>(null);

async function load() {
  loading.value = true; error.value = "";
  try {
    const [restore, gc, budgetResult] = await Promise.all([http.get("/restore-requests"), http.get("/storage/gc/candidates"), http.get("/admin/restore-budget-policy")]);
    const restorePage: any = restore;
    restores.value = Array.isArray(restorePage) ? restorePage : (restorePage?.items || []);
    candidates.value = Array.isArray(gc) ? gc : [];
    budget.value = budgetResult;
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "归档与恢复数据加载失败"; }
  finally { loading.value = false; }
}
async function restoreResource() {
  if (!resourceId.value.trim()) { error.value = "请输入 Resource ID"; return; }
  submitting.value = true; error.value = "";
  try {
    const resource: any = await http.get(`/resources/${resourceId.value.trim()}`);
    await http.post(`/resources/${resource.id}/actions/restore`, { headers: { "If-Match": `"${resource.version ?? 0}"` } });
    resourceId.value = ""; await load();
  } catch (e: any) { error.value = e?.response?.status === 409 ? "资源版本已变化，请重新读取后重试。" : e?.response?.data?.detail || e?.message || "资源恢复失败"; }
  finally { submitting.value = false; }
}
async function purgeResource() {
  if (!resourceId.value.trim() || !window.confirm("永久删除不可撤销，确认继续吗？")) return;
  if (!window.confirm("再次确认：仅符合回收站和保留规则的 Resource 才会被永久删除。")) return;
  submitting.value = true; error.value = "";
  try { const resource: any = await http.get(`/resources/${resourceId.value.trim()}`); await http.post(`/resources/${resource.id}/actions/purge`, { headers: { "If-Match": `"${resource.version ?? 0}"`, "X-Ikaros-Confirmation": "PURGE" } }); resourceId.value = ""; await load(); }
  catch (e: any) { error.value = e?.response?.status === 409 ? "资源不满足永久删除条件或版本已变化。" : e?.response?.data?.detail || e?.message || "永久删除失败"; }
  finally { submitting.value = false; }
}
async function requestRestore() {
  if (!attachmentId.value.trim()) { error.value = "请输入 Attachment ID"; return; }
  submitting.value = true; error.value = "";
  try { await http.post(`/attachments/${attachmentId.value.trim()}/restore-requests`, { headers: { "Idempotency-Key": crypto.randomUUID() }, data: { providerRestoreClass: targetTier.value } }); attachmentId.value = ""; await load(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "恢复请求提交失败"; }
  finally { submitting.value = false; }
}
async function retryRestore(id: string) { try { await http.post(`/restore-requests/${id}/actions/retry`, { headers: { "Idempotency-Key": crypto.randomUUID() } }); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "恢复重试失败"; } }
async function cancelRestore(id: string) { if (!window.confirm("确认取消这个恢复请求吗？")) return; try { await http.request("delete", `/restore-requests/${id}`); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "恢复请求取消失败"; } }
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">归档、恢复与回收站</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">恢复是异步后台操作；Blob 清理候选仅供人工审核，不代表已经删除。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-card v-if="budget" shadow="never" class="mb-4"><template #header>恢复预算</template><div class="grid grid-cols-2 gap-4 md:grid-cols-4"><div><div class="text-xs text-[var(--el-text-color-secondary)]">单请求最大字节</div><div class="font-semibold">{{ budget.maxBytesPerRequest }}</div></div><div><div class="text-xs text-[var(--el-text-color-secondary)]">单请求最大条目</div><div class="font-semibold">{{ budget.maxItemsPerRequest }}</div></div><div><div class="text-xs text-[var(--el-text-color-secondary)]">并发最大字节</div><div class="font-semibold">{{ budget.maxConcurrentBytes }}</div></div><div><div class="text-xs text-[var(--el-text-color-secondary)]">超预算策略</div><div class="font-semibold">{{ budget.overBudgetAction }}</div></div></div></el-card>
    <el-card shadow="never"><el-tabs v-model="tab">
      <el-tab-pane label="已归档资源" name="archived"><el-empty description="归档资源列表接口待补齐" /></el-tab-pane>
      <el-tab-pane label="恢复队列" name="restore"><div class="flex flex-wrap gap-2 mb-4"><el-input v-model="resourceId" placeholder="Resource ID" clearable class="w-56" /><el-button type="warning" :loading="submitting" @click="restoreResource">恢复 Resource</el-button><el-input v-model="attachmentId" placeholder="Attachment ID" clearable class="w-56" /><el-select v-model="targetTier" class="w-36"><el-option label="HOT" value="HOT" /><el-option label="WARM" value="WARM" /><el-option label="COLD" value="COLD" /></el-select><el-button type="primary" :loading="submitting" @click="requestRestore">发起附件恢复</el-button></div><el-alert title="Resource 恢复会先读取当前版本，再带 If-Match 提交；恢复 Attachment 为异步后台操作。" type="info" show-icon :closable="false" class="mb-4" /><el-skeleton v-if="loading" :rows="5" animated /><el-empty v-else-if="!restores.length" description="暂无恢复任务" /><el-table v-else :data="restores" stripe><el-table-column prop="id" label="任务" min-width="220" /><el-table-column prop="scopeType" label="范围" width="130" /><el-table-column prop="scopeId" label="目标 ID" min-width="220" /><el-table-column prop="totalBytes" label="总大小" width="120" /><el-table-column prop="readyItems" label="已完成" width="100" /><el-table-column prop="status" label="当前阶段" width="130" /><el-table-column prop="budgetDecision" label="预算判断" width="130" /><el-table-column prop="createdAt" label="开始时间" min-width="180" /><el-table-column label="操作" width="150"><template #default="{ row }"><el-button v-if="['FAILED','PARTIAL'].includes(row.status)" link type="warning" @click="retryRestore(row.id)">重试</el-button><el-button v-if="['PENDING','ACTIVE'].includes(row.status)" link type="danger" @click="cancelRestore(row.id)">取消</el-button></template></el-table-column></el-table></el-tab-pane>
      <el-tab-pane label="回收站 / GC 候选" name="trash"><p class="text-sm text-[var(--el-text-color-secondary)] mb-4">候选必须没有有效 Attachment 引用并超过保留期。永久删除需要明确确认，并由后端再次校验保留规则。</p><div class="flex flex-wrap gap-2 mb-4"><el-input v-model="resourceId" placeholder="Resource ID" clearable class="w-56" /><el-button type="danger" :loading="submitting" @click="purgeResource">永久删除 Resource</el-button></div><el-skeleton v-if="loading" :rows="5" animated /><el-empty v-else-if="!candidates.length" description="暂无 Blob 清理候选" /><el-table v-else :data="candidates" stripe><el-table-column prop="blobId" label="Blob ID" min-width="240" /><el-table-column prop="sha256" label="SHA-256" min-width="260" /><el-table-column prop="sizeBytes" label="大小（Bytes）" width="150" /><el-table-column prop="createdAt" label="创建时间" min-width="180" /><el-table-column prop="eligibleAt" label="可清理时间" min-width="180" /></el-table></el-tab-pane>
    </el-tabs></el-card>
  </main>
</template>
