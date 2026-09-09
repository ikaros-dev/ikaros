<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, any>;
const tab = ref("sources");
const query = ref("");
const sources = ref<Row[]>([]);
const scans = ref<Row[]>([]);
const runs = ref<Row[]>([]);
const items = ref<Row[]>([]);
const candidates = ref<Row[]>([]);
const plan = ref<Row | null>(null);
const planItems = ref<Row[]>([]);
const planBusy = ref(false);
const itemSaving = ref<Set<string>>(new Set());
const loading = ref(false);
const error = ref("");
const selectedRun = ref<Row | null>(null);
const selectedScan = ref<Row | null>(null);
const sourceDialog = ref(false);
const savingSource = ref(false);
const scanningSources = ref<Set<string>>(new Set());
const sourceForm = ref({ type: "LOCAL", displayName: "", rootReference: "", credentialReference: "" });
const filteredSources = computed(() => sources.value.filter(row => !query.value || JSON.stringify(row).toLowerCase().includes(query.value.toLowerCase())));
const filteredScans = computed(() => scans.value.filter(row => !query.value || JSON.stringify(row).toLowerCase().includes(query.value.toLowerCase())));
const filteredRuns = computed(() => runs.value.filter(row => !query.value || JSON.stringify(row).toLowerCase().includes(query.value.toLowerCase())));

async function load() {
  loading.value = true; error.value = "";
  try {
    if (tab.value === "sources") { const result = await http.get<unknown, unknown>("/ingestion/sources"); sources.value = Array.isArray(result) ? result as Row[] : []; }
    else if (tab.value === "preview") { const result = await http.get<unknown, unknown>("/ingestion/sources/scans"); scans.value = Array.isArray(result) ? result as Row[] : []; }
    else if (tab.value === "scans") { const result = await http.get<unknown, unknown>("/ingestion/runs"); runs.value = Array.isArray(result) ? result as Row[] : []; }
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "内容导入数据加载失败"; }
  finally { loading.value = false; }
}
async function openScan(scan: Row) { selectedScan.value = scan; try { const result = await http.get<unknown, unknown>(`/ingestion/scans/${scan.id}/candidates`); candidates.value = Array.isArray(result) ? result as Row[] : []; } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "扫描候选加载失败"; } }
async function generatePlan() {
  if (!selectedScan.value?.id || planBusy.value) return;
  planBusy.value = true; error.value = "";
  try {
    plan.value = await http.post<unknown, unknown>(`/ingestion/scans/${selectedScan.value.id}/plans`, { data: { dryRun: false, policySnapshot: {} } }) as Row;
    await loadPlanItems();
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "导入计划生成失败"; }
  finally { planBusy.value = false; }
}
async function loadPlanItems() {
  if (!plan.value?.id) return;
  const result = await http.get<unknown, unknown>(`/ingestion/scans/plans/${plan.value.id}/items`);
  planItems.value = Array.isArray(result) ? result as Row[] : [];
}
async function savePlanItem(item: Row) {
  if (!plan.value?.id || !item.id || itemSaving.value.has(String(item.id))) return;
  const next = new Set(itemSaving.value); next.add(String(item.id)); itemSaving.value = next;
  try {
    const updated = await http.request<unknown>("patch", `/ingestion/scans/plans/${plan.value.id}/items/${item.id}`, { data: {
      expectedVersion: item.version ?? 0, action: item.action, targetId: item.targetId || null, reason: item.reason || null
    } }) as Row;
    Object.assign(item, updated);
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "保存计划项失败"; }
  finally { const done = new Set(itemSaving.value); done.delete(String(item.id)); itemSaving.value = done; }
}
async function approvePlan() {
  if (!plan.value?.id || planBusy.value) return;
  planBusy.value = true; error.value = "";
  try { plan.value = await http.post<unknown, unknown>(`/ingestion/scans/plans/${plan.value.id}/approve`, { data: { expectedVersion: plan.value.version ?? 0 } }) as Row; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "导入计划审批失败"; }
  finally { planBusy.value = false; }
}
async function startImport() {
  if (!plan.value?.id || planBusy.value) return;
  planBusy.value = true; error.value = "";
  try { await http.post(`/ingestion/plans/${plan.value.id}/runs`, { data: { expectedPlanVersion: plan.value.version ?? 0 } }); tab.value = "scans"; await load(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "导入执行提交失败"; }
  finally { planBusy.value = false; }
}
async function openRun(run: Row) { selectedRun.value = run; try { const result = await http.get<unknown, unknown>(`/ingestion/runs/${run.id}/items`); items.value = Array.isArray(result) ? result as Row[] : []; } catch (e: any) { error.value = e?.response?.data?.detail || "导入项目加载失败"; } }
async function cancelRun(run: Row) { if (!run.id || !window.confirm("确认取消该导入运行？已完成项目将保留。")) return; try { await http.request("delete", `/ingestion/runs/${run.id}`); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || "取消导入失败"; } }
async function retry(item: Row) { if (!selectedRun.value?.id || !item.id) return; try { await http.post(`/ingestion/runs/${selectedRun.value.id}/items/${item.id}/retry`); await openRun(selectedRun.value); } catch (e: any) { error.value = e?.response?.data?.detail || "重试失败"; } }
watch(tab, load); onMounted(load);
async function saveSource() { if (!sourceForm.value.displayName.trim() || !sourceForm.value.rootReference.trim()) { error.value = "请填写来源名称和根位置"; return; } savingSource.value = true; error.value = ""; try { await http.post("/ingestion/sources", { data: { ...sourceForm.value, scanPolicy: {} } }); sourceDialog.value = false; sourceForm.value = { type: "LOCAL", displayName: "", rootReference: "", credentialReference: "" }; await load(); } catch (e: any) { error.value = e?.response?.data?.detail || "创建导入来源失败"; } finally { savingSource.value = false; } }
async function toggleSource(source: Row) { try { if (source.status === "DISABLED" || source.enabled === false) await http.post(`/ingestion/sources/${source.id}/enable`); else { if (!window.confirm("确认停用该导入来源？")) return; await http.request("delete", `/ingestion/sources/${source.id}`); } await load(); } catch (e: any) { error.value = e?.response?.data?.detail || "更新来源状态失败"; } }
async function startScan(source: Row) { const id = String(source.id || ""); if (!id || scanningSources.value.has(id)) return; const next = new Set(scanningSources.value); next.add(id); scanningSources.value = next; try { await http.post(`/ingestion/sources/${id}/scans`, { data: { trigger: "console" } }); tab.value = "preview"; await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "扫描任务提交失败"; } finally { const done = new Set(scanningSources.value); done.delete(id); scanningSources.value = done; } }
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">内容导入</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">管理导入来源、扫描任务和失败项目；导入过程由后台任务执行。</p></div><div class="flex gap-2"><el-button v-if="tab === 'sources'" type="primary" @click="sourceDialog = true">添加来源</el-button><el-button :loading="loading" @click="load">刷新</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4"/>
    <el-tabs v-model="tab"><el-tab-pane label="来源" name="sources"/><el-tab-pane label="扫描预览" name="preview"/><el-tab-pane label="导入任务" name="scans"/><el-tab-pane label="失败项目" name="failed"/></el-tabs>
    <el-card v-if="tab === 'sources'" shadow="never"><template #header><div class="flex justify-between items-center"><span>导入来源</span><el-input v-model="query" clearable placeholder="搜索名称" class="w-64"/></div></template><el-skeleton v-if="loading" :rows="7" animated/><el-empty v-else-if="!filteredSources.length" description="暂无导入来源"/><el-table v-else :data="filteredSources" stripe><el-table-column prop="displayName" label="来源名称" min-width="220"/><el-table-column prop="type" label="类型" width="140"/><el-table-column prop="rootReference" label="位置摘要" min-width="240"/><el-table-column prop="status" label="状态" width="120"/><el-table-column prop="updatedAt" label="更新时间" min-width="180"/><el-table-column label="操作" width="180"><template #default="{ row }"><el-button link type="primary" :loading="scanningSources.has(String(row.id))" @click="startScan(row)">{{ scanningSources.has(String(row.id)) ? '扫描中' : '开始扫描' }}</el-button><el-button link @click="toggleSource(row)">{{ row.status === 'DISABLED' || row.enabled === false ? '启用' : '停用' }}</el-button></template></el-table-column></el-table></el-card>
    <el-card v-else-if="tab === 'preview'" shadow="never"><template #header><div class="flex justify-between items-center"><span>扫描结果预览</span><el-input v-model="query" clearable placeholder="搜索 Scan ID" class="w-64"/></div></template><el-empty v-if="!filteredScans.length" description="暂无扫描运行；请先从来源开始扫描"/><el-table v-else :data="filteredScans" stripe><el-table-column prop="id" label="Scan ID" min-width="230"/><el-table-column prop="sourceId" label="来源" min-width="180"/><el-table-column prop="status" label="状态" width="130"/><el-table-column prop="discoveredCount" label="发现" width="90"/><el-table-column prop="changedCount" label="变化" width="90"/><el-table-column prop="skippedCount" label="跳过" width="90"/><el-table-column prop="createdAt" label="开始时间" min-width="180"/><el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="openScan(row)">查看候选</el-button></template></el-table-column></el-table><el-divider v-if="selectedScan"/><template v-if="selectedScan"><div class="flex justify-between items-center mb-3"><span class="font-medium">候选条目：{{ selectedScan.id }}</span><div class="flex items-center gap-3"><span class="text-sm text-[var(--el-text-color-secondary)]">{{ candidates.length }} 条</span><el-button type="primary" :loading="planBusy" @click="generatePlan">生成导入计划</el-button></div></div><el-empty v-if="!candidates.length" description="该扫描暂无候选条目"/><el-table v-else :data="candidates" stripe><el-table-column prop="id" label="Candidate ID" min-width="220"/><el-table-column prop="titleHint" label="标题提示" min-width="220"/><el-table-column prop="suggestedResourceType" label="建议类型" width="150"/><el-table-column prop="externalIdHint" label="外部 ID" min-width="180"/><el-table-column prop="confidence" label="置信度" width="100"/><el-table-column prop="status" label="状态" width="120"/></el-table><template v-if="plan"><el-divider/><div class="flex justify-between items-center mb-3"><div><span class="font-medium">导入计划：{{ plan.id }}</span><span class="ml-3 text-sm text-[var(--el-text-color-secondary)]">状态 {{ plan.status }} · 版本 {{ plan.version }}</span></div><div class="flex gap-2"><el-button v-if="plan.status === 'GENERATED'" :loading="planBusy" @click="approvePlan">批准计划</el-button><el-button v-if="plan.status === 'APPROVED'" type="primary" :loading="planBusy" @click="startImport">启动导入</el-button></div></div><el-alert v-if="plan.status === 'GENERATED'" title="默认计划项为 REQUIRE_REVIEW；请逐项选择动作并保存，全部审核项解决后才能批准。暂不导入的条目请选择 SKIP。" type="info" show-icon :closable="false" class="mb-3"/><el-table :data="planItems" stripe><el-table-column prop="candidateId" label="Candidate ID" min-width="220"/><el-table-column label="动作" min-width="220"><template #default="{ row }"><el-select v-model="row.action" :disabled="plan.status !== 'GENERATED'" class="w-full"><el-option v-for="action in ['CREATE_RESOURCE','LINK_EXISTING_RESOURCE','CREATE_ATTACHMENT','LINK_EXISTING_BLOB','BIND_EXTERNAL_IDENTITY','UPDATE_METADATA_CANDIDATE','ADD_RELATION','ADD_COLLECTION_MEMBER','MARK_SOURCE_UNAVAILABLE','SKIP','REQUIRE_REVIEW','CONFLICT']" :key="action" :label="action" :value="action"/></el-select></template></el-table-column><el-table-column label="目标 ID" min-width="230"><template #default="{ row }"><el-input v-model="row.targetId" :disabled="plan.status !== 'GENERATED'" placeholder="可选 UUID"/></template></el-table-column><el-table-column label="原因/备注" min-width="240"><template #default="{ row }"><el-input v-model="row.reason" :disabled="plan.status !== 'GENERATED'"/></template></el-table-column><el-table-column label="操作" width="90"><template #default="{ row }"><el-button v-if="plan.status === 'GENERATED'" link type="primary" :loading="itemSaving.has(String(row.id))" @click="savePlanItem(row)">保存</el-button></template></el-table-column></el-table></template></template></el-card>
    <el-card v-else-if="tab === 'scans'" shadow="never"><template #header><div class="flex justify-between items-center"><span>导入运行</span><el-input v-model="query" clearable placeholder="搜索名称或任务 ID" class="w-64"/></div></template><el-skeleton v-if="loading" :rows="7" animated/><el-empty v-else-if="!filteredRuns.length" description="暂无导入运行"/><el-table v-else :data="filteredRuns" stripe><el-table-column prop="id" label="Run ID" min-width="230"/><el-table-column prop="sourceId" label="来源" min-width="180"/><el-table-column prop="status" label="状态" width="130"/><el-table-column prop="completedCount" label="已完成" width="100"/><el-table-column prop="failedCount" label="失败" width="90"/><el-table-column prop="startedAt" label="开始时间" min-width="180"/><el-table-column label="操作" width="150"><template #default="{ row }"><el-button link @click="openRun(row)">查看项目</el-button><el-button link type="danger" @click="cancelRun(row)">取消</el-button></template></el-table-column></el-table><template v-if="selectedRun"><el-divider/><div class="flex justify-between items-center mb-3"><div><span class="font-medium">逐项结果：{{ selectedRun.id }}</span><span class="ml-3 text-sm text-[var(--el-text-color-secondary)]">{{ items.length }} 项</span></div><el-button @click="openRun(selectedRun)">刷新结果</el-button></div><el-empty v-if="!items.length" description="该运行暂无项目结果"/><el-table v-else :data="items" stripe><el-table-column prop="id" label="Item ID" min-width="220"/><el-table-column prop="status" label="状态" width="130"/><el-table-column prop="attemptCount" label="尝试次数" width="110"/><el-table-column prop="errorMessage" label="安全错误摘要" min-width="300"/><el-table-column label="操作" width="90"><template #default="{ row }"><el-button v-if="String(row.status).toUpperCase() === 'FAILED'" link type="primary" @click="retry(row)">重试</el-button></template></el-table-column></el-table></template></el-card>
    <el-card v-else shadow="never"><template #header>失败项目</template><el-empty v-if="!items.length" description="请先在导入运行中打开项目详情"/><el-table v-else :data="items.filter(item => String(item.status).toUpperCase() === 'FAILED')" stripe><el-table-column prop="id" label="Item ID" min-width="220"/><el-table-column prop="attemptCount" label="尝试次数" width="110"/><el-table-column prop="errorMessage" label="安全错误摘要" min-width="260"/><el-table-column label="操作" width="90"><template #default="{ row }"><el-button link @click="retry(row)">重试</el-button></template></el-table-column></el-table></el-card>
    <el-dialog v-model="sourceDialog" title="添加导入来源" width="520px"><el-form label-position="top"><el-form-item label="类型"><el-select v-model="sourceForm.type" class="w-full"><el-option label="本地目录" value="LOCAL"/><el-option label="WebDAV" value="WEBDAV"/><el-option label="S3" value="S3"/></el-select></el-form-item><el-form-item label="来源名称" required><el-input v-model="sourceForm.displayName"/></el-form-item><el-form-item label="根位置" required><el-input v-model="sourceForm.rootReference" placeholder="目录或 URI"/></el-form-item><el-form-item label="凭据引用"><el-input v-model="sourceForm.credentialReference" placeholder="仅填写凭据引用，不要填写密钥"/></el-form-item></el-form><template #footer><el-button @click="sourceDialog = false">取消</el-button><el-button type="primary" :loading="savingSource" @click="saveSource">创建</el-button></template></el-dialog>
  </main>
</template>
