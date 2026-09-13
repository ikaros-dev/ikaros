<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { http } from "@/utils/http";

type Check = { name: string; status: string; detail: string };
const checks = ref<Check[]>([]);
const loading = ref(false);
const error = ref("");
const refresh = ref("off");
const metricRange = ref("1h");
const lastChecked = ref("");
const router = useRouter();
let refreshTimer: number | undefined;

async function load() {
  loading.value = true;
  error.value = "";
  checks.value = [];
  try {
    const [live, ready, providers, operations] = await Promise.all([
      http.get<any, any>("/health/live"),
      http.get<any, any>("/health/ready"),
      http.get<any, any>("/storage/providers"),
      http.get<any, any>("/health/operations")
    ]);
    const providerRows = Array.isArray(providers) ? providers : [];
    const providerResults = await Promise.allSettled(providerRows.map((provider: any) => http.get<any, any>(`/admin/storage-providers/${provider.id}/status`)));
    const failedTasks = Number(operations?.failedTasks || 0);
    const timedOutTasks = Number(operations?.timedOutTasks || 0);
    const pendingEvents = Number(operations?.durableEventDelivery?.pendingCount || 0);
    checks.value = [
      { name: "Application / API", status: String(live?.status || "UNKNOWN").toUpperCase(), detail: "应用存活探针" },
      { name: "Application Readiness / PostgreSQL", status: String(ready?.status || "UNKNOWN").toUpperCase(), detail: "应用就绪及数据库探针" },
      { name: "Background Task Queue", status: failedTasks || timedOutTasks ? "DEGRADED" : "UP", detail: `待处理 ${operations?.pendingTasks || 0} · 运行中 ${operations?.runningTasks || 0} · 失败 ${failedTasks} · 超时 ${timedOutTasks}` },
      { name: "Durable Event Delivery", status: pendingEvents ? "DEGRADED" : "UP", detail: `待投递 ${pendingEvents} · 已尝试未完成 ${operations?.durableEventDelivery?.attemptedPendingCount || 0} · 最近尝试 ${operations?.durableEventDelivery?.lastAttemptAt || "—"}` },
      ...providerRows.map((provider: any, index: number) => {
        const result: any = providerResults[index];
        const value = result.status === "fulfilled" ? result.value : null;
        return { name: `Storage / ${provider.providerKey || provider.id}`, status: String(value?.health?.status || value?.providerStatus || provider?.status || "UNKNOWN").toUpperCase(), detail: result.status === "fulfilled" ? `最近检查：${value?.checkedAt || "—"}` : "存储状态探针请求失败" };
      })
    ];
    lastChecked.value = new Date().toLocaleString();
  } catch (e: any) {
    checks.value = [{ name: "Application Readiness", status: "DOWN", detail: "健康探针请求失败" }];
    error.value = e?.response?.data?.detail || e?.message || "健康检查失败";
  } finally {
    loading.value = false;
  }
}

function openAction(name: string) {
  const path = name === "Background Task Queue" ? "/activity" : name === "Durable Event Delivery" ? "/system/notifications" : name.startsWith("Storage /") ? "/storage/providers" : "";
  if (path) router.push(path);
}

function actionLabel(name: string) {
  return name === "Background Task Queue" ? "查看后台任务" : name === "Durable Event Delivery" ? "查看通知投递" : name.startsWith("Storage /") ? "查看存储 Provider" : "暂无操作";
}

function hasAction(name: string) {
  return name === "Background Task Queue" || name === "Durable Event Delivery" || name.startsWith("Storage /");
}

function scheduleRefresh() {
  if (refreshTimer !== undefined) window.clearInterval(refreshTimer);
  const seconds = Number(refresh.value);
  if (seconds > 0) refreshTimer = window.setInterval(load, seconds * 1000);
}

watch(refresh, scheduleRefresh);
onMounted(() => { load(); scheduleRefresh(); });
onBeforeUnmount(() => { if (refreshTimer !== undefined) window.clearInterval(refreshTimer); });
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">系统健康与告警</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">查看后端定义的健康状态和组件探针结果。</p></div><div class="flex gap-2"><el-tag>开发环境</el-tag><el-select v-model="refresh" class="w-32"><el-option label="自动刷新：关闭" value="off"/><el-option label="30 秒" value="30"/><el-option label="1 分钟" value="60"/><el-option label="5 分钟" value="300"/></el-select><el-button :loading="loading" @click="load">刷新</el-button></div></div>
    <el-alert :title="checks.some(item => item.status === 'DOWN') ? '后端报告异常' : '后端健康状态已返回'" :type="checks.some(item => item.status === 'DOWN') ? 'error' : 'success'" show-icon :closable="false" class="mb-2" />
    <div class="flex justify-between items-center mb-5 text-sm text-[var(--el-text-color-secondary)]"><span>整体状态由后端健康模型计算，前端不根据单个指标推导。</span><span>最近检查：{{ lastChecked || '—' }}</span></div>
    <el-skeleton v-if="loading" :rows="8" animated />
    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <el-card v-for="item in checks" :key="item.name" shadow="never"><div class="flex justify-between"><span class="font-medium">{{ item.name }}</span><el-tag :type="item.status === 'UP' ? 'success' : item.status === 'UNKNOWN' ? 'info' : 'danger'">{{ item.status }}</el-tag></div><p class="text-sm text-[var(--el-text-color-secondary)] mt-3">{{ item.detail }}</p><div class="text-xs text-[var(--el-text-color-secondary)] mt-4">响应延迟：— · Incident：—</div><el-button link class="mt-2" :disabled="!hasAction(item.name)" @click="openAction(item.name)">{{ actionLabel(item.name) }}</el-button></el-card>
      <el-card shadow="never"><template #header><div class="flex justify-between"><span>资源指标</span><el-radio-group v-model="metricRange" size="small"><el-radio-button label="15m">15m</el-radio-button><el-radio-button label="1h">1h</el-radio-button><el-radio-button label="6h">6h</el-radio-button><el-radio-button label="24h">24h</el-radio-button><el-radio-button label="7d">7d</el-radio-button></el-radio-group></div></template><el-empty description="暂无 CPU、Memory、Heap、Disk、DB Pool、Queue Sample" :image-size="70" /><div class="text-xs text-[var(--el-text-color-secondary)]">单位与时间范围：{{ metricRange }}；缺失采样保持 Gap。</div></el-card>
      <el-card shadow="never"><template #header>告警面板</template><el-table :data="[]"><el-table-column label="Severity" /><el-table-column label="Alert Name" /><el-table-column label="Component" /><el-table-column label="Opened Time" /><el-table-column label="State" /></el-table><el-empty description="暂无告警数据" :image-size="70" /></el-card>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mt-4" />
  </main>
</template>
