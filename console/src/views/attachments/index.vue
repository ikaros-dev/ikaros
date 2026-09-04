<script setup lang="ts">
import { computed, ref } from "vue";
import { http } from "@/utils/http";

type Placement = Record<string, unknown>;
type Attachment = Record<string, any> & { placements?: Placement[] };
const resourceId = ref("");
const attachments = ref<Attachment[]>([]);
const selected = ref<Attachment | null>(null);
const detailVisible = computed({ get: () => Boolean(selected.value), set: (value: boolean) => { if (!value) selected.value = null; } });
const loading = ref(false);
const error = ref("");
const loaded = ref(false);
const uploadDialog = ref(false);
const uploadLoading = ref(false);
const file = ref<File | null>(null);
const upload = ref({ objectKey: "", provider: "s3-main", kind: "DOCUMENT" });
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
async function commitUpload() {
  if (!resourceId.value.trim() || !file.value || !upload.value.objectKey) { error.value = "Resource ID、文件和 Object Key 均不能为空"; return; }
  uploadLoading.value = true;
  try {
    const digest = await crypto.subtle.digest("SHA-256", await file.value.arrayBuffer());
    const sha256 = Array.from(new Uint8Array(digest)).map(byte => byte.toString(16).padStart(2, "0")).join("");
    await http.post(`/resources/${resourceId.value.trim()}/attachments/commit`, { data: { sha256, sizeBytes: file.value.size, mediaType: file.value.type || "application/octet-stream", fileName: file.value.name, kind: upload.value.kind, provider: upload.value.provider, tier: "HOT", objectKey: upload.value.objectKey, idempotencyKey: crypto.randomUUID() } });
    uploadDialog.value = false; await load();
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "附件提交失败，请确认对象已存在于 Provider"; }
  finally { uploadLoading.value = false; }
}
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
    <el-drawer v-model="detailVisible" title="附件详情" size="420px"><template v-if="selected"><el-descriptions :column="1" border><el-descriptions-item label="Attachment ID">{{ selected.id }}</el-descriptions-item><el-descriptions-item label="文件名">{{ selected.fileName }}</el-descriptions-item><el-descriptions-item label="MIME">{{ selected.mediaType || '-' }}</el-descriptions-item><el-descriptions-item label="Blob ID">{{ selected.blobId || '-' }}</el-descriptions-item><el-descriptions-item label="SHA-256">{{ selected.sha256 || '-' }}</el-descriptions-item><el-descriptions-item label="大小">{{ selected.sizeBytes }} Bytes</el-descriptions-item></el-descriptions><el-button class="mt-4" @click="$router.push(`/console/attachments/${selected.id}`)">打开完整详情</el-button></template></el-drawer>
    <el-dialog v-model="uploadDialog" title="上传附件" width="520px"><el-alert title="后端当前 commit 接口登记已写入 Provider 的对象；请先将文件上传至 S3。" type="info" :closable="false" class="mb-4"/><el-form label-position="top"><el-form-item label="文件" required><input type="file" @change="file = ($event.target as HTMLInputElement).files?.[0] || null" /></el-form-item><el-form-item label="S3 Object Key" required><el-input v-model="upload.objectKey" placeholder="attachments/example.bin" /></el-form-item><el-form-item label="Provider"><el-input v-model="upload.provider" /></el-form-item><el-form-item label="Attachment 角色"><el-input v-model="upload.kind" /></el-form-item></el-form><template #footer><el-button @click="uploadDialog = false">取消</el-button><el-button type="primary" :loading="uploadLoading" @click="commitUpload">提交附件</el-button></template></el-dialog>
  </main>
</template>
