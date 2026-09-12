<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Photo = Record<string, any>;
type Asset = Record<string, any>;

const photos = ref<Photo[]>([]);
const selected = ref<Photo | null>(null);
const assets = ref<Asset[]>([]);
const attachment = ref<Photo | null>(null);
const previewUrl = ref("");
const photoDrawer = ref(false);
const loading = ref(false);
const saving = ref(false);
const detailLoading = ref(false);
const error = ref("");
const message = ref("");
const dialog = ref(false);
const form = ref({ title: "", attachmentId: "", locale: "zh-CN" });

const primaryAsset = computed(() => assets.value.find(asset => asset.primary) || assets.value[0]);

function fail(errorValue: any, fallback: string) {
  return errorValue?.response?.data?.detail || errorValue?.message || fallback;
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const result: any = await http.get("/photos/timeline");
    photos.value = Array.isArray(result) ? result : result?.items || [];
  } catch (e: any) {
    error.value = fail(e, "图片时间线加载失败");
  } finally {
    loading.value = false;
  }
}

async function openPhoto(photo: Photo) {
  selected.value = photo;
  photoDrawer.value = true;
  assets.value = [];
  attachment.value = null;
  previewUrl.value = "";
  detailLoading.value = true;
  error.value = "";
  try {
    const rows: any = await http.get(`/photos/${photo.id}/assets`);
    assets.value = Array.isArray(rows) ? rows : rows?.items || [];
    const asset = assets.value.find(item => item.primary) || assets.value[0];
    if (asset?.attachmentId) {
      attachment.value = await http.get(`/attachments/${asset.attachmentId}`);
      const preview: any = await http.get(`/attachments/${asset.attachmentId}/preview-url`);
      previewUrl.value = preview?.url || "";
    }
  } catch (e: any) {
    error.value = fail(e, "图片资产或原图授权加载失败");
  } finally {
    detailLoading.value = false;
  }
}

async function createPhoto() {
  if (!form.value.title.trim() || !form.value.attachmentId.trim()) {
    error.value = "标题和 Attachment ID 不能为空";
    return;
  }
  saving.value = true;
  error.value = "";
  message.value = "";
  try {
    const created = await http.post("/photos", {
      data: {
        title: form.value.title.trim(),
        attachmentId: form.value.attachmentId.trim(),
        locale: form.value.locale || undefined
      }
    });
    message.value = "图片已创建；元数据和原图访问均来自服务端授权结果。";
    dialog.value = false;
    form.value = { title: "", attachmentId: "", locale: "zh-CN" };
    await load();
    if (created) await openPhoto(created);
  } catch (e: any) {
    error.value = fail(e, "图片创建失败；请确认 Attachment 存在且属于当前用户");
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex flex-wrap justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-semibold">照片管理</h1>
        <p class="mt-1 text-[var(--el-text-color-secondary)]">从已上传的图片 Attachment 创建照片，并通过授权预览原图。</p>
      </div>
      <div class="flex gap-2">
        <el-button type="primary" @click="dialog = true">添加图片</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4" />
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-empty v-else-if="!photos.length" description="暂无照片；请先在附件管理上传图片" />
    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <el-card v-for="photo in photos" :key="photo.id" shadow="never" class="cursor-pointer" @click="openPhoto(photo)">
        <div class="aspect-video rounded bg-[var(--el-fill-color-light)] flex items-center justify-center overflow-hidden">
          <span class="text-sm text-[var(--el-text-color-secondary)]">{{ photo.width || "?" }} × {{ photo.height || "?" }}</span>
        </div>
        <div class="mt-3 font-medium truncate">{{ photo.id }}</div>
        <div class="mt-1 text-sm text-[var(--el-text-color-secondary)]">{{ photo.captureTime || "未记录拍摄时间" }}</div>
      </el-card>
    </div>

    <el-drawer v-model="photoDrawer" title="照片详情" size="560px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="selected">
        <div v-if="previewUrl" class="rounded bg-black p-2 mb-5 flex justify-center"><img :src="previewUrl" alt="原图预览" class="max-h-[45vh] max-w-full object-contain" /></div>
        <el-empty v-else description="原图预览不可用或未配置分发 Provider" />
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Photo ID">{{ selected.id }}</el-descriptions-item>
          <el-descriptions-item label="Resource ID">{{ selected.resourceId || "—" }}</el-descriptions-item>
          <el-descriptions-item label="尺寸">{{ selected.width || "—" }} × {{ selected.height || "—" }}</el-descriptions-item>
          <el-descriptions-item label="相机">{{ [selected.cameraMake, selected.cameraModel].filter(Boolean).join(" ") || "—" }}</el-descriptions-item>
          <el-descriptions-item label="原图 Attachment">{{ primaryAsset?.attachmentId || "—" }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ attachment?.fileName || "—" }}</el-descriptions-item>
          <el-descriptions-item label="MIME">{{ attachment?.mediaType || "—" }}</el-descriptions-item>
          <el-descriptions-item label="可用性">{{ attachment?.availability || primaryAsset?.availability || "—" }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog v-model="dialog" title="添加图片" width="520px">
      <el-form label-position="top">
        <el-form-item label="标题" required><el-input v-model="form.title" placeholder="例如：旅行照片" /></el-form-item>
        <el-form-item label="图片 Attachment ID" required><el-input v-model="form.attachmentId" placeholder="先在附件管理上传图片，再粘贴 ID" /></el-form-item>
        <el-form-item label="语言"><el-input v-model="form.locale" placeholder="zh-CN" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="createPhoto">创建</el-button></template>
    </el-dialog>
  </main>
</template>
