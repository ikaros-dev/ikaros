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

async function load() {
  loading.value = true; error.value = "";
  try {
    const [restore, gc] = await Promise.all([http.get("/storage/restore-requests"), http.get("/storage/gc/candidates")]);
    restores.value = Array.isArray(restore) ? restore : [];
    candidates.value = Array.isArray(gc) ? gc : [];
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
  try { await http.post("/storage/restore-requests/attachments", { data: { attachmentId: attachmentId.value.trim(), targetTier: targetTier.value } }); attachmentId.value = ""; await load(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "恢复请求提交失败"; }
  finally { submitting.value = false; }
}
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">归档、恢复与回收站</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">恢复是异步后台操作；Blob 清理候选仅供人工审核，不代表已经删除。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never"><el-tabs v-model="tab">
      <el-tab-pane label="已归档资源" name="archived"><el-empty description="归档资源列表接口待补齐" /></el-tab-pane>
      <el-tab-pane label="恢复队列" name="restore"><div class="flex flex-wrap gap-2 mb-4"><el-input v-model="resourceId" placeholder="Resource ID" clearable class="w-56" /><el-button type="warning" :loading="submitting" @click="restoreResource">恢复 Resource</el-button><el-input v-model="attachmentId" placeholder="Attachment ID" clearable class="w-56" /><el-select v-model="targetTier" class="w-36"><el-option label="HOT" value="HOT" /><el-option label="WARM" value="WARM" /><el-option label="COLD" value="COLD" /></el-select><el-button type="primary" :loading="submitting" @click="requestRestore">发起附件恢复</el-button></div><el-alert title="Resource 恢复会先读取当前版本，再带 If-Match 提交；恢复 Attachment 为异步后台操作。" type="info" show-icon :closable="false" class="mb-4" /><el-skeleton v-if="loading" :rows="5" animated /><el-empty v-else-if="!restores.length" description="暂无恢复任务" /><el-table v-else :data="restores" stripe><el-table-column prop="id" label="任务" min-width="220" /><el-table-column prop="scope" label="目标资源 / Blob" width="150" /><el-table-column prop="targetTier" label="目标位置" width="120" /><el-table-column prop="totalItems" label="目标数量" width="100" /><el-table-column prop="completedItems" label="已完成" width="100" /><el-table-column prop="status" label="当前阶段" width="150" /><el-table-column prop="createdAt" label="开始时间" min-width="180" /></el-table></el-tab-pane>
      <el-tab-pane label="回收站 / GC 候选" name="trash"><p class="text-sm text-[var(--el-text-color-secondary)] mb-4">候选必须没有有效 Attachment 引用并超过保留期。永久删除需要明确确认，并由后端再次校验保留规则。</p><div class="flex flex-wrap gap-2 mb-4"><el-input v-model="resourceId" placeholder="Resource ID" clearable class="w-56" /><el-button type="danger" :loading="submitting" @click="purgeResource">永久删除 Resource</el-button></div><el-skeleton v-if="loading" :rows="5" animated /><el-empty v-else-if="!candidates.length" description="暂无 Blob 清理候选" /><el-table v-else :data="candidates" stripe><el-table-column prop="blobId" label="Blob ID" min-width="240" /><el-table-column prop="sha256" label="SHA-256" min-width="260" /><el-table-column prop="sizeBytes" label="大小（Bytes）" width="150" /><el-table-column prop="createdAt" label="创建时间" min-width="180" /><el-table-column prop="eligibleAt" label="可清理时间" min-width="180" /></el-table></el-tab-pane>
    </el-tabs></el-card>
  </main>
</template>
