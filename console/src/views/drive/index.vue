<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, unknown>;
const spaces = ref<Row[]>([]); const nodes = ref<Row[]>([]); const selected = ref<Row | null>(null); const loading = ref(false); const nodeLoading = ref(false); const error = ref("");
const used = computed(() => Number(selected.value?.usedBytes || 0)); const quota = computed(() => Number(selected.value?.quotaBytes || 0)); const percent = computed(() => quota.value ? Math.min(100, Math.round(used.value / quota.value * 100)) : 0);
async function loadSpaces() { loading.value = true; error.value = ""; try { const result = await http.get<unknown, unknown>("/drive/spaces"); spaces.value = Array.isArray(result) ? result as Row[] : []; if (spaces.value.length) await selectSpace(spaces.value[0]); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "云盘空间加载失败"; } finally { loading.value = false; } }
async function selectSpace(space: Row) { selected.value = space; nodeLoading.value = true; try { const result = await http.get<unknown, unknown>(`/drive/spaces/${space.id}/children`); nodes.value = Array.isArray(result) ? result as Row[] : []; } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "文件列表加载失败"; } finally { nodeLoading.value = false; } }
onMounted(loadSpaces);
</script>

<template>
  <main class="p-4 md:p-6"><div class="flex items-start justify-between gap-4 mb-6"><div><h1 class="text-2xl font-semibold">个人云盘</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">管理文件空间、文件夹和同步状态。</p></div><el-button :loading="loading" @click="loadSpaces">刷新</el-button></div><el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-skeleton v-if="loading && !spaces.length" :rows="6" animated />
    <template v-else><section class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6"><el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">空间数量</div><div class="text-2xl font-semibold mt-2">{{ spaces.length }}</div></el-card><el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">已使用</div><div class="text-2xl font-semibold mt-2">{{ used.toLocaleString() }} B</div></el-card><el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">空间配额</div><el-progress class="mt-3" :percentage="percent" :status="percent > 90 ? 'exception' : undefined" /></el-card></section>
      <el-card shadow="never" class="mb-4"><template #header><span class="font-medium">Drive Space</span></template><el-empty v-if="!spaces.length" description="暂无云盘空间" /><div v-else class="flex flex-wrap gap-3"><el-button v-for="space in spaces" :key="String(space.id)" :type="selected?.id === space.id ? 'primary' : 'default'" @click="selectSpace(space)">{{ space.name || space.title || space.id }}</el-button></div></el-card>
      <el-card shadow="never"><template #header><div class="flex justify-between"><span class="font-medium">根目录</span><span class="text-sm text-[var(--el-text-color-secondary)]">{{ selected?.name || '请选择空间' }}</span></div></template><el-skeleton v-if="nodeLoading" :rows="5" animated /><el-empty v-else-if="!nodes.length" description="此空间暂无文件" /><el-table v-else :data="nodes" stripe @row-click="row => row.id && $router.push(`/storage-center/drive/nodes/${row.id}`)"><el-table-column prop="name" label="名称" min-width="240" /><el-table-column prop="nodeType" label="类型" width="140" /><el-table-column prop="sizeBytes" label="大小" width="140" /><el-table-column prop="updatedAt" label="修改时间" min-width="180" /></el-table></el-card>
    </template></main>
</template>
