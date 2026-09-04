<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

const route = useRoute();
const data = ref<Record<string, any> | null>(null);
const loading = ref(false); const actionLoading = ref(false); const error = ref(""); const resourceId = ref("");
const previewVisible = ref(false); const previewLoading = ref(false); const previewUrl = ref("");
const previewType = computed(() => String(data.value?.mediaType || "application/octet-stream").toLowerCase());
const isImage = computed(() => previewType.value.startsWith("image/"));
const isVideo = computed(() => previewType.value.startsWith("video/"));
const isAudio = computed(() => previewType.value.startsWith("audio/"));
const isDocument = computed(() => previewType.value === "application/pdf" || previewType.value.startsWith("text/"));
const canPreview = computed(() => isImage.value || isVideo.value || isAudio.value || isDocument.value);

async function load() { loading.value = true; error.value = ""; try { data.value = await http.get(`/attachments/${route.params.attachmentId}`); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件详情加载失败"; } finally { loading.value = false; } }
async function fetchContent() { return http.get<Blob, unknown>(`/attachments/${route.params.attachmentId}/content`, { responseType: "blob" }); }
async function preview() { previewLoading.value = true; error.value = ""; try { const blob = await fetchContent(); previewUrl.value = URL.createObjectURL(blob); previewVisible.value = true; } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件内容读取失败"; } finally { previewLoading.value = false; } }
async function download() { actionLoading.value = true; try { const blob = await fetchContent(); const url = URL.createObjectURL(blob); const link = document.createElement("a"); link.href = url; link.download = data.value?.fileName || "attachment"; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件下载失败"; } finally { actionLoading.value = false; } }
function closePreview() { if (previewUrl.value) URL.revokeObjectURL(previewUrl.value); previewUrl.value = ""; previewVisible.value = false; }
async function archive() { if (!resourceId.value.trim()) { error.value = "归档前请输入 Resource ID"; return; } actionLoading.value = true; try { await http.post(`/attachments/${route.params.attachmentId}/archive?resourceId=${encodeURIComponent(resourceId.value.trim())}`); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件归档失败"; } finally { actionLoading.value = false; } }
async function removeRelation() { if (!resourceId.value.trim()) { error.value = "移除附件关系前请输入 Resource ID"; return; } if (!window.confirm("确认移除该 Resource 的 Attachment 关系吗？此操作不会直接删除 Blob。")) return; actionLoading.value = true; try { await http.request("delete", `/resources/${resourceId.value.trim()}/attachments/${route.params.attachmentId}`); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件关系移除失败"; } finally { actionLoading.value = false; } }
async function purge() { if (!resourceId.value.trim() || !window.confirm("高风险操作：确认永久清理该 Attachment 及其可清理物理对象吗？此操作不可恢复。")) return; actionLoading.value = true; try { await http.post(`/resources/${resourceId.value.trim()}/attachments/${route.params.attachmentId}/actions/purge`); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件永久清理失败"; } finally { actionLoading.value = false; } }
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex flex-wrap justify-between gap-4 mb-6"><div><h1 class="text-2xl font-semibold">{{ data?.fileName || '附件详情' }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">Attachment {{ route.params.attachmentId }}</p></div><div class="flex flex-wrap gap-2"><el-button @click="$router.back()">返回</el-button><el-button :loading="loading" @click="load">刷新</el-button><el-button type="primary" :loading="previewLoading" @click="preview">预览</el-button><el-button :loading="actionLoading" @click="download">下载</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false"/><el-skeleton v-if="loading" :rows="8" animated/>
    <template v-else-if="data"><el-card shadow="never" class="mt-4"><template #header><span class="font-medium">概览</span></template><el-descriptions :column="1" border><el-descriptions-item label="Attachment ID">{{ data.id }}</el-descriptions-item><el-descriptions-item label="文件名">{{ data.fileName }}</el-descriptions-item><el-descriptions-item label="角色">{{ data.kind }}</el-descriptions-item><el-descriptions-item label="MIME">{{ data.mediaType || '-' }}</el-descriptions-item><el-descriptions-item label="大小">{{ data.sizeBytes }} Bytes</el-descriptions-item><el-descriptions-item label="Blob ID">{{ data.blobId || '-' }}</el-descriptions-item><el-descriptions-item label="SHA-256">{{ data.sha256 || '-' }}</el-descriptions-item><el-descriptions-item label="可用性"><el-tag :type="String(data.availability).toUpperCase() === 'AVAILABLE' ? 'success' : 'danger'">{{ data.availability || '未知' }}</el-tag></el-descriptions-item></el-descriptions></el-card><el-card shadow="never" class="mt-4"><template #header><span class="font-medium">Resource 关系操作</span></template><p class="text-sm text-[var(--el-text-color-secondary)] mb-3">归档和移除关系需要 Resource ID。移除 Attachment 关系不会直接删除 Blob；永久清理是不可恢复的高风险操作。</p><div class="flex flex-wrap gap-2"><el-input v-model="resourceId" placeholder="Resource ID" clearable/><el-button :loading="actionLoading" @click="archive">归档附件</el-button><el-button type="danger" plain :loading="actionLoading" @click="removeRelation">移除附件关系</el-button><el-button type="danger" :loading="actionLoading" @click="purge">永久清理</el-button></div></el-card></template><el-empty v-else description="暂无附件数据"/>
    <el-dialog v-model="previewVisible" :title="`预览：${data?.fileName || '附件'}`" width="80%" top="5vh" @closed="closePreview"><div v-if="previewUrl && isImage" class="flex justify-center max-h-[75vh] overflow-auto"><img :src="previewUrl" :alt="data?.fileName" class="max-w-full object-contain" /></div><video v-else-if="previewUrl && isVideo" :src="previewUrl" controls class="w-full max-h-[75vh]"/><audio v-else-if="previewUrl && isAudio" :src="previewUrl" controls class="w-full mt-4"/><iframe v-else-if="previewUrl && isDocument" :src="previewUrl" class="w-full h-[75vh] border-0"/><div v-else class="py-12 text-center"><el-empty description="当前文件类型不支持在线预览"/><el-button type="primary" @click="download">下载文件</el-button></div></el-dialog>
  </main>
</template>
