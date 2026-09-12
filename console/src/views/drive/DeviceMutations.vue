<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Row = Record<string, any>;

const bindings = ref<Row[]>([]);
const bindingId = ref("");
const kind = ref("RENAME");
const nodeId = ref("");
const expectedVersion = ref(0);
const name = ref("");
const parentId = ref("");
const operationId = ref(crypto.randomUUID());
const results = ref<Row[]>([]);
const saving = ref(false);
const error = ref("");
const message = ref("");

async function loadBindings() {
  try {
    const result = await http.get<unknown, unknown>("/drive/bindings");
    bindings.value = Array.isArray(result) ? (result as Row[]).filter(item => item.mode !== "BACKUP") : [];
    if (!bindingId.value && bindings.value[0]?.id) bindingId.value = String(bindings.value[0].id);
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "同步配置加载失败";
  }
}

async function submitMutation() {
  if (!bindingId.value || !nodeId.value.trim()) {
    error.value = "请选择同步配置并填写节点 ID";
    return;
  }
  if (kind.value === "RENAME" && !name.value.trim()) {
    error.value = "重命名需要填写新名称";
    return;
  }
  if (kind.value === "MOVE" && !parentId.value.trim()) {
    error.value = "移动需要填写目标父目录 ID";
    return;
  }

  saving.value = true;
  error.value = "";
  message.value = "";
  try {
    const result = await http.post<unknown, unknown>(`/drive/bindings/${bindingId.value}/mutations`, {
      data: [{
        operationId: operationId.value,
        nodeId: nodeId.value.trim(),
        kind: kind.value,
        expectedVersion: expectedVersion.value,
        name: name.value.trim() || undefined,
        parentId: parentId.value.trim() || undefined
      }]
    });
    results.value = Array.isArray(result) ? (result as Row[]) : [];
    message.value = "设备变更已提交，结果已返回";
    operationId.value = crypto.randomUUID();
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "设备变更提交失败";
  } finally {
    saving.value = false;
  }
}

onMounted(loadBindings);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex items-start justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-semibold">设备变更上传</h1>
        <p class="mt-1 text-[var(--el-text-color-secondary)]">提交设备产生的目录操作，服务端按节点版本逐项应用并返回结果。</p>
      </div>
      <el-button @click="loadBindings">刷新配置</el-button>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4" />

    <el-card shadow="never" class="mb-4">
      <template #header><span>提交设备变更</span></template>
      <el-form label-position="top">
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-3">
          <el-form-item label="同步配置" required>
            <el-select v-model="bindingId" class="w-full">
              <el-option v-for="binding in bindings" :key="binding.id" :label="`${binding.localDisplayPath || binding.localScopeId} → ${binding.driveSpaceId}`" :value="binding.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="变更类型" required>
            <el-select v-model="kind" class="w-full">
              <el-option label="重命名" value="RENAME" />
              <el-option label="移动" value="MOVE" />
              <el-option label="移入回收站" value="TRASH" />
              <el-option label="恢复" value="RESTORE" />
            </el-select>
          </el-form-item>
          <el-form-item label="节点 ID" required><el-input v-model="nodeId" placeholder="Drive Node UUID" clearable /></el-form-item>
          <el-form-item label="期望节点版本" required><el-input-number v-model="expectedVersion" :min="0" class="w-full" /></el-form-item>
          <el-form-item label="新名称"><el-input v-model="name" placeholder="重命名时填写" clearable /></el-form-item>
          <el-form-item label="目标父目录 ID"><el-input v-model="parentId" placeholder="移动时填写" clearable /></el-form-item>
        </div>
        <el-button type="primary" :loading="saving" @click="submitMutation">提交变更</el-button>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><span>最近提交结果</span></template>
      <el-empty v-if="!results.length" description="暂无提交结果" />
      <el-table v-else :data="results" stripe>
        <el-table-column prop="operationId" label="操作 ID" min-width="260" />
        <el-table-column prop="applied" label="是否应用" width="120" />
        <el-table-column prop="errorCode" label="错误码" width="220" />
        <el-table-column prop="errorMessage" label="结果说明" min-width="260" show-overflow-tooltip />
        <el-table-column prop="node.id" label="节点 ID" min-width="260" />
      </el-table>
    </el-card>
  </main>
</template>
