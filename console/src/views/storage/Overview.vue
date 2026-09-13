<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, any>;
const loading = ref(false);
const error = ref("");
const providers = ref<Row[]>([]);
const restores = ref<Row[]>([]);
const backupPoints = ref<Row[]>([]);
const candidates = ref<Row[]>([]);
const unknown = "Unknown";
const health = (row: Row) => String(row?.health?.status || row?.healthStatus || unknown).toUpperCase() === "HEALTHY" ? "Healthy" : unknown;
const attention = computed(() => [
  ...providers.value.filter(row => health(row) !== "Healthy").map(row => `${row.providerKey || row.name || unknown}: Provider health ${health(row)}`),
  ...providers.value.filter(row => String(row.status || "").toUpperCase() === "DISABLED").map(row => `${row.providerKey || row.name || unknown}: disabled`),
  ...candidates.value.map(() => "Integrity / GC candidate requires Maintenance review")
]);
async function load() {
  loading.value = true; error.value = "";
  try {
    const results = await Promise.allSettled([
      http.get<unknown, unknown>("/storage/providers"),
      http.get<unknown, unknown>("/restore-requests"),
      http.get<unknown, unknown>("/admin/backup/restore-points"),
      http.get<unknown, unknown>("/storage/gc/candidates")
    ]);
    providers.value = results[0].status === "fulfilled" && Array.isArray(results[0].value) ? results[0].value as Row[] : [];
    restores.value = results[1].status === "fulfilled" && Array.isArray(results[1].value) ? results[1].value as Row[] : [];
    backupPoints.value = results[2].status === "fulfilled" && Array.isArray(results[2].value) ? results[2].value as Row[] : [];
    candidates.value = results[3].status === "fulfilled" && Array.isArray(results[3].value) ? results[3].value as Row[] : [];
    if (results.some(result => result.status === "rejected")) error.value = "部分 Storage 状态暂时未知，请刷新或进入对应页面诊断。";
  } finally { loading.value = false; }
}
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">Storage Overview</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">数据层级、可用性、恢复和维护风险摘要。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" class="mb-4" />
    <section class="grid grid-cols-1 md:grid-cols-5 gap-4 mb-6"><el-card v-for="item in [{ label: 'Providers', value: providers.length }, { label: 'Restoring', value: restores.filter(row => ['PENDING','ACTIVE','RESTORING'].includes(String(row.status).toUpperCase())).length }, { label: 'Backup points', value: backupPoints.length }, { label: 'Maintenance attention', value: attention.length }, { label: 'Unknown health', value: providers.filter(row => health(row) !== 'Healthy').length }]" :key="item.label" shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">{{ item.label }}</div><div class="text-2xl font-semibold mt-2">{{ item.value }}</div></el-card></section>
    <el-card shadow="never" class="mb-5"><template #header><span class="font-medium">Tier 分布</span></template><div class="grid grid-cols-2 md:grid-cols-5 gap-3"><div v-for="tier in ['HOT','WARM','COLD','ARCHIVE','DEEP']" :key="tier" class="rounded bg-[var(--el-fill-color-light)] p-4"><div class="text-sm text-[var(--el-text-color-secondary)]">{{ tier }}</div><div class="text-xl font-semibold mt-1">{{ providers.filter(row => String(row.tier || '').toUpperCase() === tier).length || unknown }}</div><div class="text-xs mt-1">Provider 摘要</div></div></div></el-card>
    <el-card shadow="never"><template #header><span class="font-medium">需要关注</span></template><el-empty v-if="!attention.length" description="暂无已知风险；未探测状态仍显示为 Unknown" /><el-alert v-for="item in attention" v-else :key="item" :title="item" type="warning" show-icon :closable="false" class="mb-2" /></el-card>
  </main>
</template>
