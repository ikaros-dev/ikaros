<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { http } from "@/utils/http";

type Collection = { id: string; parentId?: string; name: string; description?: string; createdAt?: string; updatedAt?: string };
const router = useRouter();
const activeTab = ref("collections");
const items = ref<Collection[]>([]);
const loading = ref(false);
const error = ref("");
const dialog = ref(false);
const saving = ref(false);
const form = ref({ name: "", description: "", parentId: "" });
const roots = computed(() => items.value.filter(item => !item.parentId));

async function load() {
  loading.value = true; error.value = "";
  try { const result = await http.get<unknown, unknown>("/collections"); items.value = Array.isArray(result) ? result as Collection[] : []; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "集合加载失败"; }
  finally { loading.value = false; }
}
async function createCollection() {
  if (!form.value.name.trim()) { error.value = "集合名称不能为空"; return; }
  saving.value = true;
  try { await http.post("/collections", { data: { name: form.value.name.trim(), description: form.value.description || null, parentId: form.value.parentId || null } }); dialog.value = false; form.value = { name: "", description: "", parentId: "" }; await load(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "集合创建失败"; }
  finally { saving.value = false; }
}
function openDetail(item: Collection) { router.push({ path: "/console/collections", query: { id: item.id } }); }
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start gap-4 mb-6"><div><h1 class="text-2xl font-semibold">集合</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">用逻辑集合组织资源，不改变附件的物理存储位置。</p></div><div class="flex gap-2"><el-button @click="dialog = true">新建集合</el-button><el-button :loading="loading" @click="load">刷新</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-tabs v-model="activeTab" class="mb-4"><el-tab-pane label="集合" name="collections" /><el-tab-pane label="标签" name="tags" /><el-tab-pane label="关系类型关系浏览器" name="relations" /></el-tabs>
    <template v-if="activeTab === 'collections'"><el-skeleton v-if="loading" :rows="8" animated /><el-card v-else shadow="never"><el-empty v-if="!items.length" description="暂无集合，先创建一个集合吧" /><el-table v-else :data="items" stripe><el-table-column prop="name" label="名称" min-width="220"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">{{ row.name }}</el-button></template></el-table-column><el-table-column label="类型" width="110"><template #default="{ row }">{{ row.parentId ? '子集合' : '手动' }}</template></el-table-column><el-table-column label="资源数量" width="120">—</el-table-column><el-table-column label="可见性" width="120">私有</el-table-column><el-table-column prop="updatedAt" label="最近更新" min-width="180" /><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link @click="openDetail(row)">详情</el-button></template></el-table-column></el-table></el-card></template>
    <el-card v-else shadow="never"><el-empty :description="activeTab === 'tags' ? '标签管理接口尚未提供，待后端标签模型接入' : '关系类型浏览器接口尚未提供，待后端关系模型接入'" /></el-card>
    <el-dialog v-model="dialog" title="新建集合" width="520px"><el-form label-position="top"><el-form-item label="名称" required><el-input v-model="form.name" maxlength="256" show-word-limit /></el-form-item><el-form-item label="描述"><el-input v-model="form.description" type="textarea" maxlength="2000" show-word-limit /></el-form-item><el-form-item label="父集合"><el-select v-model="form.parentId" clearable placeholder="无（顶级集合）" class="w-full"><el-option v-for="item in roots" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-form><template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="createCollection">创建</el-button></template></el-dialog>
  </main>
</template>
