<script setup lang="ts">
import { ref } from "vue";
import { http } from "@/utils/http";

type Share = Record<string, any>;
type AccessFailure = { code?: string; reason?: string; detail?: string; status?: number };
const token = ref("");
const result = ref<Share | null>(null);
const loading = ref(false);
const error = ref("");
const failure = ref<AccessFailure | null>(null);
const failureLabels: Record<string, string> = { MISSING_TOKEN: "缺少分享令牌", INVALID_TOKEN: "分享链接无效或已失效", REVOKED: "分享链接已撤销", EXPIRED: "分享链接已过期" };
async function redeem() {
  if (!token.value.trim()) { failure.value = { reason: "MISSING_TOKEN" }; error.value = failureLabels.MISSING_TOKEN; return; }
  loading.value = true; error.value = ""; failure.value = null; result.value = null;
  try { result.value = await http.post(`/shares/redeem?token=${encodeURIComponent(token.value.trim())}`) as Share; }
  catch (e: any) {
    const data = e?.response?.data as AccessFailure | undefined;
    failure.value = data || { code: "share.access.unknown" };
    error.value = failureLabels[data?.reason || ""] || data?.detail || e?.message || "分享访问失败";
  }
  finally { loading.value = false; }
}
</script>
<template>
  <main class="p-4 md:p-6"><div class="mb-6"><h1 class="text-2xl font-semibold">验证分享</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">通过 Share Grant 验证链接令牌；服务端只返回安全的失败原因，不会回显令牌。</p></div><el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4"><template #default><div>{{ error }}</div><div v-if="failure?.code" class="text-xs mt-1">错误码：{{ failure.code }}</div></template></el-alert><el-card shadow="never" class="max-w-2xl"><el-form label-position="top" @submit.prevent="redeem"><el-form-item label="分享令牌" required><el-input v-model="token" clearable placeholder="粘贴 token"/></el-form-item><el-button type="primary" :loading="loading" @click="redeem">验证访问</el-button></el-form><el-descriptions v-if="result" :column="1" border class="mt-5"><el-descriptions-item label="目标类型">{{ result.targetType }}</el-descriptions-item><el-descriptions-item label="目标 ID">{{ result.targetId }}</el-descriptions-item><el-descriptions-item label="授予方式">{{ result.granteeType }}</el-descriptions-item><el-descriptions-item label="能力">{{ result.capabilities }}</el-descriptions-item><el-descriptions-item label="过期时间">{{ result.expiresAt }}</el-descriptions-item></el-descriptions></el-card></main>
</template>
