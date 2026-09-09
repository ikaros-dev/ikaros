<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { http } from "@/utils/http";

type Collection = { id: string; parentId?: string; name: string; type?: string; description?: string; createdAt?: string; updatedAt?: string; version?: number };
const router = useRouter();
const activeTab = ref("collections");
const items = ref<Collection[]>([]);
const loading = ref(false);
const error = ref("");
const dialog = ref(false);
const saving = ref(false);
const editDialog = ref(false);
const editing = ref<Collection | null>(null);
const editForm = ref({ name: "", description: "" });
const editTargetId = ref("");
const form = ref({ name: "", description: "", parentId: "" });
const search = ref("");
const collectionType = ref("manual");
const visibility = ref("private");
const roots = computed(() => items.value.filter(item => !item.parentId));
const filteredItems = computed(() => items.value.filter(item => item.name.toLowerCase().includes(search.value.trim().toLowerCase())));

async function load() {
  loading.value = true; error.value = "";
  try { const result = await http.get<unknown, unknown>("/collections"); items.value = Array.isArray(result) ? result as Collection[] : []; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "集合加载失败"; }
  finally { loading.value = false; }
}
async function createCollection() {
  if (!form.value.name.trim()) { error.value = "集合名称不能为空"; return; }
  saving.value = true;
  try { await http.post("/collections", { data: { name: form.value.name.trim(), description: form.value.description || null, parentId: form.value.parentId || null, type: collectionType.value, visibility: visibility.value } }); dialog.value = false; form.value = { name: "", description: "", parentId: "" }; await load(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "集合创建失败"; }
  finally { saving.value = false; }
}
function openDetail(item: Collection) { router.push({ path: "/resource-center/collections", query: { id: item.id } }); }
function beginEdit(item: Collection) { editing.value = item; editForm.value = { name: item.name, description: item.description || "" }; editDialog.value = true; }
function editSelected() { const item = items.value.find(value => value.id === editTargetId.value); if (item) beginEdit(item); }
async function updateCollection() { if (!editing.value || !editForm.value.name.trim()) { error.value = "集合名称不能为空"; return; } saving.value = true; try { await http.request("put", `/collections/${editing.value.id}`, { data: { name: editForm.value.name.trim(), description: editForm.value.description || null }, headers: { "If-Match": `"${editing.value.version ?? 0}"` } }); editDialog.value = false; await load(); } catch (e: any) { error.value = e?.response?.status === 409 ? "集合已被其他请求修改，请刷新后重试。" : e?.response?.data?.detail || e?.message || "集合更新失败"; } finally { saving.value = false; } }
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start gap-4 mb-6"><div><h1 class="text-2xl font-semibold">集合</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">用逻辑集合组织资源，不改变附件的物理存储位置。</p></div><div class="flex gap-2"><el-button @click="dialog = true">新建集合</el-button><el-button :loading="loading" @click="load">刷新</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" /><div class="flex gap-2 mb-4"><el-select v-model="editTargetId" clearable placeholder="选择集合后编辑" class="w-64"><el-option v-for="item in items" :key="item.id" :label="item.name" :value="item.id" /></el-select><el-button :disabled="!editTargetId" @click="editSelected">编辑所选集合</el-button></div>
    <el-tabs v-model="activeTab" class="mb-4"><el-tab-pane label="集合" name="collections" /><el-tab-pane label="标签" name="tags" /><el-tab-pane label="关系类型关系浏览器" name="relations" /></el-tabs>
    <template v-if="activeTab === 'collections'"><div class="flex flex-wrap gap-3 mb-4"><el-input v-model="search" clearable placeholder="搜索集合名称" class="w-64"/><el-select v-model="collectionType" class="w-32"><el-option label="全部类型" value="all"/><el-option label="手动" value="manual"/><el-option label="动态" value="dynamic"/></el-select><el-button @click="load">应用筛选</el-button></div><el-skeleton v-if="loading" :rows="8" animated /><el-card v-else shadow="never"><el-empty v-if="!filteredItems.length" description="暂无匹配集合，先创建一个集合吧" /><el-table v-else :data="filteredItems" stripe><el-table-column prop="name" label="名称" min-width="220"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">{{ row.name }}</el-button></template></el-table-column><el-table-column label="类型" width="110"><template #default="{ row }">{{ row.type === 'dynamic' ? '动态' : '手动' }}</template></el-table-column><el-table-column label="Resource 数量" width="140">—</el-table-column><el-table-column label="所有者" width="140">当前用户</el-table-column><el-table-column label="可见性" width="120">私有</el-table-column><el-table-column prop="updatedAt" label="最近更新" min-width="180" /><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link @click="openDetail(row)">详情</el-button></template></el-table-column></el-table></el-card></template>
    <el-card v-else-if="activeTab === 'tags'" shadow="never"><el-table :data="[]"><el-table-column label="Tag 名称"/><el-table-column label="命名空间 / 类型"/><el-table-column label="使用数量"/><el-table-column label="颜色 / 视觉 Token"/><el-table-column label="更新时间"/></el-table><el-empty description="标签管理接口尚未提供，待后端标签模型接入" /></el-card><el-card v-else shadow="never"><div class="flex flex-wrap gap-3 mb-4"><el-select placeholder="选择来源 Resource" class="w-64" disabled/><el-tag v-for="type in ['相关','引用','衍生']" :key="type" effect="plain">{{ type }}</el-tag><el-button disabled>表格</el-button><el-button disabled>Graph</el-button></div><el-empty description="关系类型浏览器接口尚未提供，待后端关系模型接入" /></el-card>
    <el-dialog v-model="dialog" title="新建集合" width="520px"><el-form label-position="top"><el-form-item label="名称" required><el-input v-model="form.name" maxlength="256" show-word-limit /></el-form-item><el-form-item label="描述"><el-input v-model="form.description" type="textarea" maxlength="2000" show-word-limit /></el-form-item><el-form-item label="类型"><el-radio-group v-model="collectionType"><el-radio value="manual">手动</el-radio><el-radio value="dynamic">动态</el-radio></el-radio-group></el-form-item><el-form-item label="父集合"><el-select v-model="form.parentId" clearable placeholder="无（顶级集合）" class="w-full"><el-option v-for="item in roots" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="可见性"><el-select v-model="visibility" class="w-full"><el-option label="私有" value="private"/><el-option label="共享" value="shared"/></el-select></el-form-item></el-form><template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="createCollection">创建</el-button></template></el-dialog><el-dialog v-model="editDialog" title="编辑集合" width="520px"><el-form label-position="top"><el-form-item label="名称" required><el-input v-model="editForm.name" maxlength="256" /></el-form-item><el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" maxlength="2000" /></el-form-item></el-form><template #footer><el-button @click="editDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="updateCollection">保存</el-button></template></el-dialog>
  </main>
</template>
