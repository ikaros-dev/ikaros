<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

type Row = Record<string, unknown>;
const route = useRoute(); const revisions = ref<Row[]>([]); const loading = ref(false); const error = ref("");
async function load() { loading.value = true; error.value = ""; try { const result = await http.get<unknown, unknown>(`/drive/nodes/${route.params.nodeId}/revisions`); revisions.value = Array.isArray(result) ? result as Row[] : []; } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "版本历史加载失败"; } finally { loading.value = false; } }
onMounted(load);
</script>
<template>
  <main class="p-4 md:p-6"><div class="flex items-start justify-between gap-4 mb-6"><div><p class="text-sm text-[var(--el-text-color-secondary)]">个人云盘 / 文件详情</p><h1 class="mt-2 text-2xl font-semibold">{{ route.params.nodeId }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">查看文件当前版本、修订历史和引用关系。</p></div><div class="flex gap-2"><el-button @click="$router.back()">返回</el-button><el-button :loading="loading" @click="load">刷新</el-button></div></div><el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" /><section class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6"><el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">文件类型</div><div class="mt-2 font-medium">Drive File</div></el-card><el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">修订版本</div><div class="mt-2 text-2xl font-semibold">{{ revisions.length }}</div></el-card><el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">当前状态</div><div class="mt-2"><el-tag type="success">可用</el-tag></div></el-card></section><el-card shadow="never"><template #header><span class="font-medium">Revision History</span></template><el-skeleton v-if="loading" :rows="6" animated /><el-empty v-else-if="!revisions.length" description="暂无修订历史" /><el-table v-else :data="revisions" stripe><el-table-column prop="revision" label="版本" width="120" /><el-table-column prop="createdAt" label="创建时间" min-width="180" /><el-table-column prop="source" label="来源" min-width="160" /><el-table-column prop="logicalSize" label="逻辑大小" width="140" /><el-table-column prop="attachmentId" label="附件" min-width="220" /><el-table-column prop="integrityState" label="完整性" width="140" /></el-table></el-card></main>
</template>
