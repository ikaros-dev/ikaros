<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Device = {
  id: string;
  installationId: string;
  displayName: string;
  platform: string;
  appVersion?: string;
  trustState: string;
  registeredAt?: string;
  lastSeenAt?: string;
};

const devices = ref<Device[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const dialog = ref(false);
const form = ref({ installationId: "", displayName: "", platform: "ANDROID", appVersion: "" });

function messageOf(errorValue: any, fallback: string) {
  return errorValue?.response?.data?.detail || errorValue?.message || fallback;
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const result = await http.get<unknown, unknown>("/sync/devices");
    devices.value = Array.isArray(result) ? (result as Device[]) : [];
  } catch (e: any) {
    error.value = messageOf(e, "同步设备加载失败");
  } finally {
    loading.value = false;
  }
}

async function register() {
  if (!form.value.installationId.trim() || !form.value.displayName.trim() || !form.value.platform.trim()) {
    error.value = "安装标识、设备名称和平台不能为空";
    return;
  }
  saving.value = true;
  error.value = "";
  try {
    await http.post("/sync/devices", {
      data: {
        installationId: form.value.installationId.trim(),
        displayName: form.value.displayName.trim(),
        platform: form.value.platform.trim(),
        appVersion: form.value.appVersion.trim() || undefined
      }
    });
    dialog.value = false;
    form.value = { installationId: "", displayName: "", platform: "ANDROID", appVersion: "" };
    await load();
  } catch (e: any) {
    error.value = messageOf(e, "同步设备登记失败");
  } finally {
    saving.value = false;
  }
}

function stateLabel(state: string) {
  return ({ ACTIVE: "已登记", LIMITED: "受限", REVOKED: "已撤销" } as Record<string, string>)[state] || state;
}

onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="mb-6 flex items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold">同步设备</h1>
        <p class="mt-1 text-[var(--el-text-color-secondary)]">登记允许访问离线同步与设备备份的客户端设备。</p>
      </div>
      <div class="flex gap-2">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="dialog = true">登记设备</el-button>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never">
      <el-skeleton v-if="loading && !devices.length" :rows="5" animated />
      <el-empty v-else-if="!devices.length" description="暂无已登记设备" />
      <el-table v-else :data="devices" stripe>
        <el-table-column prop="displayName" label="设备名称" min-width="160" />
        <el-table-column prop="platform" label="平台" width="130" />
        <el-table-column prop="appVersion" label="客户端版本" width="140" />
        <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="row.trustState === 'ACTIVE' ? 'success' : row.trustState === 'REVOKED' ? 'danger' : 'warning'">{{ stateLabel(row.trustState) }}</el-tag></template></el-table-column>
        <el-table-column prop="installationId" label="安装标识" min-width="220" />
        <el-table-column prop="lastSeenAt" label="最近活动" min-width="190" />
      </el-table>
    </el-card>
    <el-dialog v-model="dialog" title="登记同步设备" width="520px">
      <el-form label-position="top">
        <el-form-item label="安装标识" required><el-input v-model="form.installationId" placeholder="客户端安装实例的稳定标识" /></el-form-item>
        <el-form-item label="设备名称" required><el-input v-model="form.displayName" placeholder="例如：我的 Android 手机" /></el-form-item>
        <el-form-item label="平台" required><el-select v-model="form.platform" class="w-full"><el-option label="Android" value="ANDROID" /><el-option label="iOS" value="IOS" /><el-option label="Windows" value="WINDOWS" /><el-option label="macOS" value="MACOS" /><el-option label="Linux" value="LINUX" /></el-select></el-form-item>
        <el-form-item label="客户端版本"><el-input v-model="form.appVersion" placeholder="例如：1.0.0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="register">登记</el-button></template>
    </el-dialog>
  </main>
</template>
