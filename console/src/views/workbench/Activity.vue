<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, unknown>;
const activeTab = ref("activity");
const loading = ref(false);
const error = ref("");
const activities = ref<Row[]>([]);

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const result = await http.get<unknown, unknown>("/activity");
    activities.value = Array.isArray(result) ? result as Row[] : [];
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "活动加载失败";
  } finally { loading.value = false; }
}
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex items-start justify-between gap-4 mb-6"><div><h1 class="text-2xl font-semibold">我的活动与收藏</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">集中查看最近操作、收藏内容和消费进度。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never"><el-tabs v-model="activeTab">
      <el-tab-pane label="活动" name="activity"><el-skeleton v-if="loading" :rows="6" animated /><el-empty v-else-if="!activities.length" description="暂无活动记录" /><el-table v-else :data="activities" stripe><el-table-column prop="type" label="活动" min-width="180" /><el-table-column prop="resourceId" label="资源" min-width="220" /><el-table-column prop="createdAt" label="时间" min-width="180" /></el-table></el-tab-pane>
      <el-tab-pane label="收藏" name="favorites"><el-empty description="暂无收藏内容" /></el-tab-pane>
      <el-tab-pane label="进度" name="progress"><el-empty description="暂无进行中的内容" /></el-tab-pane>
    </el-tabs></el-card>
  </main>
</template>
