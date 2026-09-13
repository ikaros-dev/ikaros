<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";
type Row = Record<string, any>;
const rows = ref<Row[]>([]); const loading = ref(false); const error = ref("");
async function load() { loading.value = true; error.value = ""; try { const result = await http.get<unknown, unknown>("/storage/policies"); rows.value = Array.isArray(result) ? result as Row[] : result ? [result as Row] : []; } catch (e: any) { error.value = e?.response?.data?.detail || "Storage Policy 加载失败"; } finally { loading.value = false; } }
onMounted(load);
</script>
<template><main class="p-4 md:p-6"><div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">Storage Policy</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">配置生命周期、目标 Tier、副本数和保留期；启用前应先 Dry-run。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div><el-alert v-if="error" :title="error" type="info" show-icon :closable="false" class="mb-4"/><el-card shadow="never"><el-empty v-if="!rows.length && !loading" description="暂无已配置策略；策略和影响预览由服务端提供。"/><el-table v-else :data="rows" stripe><el-table-column prop="name" label="策略" min-width="180"/><el-table-column prop="conditions" label="条件" min-width="240"/><el-table-column prop="targetTier" label="目标 Tier" width="130"/><el-table-column prop="replicaCount" label="副本数" width="100"/><el-table-column prop="retentionDays" label="保留期" width="120"/><el-table-column prop="status" label="状态" width="120"/></el-table></el-card></main></template>
