<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { http } from "@/utils/http";

type AuditEvent = { id: string; actorType?: string; actorId?: string; action?: string; targetType?: string; targetId?: string; details?: string; occurredAt?: string; requestId?: string; correlationId?: string };
const tab = ref("audit");
const query = ref("");
const range = ref("7d");
const actorId = ref("");
const rows = ref<AuditEvent[]>([]);
const loading = ref(false);
const error = ref("");
const detail = ref<AuditEvent | null>(null);
const detailOpen = ref(false);
const filtered = computed(() => rows.value.filter(item => !query.value || [item.action, item.actorId, item.targetId, item.targetType].some(value => String(value || "").toLowerCase().includes(query.value.toLowerCase()))));
function bounds() { const to = new Date(); const from = new Date(to.getTime() - (range.value === "30d" ? 30 : 7) * 86400000); return { from: from.toISOString(), to: to.toISOString() }; }
async function load() {
  loading.value = true; error.value = "";
  try {
    const result: any = await http.get("/audit-events", { params: { ...bounds(), actor_id: actorId.value || undefined, page: 0, size: 100 } });
    rows.value = Array.isArray(result) ? result : (result?.content || result?.items || []);
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "审计日志加载失败"; }
  finally { loading.value = false; }
}
async function open(item: AuditEvent) {
  try { detail.value = await http.get(`/audit-events/${item.id}`); detailOpen.value = true; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "审计详情加载失败"; }
}
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">操作审计与安全事件</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">只展示服务端按权限过滤并脱敏后的审计元数据。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-tabs v-model="tab"><el-tab-pane label="审计日志" name="audit" /><el-tab-pane label="安全事件" name="security" /></el-tabs>
    <div class="flex flex-wrap gap-3 mb-4"><el-input v-model="query" clearable placeholder="搜索操作、Actor 或资源 ID" class="max-w-sm" /><el-input v-model="actorId" clearable placeholder="按 Actor UUID 筛选" class="max-w-sm" @keyup.enter="load" /><el-select v-model="range" class="w-32" @change="load"><el-option label="近 7 天" value="7d" /><el-option label="近 30 天" value="30d" /></el-select><el-button @click="load">筛选</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-skeleton v-if="loading" :rows="8" animated /><el-card v-else shadow="never"><el-empty v-if="!filtered.length" description="暂无匹配审计事件" /><el-table v-else :data="filtered" stripe @row-click="open"><el-table-column prop="occurredAt" label="时间" min-width="190" /><el-table-column prop="action" label="操作" min-width="240" /><el-table-column prop="actorType" label="Actor 类型" width="130" /><el-table-column prop="actorId" label="Actor" min-width="230" /><el-table-column label="关联对象" min-width="230"><template #default="{ row }">{{ row.targetType || "—" }} / {{ row.targetId || "—" }}</template></el-table-column><el-table-column prop="requestId" label="Request ID" min-width="220" /></el-table></el-card>
    <el-drawer v-model="detailOpen" title="审计事件详情" size="520px"><template v-if="detail"><el-descriptions :column="1" border><el-descriptions-item label="Event ID">{{ detail.id }}</el-descriptions-item><el-descriptions-item label="操作">{{ detail.action }}</el-descriptions-item><el-descriptions-item label="Actor">{{ detail.actorType }} / {{ detail.actorId }}</el-descriptions-item><el-descriptions-item label="关联对象">{{ detail.targetType }} / {{ detail.targetId }}</el-descriptions-item><el-descriptions-item label="发生时间">{{ detail.occurredAt }}</el-descriptions-item><el-descriptions-item label="Request ID">{{ detail.requestId || "—" }}</el-descriptions-item><el-descriptions-item label="Correlation ID">{{ detail.correlationId || "—" }}</el-descriptions-item><el-descriptions-item label="详情">{{ detail.details || "—" }}</el-descriptions-item></el-descriptions></template></el-drawer>
  </main>
</template>
