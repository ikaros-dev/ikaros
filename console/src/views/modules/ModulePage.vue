<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

const route = useRoute();
const title = computed(() => String(route.meta.title || "模块"));
const description = computed(() => String(route.meta.description || ""));
const endpoint = computed(() => String(route.meta.endpoint || ""));
const columns = computed(() => (route.meta.columns as string[]) || ["id"]);
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
          <el-button :loading="loading" @click="load">刷新</el-button>
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
  </div>
</template>
