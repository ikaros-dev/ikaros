<script setup lang="ts">
import { onMounted, ref, computed, reactive } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

const route = useRoute();
const title = computed(() => String(route.meta.title || "模块"));
const description = computed(() => String(route.meta.description || ""));
const endpoint = computed(() => String(route.meta.endpoint || ""));
const columns = computed(() => (route.meta.columns as string[]) || ["id"]);
const createEndpoint = computed(() => String(route.meta.createEndpoint || ""));
const createFields = computed(() => route.meta.createFields || []);
const deleteEndpoint = computed(() => String(route.meta.deleteEndpoint || ""));
const actions = computed(() => route.meta.actions || []);
const detailPath = computed(() => String(route.meta.detailPath || ""));
const createVisible = ref(false);
const createLoading = ref(false);
const createForm = reactive<Record<string, string>>({});

function openCreate() {
  createFields.value.forEach(field => { createForm[field.name] = field.defaultValue || ""; });
  createVisible.value = true;
}

async function create() {
  createLoading.value = true;
  try {
    await http.request("post", createEndpoint.value, { data: { ...createForm } });
    createVisible.value = false;
    await load();
  } finally { createLoading.value = false; }
}

async function remove(row: Record<string, unknown>) {
  const id = row.id;
  if (!id || !window.confirm("确定删除此条数据吗？")) return;
  await http.request("delete", `${deleteEndpoint.value}/${id}`);
  await load();
}

async function executeAction(action: { path: string; label: string; method?: string; confirm?: string }, row: Record<string, unknown>) {
  if (!row.id || (action.confirm && !window.confirm(action.confirm))) return;
  await http.request((action.method || "post") as any, action.path.replace("{id}", String(row.id)));
  await load();
}
const loading = ref(false);
const error = ref("");
const rows = ref<Record<string, unknown>[]>([]);
const filterText = ref("");
const filteredRows = computed(() => {
  const keyword = filterText.value.trim().toLowerCase();
  if (!keyword) return rows.value;
  return rows.value.filter(row => JSON.stringify(row).toLowerCase().includes(keyword));
});
function openDetail(row: Record<string, unknown>) { if (detailPath.value && row.id) window.location.assign(detailPath.value.replace("{id}", String(row.id))); }

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const result = await http.get<unknown, unknown>(endpoint.value);
    rows.value = Array.isArray(result) ? result as Record<string, unknown>[] : result ? [result as Record<string, unknown>] : [];
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "接口请求失败";
  } finally { loading.value = false; }
}
onMounted(load);
</script>

<template>
  <div class="p-4 md:p-6">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between gap-4">
          <div><h2 class="text-xl font-semibold">{{ title }}</h2><p class="mt-1 text-sm text-[var(--el-text-color-secondary)]">{{ description }}</p></div>
          <div class="flex gap-2"><el-input v-model="filterText" clearable placeholder="筛选当前结果" class="w-48" /><el-button v-if="createEndpoint" type="primary" @click="openCreate">新建</el-button><el-button :loading="loading" @click="load">刷新</el-button></div>
        </div>
      </template>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
      <el-skeleton v-if="loading" :rows="5" animated />
      <el-empty v-else-if="!filteredRows.length && !error" :description="rows.length ? '没有匹配结果' : '暂无数据'" />
      <el-table v-else :data="filteredRows" stripe :row-class-name="detailPath ? () => 'cursor-pointer' : undefined" @row-click="openDetail">
        <el-table-column v-for="column in columns" :key="column" :prop="column" :label="column" min-width="160" show-overflow-tooltip />
        <el-table-column label="原始数据" min-width="220"><template #default="scope"><span class="text-xs text-[var(--el-text-color-secondary)]">{{ JSON.stringify(scope.row) }}</span></template></el-table-column>
        <el-table-column v-if="deleteEndpoint || actions.length" label="操作" width="180" fixed="right"><template #default="scope"><el-button v-for="action in actions" :key="action.name" link @click="executeAction(action, scope.row)">{{ action.label }}</el-button><el-button v-if="deleteEndpoint" link type="danger" @click="remove(scope.row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="createVisible" title="新建" width="480px">
      <el-form label-position="top" @submit.prevent="create">
        <el-form-item v-for="field in createFields" :key="field.name" :label="field.label" :required="field.required">
          <el-input v-model="createForm[field.name]" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" :loading="createLoading" @click="create">保存</el-button></template>
    </el-dialog>
  </div>
</template>
