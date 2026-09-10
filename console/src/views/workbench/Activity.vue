<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, unknown>;
const activeTab = ref("activity");
const loading = ref(false);
const error = ref("");
const activities = ref<Row[]>([]);
const progressRows = ref<Row[]>([]);
const progressLoading = ref(false);
const filter = ref("");
function clearLocalHistory() { localStorage.removeItem("activity-history"); activities.value = []; }
async function removeActivity(row: Row) { if (!row.id || !window.confirm("确认删除这条活动记录吗？删除动作本身仍会保留审计记录。")) return; try { await http.request("delete", `/activity/${row.id}`); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "活动删除失败"; } }

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const result = await http.get<unknown, unknown>("/activity", { params: { limit: 200 } });
    activities.value = Array.isArray(result) ? result as Row[] : [];
    progressLoading.value = true;
    const [mediaResult, readingResult] = await Promise.allSettled([http.get<unknown, unknown>("/media/playback/history"), http.get<unknown, unknown>("/reading/history")]);
    const media = mediaResult.status === "fulfilled" && Array.isArray(mediaResult.value) ? mediaResult.value as Row[] : [];
    const reading = readingResult.status === "fulfilled" && Array.isArray(readingResult.value) ? readingResult.value as Row[] : [];
    progressRows.value = [
      ...media.map(row => ({ ...row, progressType: "媒体", progressKey: row.resourceId || row.trackId || row.id })),
      ...reading.map(row => ({ ...row, progressType: "阅读", progressKey: row.workId || row.id }))
    ];
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "活动加载失败";
  } finally { progressLoading.value = false; loading.value = false; }
}
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex items-start justify-between gap-4 mb-6"><div><h1 class="text-2xl font-semibold">我的活动与收藏</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">集中查看最近操作、收藏内容和消费进度。</p></div><div class="flex gap-2"><el-button @click="clearLocalHistory">清除本地历史</el-button><el-button :loading="loading" @click="load">刷新</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never"><el-tabs v-model="activeTab">
      <el-tab-pane label="活动" name="activity"><div class="mb-4"><el-input v-model="filter" clearable placeholder="按动作或 Resource ID 筛选" /></div><el-skeleton v-if="loading" :rows="6" animated /><el-empty v-else-if="!activities.filter(row => !filter || JSON.stringify(row).toLowerCase().includes(filter.toLowerCase())).length" description="暂无活动记录" /><el-table v-else :data="activities.filter(row => !filter || JSON.stringify(row).toLowerCase().includes(filter.toLowerCase()))" stripe><el-table-column prop="type" label="活动" min-width="180" /><el-table-column prop="resourceId" label="资源" min-width="220" /><el-table-column prop="createdAt" label="时间" min-width="180" /><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="danger" @click="removeActivity(row)">删除</el-button></template></el-table-column></el-table></el-tab-pane>
      <el-tab-pane label="收藏" name="favorites"><el-empty description="暂无收藏内容" /></el-tab-pane>
      <el-tab-pane label="进度" name="progress"><el-skeleton v-if="progressLoading" :rows="5" animated /><el-empty v-else-if="!progressRows.length" description="暂无消费进度" /><template v-else><el-table class="hidden md:block" :data="progressRows" stripe><el-table-column prop="progressType" label="类型" width="100" /><el-table-column prop="progressKey" label="内容" min-width="260" /><el-table-column prop="positionSeconds" label="播放秒数" width="120" /><el-table-column prop="locatorValue" label="阅读位置" min-width="220" /><el-table-column prop="updatedAt" label="更新时间" min-width="180" /></el-table><div class="space-y-3 md:hidden"><el-card v-for="row in progressRows" :key="`${row.progressType}-${row.id}`" shadow="never"><div class="font-medium">{{ row.progressType }} · {{ row.progressKey }}</div><div class="mt-2 text-sm text-[var(--el-text-color-secondary)]">{{ row.positionSeconds != null ? `播放 ${row.positionSeconds} 秒` : `阅读位置：${row.locatorValue || '未记录'}` }}</div><div class="mt-1 text-xs text-[var(--el-text-color-secondary)]">{{ row.updatedAt || '未记录时间' }}</div></el-card></div></template></el-tab-pane>
    </el-tabs></el-card>
  </main>
</template>
