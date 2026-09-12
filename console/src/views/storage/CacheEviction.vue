<script setup lang="ts">
import { ref } from "vue";
import { http } from "@/utils/http";

type Result = { deviceId: string; evictedCount: number; evictedBytes: number; protectedDownloadCount: number };
const deviceId = ref(""); const result = ref<Result | null>(null); const loading = ref(false); const error = ref(""); const message = ref("");
function formatBytes(value: number) { const units = ["Bytes", "KiB", "MiB", "GiB", "TiB"]; let size = Math.max(0, value); let index = 0; while (size >= 1024 && index < units.length - 1) { size /= 1024; index++; } return `${size.toFixed(index ? 2 : 0)} ${units[index]}`; }
async function evict() { if (!deviceId.value.trim() || !window.confirm("确认清理该设备的可淘汰自动缓存？明确下载会被保护，不会删除 Resource 或 Blob。")) return; loading.value = true; error.value = ""; message.value = ""; try { result.value = await http.post<unknown, unknown>(`/offline/cache/evict-eligible?deviceId=${encodeURIComponent(deviceId.value.trim())}`) as Result; message.value = "可淘汰缓存清理完成，结果已从服务端返回"; } catch (e: any) { result.value = null; error.value = e?.response?.data?.detail || e?.message || "缓存清理失败"; } finally { loading.value = false; } }
</script>

<template>
  <main class="p-4 md:p-6"><div class="flex items-start justify-between gap-4 mb-6"><div><h1 class="text-2xl font-semibold">清理可淘汰缓存</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">只清理自动缓存；仍存在的明确下载目标会被保护。</p></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4"/><el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4"/>
    <el-card shadow="never" class="mb-4"><template #header><span>执行清理</span></template><el-form inline @submit.prevent="evict"><el-form-item label="Device ID" required><el-input v-model="deviceId" placeholder="设备 UUID" clearable/></el-form-item><el-button type="danger" :loading="loading" @click="evict">清理可淘汰缓存</el-button></el-form><p class="mt-2 text-sm text-[var(--el-text-color-secondary)]">服务端按 ACTIVE Cache Entry 与未移除的 DOWNLOAD Intent 比对后执行，避免清理用户明确下载。</p></el-card>
    <el-card v-if="result" shadow="never"><template #header><span>清理结果</span></template><el-descriptions :column="1" border><el-descriptions-item label="设备">{{ result.deviceId }}</el-descriptions-item><el-descriptions-item label="清理对象">{{ result.evictedCount }} 个</el-descriptions-item><el-descriptions-item label="释放空间">{{ formatBytes(result.evictedBytes) }}（{{ result.evictedBytes.toLocaleString() }} Bytes）</el-descriptions-item><el-descriptions-item label="保护的明确下载">{{ result.protectedDownloadCount }} 个目标</el-descriptions-item></el-descriptions></el-card>
  </main>
</template>
