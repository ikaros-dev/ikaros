<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

type Row = Record<string, unknown>;
const route = useRoute(); const rows = ref<Row[]>([]); const spaces = ref<Row[]>([]); const loading = ref(false); const error = ref("");
const kind = computed(() => String(route.path.split("/").pop()));
const config = computed(() => ({
  trash: ["回收站", "已删除节点", ["id", "name", "deletedAt", "expiresAt"]],
  transfers: ["传输中心", "上传、下载与同步传输状态", ["id", "name", "status", "progress"]],
  sync: ["同步绑定", "设备绑定、同步游标与运行状态", ["id", "deviceId", "status", "lastSyncAt"]],
  conflicts: ["冲突处理", "需要人工决策的同步冲突", ["id", "nodeId", "state", "createdAt"]],
  quota: ["配额与用量", "Drive Space 的逻辑用量", ["spaceId", "usedBytes", "quotaBytes", "updatedAt"]],
  revisions: ["文件版本", "节点的 revision 历史", ["id", "nodeId", "revision", "createdAt"]]
}[kind.value] || ["Drive 运维", "Drive 诊断信息", ["id", "status", "updatedAt"]]));
async function load() { loading.value = true; error.value = ""; try { if (kind.value === "policies") { rows.value = []; error.value = "云盘策略接口尚未接入；请在后端策略服务启用后管理"; return; } if (route.path.includes("/sync/")) { const result = await http.get<unknown, unknown>("/drive/bindings"); rows.value = (Array.isArray(result) ? result as Row[] : []).filter(row => String(row.id) === String(route.params.bindingId)); return; } const s = await http.get<unknown, unknown>("/drive/spaces"); spaces.value = Array.isArray(s) ? s as Row[] : []; const space = spaces.value[0]; if (!space) return; let endpoint = `/drive/spaces/${space.id}/quota`; if (kind.value === "trash") endpoint = `/drive/spaces/${space.id}/tombstones`; if (kind.value === "sync") endpoint = "/drive/bindings"; if (kind.value === "conflicts") { const b = await http.get<unknown, unknown>("/drive/bindings"); const binding = Array.isArray(b) ? (b as Row[])[0] : null; endpoint = binding?.id ? `/drive/bindings/${binding.id}/conflicts` : "/drive/bindings"; } const result = await http.get<unknown, unknown>(endpoint); rows.value = Array.isArray(result) ? result as Row[] : result ? [result as Row] : []; } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "Drive 数据加载失败"; } finally { loading.value = false; } }
onMounted(load);
</script>
<template><main class="p-4 md:p-6"><div class="flex items-start justify-between mb-6"><div><h1 class="text-2xl font-semibold">{{ config[0] }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">{{ config[1] }}</p></div><el-button :loading="loading" @click="load">刷新</el-button></div><el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" class="mb-4" /><el-card shadow="never"><el-skeleton v-if="loading" :rows="6" animated /><el-empty v-else-if="!rows.length" description="暂无可展示数据" /><el-table v-else :data="rows" stripe><el-table-column v-for="column in config[2]" :key="column" :prop="column" :label="column" min-width="160" show-overflow-tooltip /></el-table></el-card></main></template>
