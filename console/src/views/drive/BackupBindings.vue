<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, any>;
const devices = ref<Row[]>([]);
const spaces = ref<Row[]>([]);
const bindings = ref<Row[]>([]);
const loading = ref(false);
const saving = ref(false);
const startingBinding = ref(false);
const resumingBinding = ref(false);
const scopeSaving = ref(false);
const selectedBindingId = ref("");
const cameraScopeKind = ref("ALL_PHOTOS");
const cameraAlbumId = ref("");
const error = ref("");
const message = ref("");
const form = ref({ deviceId: "", driveSpaceId: "", localScopeId: "", localDisplayPath: "", sourceKind: "DIRECTORY", cameraScopeKind: "ALL_PHOTOS", cameraAlbumId: "", deletePolicy: "KEEP_REMOTE", conflictPolicy: "PRESERVE_BOTH" });

function detail(errorValue: any, fallback: string) {
  return errorValue?.response?.data?.detail || errorValue?.message || fallback;
}

function cameraScopeId(kind: string, albumId: string) {
  return kind === "ALBUM" ? `camera-roll:album:${albumId.trim()}` : "camera-roll:all-photos";
}

function selectBinding(bindingId: string) {
  const binding = bindings.value.find(item => String(item.id) === String(bindingId));
  const scope = String(binding?.localScopeId || "");
  if (scope === "camera-roll" || scope === "camera-roll:all-photos") {
    cameraScopeKind.value = "ALL_PHOTOS";
    cameraAlbumId.value = "";
  } else if (scope.startsWith("camera-roll:album:")) {
    cameraScopeKind.value = "ALBUM";
    cameraAlbumId.value = scope.slice("camera-roll:album:".length);
  }
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [deviceResult, spaceResult, bindingResult] = await Promise.all([
      http.get<unknown, unknown>("/sync/devices"),
      http.get<unknown, unknown>("/drive/spaces"),
      http.get<unknown, unknown>("/drive/bindings")
    ]);
    devices.value = Array.isArray(deviceResult) ? deviceResult as Row[] : [];
    spaces.value = Array.isArray(spaceResult) ? spaceResult as Row[] : [];
    bindings.value = Array.isArray(bindingResult) ? bindingResult as Row[] : [];
    if (!form.value.deviceId && devices.value[0]?.id) form.value.deviceId = String(devices.value[0].id);
    if (!form.value.driveSpaceId && spaces.value[0]?.id) form.value.driveSpaceId = String(spaces.value[0].id);
  } catch (errorValue) {
    error.value = detail(errorValue, "备份目录数据加载失败");
  } finally {
    loading.value = false;
  }
}

async function createBinding() {
  const space = spaces.value.find(item => String(item.id) === form.value.driveSpaceId);
  const localScopeId = form.value.sourceKind === "CAMERA_ROLL"
    ? cameraScopeId(form.value.cameraScopeKind, form.value.cameraAlbumId)
    : form.value.localScopeId.trim();
  if (!form.value.deviceId || !space?.rootNodeId || !localScopeId ||
      (form.value.sourceKind === "CAMERA_ROLL" && form.value.cameraScopeKind === "ALBUM" && !form.value.cameraAlbumId.trim())) {
    error.value = form.value.sourceKind === "CAMERA_ROLL" ? "请选择照片范围；指定相册时必须填写相册 ID" : "请选择设备、云盘空间并填写本地 Scope";
    return;
  }
  saving.value = true;
  error.value = "";
  message.value = "";
  try {
    await http.post("/drive/bindings", { data: {
      deviceId: form.value.deviceId,
      driveSpaceId: form.value.driveSpaceId,
      remoteRootNodeId: space.rootNodeId,
      localScopeId,
      localDisplayPath: form.value.localDisplayPath.trim() || undefined,
      sourceKind: form.value.sourceKind,
      mode: "BACKUP",
      deletePolicy: form.value.deletePolicy,
      conflictPolicy: form.value.conflictPolicy
    }});
    form.value.localScopeId = "";
    form.value.cameraAlbumId = "";
    form.value.localDisplayPath = "";
    message.value = "备份目录已配置";
    await load();
  } catch (errorValue) {
    error.value = detail(errorValue, "备份目录配置失败");
  } finally {
    saving.value = false;
  }
}

async function configureCameraScope() {
  const binding = bindings.value.find(item => String(item.id) === String(selectedBindingId.value));
  if (!binding || binding.sourceKind !== "CAMERA_ROLL" || binding.mode !== "BACKUP") {
    error.value = "请选择相机胶卷备份配置";
    return;
  }
  if (cameraScopeKind.value === "ALBUM" && !cameraAlbumId.value.trim()) {
    error.value = "指定相册时必须填写相册 ID";
    return;
  }
  scopeSaving.value = true;
  error.value = "";
  message.value = "";
  try {
    await http.request("put", `/drive/bindings/${selectedBindingId.value}/camera-backup-scope`, { data: {
      scopeKind: cameraScopeKind.value,
      albumId: cameraScopeKind.value === "ALBUM" ? cameraAlbumId.value.trim() : undefined
    }});
    message.value = "照片备份范围已保存";
    await load();
    selectBinding(selectedBindingId.value);
  } catch (errorValue) {
    error.value = detail(errorValue, "照片备份范围保存失败");
  } finally {
    scopeSaving.value = false;
  }
}

async function startInitialBackup() {
  if (!selectedBindingId.value) {
    error.value = "请选择备份配置";
    return;
  }
  startingBinding.value = true;
  error.value = "";
  message.value = "";
  try {
    await http.post(`/drive/bindings/${selectedBindingId.value}/full-resync`);
    message.value = "首次备份已提交，绑定已进入重新扫描流程";
    await load();
  } catch (errorValue) {
    error.value = detail(errorValue, "首次备份提交失败");
  } finally {
    startingBinding.value = false;
  }
}

async function resumeInterruptedBackup() {
  if (!selectedBindingId.value) {
    error.value = "请选择备份配置";
    return;
  }
  const binding = bindings.value.find(item => String(item.id) === selectedBindingId.value);
  if (String(binding?.state) !== "DEGRADED") {
    error.value = "当前绑定没有可恢复的中断备份";
    return;
  }
  resumingBinding.value = true;
  error.value = "";
  message.value = "";
  try {
    await http.post(`/drive/bindings/${selectedBindingId.value}/resume`);
    message.value = "中断备份已恢复，绑定重新进入活动状态";
    await load();
  } catch (errorValue) {
    error.value = detail(errorValue, "中断备份恢复失败");
  } finally {
    resumingBinding.value = false;
  }
}

onMounted(load);
</script>

<template>
  <el-card shadow="never" class="mx-4 mt-4">
    <template #header><span>备份操作</span></template>
    <div class="flex flex-wrap gap-3 items-center">
      <el-select v-model="selectedBindingId" placeholder="选择备份配置" class="w-96" @change="selectBinding">
        <el-option v-for="binding in bindings.filter(item => item.mode === 'BACKUP')" :key="binding.id" :label="`${binding.localDisplayPath || binding.localScopeId} → ${binding.driveSpaceId}`" :value="binding.id" />
      </el-select>
      <el-button type="primary" :loading="startingBinding" @click="startInitialBackup">提交首次备份</el-button>
      <el-button :loading="resumingBinding" :disabled="bindings.find(item => String(item.id) === selectedBindingId)?.state !== 'DEGRADED'" @click="resumeInterruptedBackup">恢复中断备份</el-button>
    </div>
    <p class="mt-3 text-sm text-[var(--el-text-color-secondary)]">首次备份从头扫描；恢复操作保留当前游标，从上次中断位置继续。</p>
  </el-card>
  <el-card v-if="bindings.find(item => String(item.id) === String(selectedBindingId))?.sourceKind === 'CAMERA_ROLL'" shadow="never" class="mx-4 mt-4">
    <template #header><span>选择照片备份范围</span></template>
    <div class="flex flex-wrap gap-3 items-end">
      <el-form-item label="照片范围" required>
        <el-select v-model="cameraScopeKind" class="w-56">
          <el-option label="全部照片" value="ALL_PHOTOS" />
          <el-option label="指定相册" value="ALBUM" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="cameraScopeKind === 'ALBUM'" label="相册 ID" required>
        <el-input v-model="cameraAlbumId" class="w-72" placeholder="客户端稳定相册 ID" clearable />
      </el-form-item>
      <el-button type="primary" :loading="scopeSaving" @click="configureCameraScope">保存照片范围</el-button>
    </div>
    <p class="mt-2 text-sm text-[var(--el-text-color-secondary)]">范围会保存到当前设备的相机胶卷备份配置；服务端会拒绝空相册或非相机备份绑定。</p>
  </el-card>
  <main class="p-4 md:p-6">
    <div class="flex items-start justify-between gap-4 mb-6"><div><h1 class="text-2xl font-semibold">备份目录</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">将已登记设备的本地 Scope 配置为单向备份到指定 Drive Space。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never" class="mb-4"><template #header><span>配置单向备份</span></template><el-form label-position="top"><div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-3"><el-form-item label="设备" required><el-select v-model="form.deviceId" class="w-full"><el-option v-for="device in devices" :key="device.id" :label="`${device.displayName || device.id} · ${device.platform || ''}`" :value="device.id" /></el-select></el-form-item><el-form-item label="Drive Space" required><el-select v-model="form.driveSpaceId" class="w-full"><el-option v-for="space in spaces" :key="space.id" :label="space.displayName || space.id" :value="space.id" /></el-select></el-form-item><el-form-item v-if="form.sourceKind !== 'CAMERA_ROLL'" label="本地 Scope ID" required><el-input v-model="form.localScopeId" placeholder="客户端稳定 Scope 标识" clearable /></el-form-item><el-form-item v-else label="照片范围" required><el-select v-model="form.cameraScopeKind" class="w-full"><el-option label="全部照片" value="ALL_PHOTOS" /><el-option label="指定相册" value="ALBUM" /></el-select></el-form-item><el-form-item v-if="form.sourceKind === 'CAMERA_ROLL' && form.cameraScopeKind === 'ALBUM'" label="相册 ID" required><el-input v-model="form.cameraAlbumId" placeholder="客户端稳定相册 ID" clearable /></el-form-item><el-form-item label="本地显示路径"><el-input v-model="form.localDisplayPath" placeholder="仅作为客户端显示信息" clearable /></el-form-item></div><div class="flex flex-wrap gap-3 items-end"><el-form-item label="来源类型"><el-select v-model="form.sourceKind" class="w-44"><el-option label="目录" value="DIRECTORY" /><el-option label="相机胶卷" value="CAMERA_ROLL" /><el-option label="媒体集合" value="MEDIA_COLLECTION" /></el-select></el-form-item><el-form-item label="删除策略"><el-select v-model="form.deletePolicy" class="w-44"><el-option label="保留远端" value="KEEP_REMOTE" /><el-option label="移入远端回收站" value="TRASH_REMOTE" /><el-option label="传播删除" value="PROPAGATE" /></el-select></el-form-item><el-form-item label="冲突策略"><el-select v-model="form.conflictPolicy" class="w-44"><el-option label="保留双方" value="PRESERVE_BOTH" /><el-option label="以远端为准" value="KEEP_REMOTE" /><el-option label="以本地为准" value="KEEP_LOCAL" /></el-select></el-form-item><el-button type="primary" :loading="saving" @click="createBinding">保存备份配置</el-button></div></el-form></el-card>
    <el-card shadow="never"><template #header><span>已配置备份</span></template><el-skeleton v-if="loading" :rows="5" animated /><el-empty v-else-if="!bindings.length" description="暂无备份配置" /><el-table v-else :data="bindings" stripe><el-table-column prop="deviceId" label="设备 ID" min-width="220" /><el-table-column prop="driveSpaceId" label="Drive Space" min-width="220" /><el-table-column prop="localScopeId" label="本地 Scope" min-width="220" /><el-table-column prop="localDisplayPath" label="显示路径" min-width="200" /><el-table-column prop="mode" label="模式" width="120" /><el-table-column prop="state" label="状态" width="120" /><el-table-column prop="enabled" label="启用" width="90" /></el-table></el-card>
  </main>
</template>
