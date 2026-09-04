<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getHealth, type HealthResponse } from "@/api/platform";

defineOptions({
  name: "Welcome"
});

const loading = ref(true);
const error = ref("");
const health = ref<HealthResponse | null>(null);

async function loadHealth() {
  loading.value = true;
  error.value = "";
  try {
    health.value = await getHealth();
  } catch (e) {
    health.value = null;
    error.value = e instanceof Error ? e.message : "健康检查接口不可用";
  } finally {
    loading.value = false;
  }
}

onMounted(loadHealth);
</script>

<template>
  <div class="p-6">
    <h1 class="text-2xl font-semibold">Ikaros Console</h1>
    <p class="mt-2 text-gray-500">平台服务概览</p>
    <el-card class="mt-6" shadow="never">
      <template #header><span>后端服务状态</span></template>
      <el-skeleton v-if="loading" :rows="2" animated />
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <el-result v-else icon="success" title="服务正常">
        <template #sub-title>{{ health?.status || "健康检查已通过" }}</template>
      </el-result>
      <el-button class="mt-4" :loading="loading" @click="loadHealth">刷新</el-button>
    </el-card>
  </div>
</template>
