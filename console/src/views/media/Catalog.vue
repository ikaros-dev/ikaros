<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

type Row = Record<string, any>;
const route = useRoute();
const query = ref("");
const tab = ref("catalog");
const rows = ref<Row[]>([]);
const loading = ref(false);
const submitting = ref(false);
const error = ref("");
const message = ref("");
const candidateDialog = ref(false);
const candidateLoading = ref(false);
const candidates = ref<Row[]>([]);
const candidateTrackId = ref("");
const associations = ref<Row[]>([]);
const associationLoading = ref("");
const duplicateLoading = ref(false);
const duplicateRows = ref<Row[]>([]);
const correctionDialog = ref(false);
const correctionSaving = ref(false);
const correctionCandidate = ref<Row>({});
const playbackLoading = ref("");
const playbackError = ref("");
const playbackUrl = ref("");
const playbackTrack = ref<Row | null>(null);
const playbackSession = ref<Row | null>(null);
const musicForm = ref({ attachmentId: "", title: "", durationMillis: null as number | null, codec: "", container: "" });

const kind = computed(() => String(route.path.split("/").pop()));
const isMusic = computed(() => kind.value === "music");
const title = computed(() => ({ music: "音乐库", photos: "照片管理", games: "游戏档案" }[kind.value] || "媒体目录"));
const filtered = computed(() => !query.value ? rows.value : rows.value.filter(row => JSON.stringify(row).toLowerCase().includes(query.value.toLowerCase())));

async function load() {
  loading.value = true; error.value = "";
  try {
    const endpoint = isMusic.value ? "/music/imports" : kind.value === "photos" ? "/photos/timeline" : "/games";
    const result = await http.get<unknown, unknown>(endpoint);
    rows.value = Array.isArray(result) ? result as Row[] : [];
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "媒体目录加载失败"; }
  finally { loading.value = false; }
}

async function importMusic() {
  if (!musicForm.value.attachmentId.trim()) { error.value = "请输入音频 Attachment ID"; return; }
  submitting.value = true; error.value = ""; message.value = "";
  try {
    const result: any = await http.post("/music/imports", { headers: { "Idempotency-Key": crypto.randomUUID() }, data: { attachmentId: musicForm.value.attachmentId.trim(), title: musicForm.value.title.trim() || undefined, durationMillis: musicForm.value.durationMillis || undefined, codec: musicForm.value.codec.trim() || undefined, container: musicForm.value.container.trim() || undefined } });
    message.value = `音乐已导入：${result?.title || result?.trackId || "已创建"}`;
    musicForm.value = { attachmentId: "", title: "", durationMillis: null, codec: "", container: "" };
    await load();
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "音乐导入失败"; }
  finally { submitting.value = false; }
}

async function checkDuplicates() {
  if (!musicForm.value.attachmentId.trim()) { error.value = "请输入音频 Attachment ID"; return; }
  duplicateLoading.value = true; error.value = "";
  try { duplicateRows.value = await http.get<unknown, unknown>(`/music/duplicates?attachmentId=${encodeURIComponent(musicForm.value.attachmentId.trim())}`) as Row[]; }
  catch (e: any) { duplicateRows.value = []; error.value = e?.response?.data?.detail || e?.message || "重复检查失败"; }
  finally { duplicateLoading.value = false; }
}

async function recognizeMusic(row: Row) {
  if (!row.trackId) { error.value = "该导入记录没有 Track，无法识别"; return; }
  candidateTrackId.value = row.trackId; candidateDialog.value = true; candidateLoading.value = true; error.value = "";
  try {
    await http.post(`/music/tracks/${row.trackId}/metadata/recognize`);
    candidates.value = await http.get<unknown, unknown>(`/music/tracks/${row.trackId}/metadata/candidates`) as Row[];
    associations.value = await http.get<unknown, unknown>(`/music/tracks/${row.trackId}/associations`) as Row[];
    message.value = "歌曲信息识别完成，结果已持久化为候选元数据";
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "歌曲信息识别失败"; candidates.value = []; }
  finally { candidateLoading.value = false; }
}

async function associateCandidate(candidate: Row) {
  associationLoading.value = candidate.id; error.value = "";
  try {
    await http.post(`/music/tracks/${candidateTrackId.value}/associations`, { data: { candidateId: candidate.id } });
    associations.value = await http.get<unknown, unknown>(`/music/tracks/${candidateTrackId.value}/associations`) as Row[];
    message.value = "专辑与艺术家已关联，Track Membership 已持久化";
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "关联专辑与艺术家失败"; }
  finally { associationLoading.value = ""; }
}

function associated(candidateId: string) { return associations.value.some(item => item.candidateId === candidateId); }

function editCandidate(candidate: Row) { correctionCandidate.value = { ...candidate }; correctionDialog.value = true; }
async function saveCorrection() {
  correctionSaving.value = true; error.value = "";
  try {
    const c = correctionCandidate.value;
    await http.request("patch", `/music/metadata-candidates/${c.id}`, { headers: { "If-Match": String(c.version ?? 0) }, data: { title: c.title || null, artist: c.artist || null, album: c.album || null, albumArtist: c.albumArtist || null, isrc: c.isrc || null, genre: c.genre || null, trackNumber: c.trackNumber || null, discNumber: c.discNumber || null, releaseYear: c.releaseYear || null } });
    candidates.value = await http.get<unknown, unknown>(`/music/tracks/${candidateTrackId.value}/metadata/candidates`) as Row[];
    correctionDialog.value = false; message.value = "人工修正已保存，候选来源已标记为 MANUAL";
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "保存元数据修正失败"; }
  finally { correctionSaving.value = false; }
}

async function playMusic(row: Row) {
  if (!row.trackId) { playbackError.value = "该导入记录没有 Track，无法播放"; return; }
  playbackLoading.value = String(row.trackId); playbackError.value = ""; playbackUrl.value = "";
  try {
    const sources = await http.get<unknown, unknown>(`/music/tracks/${row.trackId}/audio-sources`) as Row[];
    const source = sources.find(item => String(item.availability).toUpperCase() === "AVAILABLE");
    if (!source?.id || !source.attachmentId) throw new Error("没有可用的 Audio Source");
    playbackSession.value = await http.post<unknown, unknown>(`/music/playback/tracks/${row.trackId}/sessions`, { data: { sourceId: source.id, positionMillis: 0 } }) as Row;
    const preview = await http.get<unknown, unknown>(`/attachments/${source.attachmentId}/preview-url`) as Row;
    if (!preview.url) throw new Error("服务端没有返回可播放地址");
    playbackTrack.value = row; playbackUrl.value = preview.url;
  } catch (e: any) { playbackSession.value = null; playbackError.value = e?.response?.data?.detail || e?.message || "播放歌曲失败"; }
  finally { playbackLoading.value = ""; }
}

function clearPlayback() { playbackUrl.value = ""; playbackTrack.value = null; playbackSession.value = null; }

onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">{{ title }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">资源、附件和技术元数据分层管理，敏感内容遵循授权边界。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" class="mb-4"/><el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4"/>
    <template v-if="isMusic">
      <el-card shadow="never" class="mb-4"><template #header><span>导入音乐附件</span></template><el-form label-position="top" class="max-w-3xl"><el-form-item label="音频 Attachment ID" required><el-input v-model="musicForm.attachmentId" placeholder="先在附件与存储上传音频，再粘贴 Attachment ID" clearable/></el-form-item><div class="grid grid-cols-1 md:grid-cols-2 gap-4"><el-form-item label="歌曲标题"><el-input v-model="musicForm.title" placeholder="留空使用附件文件名"/></el-form-item><el-form-item label="时长（毫秒）"><el-input-number v-model="musicForm.durationMillis" :min="0" controls-position="right" class="w-full"/></el-form-item><el-form-item label="编码"><el-input v-model="musicForm.codec" placeholder="如 MP3、FLAC"/></el-form-item><el-form-item label="容器"><el-input v-model="musicForm.container" placeholder="如 MPEG、OGG"/></el-form-item></div><div class="flex gap-2"><el-button :loading="duplicateLoading" @click="checkDuplicates">检查重复</el-button><el-button type="primary" :loading="submitting" @click="importMusic">导入音乐</el-button></div></el-form><el-alert v-if="duplicateRows.length" title="发现相同内容的已入库歌曲，导入前请确认是否复用。" type="warning" show-icon :closable="false" class="mt-4"/><el-table v-if="duplicateRows.length" :data="duplicateRows" stripe class="mt-3"><el-table-column prop="title" label="已存在歌曲"/><el-table-column prop="trackId" label="Track ID" min-width="280"/><el-table-column prop="attachmentId" label="附件 ID" min-width="280"/></el-table><el-empty v-else-if="!duplicateLoading && musicForm.attachmentId" description="未发现相同内容的音乐" :image-size="50" class="py-3"/></el-card>
      <el-alert v-if="playbackError" :title="playbackError" type="warning" show-icon :closable="false" class="mb-4"/><el-card v-if="playbackUrl" shadow="never" class="mb-4"><template #header><div class="flex justify-between items-center"><span>正在播放：{{ playbackTrack?.title || playbackTrack?.trackId }}</span><el-button link @click="clearPlayback">关闭</el-button></div></template><audio :src="playbackUrl" controls autoplay class="w-full" @error="playbackError = '音频加载失败，请检查附件可用状态'"/><div class="text-xs text-[var(--el-text-color-secondary)] mt-2">播放会话：{{ playbackSession?.id || "—" }} · 来源：{{ playbackSession?.sourceId || "—" }}</div></el-card><el-card shadow="never"><template #header><div class="flex justify-between"><span>音乐导入记录</span><el-input v-model="query" clearable placeholder="搜索标题或 Track ID" class="w-64"/></div></template><el-skeleton v-if="loading" :rows="5" animated/><el-empty v-else-if="!filtered.length" description="暂无音乐导入记录"/><el-table v-else :data="filtered" stripe @row-click="playMusic"><el-table-column prop="title" label="歌曲" min-width="220"/><el-table-column prop="attachmentId" label="源附件" min-width="280"/><el-table-column prop="trackId" label="Track ID" min-width="280"/><el-table-column prop="durationMillis" label="时长（毫秒）" width="140"/><el-table-column prop="status" label="状态" width="120"/><el-table-column prop="errorMessage" label="错误" min-width="220"/><el-table-column prop="createdAt" label="导入时间" min-width="180"/><el-table-column label="操作" width="130" fixed="right"><template #default="scope"><el-button link type="primary" @click.stop="recognizeMusic(scope.row)">识别信息</el-button></template></el-table-column></el-table></el-card>
      <el-dialog v-model="candidateDialog" title="歌曲信息候选" width="860px"><el-skeleton v-if="candidateLoading" :rows="5" animated/><el-empty v-else-if="!candidates.length" description="未识别到嵌入标签"/><el-table v-else :data="candidates" stripe><el-table-column prop="source" label="来源" width="100"/><el-table-column prop="title" label="标题" min-width="160"/><el-table-column prop="artist" label="艺术家" min-width="140"/><el-table-column prop="album" label="专辑" min-width="140"/><el-table-column prop="trackNumber" label="曲目号" width="90"/><el-table-column prop="releaseYear" label="年份" width="90"/><el-table-column label="操作" width="180"><template #default="scope"><el-button link type="primary" @click="editCandidate(scope.row)">修正</el-button><el-button v-if="associated(scope.row.id)" link type="success" disabled>已关联</el-button><el-button v-else link type="primary" :loading="associationLoading === scope.row.id" @click="associateCandidate(scope.row)">关联</el-button></template></el-table-column></el-table></el-dialog>
      <el-dialog v-model="correctionDialog" title="修正歌曲信息" width="560px"><el-form label-position="top"><div class="grid grid-cols-1 md:grid-cols-2 gap-3"><el-form-item label="标题"><el-input v-model="correctionCandidate.title"/></el-form-item><el-form-item label="艺术家"><el-input v-model="correctionCandidate.artist"/></el-form-item><el-form-item label="专辑"><el-input v-model="correctionCandidate.album"/></el-form-item><el-form-item label="专辑艺术家"><el-input v-model="correctionCandidate.albumArtist"/></el-form-item><el-form-item label="曲目号"><el-input-number v-model="correctionCandidate.trackNumber" :min="1" class="w-full"/></el-form-item><el-form-item label="碟号"><el-input-number v-model="correctionCandidate.discNumber" :min="1" class="w-full"/></el-form-item><el-form-item label="流派"><el-input v-model="correctionCandidate.genre"/></el-form-item><el-form-item label="发行年份"><el-input v-model="correctionCandidate.releaseYear"/></el-form-item></div></el-form><template #footer><el-button @click="correctionDialog=false">取消</el-button><el-button type="primary" :loading="correctionSaving" @click="saveCorrection">保存修正</el-button></template></el-dialog>
    </template>
    <template v-else><el-tabs v-model="tab"><el-tab-pane label="目录" name="catalog"/><el-tab-pane label="元数据" name="metadata"/><el-tab-pane label="播放 / 时间线" name="activity"/></el-tabs><el-card shadow="never"><template #header><div class="flex justify-between"><span>{{ title }}目录</span><el-input v-model="query" clearable placeholder="搜索标题或资源 ID" class="w-64"/></div></template><el-skeleton v-if="loading" :rows="5" animated/><el-empty v-else-if="!filtered.length" description="暂无可展示条目；其他媒体目录接口尚未接入"/><el-table v-else :data="filtered" stripe><el-table-column prop="id" label="ID" min-width="240"/><el-table-column prop="name" label="名称" min-width="200"/><el-table-column prop="status" label="状态" width="140"/><el-table-column prop="createdAt" label="创建时间" min-width="180"/></el-table><el-alert title="附件物理存储请在“附件与存储”模块管理。" type="info" show-icon :closable="false" class="mt-4"/></el-card></template>
  </main>
</template>
