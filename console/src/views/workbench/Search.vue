<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { http } from "@/utils/http";

type SearchItem = { resourceId: string; sourceVersion: number; fields: Record<string, unknown> };
type SearchPage = { items: SearchItem[]; nextCursor: string | null };
type ProjectionFailure = { id: string; sourceId: string; sourceVersion: number; reason?: string; failedAt?: string; resolvedAt?: string };

const route = useRoute();
const router = useRouter();
const query = ref("");
const kind = ref("");
const tag = ref("");
const nextCursor = ref<string | null>(null);
const loading = ref(false);
const searched = ref(false);
const error = ref("");
const results = ref<SearchItem[]>([]);
const failures = ref<ProjectionFailure[]>([]);
const rebuildLoading = ref(false);
const failureLoading = ref(false);

function displayField(item: SearchItem, key: string, fallback = "—") {
  const value = item.fields[key];
  if (Array.isArray(value)) return value.join(", ");
  return value == null || value === "" ? fallback : String(value);
}

async function search(cursor?: string | null) {
  if (!query.value.trim()) {
    results.value = [];
    nextCursor.value = null;
    searched.value = false;
    return;
  }
  loading.value = true;
  searched.value = true;
  error.value = "";
  await router.replace({ query: { ...route.query, q: query.value.trim(), type: kind.value || undefined, tag: tag.value.trim() || undefined } });
  try {
    const page = await http.get<SearchPage, SearchPage>("/search", {
      params: { q: query.value.trim(), cursor: cursor || undefined, type: kind.value || undefined, tag: tag.value.trim() || undefined, limit: 20 }
    });
    results.value = page?.items || [];
    nextCursor.value = page?.nextCursor || null;
  } catch (e: any) {
    results.value = [];
    nextCursor.value = null;
    error.value = e?.response?.data?.detail || e?.message || "搜索失败";
  } finally {
    loading.value = false;
  }
}

function resetFilters() { kind.value = ""; tag.value = ""; search(); }

async function loadFailures() {
  failureLoading.value = true;
  try {
    const result = await http.get<unknown, unknown>("/admin/search/projection-failures");
    failures.value = Array.isArray(result) ? result as ProjectionFailure[] : [];
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "搜索投影失败列表加载失败";
  } finally {
    failureLoading.value = false;
  }
}

async function rebuildIndex() {
  rebuildLoading.value = true;
  error.value = "";
  try {
    await http.post("/admin/search/rebuild", { data: { projector_version: "resource-v1" } });
    await loadFailures();
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "搜索索引重建失败";
  } finally {
    rebuildLoading.value = false;
  }
}

async function retryFailure(row: ProjectionFailure) {
  failureLoading.value = true;
  error.value = "";
  try {
    await http.post(`/admin/search/projection-failures/${row.id}/retry`);
    await loadFailures();
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "搜索投影重试失败";
  } finally {
    failureLoading.value = false;
  }
}

onMounted(() => {
  if (typeof route.query.q === "string") query.value = route.query.q;
  if (typeof route.query.type === "string") kind.value = route.query.type;
  if (typeof route.query.tag === "string") tag.value = route.query.tag;
  if (query.value) search();
  loadFailures();
});
</script>

<template>
  <main class="p-4 md:p-6">
    <section class="mb-6"><h1 class="text-2xl font-semibold">全局搜索</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">搜索当前账号有权访问的资源和内容。</p></section>
    <el-card shadow="never" class="mb-4">
      <el-form @submit.prevent="search()"><el-input v-model="query" size="large" clearable autofocus placeholder="搜索标题、别名或资源内容" @keyup.enter="search()"><template #prefix>⌕</template><template #append><el-button type="primary" :loading="loading" @click="search()">搜索</el-button></template></el-input></el-form>
      <div class="flex flex-wrap items-center gap-2 mt-4"><el-tag v-for="item in ['', 'VIDEO', 'BOOK', 'MUSIC', 'PHOTO', 'DOCUMENT']" :key="item || 'ALL'" :type="kind === item ? 'primary' : 'info'" class="cursor-pointer" @click="kind = item; search()">{{ item || "全部类型" }}</el-tag><el-input v-model="tag" clearable placeholder="标签过滤" class="w-44" @keyup.enter="search()"/><el-button link @click="resetFilters">清除筛选</el-button></div>
    </el-card>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never"><template #header><div class="flex justify-between"><span>{{ searched ? `${results.length} 条结果` : "搜索结果" }}</span><el-button v-if="nextCursor" size="small" :loading="loading" @click="search(nextCursor)">下一页</el-button></div></template>
      <el-skeleton v-if="loading" :rows="6" animated /><el-empty v-else-if="searched && !results.length" description="没有找到匹配结果" /><el-empty v-else-if="!searched" description="输入关键词开始搜索" /><el-table v-else :data="results" stripe><el-table-column label="标题" min-width="220"><template #default="{ row }">{{ displayField(row, "title", displayField(row, "type")) }}</template></el-table-column><el-table-column label="类型" width="140"><template #default="{ row }">{{ displayField(row, "type") }}</template></el-table-column><el-table-column label="标签" min-width="180"><template #default="{ row }">{{ displayField(row, "tags") }}</template></el-table-column><el-table-column label="Resource ID" min-width="280"><template #default="{ row }">{{ row.resourceId }}</template></el-table-column></el-table>
    </el-card>
    <el-card shadow="never" class="mt-4"><template #header><div class="flex justify-between items-center"><span>搜索索引运维</span><div class="flex gap-2"><el-button :loading="failureLoading" @click="loadFailures">刷新失败项</el-button><el-button type="primary" :loading="rebuildLoading" @click="rebuildIndex">重建并切换索引</el-button></div></div></template><el-alert title="重建使用当前 Resource 业务投影生成新 generation；失败项不会覆盖已有新版本投影。" type="info" show-icon :closable="false" class="mb-4"/><el-empty v-if="!failures.length" description="暂无待重试的投影失败"/><el-table v-else :data="failures" stripe><el-table-column prop="sourceId" label="Resource ID" min-width="280"/><el-table-column prop="sourceVersion" label="来源版本" width="100"/><el-table-column prop="reason" label="失败原因" min-width="260"/><el-table-column prop="failedAt" label="失败时间" min-width="180"/><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="warning" :loading="failureLoading" @click="retryFailure(row)">重试</el-button></template></el-table-column></el-table></el-card>
  </main>
</template>
