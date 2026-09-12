<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, unknown>;

const devices = ref<Row[]>([]);
const spaces = ref<Row[]>([]);
const bindings = ref<Row[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const message = ref("");
const form = ref({
  deviceId: "",
  driveSpaceId: "",
  localScopeId: "",
  localDisplayPath: "",
  sourceKind: "DIRECTORY",
  deletePolicy: "KEEP_REMOTE",
  conflictPolicy: "PRESERVE_BOTH"
});

function detail(errorValue: any, fallback: string) {
  return errorValue?.response?.data?.detail || errorValue?.message || fallback;
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
    devices.value = Array.isArray(deviceResult) ? (deviceResult as Row[]) : [];
    spaces.value = Array.isArray(spaceResult) ? (spaceResult as Row[]) : [];
    bindings.value = Array.isArray(bindingResult) ? (bindingResult as Row[]) : [];
    if (!form.value.deviceId && devices.value[0]?.id) {
      form.value.deviceId = String(devices.value[0].id);
    }
    if (!form.value.driveSpaceId && spaces.value[0]?.id) {
      form.value.driveSpaceId = String(spaces.value[0].id);
    }
  } catch (errorValue) {
    error.value = detail(errorValue, "备份目录数据加载失败");
  } finally {
    loading.value = false;
  }
}

async function createBinding() {
  const space = spaces.value.find(item => String(item.id) === form.value.driveSpaceId);
  if (!form.value.deviceId || !space?.rootNodeId || !form.value.localScopeId.trim()) {
    error.value = "请选择设备、云盘空间并填写本地 Scope";
    return;
  }
  saving.value = true;
  error.value = "";
  message.value = "";
  try {
    await http.post("/drive/bindings", {
      data: {
        deviceId: form.value.deviceId,
        driveSpaceId: form.value.driveSpaceId,
        remoteRootNodeId: space.rootNodeId,
        localScopeId: form.value.localScopeId.trim(),
        localDisplayPath: form.value.localDisplayPath.trim() || undefined,
        sourceKind: form.value.sourceKind,
        mode: "BACKUP",
        deletePolicy: form.value.deletePolicy,
        conflictPolicy: form.value.conflictPolicy
      }
    });
    form.value.localScopeId = "";
    form.value.localDisplayPath = "";
    message.value = "备份目录已配置";
    await load();
  } catch (errorValue) {
    error.value = detail(errorValue, "备份目录配置失败");
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex items-start justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-semibold">备份目录</h1>
        <p class="mt-1 text-[var(--el-text-color-secondary)]">将已登记设备的本地 Scope 配置为单向备份到指定 Drive Space。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4" />

    <el-card shadow="never" class="mb-4">
      <template #header><span>配置单向备份</span></template>
      <el-form label-position="top">
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-3">
          <el-form-item label="设备" required>
            <el-select v-model="form.deviceId" class="w-full" placeholder="请选择已登记设备">
              <el-option v-for="device in devices" :key="String(device.id)" :label="`${device.displayName || device.id} · ${device.platform || ''}`" :value="device.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="Drive Space" required>
            <el-select v-model="form.driveSpaceId" class="w-full" placeholder="请选择目标空间">
              <el-option v-for="space in spaces" :key="String(space.id)" :label="String(space.displayName || space.id)" :value="space.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="本地 Scope ID" required>
            <el-input v-model="form.localScopeId" placeholder="客户端稳定 Scope 标识" clearable />
          </el-form-item>
          <el-form-item label="本地显示路径">
            <el-input v-model="form.localDisplayPath" placeholder="仅作为客户端显示信息" clearable />
          </el-form-item>
        </div>
        <div class="flex flex-wrap gap-3 items-end">
          <el-form-item label="来源类型">
            <el-select v-model="form.sourceKind" class="w-44">
              <el-option label="目录" value="DIRECTORY" />
              <el-option label="相机胶卷" value="CAMERA_ROLL" />
              <el-option label="媒体集合" value="MEDIA_COLLECTION" />
            </el-select>
          </el-form-item>
          <el-form-item label="删除策略">
            <el-select v-model="form.deletePolicy" class="w-44">
              <el-option label="保留远端" value="KEEP_REMOTE" />
              <el-option label="移入远端回收站" value="TRASH_REMOTE" />
              <el-option label="传播删除" value="PROPAGATE" />
            </el-select>
          </el-form-item>
          <el-form-item label="冲突策略">
            <el-select v-model="form.conflictPolicy" class="w-44">
              <el-option label="保留双方" value="PRESERVE_BOTH" />
              <el-option label="以远端为准" value="KEEP_REMOTE" />
              <el-option label="以本地为准" value="KEEP_LOCAL" />
            </el-select>
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="createBinding">保存备份配置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><span>已配置备份</span></template>
      <el-skeleton v-if="loading" :rows="5" animated />
      <el-empty v-else-if="!bindings.length" description="暂无备份配置" />
      <el-table v-else :data="bindings" stripe>
        <el-table-column prop="deviceId" label="设备 ID" min-width="220" />
        <el-table-column prop="driveSpaceId" label="Drive Space" min-width="220" />
        <el-table-column prop="localScopeId" label="本地 Scope" min-width="220" />
        <el-table-column prop="localDisplayPath" label="显示路径" min-width="200" />
        <el-table-column prop="mode" label="模式" width="120" />
        <el-table-column prop="state" label="状态" width="120" />
        <el-table-column prop="enabled" label="启用" width="90" />
      </el-table>
    </el-card>
  </main>
</template>
