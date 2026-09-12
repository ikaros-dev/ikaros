<script setup lang="ts">
import { ref } from "vue";
import { http } from "@/utils/http";

type Download = Record<string, any>;

const deviceId = ref("");
const downloads = ref<Download[]>([]);
const loading = ref(false);
const actionId = ref("");
const error = ref("");
const message = ref("");

async function load() {
  const id = deviceId.value.trim();
  if (!id) {
    error.value = "请输入 Device ID";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const result = await http.get<unknown, unknown>(`/offline/downloads?deviceId=${encodeURIComponent(id)}`);
    downloads.value = Array.isArray(result) ? (result as Download[]) : [];
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "下载任务加载失败";
  } finally {
    loading.value = false;
  }
}

async function updateState(row: Download, state: "PAUSED" | "DOWNLOADING") {
  const id = String(row.id || "");
  if (!id || !window.confirm(state === "PAUSED" ? "确认暂停此下载任务？" : "确认继续此下载任务？")) return;
  actionId.value = id;
  error.value = "";
  message.value = "";
  try {
    await http.request("patch", `/offline/downloads/${id}`, { data: { state } });
    message.value = state === "PAUSED" ? "下载任务已暂停" : "下载任务已继续";
    await load();
  } catch (e: any) {
    error.value = e?.response?.data?.detail || e?.message || "下载状态更新失败";
  } finally {
    actionId.value = "";
  }
}
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex flex-wrap justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-semibold">下载管理</h1>
        <p class="mt-1 text-[var(--el-text-color-secondary)]">查看设备下载任务，并暂停或继续客户端执行。</p>
      </div>
      <div class="flex gap-2">
        <el-input v-model="deviceId" placeholder="Device ID" clearable @keyup.enter="load" />
        <el-button type="primary" :loading="loading" @click="load">查询</el-button>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4" />
    <el-card shadow="never">
      <el-empty v-if="!downloads.length" description="暂无下载任务" />
      <el-table v-else :data="downloads" stripe>
        <el-table-column prop="resourceId" label="Resource" min-width="220" />
        <el-table-column prop="attachmentId" label="Attachment" min-width="220" />
        <el-table-column prop="kind" label="类型" width="120" />
        <el-table-column prop="state" label="状态" width="140" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="190" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="row.state === 'DOWNLOADING'" link :loading="actionId === row.id" @click="updateState(row, 'PAUSED')">暂停</el-button>
            <el-button v-if="row.state === 'PAUSED'" link type="primary" :loading="actionId === row.id" @click="updateState(row, 'DOWNLOADING')">继续</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </main>
</template>
