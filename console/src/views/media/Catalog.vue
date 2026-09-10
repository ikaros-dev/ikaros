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
const activeSessions = ref<Row[]>([]);
const sessionLoading = ref(false);
const queueId = ref("");
const queueTrackIds = ref("");
const queueEntries = ref<Row[]>([]);
const queueLoading = ref(false);
const queueSaving = ref(false);
const queueVersion = ref("0");
const queueOrderText = ref("");
const queueRepeatMode = ref("OFF");
const queueShuffleEnabled = ref(false);
const lyricsTrackId = ref("");
const lyricsRows = ref<Row[]>([]);
const lyricsLoading = ref(false);
const playlists = ref<Row[]>([]);
const playlistLoading = ref(false);
const playlistSaving = ref(false);
const playlistForm = ref({ id: "", name: "", description: "", version: "0" });
const playlistEntries = ref<Row[]>([]);
const playlistEntryLoading = ref(false);
const playlistEntrySaving = ref(false);
const playlistTrackId = ref("");
const musicForm = ref({ attachmentId: "", title: "", durationMillis: null as number | null, codec: "", container: "" });

const kind = computed(() => String(route.path.split("/").pop()));
const isMusic = computed(() => kind.value === "music");
const isPhotos = computed(() => kind.value === "photos");
const title = computed(() => ({ music: "音乐库", photos: "照片管理", games: "游戏档案" }[kind.value] || "媒体目录"));
const filtered = computed(() => !query.value ? rows.value : rows.value.filter(row => JSON.stringify(row).toLowerCase().includes(query.value.toLowerCase())));
const photoForm = ref({ attachmentId: "", title: "", locale: "zh-CN" });
const photoSubmitting = ref(false);
const photoDetail = ref<Row | null>(null);
const photoDrawerVisible = ref(false);
const photoAssets = ref<Row[]>([]);
const photoDetailLoading = ref(false);
const photoPreviewUrl = ref("");
const photoThumbnailLoading = ref(false);
const photoThumbnailTask = ref("");
const photoMetadataForm = ref({ captureTime: "", captureTimeLocal: "", timeZone: "", width: null as number | null, height: null as number | null, orientation: null as number | null, cameraMake: "", cameraModel: "", lensModel: "", latitude: "", longitude: "" });

async function load() {
  loading.value = true; error.value = "";
  try {
    const endpoint = isMusic.value ? "/music/imports" : kind.value === "photos" ? "/photos/timeline" : "/games";
    const result = await http.get<unknown, unknown>(endpoint);
    rows.value = Array.isArray(result) ? result as Row[] : [];
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "媒体目录加载失败"; }
  finally { loading.value = false; }
}

async function createPhoto() {
  if (!photoForm.value.attachmentId.trim() || !photoForm.value.title.trim()) { error.value = "图片 Attachment ID 和标题不能为空"; return; }
  photoSubmitting.value = true; error.value = ""; message.value = "";
  try {
    const result: any = await http.post("/photos", { data: { attachmentId: photoForm.value.attachmentId.trim(), title: photoForm.value.title.trim(), locale: photoForm.value.locale || "zh-CN" } });
    photoForm.value = { attachmentId: "", title: "", locale: "zh-CN" };
    message.value = `图片已登记：${result?.id || "已创建"}`;
    await load();
    if (result?.id) await openPhoto(result);
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "图片登记失败"; }
  finally { photoSubmitting.value = false; }
}

async function openPhoto(row: Row) {
  if (!row?.id) return;
  photoDrawerVisible.value = true;
  photoDetailLoading.value = true; error.value = ""; photoPreviewUrl.value = "";
  try {
    const [metadata, assets] = await Promise.all([http.get(`/photos/${row.id}/metadata`), http.get(`/photos/${row.id}/assets`)]);
    photoDetail.value = metadata as Row;
    photoAssets.value = Array.isArray(assets) ? assets as Row[] : [];
    photoMetadataForm.value = { ...photoMetadataForm.value, ...(metadata as Row), captureTime: String((metadata as Row)?.captureTime || ""), width: (metadata as Row)?.width ?? null, height: (metadata as Row)?.height ?? null, orientation: (metadata as Row)?.orientation ?? null };
    const primary = photoAssets.value.find(asset => asset.primary) || photoAssets.value[0];
    if (primary?.attachmentId) {
      const preview: any = await http.get(`/attachments/${primary.attachmentId}/preview-url`);
      photoPreviewUrl.value = String(preview?.url || "");
    }
  } catch (e: any) { photoDetail.value = null; photoAssets.value = []; error.value = e?.response?.data?.detail || e?.message || "图片元数据加载失败"; }
  finally { photoDetailLoading.value = false; }
}

async function savePhotoMetadata() {
  if (!photoDetail.value?.id) return;
  photoDetailLoading.value = true; error.value = "";
  try {
    const result: any = await http.request("put", `/photos/${photoDetail.value.id}/metadata`, { headers: { "If-Match": `\"${photoDetail.value.version ?? 0}\"` }, data: { ...photoMetadataForm.value, captureTime: photoMetadataForm.value.captureTime || null, width: photoMetadataForm.value.width || null, height: photoMetadataForm.value.height || null, orientation: photoMetadataForm.value.orientation || null } });
    photoDetail.value = result as Row; message.value = "图片元数据已保存并重新读取"; await load();
  } catch (e: any) { error.value = e?.response?.status === 412 ? "图片版本已变化，请重新加载后再保存" : e?.response?.data?.detail || e?.message || "图片元数据保存失败"; }
  finally { photoDetailLoading.value = false; }
}

async function generatePhotoThumbnail() {
  if (!photoDetail.value?.id) return;
  photoThumbnailLoading.value = true; error.value = ""; photoThumbnailTask.value = "";
  try {
    const task: any = await http.post(`/photos/${photoDetail.value.id}/actions/generate-thumbnail`);
    photoThumbnailTask.value = String(task?.taskId || task?.id || "");
    if (!photoThumbnailTask.value) throw new Error("服务端未返回缩略图任务 ID");
    for (let attempt = 0; attempt < 30; attempt += 1) {
      const state: any = await http.get(`/background-tasks/${photoThumbnailTask.value}`);
      const status = String(state?.status || "").toUpperCase();
      if (["COMPLETED", "FAILED", "CANCELLED"].includes(status)) {
        if (status !== "COMPLETED") throw new Error(state?.error?.message || "缩略图生成失败，请查看后台任务详情");
        message.value = "缩略图生成完成";
        await openPhoto(photoDetail.value);
        return;
      }
      await new Promise(resolve => window.setTimeout(resolve, 1000));
    }
    throw new Error("缩略图任务仍在后台运行，请稍后刷新");
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "缩略图生成失败"; }
  finally { photoThumbnailLoading.value = false; }
}

function openOriginalPhoto() {
  if (!photoPreviewUrl.value) { error.value = "原图当前不可预览"; return; }
  window.open(photoPreviewUrl.value, "_blank", "noopener,noreferrer");
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

async function playMusic(row: Row, startQueueId = "") {
  if (!row.trackId) { playbackError.value = "该导入记录没有 Track，无法播放"; return; }
  playbackLoading.value = String(row.trackId); playbackError.value = ""; playbackUrl.value = "";
  try {
    const sources = await http.get<unknown, unknown>(`/music/tracks/${row.trackId}/audio-sources`) as Row[];
    const source = sources.find(item => String(item.availability).toUpperCase() === "AVAILABLE");
    if (!source?.id || !source.attachmentId) throw new Error("没有可用的 Audio Source");
    playbackSession.value = await http.post<unknown, unknown>(`/music/playback/tracks/${row.trackId}/sessions`, { data: { sourceId: source.id, queueId: startQueueId || undefined, positionMillis: 0 } }) as Row;
    const preview = await http.get<unknown, unknown>(`/attachments/${source.attachmentId}/preview-url`) as Row;
    if (!preview.url) throw new Error("服务端没有返回可播放地址");
    playbackTrack.value = row; playbackUrl.value = preview.url;
  } catch (e: any) { playbackSession.value = null; playbackError.value = e?.response?.data?.detail || e?.message || "播放歌曲失败"; }
  finally { playbackLoading.value = ""; }
}

async function loadActiveSessions() {
  sessionLoading.value = true; error.value = "";
  try { const result = await http.get<unknown, unknown>("/music/playback/sessions"); activeSessions.value = Array.isArray(result) ? result as Row[] : []; }
  catch (e: any) { activeSessions.value = []; error.value = e?.response?.data?.detail || e?.message || "加载未完成播放会话失败"; }
  finally { sessionLoading.value = false; }
}

async function resumeMusicSession(session: Row) {
  sessionLoading.value = true; playbackError.value = ""; playbackUrl.value = "";
  try {
    const sources = await http.get<unknown, unknown>(`/music/tracks/${session.trackId}/audio-sources`) as Row[];
    const source = sources.find(item => String(item.id) === String(session.sourceId) && String(item.availability).toUpperCase() === "AVAILABLE");
    if (!source?.attachmentId) throw new Error("播放来源已不可用");
    const preview = await http.get<unknown, unknown>(`/attachments/${source.attachmentId}/preview-url`) as Row;
    if (!preview.url) throw new Error("服务端没有返回可播放地址");
    playbackSession.value = session; playbackTrack.value = rows.value.find(row => String(row.trackId) === String(session.trackId)) || { trackId: session.trackId }; playbackUrl.value = preview.url;
    message.value = `已恢复播放会话，位置 ${session.positionMillis ?? 0} 毫秒`;
  } catch (e: any) { playbackError.value = e?.response?.data?.detail || e?.message || "恢复播放会话失败"; }
  finally { sessionLoading.value = false; }
}

function clearPlayback() { playbackUrl.value = ""; playbackTrack.value = null; playbackSession.value = null; }

async function createQueue() {
  const trackIds = queueTrackIds.value.split(",").map(value => value.trim()).filter(Boolean);
  if (!trackIds.length) { error.value = "请输入至少一个 Track ID"; return; }
  queueSaving.value = true; error.value = "";
  try { const result: any = await http.post("/music/queues", { data: { trackIds } }); queueId.value = result.id; queueEntries.value = await loadQueueEntries(result.id); message.value = "播放队列已创建"; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "创建播放队列失败"; }
  finally { queueSaving.value = false; }
}

async function loadQueueEntries(id = queueId.value) {
  if (!id.trim()) { queueEntries.value = []; return []; }
  queueLoading.value = true; error.value = "";
  try { const result = await http.get<unknown, unknown>(`/music/queues/${id.trim()}/entries`) as Row[]; queueEntries.value = Array.isArray(result) ? result : []; queueOrderText.value = queueEntries.value.map(item => String(item.id)).join(","); return queueEntries.value; }
  catch (e: any) { queueEntries.value = []; error.value = e?.response?.data?.detail || e?.message || "加载播放队列失败"; return []; }
  finally { queueLoading.value = false; }
}

async function addQueueEntry() {
  const trackId = queueTrackIds.value.split(",").map(value => value.trim()).filter(Boolean)[0];
  if (!queueId.value.trim() || !trackId) { error.value = "请输入 Queue ID 和 Track ID"; return; }
  queueSaving.value = true; error.value = "";
  try { await http.post(`/music/queues/${queueId.value.trim()}/entries`, { data: { trackId } }); await loadQueueEntries(); message.value = "歌曲已加入播放队列"; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "加入播放队列失败"; }
  finally { queueSaving.value = false; }
}

async function removeQueueEntry(entry: Row) {
  queueSaving.value = true; error.value = "";
  try { await http.request("delete", `/music/queues/entries/${entry.id}`); await loadQueueEntries(); message.value = "歌曲已从播放队列移除"; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "移除播放队列项失败"; }
  finally { queueSaving.value = false; }
}

async function reorderQueue() {
  if (!queueId.value.trim()) { error.value = "请输入 Queue ID"; return; }
  const entryIds = queueOrderText.value.split(",").map(value => value.trim()).filter(Boolean);
  if (!entryIds.length) { error.value = "请输入完整的 Entry ID 顺序"; return; }
  queueSaving.value = true; error.value = "";
  try { const result: any = await http.request("patch", `/music/queues/${queueId.value.trim()}/entries/order`, { headers: { "If-Match": queueVersion.value }, data: { entryIds } }); queueVersion.value = String(result?.version ?? Number(queueVersion.value) + 1); await loadQueueEntries(); message.value = "播放队列顺序已保存"; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "保存播放队列顺序失败"; }
  finally { queueSaving.value = false; }
}

async function saveQueuePolicy() {
  if (!queueId.value.trim()) { error.value = "请输入 Queue ID"; return; }
  queueSaving.value = true; error.value = "";
  try { const result: any = await http.request("patch", `/music/queues/${queueId.value.trim()}/policy`, { headers: { "If-Match": queueVersion.value }, data: { repeatMode: queueRepeatMode.value, shuffleEnabled: queueShuffleEnabled.value } }); queueVersion.value = String(result?.version ?? Number(queueVersion.value) + 1); message.value = "播放模式已保存"; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "保存播放模式失败"; }
  finally { queueSaving.value = false; }
}

async function loadLyrics(trackId = lyricsTrackId.value) {
  if (!trackId.trim()) { error.value = "请输入 Track ID"; return; }
  lyricsLoading.value = true; error.value = ""; message.value = "";
  try {
    const result = await http.get<unknown, unknown>(`/music/tracks/${trackId.trim()}/lyrics`);
    lyricsRows.value = Array.isArray(result) ? result as Row[] : [];
    message.value = lyricsRows.value.length ? `已加载 ${lyricsRows.value.length} 个歌词版本` : "该歌曲暂无可用歌词";
  } catch (e: any) { lyricsRows.value = []; error.value = e?.response?.data?.detail || e?.message || "加载歌词失败"; }
  finally { lyricsLoading.value = false; }
}

async function loadPlaylists() {
  playlistLoading.value = true; error.value = "";
  try { const result = await http.get<unknown, unknown>("/music/playlists"); playlists.value = Array.isArray(result) ? result as Row[] : []; }
  catch (e: any) { playlists.value = []; error.value = e?.response?.data?.detail || e?.message || "加载播放列表失败"; }
  finally { playlistLoading.value = false; }
}

async function savePlaylist() {
  if (!playlistForm.value.name.trim()) { error.value = "请输入播放列表名称"; return; }
  playlistSaving.value = true; error.value = "";
  try {
    const data = { name: playlistForm.value.name.trim(), description: playlistForm.value.description.trim() || null };
    const result: any = playlistForm.value.id ? await http.request("patch", `/music/playlists/${playlistForm.value.id}`, { headers: { "If-Match": playlistForm.value.version }, data }) : await http.post("/music/playlists", { data });
    playlistForm.value = { id: String(result.id), name: result.name, description: result.description || "", version: String(result.version ?? 0) };
    await loadPlaylists(); message.value = playlistForm.value.id ? "播放列表已保存" : "播放列表已创建";
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "保存播放列表失败"; }
  finally { playlistSaving.value = false; }
}

function editPlaylist(item: Row) { playlistForm.value = { id: String(item.id), name: item.name || "", description: item.description || "", version: String(item.version ?? 0) }; loadPlaylistEntries(String(item.id)); }
function resetPlaylist() { playlistForm.value = { id: "", name: "", description: "", version: "0" }; }

async function loadPlaylistEntries(id = playlistForm.value.id) {
  if (!id.trim()) { playlistEntries.value = []; return; }
  playlistEntryLoading.value = true; error.value = "";
  try { const result = await http.get<unknown, unknown>(`/music/playlists/${id}/entries`); playlistEntries.value = Array.isArray(result) ? result as Row[] : []; }
  catch (e: any) { playlistEntries.value = []; error.value = e?.response?.data?.detail || e?.message || "加载播放列表歌曲失败"; }
  finally { playlistEntryLoading.value = false; }
}

async function addPlaylistEntry() {
  if (!playlistForm.value.id || !playlistTrackId.value.trim()) { error.value = "请先选择列表并输入 Track ID"; return; }
  playlistEntrySaving.value = true; error.value = "";
  try { await http.post(`/music/playlists/${playlistForm.value.id}/entries`, { data: { trackId: playlistTrackId.value.trim() } }); playlistTrackId.value = ""; await loadPlaylistEntries(); message.value = "歌曲已加入播放列表"; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "添加歌曲失败"; }
  finally { playlistEntrySaving.value = false; }
}

async function removePlaylistEntry(entry: Row) {
  playlistEntrySaving.value = true; error.value = "";
  try { await http.request("delete", `/music/playlists/entries/${entry.id}`); await loadPlaylistEntries(); message.value = "歌曲已从播放列表移除"; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "移除歌曲失败"; }
  finally { playlistEntrySaving.value = false; }
}

async function movePlaylistEntry(index: number, direction: -1 | 1) {
  const target = index + direction;
  if (target < 0 || target >= playlistEntries.value.length || !playlistForm.value.id) return;
  const reordered = [...playlistEntries.value];
  [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
  playlistEntrySaving.value = true; error.value = "";
  try {
    const result: any = await http.request("patch", `/music/playlists/${playlistForm.value.id}/entries/order`, { headers: { "If-Match": playlistForm.value.version }, data: { entryIds: reordered.map(entry => entry.id) } });
    playlistForm.value.version = String(result.version ?? Number(playlistForm.value.version) + 1);
    await loadPlaylistEntries(); message.value = "播放列表顺序已保存";
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "调整歌曲顺序失败"; }
  finally { playlistEntrySaving.value = false; }
}

async function playPlaylistEntry(index: number) {
  const entries = playlistEntries.value.slice(index);
  if (!entries.length || !playlistForm.value.id) { error.value = "请先选择有歌曲的播放列表"; return; }
  playlistEntrySaving.value = true; error.value = "";
  try {
    const queue: any = await http.post("/music/queues", { data: { trackIds: entries.map(entry => entry.trackId) } });
    queueId.value = String(queue.id);
    message.value = `已从第 ${index + 1} 首开始播放`;
    await playMusic({ trackId: entries[0].trackId, title: `播放列表 · 第 ${index + 1} 首` }, String(queue.id));
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "从播放列表开始播放失败"; }
  finally { playlistEntrySaving.value = false; }
}

onMounted(() => { load(); if (isMusic.value) { loadActiveSessions(); loadPlaylists(); } });
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">{{ title }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">资源、附件和技术元数据分层管理，敏感内容遵循授权边界。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" class="mb-4"/><el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4"/>
    <template v-if="isMusic">
      <el-card shadow="never" class="mb-4"><template #header><span>列表歌曲</span></template><div class="flex flex-wrap gap-2 mb-3"><el-input v-model="playlistTrackId" placeholder="Track ID" class="w-96"/><el-button :disabled="!playlistForm.id" :loading="playlistEntrySaving" type="primary" @click="addPlaylistEntry">添加歌曲</el-button><el-button :disabled="!playlistForm.id" :loading="playlistEntryLoading" @click="loadPlaylistEntries()">刷新歌曲</el-button><span class="text-xs text-[var(--el-text-color-secondary)] self-center">先在下方播放列表点击“编辑”选择目标列表</span></div><el-skeleton v-if="playlistEntryLoading" :rows="2" animated/><el-empty v-else-if="!playlistEntries.length" description="当前列表暂无歌曲"/><el-table v-else :data="playlistEntries" stripe><el-table-column prop="position" label="位置" width="90"/><el-table-column prop="trackId" label="Track ID" min-width="300"/><el-table-column label="顺序" width="150"><template #default="scope"><el-button link :disabled="playlistEntrySaving || scope.$index === 0" @click="movePlaylistEntry(scope.$index, -1)">上移</el-button><el-button link :disabled="playlistEntrySaving || scope.$index === playlistEntries.length - 1" @click="movePlaylistEntry(scope.$index, 1)">下移</el-button></template></el-table-column><el-table-column label="操作" width="220"><template #default="scope"><el-button link type="primary" :loading="playlistEntrySaving || playbackLoading === scope.row.trackId" @click="playPlaylistEntry(scope.$index)">从此处播放</el-button><el-button link type="danger" :loading="playlistEntrySaving" @click="removePlaylistEntry(scope.row)">移除</el-button></template></el-table-column></el-table></el-card>
      <el-card shadow="never" class="mb-4"><template #header><span>播放列表</span></template><el-form inline><el-form-item label="名称"><el-input v-model="playlistForm.name" placeholder="播放列表名称" class="w-56"/></el-form-item><el-form-item label="描述"><el-input v-model="playlistForm.description" placeholder="可选描述" class="w-64"/></el-form-item><el-button type="primary" :loading="playlistSaving" @click="savePlaylist">{{ playlistForm.id ? "保存修改" : "创建列表" }}</el-button><el-button v-if="playlistForm.id" @click="playlistForm={ id: '', name: '', description: '', version: '0' }">新建</el-button></el-form><el-skeleton v-if="playlistLoading" :rows="2" animated/><el-empty v-else-if="!playlists.length" description="暂无播放列表"/><el-table v-else :data="playlists" stripe><el-table-column prop="name" label="名称" min-width="180"/><el-table-column prop="description" label="描述" min-width="240"/><el-table-column prop="version" label="版本" width="90"/><el-table-column label="操作" width="100"><template #default="scope"><el-button link type="primary" @click="editPlaylist(scope.row)">编辑</el-button></template></el-table-column></el-table></el-card>
      <el-card shadow="never" class="mb-4"><template #header><span>未完成播放会话</span></template><el-skeleton v-if="sessionLoading" :rows="2" animated/><el-empty v-else-if="!activeSessions.length" description="暂无可恢复的播放会话"/><el-table v-else :data="activeSessions" stripe><el-table-column prop="trackId" label="Track ID" min-width="300"/><el-table-column prop="positionMillis" label="位置（毫秒）" width="140"/><el-table-column prop="startedAt" label="开始时间" min-width="180"/><el-table-column label="操作" width="100"><template #default="scope"><el-button link type="primary" :loading="sessionLoading" @click="resumeMusicSession(scope.row)">恢复</el-button></template></el-table-column></el-table><el-button class="mt-3" :loading="sessionLoading" @click="loadActiveSessions">刷新会话</el-button></el-card>
      <el-card shadow="never" class="mb-4"><template #header><span>导入音乐附件</span></template><el-form label-position="top" class="max-w-3xl"><el-form-item label="音频 Attachment ID" required><el-input v-model="musicForm.attachmentId" placeholder="先在附件与存储上传音频，再粘贴 Attachment ID" clearable/></el-form-item><div class="grid grid-cols-1 md:grid-cols-2 gap-4"><el-form-item label="歌曲标题"><el-input v-model="musicForm.title" placeholder="留空使用附件文件名"/></el-form-item><el-form-item label="时长（毫秒）"><el-input-number v-model="musicForm.durationMillis" :min="0" controls-position="right" class="w-full"/></el-form-item><el-form-item label="编码"><el-input v-model="musicForm.codec" placeholder="如 MP3、FLAC"/></el-form-item><el-form-item label="容器"><el-input v-model="musicForm.container" placeholder="如 MPEG、OGG"/></el-form-item></div><div class="flex gap-2"><el-button :loading="duplicateLoading" @click="checkDuplicates">检查重复</el-button><el-button type="primary" :loading="submitting" @click="importMusic">导入音乐</el-button></div></el-form><el-alert v-if="duplicateRows.length" title="发现相同内容的已入库歌曲，导入前请确认是否复用。" type="warning" show-icon :closable="false" class="mt-4"/><el-table v-if="duplicateRows.length" :data="duplicateRows" stripe class="mt-3"><el-table-column prop="title" label="已存在歌曲"/><el-table-column prop="trackId" label="Track ID" min-width="280"/><el-table-column prop="attachmentId" label="附件 ID" min-width="280"/></el-table><el-empty v-else-if="!duplicateLoading && musicForm.attachmentId" description="未发现相同内容的音乐" :image-size="50" class="py-3"/></el-card>
      <el-card shadow="never" class="mb-4"><template #header><span>播放队列</span></template><div class="flex flex-wrap gap-2 mb-3"><el-input v-model="queueId" placeholder="已有 Queue ID" class="w-80" clearable/><el-input v-model="queueTrackIds" placeholder="Track ID；创建时逗号分隔，加入时取第一个" class="w-96" clearable/><el-button :loading="queueLoading" @click="loadQueueEntries()">加载队列</el-button><el-button :loading="queueSaving" type="primary" @click="addQueueEntry">加入歌曲</el-button><el-button :loading="queueSaving" @click="createQueue">创建队列</el-button></div><div class="flex flex-wrap gap-2 mb-3"><el-input v-model="queueVersion" placeholder="Queue 版本" class="w-32"/><el-input v-model="queueOrderText" placeholder="按 Entry ID 逗号分隔填写新顺序" class="w-96"/><el-button :loading="queueSaving" @click="reorderQueue">保存顺序</el-button></div><el-skeleton v-if="queueLoading" :rows="3" animated/><el-empty v-else-if="!queueEntries.length" description="暂无队列项"/><el-table v-else :data="queueEntries" stripe><el-table-column prop="activePosition" label="位置" width="90"/><el-table-column prop="trackId" label="Track ID" min-width="300"/><el-table-column label="操作" width="100"><template #default="scope"><el-button link type="danger" :loading="queueSaving" @click="removeQueueEntry(scope.row)">移除</el-button></template></el-table-column></el-table></el-card><el-alert v-if="playbackError" :title="playbackError" type="warning" show-icon :closable="false" class="mb-4"/><el-card v-if="playbackUrl" shadow="never" class="mb-4"><template #header><div class="flex justify-between items-center"><span>正在播放：{{ playbackTrack?.title || playbackTrack?.trackId }}</span><el-button link @click="clearPlayback">关闭</el-button></div></template><audio :src="playbackUrl" controls autoplay class="w-full" @error="playbackError = '音频加载失败，请检查附件可用状态'"/><div class="text-xs text-[var(--el-text-color-secondary)] mt-2">播放会话：{{ playbackSession?.id || "—" }} · 来源：{{ playbackSession?.sourceId || "—" }}</div></el-card><el-card shadow="never"><template #header><div class="flex justify-between"><span>音乐导入记录</span><el-input v-model="query" clearable placeholder="搜索标题或 Track ID" class="w-64"/></div></template><el-skeleton v-if="loading" :rows="5" animated/><el-empty v-else-if="!filtered.length" description="暂无音乐导入记录"/><el-table v-else :data="filtered" stripe @row-click="playMusic"><el-table-column prop="title" label="歌曲" min-width="220"/><el-table-column prop="attachmentId" label="源附件" min-width="280"/><el-table-column prop="trackId" label="Track ID" min-width="280"/><el-table-column prop="durationMillis" label="时长（毫秒）" width="140"/><el-table-column prop="status" label="状态" width="120"/><el-table-column prop="errorMessage" label="错误" min-width="220"/><el-table-column prop="createdAt" label="导入时间" min-width="180"/><el-table-column label="操作" width="130" fixed="right"><template #default="scope"><el-button link type="primary" @click.stop="recognizeMusic(scope.row)">识别信息</el-button></template></el-table-column></el-table></el-card>
      <el-dialog v-model="candidateDialog" title="歌曲信息候选" width="860px"><el-skeleton v-if="candidateLoading" :rows="5" animated/><el-empty v-else-if="!candidates.length" description="未识别到嵌入标签"/><el-table v-else :data="candidates" stripe><el-table-column prop="source" label="来源" width="100"/><el-table-column prop="title" label="标题" min-width="160"/><el-table-column prop="artist" label="艺术家" min-width="140"/><el-table-column prop="album" label="专辑" min-width="140"/><el-table-column prop="trackNumber" label="曲目号" width="90"/><el-table-column prop="releaseYear" label="年份" width="90"/><el-table-column label="操作" width="180"><template #default="scope"><el-button link type="primary" @click="editCandidate(scope.row)">修正</el-button><el-button v-if="associated(scope.row.id)" link type="success" disabled>已关联</el-button><el-button v-else link type="primary" :loading="associationLoading === scope.row.id" @click="associateCandidate(scope.row)">关联</el-button></template></el-table-column></el-table></el-dialog>
      <el-dialog v-model="correctionDialog" title="修正歌曲信息" width="560px"><el-form label-position="top"><div class="grid grid-cols-1 md:grid-cols-2 gap-3"><el-form-item label="标题"><el-input v-model="correctionCandidate.title"/></el-form-item><el-form-item label="艺术家"><el-input v-model="correctionCandidate.artist"/></el-form-item><el-form-item label="专辑"><el-input v-model="correctionCandidate.album"/></el-form-item><el-form-item label="专辑艺术家"><el-input v-model="correctionCandidate.albumArtist"/></el-form-item><el-form-item label="曲目号"><el-input-number v-model="correctionCandidate.trackNumber" :min="1" class="w-full"/></el-form-item><el-form-item label="碟号"><el-input-number v-model="correctionCandidate.discNumber" :min="1" class="w-full"/></el-form-item><el-form-item label="流派"><el-input v-model="correctionCandidate.genre"/></el-form-item><el-form-item label="发行年份"><el-input v-model="correctionCandidate.releaseYear"/></el-form-item></div></el-form><template #footer><el-button @click="correctionDialog=false">取消</el-button><el-button type="primary" :loading="correctionSaving" @click="saveCorrection">保存修正</el-button></template></el-dialog>
      <el-card shadow="never" class="mb-4"><template #header><span>播放模式</span></template><div class="flex flex-wrap items-center gap-3"><el-select v-model="queueRepeatMode" class="w-36"><el-option label="不循环" value="OFF"/><el-option label="队列循环" value="QUEUE"/><el-option label="单曲循环" value="ONE"/></el-select><el-switch v-model="queueShuffleEnabled" active-text="随机播放"/><el-button type="primary" :loading="queueSaving" :disabled="!queueId" @click="saveQueuePolicy">保存模式</el-button><span class="text-xs text-[var(--el-text-color-secondary)]">使用上方 Queue 版本进行并发校验</span></div></el-card>
      <el-card shadow="never" class="mb-4"><template #header><span>可用歌词</span></template><div class="flex flex-wrap gap-2 mb-3"><el-input v-model="lyricsTrackId" placeholder="Track ID" class="w-96" clearable/><el-button type="primary" :loading="lyricsLoading" @click="loadLyrics()">加载歌词</el-button></div><el-skeleton v-if="lyricsLoading" :rows="4" animated/><el-empty v-else-if="!lyricsRows.length" description="暂无可用歌词"/><el-table v-else :data="lyricsRows" stripe><el-table-column prop="language" label="语言" width="110"/><el-table-column prop="type" label="类型" width="140"/><el-table-column prop="source" label="来源" width="140"/><el-table-column prop="provenance" label="溯源" min-width="160"/><el-table-column label="内容" min-width="320"><template #default="scope"><pre class="whitespace-pre-wrap text-sm">{{ scope.row.content || scope.row.timingData || "—" }}</pre></template></el-table-column></el-table></el-card>
    </template>
    <template v-else-if="isPhotos">
      <el-alert title="先在附件与存储中创建 PHOTO Resource 并上传图片 Attachment，再在此登记图片。原图不会被复制或暴露物理路径。" type="info" show-icon :closable="false" class="mb-4" />
      <el-card shadow="never" class="mb-4"><template #header><span>登记图片并读取元数据</span></template><el-form inline @submit.prevent="createPhoto"><el-form-item label="图片 Attachment ID" required><el-input v-model="photoForm.attachmentId" class="w-80" clearable /></el-form-item><el-form-item label="标题" required><el-input v-model="photoForm.title" class="w-56" /></el-form-item><el-form-item label="语言"><el-input v-model="photoForm.locale" class="w-32" /></el-form-item><el-button type="primary" :loading="photoSubmitting" @click="createPhoto">登记图片</el-button></el-form></el-card>
      <el-card shadow="never"><template #header><div class="flex justify-between"><span>图片时间线</span><el-input v-model="query" clearable placeholder="搜索 Photo / Resource ID" class="w-64" /></div></template><el-skeleton v-if="loading" :rows="6" animated /><el-empty v-else-if="!filtered.length" description="暂无图片；请先上传图片附件并登记" /><el-table v-else :data="filtered" stripe @row-click="openPhoto"><el-table-column prop="id" label="Photo ID" min-width="260" /><el-table-column prop="resourceId" label="Resource ID" min-width="260" /><el-table-column prop="captureTime" label="拍摄时间" min-width="190" /><el-table-column prop="width" label="宽" width="80" /><el-table-column prop="height" label="高" width="80" /><el-table-column prop="cameraMake" label="相机厂商" min-width="140" /><el-table-column prop="cameraModel" label="相机型号" min-width="160" /><el-table-column label="操作" width="120"><template #default="scope"><el-button link type="primary" @click.stop="openPhoto(scope.row)">查看元数据</el-button></template></el-table-column></el-table></el-card>
      <el-drawer v-model="photoDrawerVisible" title="图片元数据" size="560px"><el-skeleton v-if="photoDetailLoading" :rows="10" animated /><template v-else-if="photoDetail"><div v-if="photoPreviewUrl" class="mb-4 flex justify-center max-h-64 overflow-auto"><img :src="photoPreviewUrl" alt="图片原图预览" class="max-w-full object-contain" /></div><el-button v-if="photoPreviewUrl" class="mb-4" @click="openOriginalPhoto">打开原图</el-button><el-form label-position="top"><div class="grid grid-cols-2 gap-3"><el-form-item label="拍摄时间"><el-input v-model="photoMetadataForm.captureTime" placeholder="RFC 3339，可留空" /></el-form-item><el-form-item label="本地时间"><el-input v-model="photoMetadataForm.captureTimeLocal" /></el-form-item><el-form-item label="时区"><el-input v-model="photoMetadataForm.timeZone" /></el-form-item><el-form-item label="方向"><el-input-number v-model="photoMetadataForm.orientation" :min="1" :max="8" class="w-full" /></el-form-item><el-form-item label="宽"><el-input-number v-model="photoMetadataForm.width" :min="1" class="w-full" /></el-form-item><el-form-item label="高"><el-input-number v-model="photoMetadataForm.height" :min="1" class="w-full" /></el-form-item><el-form-item label="相机厂商"><el-input v-model="photoMetadataForm.cameraMake" /></el-form-item><el-form-item label="相机型号"><el-input v-model="photoMetadataForm.cameraModel" /></el-form-item><el-form-item label="镜头"><el-input v-model="photoMetadataForm.lensModel" /></el-form-item><el-form-item label="纬度"><el-input v-model="photoMetadataForm.latitude" /></el-form-item><el-form-item label="经度"><el-input v-model="photoMetadataForm.longitude" /></el-form-item></div></el-form><el-divider /><div class="text-sm mb-3">资产：{{ photoAssets.length }} 个；原图与派生图身份分离。</div><el-table :data="photoAssets" size="small"><el-table-column prop="role" label="角色" width="150" /><el-table-column prop="availability" label="可用性" width="130" /><el-table-column prop="attachmentId" label="Attachment ID" min-width="220" /></el-table><div class="flex gap-2 mt-4"><el-button type="primary" :loading="photoDetailLoading" @click="savePhotoMetadata">保存元数据</el-button><el-button type="success" :loading="photoThumbnailLoading" @click="generatePhotoThumbnail">生成缩略图</el-button></div><div v-if="photoThumbnailTask" class="text-xs text-[var(--el-text-color-secondary)] mt-2">后台任务：{{ photoThumbnailTask }}</div></template><el-empty v-else description="选择一张图片" /></el-drawer>
    </template>
    <template v-else><el-tabs v-model="tab"><el-tab-pane label="目录" name="catalog"/><el-tab-pane label="元数据" name="metadata"/><el-tab-pane label="播放 / 时间线" name="activity"/></el-tabs><el-card shadow="never"><template #header><div class="flex justify-between"><span>{{ title }}目录</span><el-input v-model="query" clearable placeholder="搜索标题或资源 ID" class="w-64"/></div></template><el-skeleton v-if="loading" :rows="5" animated/><el-empty v-else-if="!filtered.length" description="暂无可展示条目；其他媒体目录接口尚未接入"/><el-table v-else :data="filtered" stripe><el-table-column prop="id" label="ID" min-width="240"/><el-table-column prop="name" label="名称" min-width="200"/><el-table-column prop="status" label="状态" width="140"/><el-table-column prop="createdAt" label="创建时间" min-width="180"/></el-table><el-alert title="附件物理存储请在“附件与存储”模块管理。" type="info" show-icon :closable="false" class="mt-4"/></el-card></template>
  </main>
</template>
