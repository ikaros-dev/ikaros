<script setup lang="ts">
import { ref } from "vue";
import { http } from "@/utils/http";

type Share = Record<string, any>;
const token = ref("");
const result = ref<Share | null>(null);
const loading = ref(false);
const error = ref("");
async function redeem() {
  if (!token.value.trim()) { error.value = "请输入分享令牌"; return; }
  loading.value = true; error.value = ""; result.value = null;
  try { result.value = await http.post(`/shares/redeem?token=${encodeURIComponent(token.value.trim())}`) as Share; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "分享访问失败"; }
  finally { loading.value = false; }
}
</script>
<template>
  <main class="p-4 md:p-6"><div class="mb-6"><h1 class="text-2xl font-semibold">验证分享</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">通过真实 Share Grant API 验证链接令牌；过期、撤销或无效令牌会展示服务端错误。</p></div><el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4"/><el-card shadow="never" class="max-w-2xl"><el-form label-position="top" @submit.prevent="redeem"><el-form-item label="分享令牌" required><el-input v-model="token" clearable placeholder="粘贴 token"/></el-form-item><el-button type="primary" :loading="loading" @click="redeem">验证访问</el-button></el-form><el-descriptions v-if="result" :column="1" border class="mt-5"><el-descriptions-item label="目标类型">{{ result.targetType }}</el-descriptions-item><el-descriptions-item label="目标 ID">{{ result.targetId }}</el-descriptions-item><el-descriptions-item label="授予方式">{{ result.granteeType }}</el-descriptions-item><el-descriptions-item label="能力">{{ result.capabilities }}</el-descriptions-item><el-descriptions-item label="过期时间">{{ result.expiresAt }}</el-descriptions-item></el-descriptions></el-card></main>
</template>
