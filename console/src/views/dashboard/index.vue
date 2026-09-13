<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { http } from "@/utils/http";

type Row = Record<string, any>;
type WidgetState<T> = { loading: boolean; error: string; value: T };
const router = useRouter();
const environment = import.meta.env.MODE;
const refreshedAt = ref("");
const resources = ref<WidgetState<Row[]>>({ loading: false, error: "", value: [] });
const tasks = ref<WidgetState<Row[]>>({ loading: false, error: "", value: [] });
const providers = ref<WidgetState<Row[]>>({ loading: false, error: "", value: [] });
function rows(value: unknown): Row[] { if (Array.isArray(value)) return value as Row[]; const page = value as Row; return Array.isArray(page?.items) ? page.items : Array.isArray(page?.content) ? page.content : []; }
function status(row: Row) { return String(row.status || "").toUpperCase(); }
function title(row: Row) { return row.title || row.primaryTitle || row.name || "未命名内容"; }
function time(row: Row) { return row.occurredAt || row.updatedAt || row.updated_at || row.createdAt || row.created_at || "时间未知"; }
function progress(row: Row) { const value = Number(row.progress?.percent ?? row.progress?.percentage ?? row.percent ?? row.percentage); return Number.isFinite(value) ? Math.max(0, Math.min(100, Math.round(value))) : null; }
const failedTasks = computed(() => tasks.value.value.filter(row => ["FAILED", "ERROR", "TIMED_OUT"].includes(status(row))));
const inProgressTasks = computed(() => tasks.value.value.filter(row => ["PENDING", "RUNNING", "PROCESSING"].includes(status(row))));
const unhealthyProviders = computed(() => providers.value.value.filter(row => ["UNHEALTHY", "DEGRADED", "DOWN"].includes(String(row.health?.status || row.healthStatus || row.status || "").toUpperCase())));
const unavailableResources = computed(() => resources.value.value.filter(row => ["MISSING", "CORRUPTED", "RESTORE_FAILED", "UNAVAILABLE"].includes(String(row.availability || row.status || "").toUpperCase())));
const attentionItems = computed(() => [
  ...failedTasks.value.map(row => ({ key: `task-${row.id}`, level: "高", levelType: "danger" as const, heading: row.taskType || row.task_type || "后台工作失败", reason: row.errorSummary || row.error_message || "后台工作未完成", at: time(row), action: "查看工作", go: () => router.push(row.id ? `/activity/${row.id}` : "/activity") })),
  ...unhealthyProviders.value.map(row => ({ key: `provider-${row.id || row.providerKey}`, level: "高", levelType: "danger" as const, heading: row.displayName || row.providerKey || "存储 Provider", reason: "存储 Provider 需要检查", at: time(row), action: "查看 Provider", go: () => router.push(row.id ? `/storage/providers/${row.id}` : "/storage/providers") })),
  ...unavailableResources.value.map(row => ({ key: `resource-${row.id}`, level: "高", levelType: "danger" as const, heading: title(row), reason: `内容状态：${row.availability || row.status}`, at: time(row), action: "查看内容", go: () => router.push(`/library/${row.id}`) }))
]);
const storageTiers = computed(() => providers.value.value.reduce<Record<string, number>>((result, provider) => { const tier = String(provider.tier || "UNKNOWN").toUpperCase(); result[tier] = (result[tier] || 0) + 1; return result; }, {}));
async function loadWidget<T>(state: WidgetState<T>, endpoint: string, fallback: T) { state.loading = true; state.error = ""; try { state.value = rows(await http.get<unknown, unknown>(endpoint)) as T; } catch (error: any) { state.value = fallback; state.error = error?.response?.data?.detail || error?.message || "加载失败"; } finally { state.loading = false; } }
async function load() { await Promise.all([loadWidget(resources.value, "/resources", []), loadWidget(tasks.value, "/background-tasks", []), loadWidget(providers.value, "/storage/providers", [])]); refreshedAt.value = new Date().toLocaleString("zh-CN"); }
function go(path: string) { router.push(path); }
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start gap-4 mb-6"><div><h1 class="text-3xl font-semibold">概览</h1><p class="mt-2 text-[var(--el-text-color-secondary)]">{{ environment }} · 最近成功刷新：{{ refreshedAt || "尚未刷新" }}</p></div><div class="flex gap-2"><el-button @click="go('/storage/providers')">配置存储</el-button><el-button :loading="resources.loading || tasks.loading || providers.loading" @click="load">刷新</el-button></div></div>
    <el-card shadow="never" class="mb-6"><template #header><div class="flex justify-between items-center"><span class="font-medium">需要关注</span><el-button link @click="go('/activity')">查看全部工作</el-button></div></template><el-skeleton v-if="tasks.loading || providers.loading || resources.loading" :rows="3" animated /><el-alert v-else-if="tasks.error || providers.error || resources.error" title="部分状态暂时未知" description="请稍后重试；未知状态不会显示为正常。" type="warning" show-icon :closable="false" /><el-empty v-else-if="!attentionItems.length" description="暂时没有需要处理的事项" /><div v-else class="divide-y"><div v-for="item in attentionItems.slice(0, 8)" :key="item.key" class="flex items-center justify-between gap-4 py-3"><div class="min-w-0"><div class="flex items-center gap-2"><el-tag :type="item.levelType">{{ item.level }}</el-tag><span class="font-medium truncate">{{ item.heading }}</span></div><p class="mt-1 text-sm text-[var(--el-text-color-secondary)]">{{ item.reason }} · {{ item.at }}</p></div><el-button link type="primary" @click="item.go">{{ item.action }}</el-button></div></div></el-card>
    <section class="grid grid-cols-1 xl:grid-cols-2 gap-4 mb-6"><el-card shadow="never"><template #header><div class="flex justify-between items-center"><span class="font-medium">正在进行</span><el-button link @click="go('/activity')">查看全部</el-button></div></template><el-skeleton v-if="tasks.loading" :rows="3" animated /><el-alert v-else-if="tasks.error" title="后台工作状态未知" type="warning" show-icon :closable="false" /><el-empty v-else-if="!inProgressTasks.length" description="暂无正在进行的工作" /><div v-else class="divide-y"><div v-for="row in inProgressTasks.slice(0, 6)" :key="row.id" class="py-3"><div class="flex justify-between gap-3"><span class="font-medium">{{ row.taskType || row.task_type || "后台工作" }}</span><span class="text-sm">{{ progress(row) === null ? "进度未知" : `${progress(row)}%` }}</span></div><div class="mt-1 text-sm text-[var(--el-text-color-secondary)]">{{ row.resourceTitle || row.resourceName || row.payload?.resource_id || "关联内容待定" }} · {{ row.status || "未知" }}</div><el-progress v-if="progress(row) !== null" class="mt-2" :percentage="progress(row)" :show-text="false" /></div></div></el-card><el-card shadow="never"><template #header><div class="flex justify-between items-center"><span class="font-medium">内容摘要</span><el-button link @click="go('/library')">打开 Library</el-button></div></template><div class="grid grid-cols-2 gap-4"><div><div class="text-sm text-[var(--el-text-color-secondary)]">可见 Resource</div><div class="text-2xl font-semibold mt-1">{{ resources.error ? "Unknown" : resources.value.length }}</div></div><div><div class="text-sm text-[var(--el-text-color-secondary)]">不可用内容</div><div class="text-2xl font-semibold mt-1">{{ resources.error ? "Unknown" : unavailableResources.length }}</div></div></div></el-card></section>
    <el-card shadow="never"><template #header><div class="flex justify-between items-center"><span class="font-medium">Storage 摘要</span><el-button link @click="go('/storage')">打开 Storage</el-button></div></template><el-alert v-if="providers.error" title="Storage 状态未知" type="warning" show-icon :closable="false" /><div v-else class="grid grid-cols-2 md:grid-cols-6 gap-4"><div v-for="(count, tier) in storageTiers" :key="tier"><div class="text-sm text-[var(--el-text-color-secondary)]">{{ tier }}</div><div class="text-2xl font-semibold mt-1">{{ count }}</div></div><div><div class="text-sm text-[var(--el-text-color-secondary)]">Provider 异常</div><div class="text-2xl font-semibold mt-1">{{ unhealthyProviders.length }}</div></div></div></el-card>
  </main>
</template>
