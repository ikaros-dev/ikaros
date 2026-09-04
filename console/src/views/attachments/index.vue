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
const uploadDialog = ref(false);
const uploadLoading = ref(false);
const file = ref<File | null>(null);
const providerOptions = ref<Provider[]>([]);
const upload = ref({ objectKey: "", provider: "s3-main", kind: "ORIGINAL" });
const availableCount = computed(() => attachments.value.filter(item => String(item.availability || "").toUpperCase() === "AVAILABLE").length);
const uniqueBlobs = computed(() => new Set(attachments.value.map(item => item.blobId).filter(Boolean)).size);

async function load() {
  if (!resourceId.value.trim()) { error.value = "请输入 Resource ID"; return; }
  loading.value = true; error.value = ""; loaded.value = false; selected.value = null;
  try {
    const result = await http.get<unknown, unknown>(`/resources/${resourceId.value.trim()}/attachments`);
    attachments.value = Array.isArray(result) ? result as Attachment[] : [];
    loaded.value = true;
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件加载失败"; }
  finally { loading.value = false; }
}

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
async function commitUpload() {
  if (!resourceId.value.trim() || !file.value || !upload.value.provider) { error.value = "Resource ID、文件和 Provider 均不能为空"; return; }
  uploadLoading.value = true;
  try {
    const mediaType = file.value.type || "application/octet-stream";
    const digest = await crypto.subtle.digest("SHA-256", await file.value.arrayBuffer());
    const digestBytes = new Uint8Array(digest);
    const sha256 = Array.from(digestBytes).map(byte => byte.toString(16).padStart(2, "0")).join("");
    const checksumSha256 = base64(digestBytes);
    const intent = await http.post<any, any>(`/resources/${resourceId.value.trim()}/attachments/upload-intents`, { data: {
      fileName: file.value.name, sizeBytes: file.value.size, mediaType, provider: upload.value.provider,
      objectKey: upload.value.objectKey || undefined, sha256
    } });
    const uploaded = await fetch(intent.url, { method: intent.method || "PUT", headers: { "Content-Type": mediaType, "x-amz-checksum-sha256": checksumSha256 }, body: file.value });
    if (!uploaded.ok) throw new Error(`对象上传失败（HTTP ${uploaded.status}）`);
    await http.post(`/resources/${resourceId.value.trim()}/attachments/commit`, { data: { sha256, uploadSha256: intent.sha256 || sha256, sizeBytes: file.value.size, mediaType, fileName: file.value.name, kind: upload.value.kind, provider: intent.provider, tier: intent.tier, objectKey: intent.objectKey, idempotencyKey: crypto.randomUUID() } });
    uploadDialog.value = false; await load();
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件提交失败，请确认对象已存在于 Provider"; }
  finally { uploadLoading.value = false; }
}
onMounted(() => { const value = route.query.resourceId; if (typeof value === "string" && value) { resourceId.value = value; load(); } });
onMounted(async () => { try { const result = await http.get<unknown, unknown>("/storage/providers"); providerOptions.value = Array.isArray(result) ? (result as Provider[]).filter(item => String(item.status).toUpperCase() === "ENABLED") : []; if (!providerOptions.value.some(item => item.providerKey === upload.value.provider)) upload.value.provider = providerOptions.value[0]?.providerKey || ""; } catch { providerOptions.value = []; } });
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex flex-wrap items-start justify-between gap-4 mb-6">
      <div><h1 class="text-2xl font-semibold">附件与 Blob</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">Resource → Attachment → Blob → Placement</p></div>
      <div class="flex gap-2"><el-input v-model="resourceId" placeholder="Resource ID" clearable @keyup.enter="load" /><el-button type="primary" :loading="loading" @click="load">查询附件</el-button><el-button @click="uploadDialog = true">上传附件</el-button></div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <section class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
      <el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">Attachment 数量</div><div class="mt-2 text-2xl font-semibold">{{ attachments.length }}</div></el-card>
      <el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">唯一 Blob</div><div class="mt-2 text-2xl font-semibold">{{ uniqueBlobs }}</div></el-card>
      <el-card shadow="never"><div class="text-sm text-[var(--el-text-color-secondary)]">可用附件</div><div class="mt-2 text-2xl font-semibold">{{ availableCount }}</div></el-card>
    </section>
    <el-card shadow="never">
      <template #header><div class="flex justify-between"><span class="font-medium">附件列表</span><span v-if="resourceId" class="text-sm text-[var(--el-text-color-secondary)]">Resource {{ resourceId }}</span></div></template>
      <el-skeleton v-if="loading" :rows="6" animated />
      <el-empty v-else-if="!loaded" description="输入 Resource ID 查询附件" />
      <el-empty v-else-if="!attachments.length" description="该资源暂无附件" />
      <el-table v-else :data="attachments" stripe @row-click="row => selected = row">
        <el-table-column prop="fileName" label="文件名" min-width="220" />
        <el-table-column prop="kind" label="角色" width="130" />
        <el-table-column label="Blob / SHA-256" min-width="240"><template #default="{ row }"><div>{{ row.blobId || '-' }}</div><div class="text-xs text-[var(--el-text-color-secondary)]">{{ row.sha256 || '-' }}</div></template></el-table-column>
        <el-table-column prop="sizeBytes" label="大小（Bytes）" width="140" />
        <el-table-column label="Placement" min-width="180"><template #default="{ row }">{{ placementSummary(row) }}</template></el-table-column>
        <el-table-column label="可用性" width="130"><template #default="{ row }"><el-tag :type="String(row.availability).toUpperCase() === 'AVAILABLE' ? 'success' : 'danger'">{{ row.availability || '未知' }}</el-tag></template></el-table-column>
      </el-table>
    </el-card>
    <el-drawer v-model="detailVisible" title="附件详情" size="420px"><template v-if="selected"><el-descriptions :column="1" border><el-descriptions-item label="Attachment ID">{{ selected.id }}</el-descriptions-item><el-descriptions-item label="文件名">{{ selected.fileName }}</el-descriptions-item><el-descriptions-item label="MIME">{{ selected.mediaType || '-' }}</el-descriptions-item><el-descriptions-item label="Blob ID">{{ selected.blobId || '-' }}</el-descriptions-item><el-descriptions-item label="SHA-256">{{ selected.sha256 || '-' }}</el-descriptions-item><el-descriptions-item label="大小">{{ selected.sizeBytes }} Bytes</el-descriptions-item></el-descriptions><el-button class="mt-4" @click="$router.push(`/storage-center/attachments/${selected.id}`)">打开完整详情</el-button></template></el-drawer>
    <el-dialog v-model="uploadDialog" title="上传附件" width="520px"><el-alert title="选择文件后将先上传到 Provider，再提交 Attachment 关系。Object Key 留空则由后端生成。" type="info" :closable="false" class="mb-4"/><el-form label-position="top"><el-form-item label="文件" required><input type="file" @change="file = ($event.target as HTMLInputElement).files?.[0] || null" /></el-form-item><el-form-item label="Object Key"><el-input v-model="upload.objectKey" placeholder="留空自动生成" /></el-form-item><el-form-item label="Provider"><el-select v-model="upload.provider" class="w-full" placeholder="请选择已启用 Provider"><el-option v-for="provider in providerOptions" :key="provider.providerKey" :label="`${provider.providerKey} (${provider.providerType})`" :value="provider.providerKey" /></el-select></el-form-item><el-form-item label="Attachment 角色"><el-select v-model="upload.kind" class="w-full"><el-option label="原始内容" value="ORIGINAL"/><el-option label="封面" value="COVER"/><el-option label="字幕" value="SUBTITLE"/><el-option label="派生内容" value="DERIVED"/></el-select></el-form-item></el-form><template #footer><el-button @click="uploadDialog = false">取消</el-button><el-button type="primary" :loading="uploadLoading" @click="commitUpload">上传并提交附件</el-button></template></el-dialog>
  </main>
</template>
