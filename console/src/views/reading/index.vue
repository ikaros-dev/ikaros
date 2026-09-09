<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, any>;
const tab = ref("works");
const works = ref<Row[]>([]); const history = ref<Row[]>([]); const imports = ref<Row[]>([]);
const loading = ref(false); const importLoading = ref(false); const error = ref(""); const importMessage = ref("");
const entryLoading = ref<Record<string, boolean>>({}); const entriesByImport = ref<Record<string, Row[]>>({});
const reorderDialog = ref(false); const reorderImportId = ref(""); const reorderChapter = ref("");
const reorderEntries = ref<Row[]>([]); const reorderSaving = ref(false);
const importForm = ref({ attachmentId: "", title: "", language: "zh-CN" });

async function load() {
  loading.value = true; error.value = "";
  try {
    const [w, h, i] = await Promise.all([http.get<unknown, unknown>("/reading/works"), http.get<unknown, unknown>("/reading/history"), http.get<unknown, unknown>("/reading/comic-imports")]);
    works.value = Array.isArray(w) ? w as Row[] : []; history.value = Array.isArray(h) ? h as Row[] : []; imports.value = Array.isArray(i) ? i as Row[] : [];
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "阅读数据加载失败"; }
  finally { loading.value = false; }
}
async function importComic() {
  if (!importForm.value.attachmentId.trim()) { error.value = "请输入漫画包 Attachment ID"; return; }
  importLoading.value = true; error.value = ""; importMessage.value = "";
  try { const result: any = await http.post("/reading/comic-imports", { headers: { "Idempotency-Key": crypto.randomUUID() }, data: { attachmentId: importForm.value.attachmentId.trim(), title: importForm.value.title.trim() || undefined, language: importForm.value.language || undefined } }); importMessage.value = `导入请求已受理：${result?.id || "已创建"}`; importForm.value = { attachmentId: "", title: "", language: "zh-CN" }; tab.value = "imports"; await load(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "漫画包导入失败"; }
  finally { importLoading.value = false; }
}
function statusType(status: string) { const value = String(status || "").toUpperCase(); return value === "SUCCEEDED" ? "success" : value === "FAILED" ? "danger" : value === "ACCEPTED" ? "warning" : "info"; }
async function parseComic(row: Row) {
  if (!row.id) return; entryLoading.value = { ...entryLoading.value, [row.id]: true }; error.value = "";
  try { const action = row.status === "FAILED" ? "retry-parse" : "parse"; await http.post(`/reading/comic-imports/${row.id}/actions/${action}`); const result: any = await http.get(`/reading/comic-imports/${row.id}/entries`); entriesByImport.value = { ...entriesByImport.value, [row.id]: Array.isArray(result) ? result : [] }; await load(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "漫画包解析失败"; }
  finally { const next = { ...entryLoading.value }; delete next[row.id]; entryLoading.value = next; }
}
function chapterKeys() { return [...new Set((entriesByImport.value[reorderImportId.value] || []).map(item => String(item.chapterKey || "")))].filter(Boolean); }
function loadReorderChapter() { reorderEntries.value = (entriesByImport.value[reorderImportId.value] || []).filter(item => item.chapterKey === reorderChapter.value).map(item => ({ ...item })); }
async function openReorder(row: Row) {
  if (!row.id) return; reorderImportId.value = row.id; error.value = ""; reorderDialog.value = true;
  if (!entriesByImport.value[row.id]?.length) { entryLoading.value = { ...entryLoading.value, [row.id]: true }; try { const result: any = await http.get(`/reading/comic-imports/${row.id}/entries`); entriesByImport.value = { ...entriesByImport.value, [row.id]: Array.isArray(result) ? result : [] }; } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "解析条目加载失败"; } finally { const next = { ...entryLoading.value }; delete next[row.id]; entryLoading.value = next; } }
  reorderChapter.value = chapterKeys()[0] || ""; loadReorderChapter();
}
function moveEntry(index: number, delta: number) { const target = index + delta; if (target < 0 || target >= reorderEntries.value.length) return; const next = [...reorderEntries.value]; [next[index], next[target]] = [next[target], next[index]]; reorderEntries.value = next; }
async function saveReorder() {
  if (!reorderImportId.value || !reorderChapter.value || !reorderEntries.value.length) return; reorderSaving.value = true; error.value = "";
  try { await http.post(`/reading/comic-imports/${reorderImportId.value}/actions/reorder-pages`, { data: { chapterKey: reorderChapter.value, entryIds: reorderEntries.value.map(item => item.id) } }); entriesByImport.value = { ...entriesByImport.value, [reorderImportId.value]: [...(entriesByImport.value[reorderImportId.value] || []).filter(item => item.chapterKey !== reorderChapter.value), ...reorderEntries.value.map((item, index) => ({ ...item, pageOrder: index }))] }; reorderDialog.value = false; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "页序保存失败"; }
  finally { reorderSaving.value = false; }
}
onMounted(load);
</script>
<template>
  <main class="p-4 md:p-6"><div class="flex flex-wrap justify-between items-start gap-4 mb-6"><div><h1 class="text-2xl font-semibold">阅读库</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">管理作品目录、漫画包导入和阅读历史。</p></div><div class="flex gap-2"><el-button :loading="loading" @click="load">刷新</el-button><el-button type="primary" @click="tab = 'import'">导入漫画包</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4"/><el-alert v-if="importMessage" :title="importMessage" type="success" show-icon :closable="false" class="mb-4"/>
    <el-card shadow="never"><el-tabs v-model="tab"><el-tab-pane label="作品目录" name="works"><el-skeleton v-if="loading" :rows="6" animated/><el-empty v-else-if="!works.length" description="暂无阅读作品"/><el-table v-else :data="works" stripe><el-table-column prop="title" label="标题" min-width="240"/><el-table-column prop="kind" label="类型" width="120"/><el-table-column prop="originalLanguage" label="原始语言" width="130"/><el-table-column prop="id" label="Work ID" min-width="280"/></el-table></el-tab-pane>
      <el-tab-pane label="漫画导入" name="imports"><div class="max-w-2xl mb-6"><el-form label-position="top"><el-form-item label="漫画包 Attachment ID" required><el-input v-model="importForm.attachmentId" placeholder="先在附件管理上传 CBZ / CBR / ZIP，再粘贴 Attachment ID" clearable/></el-form-item><div class="grid grid-cols-1 md:grid-cols-2 gap-4"><el-form-item label="作品标题"><el-input v-model="importForm.title" placeholder="留空使用文件名"/></el-form-item><el-form-item label="语言"><el-input v-model="importForm.language" placeholder="zh-CN"/></el-form-item></div><el-button type="primary" :loading="importLoading" @click="importComic">提交漫画包导入</el-button></el-form></div><el-empty v-if="!imports.length" description="暂无漫画导入记录"/><el-table v-else :data="imports" stripe><el-table-column prop="sourceAttachmentId" label="源附件" min-width="280"/><el-table-column prop="workId" label="Work ID" min-width="280"/><el-table-column prop="editionId" label="Edition ID" min-width="280"/><el-table-column label="状态" width="130"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template></el-table-column><el-table-column prop="errorMessage" label="错误" min-width="220"/><el-table-column label="操作" width="220"><template #default="{ row }"><el-button link type="primary" :loading="entryLoading[row.id]" :disabled="row.status === 'PARSING'" @click="parseComic(row)">{{ row.status === 'FAILED' ? '重试解析' : row.status === 'SUCCEEDED' ? '重新读取结果' : '解析章节页序' }}</el-button><el-button link type="primary" :disabled="!entriesByImport[row.id]?.length" @click="openReorder(row)">调整页序</el-button></template></el-table-column><el-table-column label="解析结果" min-width="260"><template #default="{ row }"><div v-if="entriesByImport[row.id]?.length" class="text-sm">{{ [...new Set(entriesByImport[row.id].map(item => item.chapterKey))].length }} 个章节，{{ entriesByImport[row.id].length }} 页</div><div v-else class="text-sm text-[var(--el-text-color-secondary)]">提交解析后显示</div></template></el-table-column></el-table></el-tab-pane>
      <el-tab-pane label="继续阅读" name="continue"><el-empty v-if="!history.length" description="暂无可继续阅读内容"/><div v-for="item in history.slice(0, 6)" :key="item.id" class="flex justify-between py-3 border-b last:border-0"><div><div class="font-medium">{{ item.title || item.workId || '作品' }}</div><div class="text-sm text-[var(--el-text-color-secondary)]">{{ item.position || item.chapterId || '未记录位置' }}</div></div><el-button link>继续</el-button></div></el-tab-pane>
      <el-tab-pane label="阅读历史" name="history"><el-empty v-if="!history.length" description="暂无阅读历史"/><el-table v-else :data="history" stripe><el-table-column prop="workId" label="作品" min-width="220"/><el-table-column prop="chapterId" label="内容位置" min-width="180"/><el-table-column prop="startedAt" label="开始时间" min-width="180"/><el-table-column prop="updatedAt" label="最后位置" min-width="180"/></el-table></el-tab-pane>
      <el-tab-pane label="导入说明" name="import"><el-alert title="请先到附件管理上传原始 CBZ、CBR 或 ZIP，再提交 Attachment ID；章节解析状态会在漫画导入列表显示。" type="info" :closable="false"/></el-tab-pane></el-tabs></el-card>
    <el-dialog v-model="reorderDialog" title="人工修正页序" width="680px"><el-select v-model="reorderChapter" class="w-full mb-4" placeholder="选择章节" @change="loadReorderChapter"><el-option v-for="chapter in chapterKeys()" :key="chapter" :label="chapter" :value="chapter"/></el-select><el-empty v-if="!reorderEntries.length" description="该章节没有解析页"/><div v-for="(entry, index) in reorderEntries" :key="entry.id" class="flex items-center gap-3 py-2 border-b"><span class="w-8 text-center">{{ index + 1 }}</span><span class="flex-1 truncate">{{ entry.entryName }}</span><el-button link :disabled="index === 0" @click="moveEntry(index, -1)">上移</el-button><el-button link :disabled="index === reorderEntries.length - 1" @click="moveEntry(index, 1)">下移</el-button></div><template #footer><el-button @click="reorderDialog = false">取消</el-button><el-button type="primary" :loading="reorderSaving" @click="saveReorder">保存页序</el-button></template></el-dialog>
  </main>
</template>
