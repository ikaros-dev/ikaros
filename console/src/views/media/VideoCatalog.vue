<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { http } from "@/utils/http";

type Subject = { id: string; resourceId?: string; kind?: string; createdAt?: string; title?: string };
type Resource = { id: string; primaryTitle?: string; title?: string };
type Season = { id: string; seasonNumber?: number; name?: string };
type Episode = { id: string; resourceId?: string; episodeNumber?: number; absoluteNumber?: number };
const router = useRouter();
const rows = ref<Subject[]>([]);
const loading = ref(false);
const saving = ref(false);
const orderLoading = ref(false);
const error = ref("");
const dialog = ref(false);
const orderDialog = ref(false);
const selectedSubject = ref<Subject | null>(null);
const seasons = ref<Season[]>([]);
const episodes = ref<Episode[]>([]);
const selectedSeasonId = ref("");
const form = ref({ title: "", kind: "SERIES", locale: "zh-CN" });

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [subjects, resources] = await Promise.all([
      http.get<unknown, unknown>("/media/subjects"),
      http.get<any, any>("/resources", { params: { type: "VIDEO", page: 0, size: 100 } })
    ]);
    const resourceRows: Resource[] = Array.isArray(resources) ? resources : resources?.items || resources?.content || [];
    const titles = new Map(resourceRows.map(resource => [resource.id, resource.primaryTitle || resource.title || ""]));
    rows.value = (Array.isArray(subjects) ? subjects : []) as Subject[];
    rows.value = rows.value.map(row => ({ ...row, title: titles.get(row.resourceId || "") || "未命名视频" }));
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "视频条目加载失败";
  } finally {
    loading.value = false;
  }
}

async function create() {
  if (!form.value.title.trim()) { error.value = "视频标题不能为空"; return; }
  saving.value = true;
  error.value = "";
  try {
    const result: any = await http.post("/media/subjects", { data: { ...form.value, title: form.value.title.trim() } });
    dialog.value = false;
    form.value = { title: "", kind: "SERIES", locale: "zh-CN" };
    await load();
    if (result?.resourceId) router.push(`/resource-center/library/${result.resourceId}`);
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "视频条目创建失败";
  } finally { saving.value = false; }
}

async function openOrder(subject: Subject) {
  selectedSubject.value = subject;
  seasons.value = [];
  episodes.value = [];
  selectedSeasonId.value = "";
  orderDialog.value = true;
  orderLoading.value = true;
  error.value = "";
  try {
    const result = await http.get<unknown, unknown>(`/media/subjects/${subject.id}/seasons`);
    seasons.value = Array.isArray(result) ? result as Season[] : [];
    if (seasons.value[0]?.id) { selectedSeasonId.value = seasons.value[0].id; await loadEpisodes(); }
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "剧集季列表加载失败";
  } finally { orderLoading.value = false; }
}

async function loadEpisodes() {
  if (!selectedSubject.value || !selectedSeasonId.value) { episodes.value = []; return; }
  orderLoading.value = true;
  try {
    const result = await http.get<unknown, unknown>(`/media/subjects/${selectedSubject.value.id}/episodes`, { params: { seasonId: selectedSeasonId.value } });
    episodes.value = Array.isArray(result) ? (result as Episode[]).sort((a, b) => Number(a.episodeNumber || 0) - Number(b.episodeNumber || 0)) : [];
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "剧集列表加载失败";
  } finally { orderLoading.value = false; }
}

function moveEpisode(index: number, direction: -1 | 1) {
  const target = index + direction;
  if (target < 0 || target >= episodes.value.length) return;
  const next = [...episodes.value];
  [next[index], next[target]] = [next[target], next[index]];
  episodes.value = next;
}

async function saveOrder() {
  if (!selectedSubject.value || !selectedSeasonId.value || !episodes.value.length) return;
  orderLoading.value = true;
  try {
    await http.post(`/media/subjects/${selectedSubject.value.id}/seasons/${selectedSeasonId.value}/actions/reorder-episodes`, { data: { episodeIds: episodes.value.map(episode => episode.id) } });
    await loadEpisodes();
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "剧集顺序保存失败";
  } finally { orderLoading.value = false; }
}

onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">视频条目</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">创建并管理视频、剧集和电影条目；Resource 与播放附件生命周期保持分离。</p></div><div class="flex gap-2"><el-button type="primary" @click="dialog = true">创建视频条目</el-button><el-button :loading="loading" @click="load">刷新</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never"><el-skeleton v-if="loading" :rows="8" animated /><el-empty v-else-if="!rows.length" description="暂无视频条目；请先创建一个视频或剧集" /><el-table v-else :data="rows" stripe><el-table-column prop="title" label="标题" min-width="260" /><el-table-column prop="kind" label="类型" width="140" /><el-table-column prop="resourceId" label="Resource ID" min-width="250" /><el-table-column prop="createdAt" label="创建时间" min-width="190" /><el-table-column label="操作" width="220"><template #default="{ row }"><el-button link type="primary" @click="openOrder(row)">维护剧集顺序</el-button><el-button link :disabled="!row.resourceId" @click="router.push(`/resource-center/library/${row.resourceId}`)">打开 Resource</el-button></template></el-table-column></el-table></el-card>
    <el-dialog v-model="dialog" title="创建视频条目" width="520px"><el-form label-position="top"><el-form-item label="标题" required><el-input v-model="form.title" placeholder="例如：示例剧集" /></el-form-item><el-form-item label="类型" required><el-select v-model="form.kind" class="w-full"><el-option label="剧集" value="SERIES" /><el-option label="电影" value="MOVIE" /><el-option label="单个视频" value="VIDEO" /></el-select></el-form-item><el-form-item label="语言"><el-input v-model="form.locale" placeholder="zh-CN" /></el-form-item></el-form><template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="create">创建</el-button></template></el-dialog>
    <el-drawer v-model="orderDialog" title="维护剧集顺序" size="620px"><el-skeleton v-if="orderLoading && !episodes.length" :rows="6" animated /><template v-else><div class="flex items-center gap-3 mb-4"><span class="text-sm">Season</span><el-select v-model="selectedSeasonId" class="flex-1" placeholder="选择 Season" @change="loadEpisodes"><el-option v-for="season in seasons" :key="season.id" :label="`第 ${season.seasonNumber ?? 0} 季 · ${season.name || season.id}`" :value="season.id" /></el-select></div><el-empty v-if="!seasons.length" description="暂无 Season；先创建 Season 后才能维护剧集顺序" /><el-empty v-else-if="!episodes.length" description="该 Season 暂无剧集" /><el-table v-else :data="episodes" row-key="id"><el-table-column label="顺序" width="90"><template #default="{ row, $index }">{{ $index + 1 }}</template></el-table-column><el-table-column prop="episodeNumber" label="当前编号" width="120" /><el-table-column prop="id" label="Episode ID" min-width="230" /><el-table-column label="调整" width="120"><template #default="{ $index }"><el-button link :disabled="$index === 0" @click="moveEpisode($index, -1)">上移</el-button><el-button link :disabled="$index === episodes.length - 1" @click="moveEpisode($index, 1)">下移</el-button></template></el-table-column></el-table><div class="flex justify-end mt-4"><el-button type="primary" :loading="orderLoading" :disabled="!episodes.length" @click="saveOrder">保存顺序</el-button></div></template></el-drawer>
  </main>
</template>
