<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { http } from "@/utils/http";

type Row = Record<string, unknown>;
const query = ref("");
const kind = ref("全部");
const loading = ref(false);
const searched = ref(false);
const error = ref("");
const results = ref<Row[]>([]);
const route = useRoute(); const router = useRouter(); const sort = ref("相关度"); const elapsed = ref(0);

async function search() {
  loading.value = true; searched.value = true; error.value = ""; const started = performance.now(); await router.replace({ query: { ...route.query, q: query.value || undefined } });
  try {
    const result = await http.get<unknown, unknown>("/resources", { params: { q: query.value || undefined, type: kind.value === "全部" ? undefined : kind.value } });
    results.value = Array.isArray(result) ? result as Row[] : [];
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "搜索失败";
  } finally { elapsed.value = Math.round(performance.now() - started); loading.value = false; }
}
onMounted(() => { if (typeof route.query.q === "string") { query.value = route.query.q; search(); } });
</script>

<template>
  <main class="p-4 md:p-6">
    <section class="mb-6"><h1 class="text-2xl font-semibold">全局搜索</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">搜索当前账号有权访问的资源和内容。</p></section>
    <el-card shadow="never" class="mb-4"><el-form @submit.prevent="search"><el-input v-model="query" size="large" clearable autofocus placeholder="搜索标题、别名或资源内容" @keyup.enter="search"><template #prefix>⌕</template><template #append><el-button type="primary" :loading="loading" @click="search">搜索</el-button></template></el-input></el-form><div class="flex gap-2 mt-4"><el-tag v-for="item in ['全部', 'VIDEO', 'BOOK', 'MUSIC', 'PHOTO', 'DOCUMENT']" :key="item" :type="kind === item ? 'primary' : 'info'" class="cursor-pointer" @click="kind = item">{{ item }}</el-tag></div></el-card>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never"><template #header><div class="flex justify-between"><span>{{ searched ? `${results.length} 条结果，用时 ${elapsed} ms` : '搜索结果' }}</span><el-select v-if="searched" v-model="sort" size="small" class="w-32"><el-option v-for="item in ['相关度', '更新时间', '创建时间', '标题']" :key="item" :label="item" :value="item" /></el-select></div></template><el-skeleton v-if="loading" :rows="6" animated /><el-empty v-else-if="searched && !results.length" description="没有找到匹配结果" /><el-empty v-else-if="!searched" description="输入关键词开始搜索" /><el-table v-else :data="results" stripe><el-table-column prop="title" label="标题" min-width="220" /><el-table-column prop="resourceType" label="类型" width="140" /><el-table-column prop="status" label="状态" width="140" /><el-table-column prop="updatedAt" label="更新时间" min-width="180" /></el-table></el-card>
  </main>
</template>
