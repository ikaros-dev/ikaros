<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, any>;

const bindings = ref<Row[]>([]);
const conflicts = ref<Row[]>([]);
const bindingId = ref("");
const nodeId = ref("");
const baseRevisionId = ref("");
const remoteRevisionId = ref("");
const localFingerprint = ref("");
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const message = ref("");

const selectedBinding = () => bindings.value.find(item => String(item.id) === bindingId.value);
const detail = (errorValue: any, fallback: string) => errorValue?.response?.data?.detail || errorValue?.message || fallback;

async function loadBindings() {
  try {
    const result = await http.get<unknown, unknown>("/drive/bindings");
    bindings.value = Array.isArray(result) ? (result as Row[]).filter(item => item.mode !== "BACKUP") : [];
    if (!bindingId.value && bindings.value[0]?.id) bindingId.value = String(bindings.value[0].id);
  } catch (e: any) {
    error.value = detail(e, "同步配置加载失败");
  }
}

async function loadConflicts() {
  if (!selectedBinding()) {
    conflicts.value = [];
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const result = await http.get<unknown, unknown>(`/drive/bindings/${bindingId.value}/conflicts`);
    conflicts.value = Array.isArray(result) ? result as Row[] : [];
  } catch (e: any) {
    error.value = detail(e, "冲突记录加载失败");
  } finally {
    loading.value = false;
  }
}

async function detectConflict() {
  if (!bindingId.value || !nodeId.value.trim()) {
    error.value = "请选择同步配置并填写节点 ID";
    return;
  }
  saving.value = true;
  error.value = "";
  message.value = "";
  try {
    await http.post("/drive/conflicts", {
      data: {
        bindingId: bindingId.value,
        nodeId: nodeId.value.trim(),
        baseRevisionId: baseRevisionId.value.trim() || undefined,
        remoteRevisionId: remoteRevisionId.value.trim() || undefined,
        localFingerprint: localFingerprint.value.trim() || undefined
      }
    });
    message.value = "冲突已保留为 OPEN 记录，双方版本信息未被覆盖";
    nodeId.value = "";
    baseRevisionId.value = "";
    remoteRevisionId.value = "";
    localFingerprint.value = "";
    await loadConflicts();
  } catch (e: any) {
    error.value = detail(e, "冲突记录失败");
  } finally {
    saving.value = false;
  }
}

async function resolve(id: string, state: "RESOLVED" | "DISMISSED") {
  if (!window.confirm(state === "RESOLVED" ? "确认已处理此冲突？" : "确认忽略此冲突？")) return;
  try {
    await http.post(`/drive/conflicts/${id}/resolve`, { params: { state } });
    message.value = state === "RESOLVED" ? "冲突已标记为已解决" : "冲突已忽略";
    await loadConflicts();
  } catch (e: any) {
    error.value = detail(e, "冲突处理失败");
  }
}

onMounted(async () => {
  await loadBindings();
  await loadConflicts();
});
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex items-start justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-semibold">同步冲突</h1>
        <p class="mt-1 text-[var(--el-text-color-secondary)]">保留本地与远端版本信息，冲突不会静默覆盖任一方内容。</p>
      </div>
      <el-button :loading="loading" @click="loadConflicts">刷新</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never" class="mb-4">
      <template #header><span>登记冲突</span></template>
      <div class="flex flex-wrap gap-3 items-end">
        <el-form-item label="同步配置" required>
          <el-select v-model="bindingId" class="w-96" @change="loadConflicts">
            <el-option v-for="binding in bindings" :key="binding.id" :label="`${binding.localDisplayPath || binding.localScopeId} → ${binding.driveSpaceId}`" :value="binding.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="节点 ID" required><el-input v-model="nodeId" class="w-64" clearable /></el-form-item>
        <el-form-item label="基础版本 ID"><el-input v-model="baseRevisionId" class="w-64" clearable /></el-form-item>
        <el-form-item label="远端版本 ID"><el-input v-model="remoteRevisionId" class="w-64" clearable /></el-form-item>
        <el-form-item label="本地内容指纹"><el-input v-model="localFingerprint" class="w-64" clearable /></el-form-item>
        <el-button type="primary" :loading="saving" @click="detectConflict">保留冲突</el-button>
      </div>
    </el-card>
    <el-card shadow="never">
      <template #header><span>冲突记录</span></template>
      <el-skeleton v-if="loading" :rows="5" animated />
      <el-empty v-else-if="!conflicts.length" description="暂无冲突记录" />
      <el-table v-else :data="conflicts" stripe>
        <el-table-column prop="nodeId" label="节点 ID" min-width="250" />
        <el-table-column prop="state" label="状态" width="130" />
        <el-table-column prop="baseRevisionId" label="基础版本" min-width="240" />
        <el-table-column prop="remoteRevisionId" label="远端版本" min-width="240" />
        <el-table-column prop="localFingerprint" label="本地指纹" min-width="220" />
        <el-table-column prop="detectedAt" label="检测时间" min-width="190" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.state === 'OPEN'" link type="primary" @click="resolve(String(row.id), 'RESOLVED')">标记已解决</el-button>
            <el-button v-if="row.state === 'OPEN'" link @click="resolve(String(row.id), 'DISMISSED')">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </main>
</template>
