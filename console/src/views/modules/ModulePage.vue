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
const loading = ref(false);
const error = ref("");
const rows = ref<Record<string, unknown>[]>([]);

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
          <div class="flex gap-2"><el-button v-if="createEndpoint" type="primary" @click="openCreate">新建</el-button><el-button :loading="loading" @click="load">刷新</el-button></div>
        </div>
      </template>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
      <el-skeleton v-if="loading" :rows="5" animated />
      <el-empty v-else-if="!rows.length && !error" description="暂无数据" />
      <el-table v-else :data="rows" stripe>
        <el-table-column v-for="column in columns" :key="column" :prop="column" :label="column" min-width="160" show-overflow-tooltip />
        <el-table-column label="原始数据" min-width="220"><template #default="scope"><span class="text-xs text-[var(--el-text-color-secondary)]">{{ JSON.stringify(scope.row) }}</span></template></el-table-column>
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
