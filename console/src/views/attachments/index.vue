<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

type Placement = Record<string, unknown>;
type Attachment = Record<string, any> & { placements?: Placement[] };
type Provider = Record<string, any>;
const resourceId = ref("");
const route = useRoute();
const attachments = ref<Attachment[]>([]);
const selected = ref<Attachment | null>(null);
const detailVisible = computed({ get: () => Boolean(selected.value), set: (value: boolean) => { if (!value) selected.value = null; } });
const loading = ref(false);
const error = ref("");
const loaded = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const uploadDialog = ref(false);
const uploadLoading = ref(false);
const file = ref<File | null>(null);
const providerOptions = ref<Provider[]>([]);
const upload = ref({ objectKey: "", provider: "s3-main", kind: "ORIGINAL" });
const uploadSessionId = ref("");
const uploadIdempotencyKey = ref("");
const uploadPhase = ref<"idle" | "hashing" | "creating" | "uploading" | "committing" | "success" | "failed" | "aborted">("idle");
const uploadProgress = ref(0);
const uploadMessage = ref("");
const uploadRequest = ref<XMLHttpRequest | null>(null);
const uploadCancelRequested = ref(false);
const availableCount = computed(() => attachments.value.filter(item => ["READY", "AVAILABLE"].includes(String(item.availability || "").toUpperCase())).length);
const uniqueBlobs = computed(() => new Set(attachments.value.map(item => item.blobId).filter(Boolean)).size);
const allAttachments = computed(() => !resourceId.value.trim());
const attachmentCount = computed(() => allAttachments.value ? total.value : attachments.value.length);

async function load(resetPage = true) {
  if (resetPage) page.value = 1;
  loading.value = true; error.value = ""; loaded.value = false; selected.value = null;
  try {
    const filter = resourceId.value.trim();
    if (filter) {
      const result = await http.get<unknown, unknown>(`/resources/${filter}/attachments`);
      attachments.value = Array.isArray(result) ? result as Attachment[] : [];
      total.value = attachments.value.length;
    } else {
      const result: any = await http.get("/attachments", { params: { page: page.value - 1, size: pageSize.value } });
      attachments.value = Array.isArray(result) ? result : (result?.items || result?.content || []);
      total.value = Number(result?.total ?? attachments.value.length);
    }
    loaded.value = true;
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件加载失败"; }
  finally { loading.value = false; }
}

function changePage() { load(false); }
function changePageSize() { load(); }
function reload() { load(); }

function placementSummary(item: Attachment) {
  const placements = item.placements || [];
  if (!placements.length) return "暂无 Placement";
  const tiers = [...new Set(placements.map(p => String(p.tier || "未知")))].join(" / ");
  return `${placements.length} 个副本 · ${tiers}`;
}
function base64(bytes: Uint8Array) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}
function uploadPhaseLabel() {
  return ({
    idle: "等待选择文件",
    hashing: "正在计算文件指纹",
    creating: "正在创建上传会话",
    uploading: "正在上传文件",
    committing: "正在提交附件关系",
    success: "上传完成",
    failed: "上传失败",
    aborted: "上传已终止"
  } as const)[uploadPhase.value];
}
function resetUploadState() {
  file.value = null;
  uploadSessionId.value = "";
  uploadIdempotencyKey.value = "";
  uploadPhase.value = "idle";
  uploadProgress.value = 0;
  uploadMessage.value = "";
  uploadRequest.value = null;
  uploadCancelRequested.value = false;
}
function uploadErrorMessage(error: any) {
  const status = error?.response?.status;
  if (status === 403) return "上传被拒绝（403）：当前账号缺少资源写入权限或需要先完成二次验证。";
  if (status === 409) return "上传会话发生冲突（409）：可重试当前会话，或先终止后重新上传。";
  if (error?.name === "AbortError") return "上传已终止。";
  if (error?.message === "Network Error" || error?.code === "NETWORK_ERROR") return "网络连接失败：可检查网络后重试，或终止当前上传会话。";
  return error?.response?.data?.detail || error?.message || "附件上传失败，请确认对象存储可用。";
}
function throwIfUploadCancelled() {
  if (uploadCancelRequested.value) throw new DOMException("上传已终止", "AbortError");
}
function uploadToProvider(url: string, method: string, mediaType: string, checksumSha256: string, selectedFile: File) {
  return new Promise<void>((resolve, reject) => {
    const request = new XMLHttpRequest();
    uploadRequest.value = request;
    request.open(method || "PUT", url, true);
    request.setRequestHeader("Content-Type", mediaType);
    request.setRequestHeader("x-amz-checksum-sha256", checksumSha256);
    request.upload.onprogress = event => {
      if (event.lengthComputable) uploadProgress.value = Math.min(100, Math.round((event.loaded / event.total) * 100));
    };
    request.onload = () => {
      uploadRequest.value = null;
      if (request.status >= 200 && request.status < 300) resolve();
      else reject(new Error(`对象上传失败（HTTP ${request.status}）`));
    };
    request.onerror = () => { uploadRequest.value = null; reject(new Error("对象上传网络错误，请检查 Provider 连接后重试")); };
    request.onabort = () => { uploadRequest.value = null; reject(new DOMException("上传已终止", "AbortError")); };
    request.send(selectedFile);
  });
}
async function commitUpload() {
  if (!resourceId.value.trim() || !file.value || !upload.value.provider) { error.value = "Resource ID、文件和 Provider 均不能为空"; return; }
  uploadLoading.value = true;
  error.value = "";
  uploadMessage.value = "";
  uploadProgress.value = 0;
  uploadCancelRequested.value = false;
  const selectedFile = file.value;
  try {
    uploadPhase.value = "hashing";
    const mediaType = selectedFile.type || "application/octet-stream";
    const digest = await crypto.subtle.digest("SHA-256", await selectedFile.arrayBuffer());
    throwIfUploadCancelled();
    const digestBytes = new Uint8Array(digest);
    const sha256 = Array.from(digestBytes).map(byte => byte.toString(16).padStart(2, "0")).join("");
    const checksumSha256 = base64(digestBytes);
    uploadIdempotencyKey.value ||= crypto.randomUUID();
    uploadPhase.value = "creating";
    const intent = await http.post<any, any>(`/resources/${resourceId.value.trim()}/attachments/upload-intents`, { headers: { "Idempotency-Key": uploadIdempotencyKey.value }, data: {
      fileName: selectedFile.name, sizeBytes: selectedFile.size, mediaType, provider: upload.value.provider,
      objectKey: upload.value.objectKey || undefined, sha256
    } });
    uploadSessionId.value = intent.sessionId || "";
    if (uploadCancelRequested.value) {
      await abortUploadSession();
      throw new DOMException("上传已终止", "AbortError");
    }
    if (!intent.deduplicated) {
      uploadPhase.value = "uploading";
      await uploadToProvider(intent.url, intent.method || "PUT", mediaType, checksumSha256, selectedFile);
    } else {
      uploadProgress.value = 100;
    }
    uploadPhase.value = "committing";
    await http.post(`/resources/${resourceId.value.trim()}/attachments/commit`, { data: { sha256, uploadSha256: intent.sha256 || sha256, deduplicated: Boolean(intent.deduplicated), sizeBytes: file.value.size, mediaType, fileName: file.value.name, kind: upload.value.kind, provider: intent.provider, tier: intent.tier, objectKey: intent.objectKey, idempotencyKey: uploadIdempotencyKey.value } });
    uploadPhase.value = "success";
    uploadProgress.value = 100;
    uploadMessage.value = intent.deduplicated ? "检测到相同内容，已复用现有 Blob 并提交附件。" : "文件已上传并提交 Attachment。";
    uploadSessionId.value = "";
    uploadIdempotencyKey.value = "";
    await load();
  } catch (e: any) {
    if (e?.name === "AbortError") {
      uploadPhase.value = "aborted";
      uploadMessage.value = "上传已终止；临时对象清理请求正在处理。";
    } else {
      uploadPhase.value = "failed";
      uploadMessage.value = uploadErrorMessage(e);
      error.value = uploadMessage.value;
    }
  }
  finally { uploadLoading.value = false; }
}
async function abortUploadSession() {
  if (!uploadSessionId.value) return;
  await http.request("delete", `/resources/${resourceId.value.trim()}/attachments/upload-intents/${uploadSessionId.value}`);
  uploadSessionId.value = "";
  uploadIdempotencyKey.value = "";
}
async function abortUpload() {
  if ((!uploadLoading.value && !uploadSessionId.value && !uploadRequest.value) || !window.confirm("确认终止当前上传并清理临时对象吗？")) return;
  uploadCancelRequested.value = true;
  uploadRequest.value?.abort();
  try {
    await abortUploadSession();
    uploadPhase.value = "aborted";
    uploadMessage.value = "上传已终止，临时对象已请求清理。";
  } catch (e: any) {
    uploadPhase.value = "failed";
    uploadMessage.value = uploadErrorMessage(e);
    error.value = uploadMessage.value;
  }
}
onMounted(() => { const value = route.query.resourceId; if (typeof value === "string" && value) resourceId.value = value; load(); });
onMounted(async () => { try { const result = await http.get<unknown, unknown>("/storage/providers"); providerOptions.value = Array.isArray(result) ? (result as Provider[]).filter(item => String(item.status).toUpperCase() === "ENABLED") : []; if (!providerOptions.value.some(item => item.providerKey === upload.value.provider)) upload.value.provider = providerOptions.value[0]?.providerKey || ""; } catch { providerOptions.value = []; } });
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex flex-wrap items-start justify-between gap-4 mb-6">
      <div><h1 class="text-2xl font-semibold">附件与 Blob</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">Resource → Attachment → Blob → Placement</p></div>
      <div class="flex gap-2"><el-input v-model="resourceId" placeholder="Resource ID（留空查询全部）" clearable @keyup.enter="reload" /><el-button type="primary" :loading="loading" @click="reload">查询附件</el-button><el-button @click="uploadDialog = true; resetUploadState()">上传附件</el-button></div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <section class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
      <el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">Attachment 数量</div><div class="mt-2 text-2xl font-semibold">{{ attachmentCount }}</div></el-card>
      <el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">唯一 Blob</div><div class="mt-2 text-2xl font-semibold">{{ uniqueBlobs }}</div></el-card>
      <el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">可用附件</div><div class="mt-2 text-2xl font-semibold">{{ availableCount }}</div></el-card>
    </section>
    <el-card shadow="never">
      <template #header><div class="flex justify-between"><span class="font-medium">附件列表</span><span class="text-sm text-[var(--el-text-color-secondary)]">{{ resourceId.trim() ? `Resource ${resourceId.trim()}` : '全部 Resource' }}</span></div></template>
      <el-skeleton v-if="loading" :rows="6" animated />
      <el-empty v-else-if="!loaded" description="暂无附件查询结果" />
      <el-empty v-else-if="!attachments.length" :description="allAttachments ? '暂无附件' : '该资源暂无附件'" />
      <el-table v-else :data="attachments" stripe @row-click="row => selected = row">
        <el-table-column prop="fileName" label="文件名" min-width="220" />
        <el-table-column prop="kind" label="角色" width="130" />
        <el-table-column label="Blob / SHA-256" min-width="240"><template #default="{ row }"><div>{{ row.blobId || '-' }}</div><div class="text-xs text-[var(--el-text-color-secondary)]">{{ row.sha256 || '-' }}</div></template></el-table-column>
        <el-table-column prop="sizeBytes" label="大小（Bytes）" width="140" />
        <el-table-column label="Placement" min-width="180"><template #default="{ row }">{{ placementSummary(row) }}</template></el-table-column>
        <el-table-column label="可用性" width="130"><template #default="{ row }"><el-tag :type="['READY', 'AVAILABLE'].includes(String(row.availability).toUpperCase()) ? 'success' : 'danger'">{{ row.availability || '未知' }}</el-tag></template></el-table-column>
      </el-table>
      <el-pagination v-if="allAttachments && loaded" v-model:current-page="page" v-model:page-size="pageSize" class="mt-4 justify-end" :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper" @current-change="changePage" @size-change="changePageSize" />
    </el-card>
    <el-drawer v-model="detailVisible" title="附件详情" size="420px"><template v-if="selected"><el-descriptions :column="1" border><el-descriptions-item label="Attachment ID">{{ selected.id }}</el-descriptions-item><el-descriptions-item label="Resource ID">{{ selected.resourceId || '-' }}</el-descriptions-item><el-descriptions-item label="文件名">{{ selected.fileName }}</el-descriptions-item><el-descriptions-item label="MIME">{{ selected.mediaType || '-' }}</el-descriptions-item><el-descriptions-item label="Blob ID">{{ selected.blobId || '-' }}</el-descriptions-item><el-descriptions-item label="SHA-256">{{ selected.sha256 || '-' }}</el-descriptions-item><el-descriptions-item label="大小">{{ selected.sizeBytes }} Bytes</el-descriptions-item></el-descriptions><el-button class="mt-4" :disabled="!selected.resourceId" @click="selected.resourceId && $router.push(`/library/${selected.resourceId}`)">打开完整详情</el-button></template></el-drawer>
    <el-dialog v-model="uploadDialog" title="上传附件" width="560px" :close-on-click-modal="false" :close-on-press-escape="!uploadLoading">
      <el-alert title="桌面端文件会先通过短时效 Upload Intent 上传到 Provider，再提交 Attachment 关系。Object Key 留空则由后端生成。" type="info" :closable="false" class="mb-4"/>
      <el-form label-position="top"><el-form-item label="文件" required><input type="file" :disabled="uploadLoading" @change="file = ($event.target as HTMLInputElement).files?.[0] || null; uploadPhase = 'idle'; uploadMessage = ''; error = ''" /></el-form-item><div v-if="file" class="text-sm text-[var(--el-text-color-secondary)] mb-3">{{ file.name }} · {{ file.size.toLocaleString() }} Bytes</div><el-form-item label="Object Key"><el-input v-model="upload.objectKey" :disabled="uploadLoading" placeholder="留空自动生成" /></el-form-item><el-form-item label="Provider"><el-select v-model="upload.provider" :disabled="uploadLoading" class="w-full" placeholder="请选择已启用 Provider"><el-option v-for="provider in providerOptions" :key="provider.providerKey" :label="`${provider.providerKey} (${provider.providerType})`" :value="provider.providerKey" /></el-select></el-form-item><el-form-item label="Attachment 角色"><el-select v-model="upload.kind" :disabled="uploadLoading" class="w-full"><el-option label="原始内容" value="ORIGINAL"/><el-option label="封面" value="COVER"/><el-option label="字幕" value="SUBTITLE"/><el-option label="派生内容" value="DERIVED"/></el-select></el-form-item></el-form>
      <el-progress v-if="uploadLoading || uploadPhase === 'success'" :percentage="uploadProgress" :status="uploadPhase === 'failed' ? 'exception' : uploadPhase === 'success' ? 'success' : undefined" class="mb-3" />
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-3" />
      <el-alert v-if="uploadLoading || uploadMessage || uploadPhase === 'failed' || uploadPhase === 'aborted'" :title="`${uploadPhaseLabel()}：${uploadMessage || '请勿关闭窗口。'}`" :type="uploadPhase === 'failed' ? 'error' : uploadPhase === 'success' ? 'success' : 'info'" :closable="false" class="mb-3" />
      <el-alert v-if="uploadSessionId" title="上传会话已创建；可以终止上传并清理临时对象。" type="warning" :closable="false" />
      <template #footer><el-button :disabled="uploadLoading" @click="uploadDialog = false">取消</el-button><el-button v-if="uploadLoading || uploadSessionId || uploadRequest" type="danger" plain @click="abortUpload">{{ uploadSessionId ? '终止上传' : '取消上传' }}</el-button><el-button type="primary" :loading="uploadLoading" :disabled="!file || !resourceId.trim() || !upload.provider" @click="commitUpload">上传并提交附件</el-button></template>
    </el-dialog>
  </main>
</template>
