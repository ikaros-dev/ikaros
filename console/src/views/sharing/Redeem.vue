<script setup lang="ts">
import { ref } from "vue";
import { http } from "@/utils/http";

type Share = Record<string, any>;
type AccessFailure = { code?: string; reason?: string; detail?: string; status?: number };
const token = ref("");
const password = ref("");
const result = ref<Share | null>(null);
const loading = ref(false);
const error = ref("");
const checkMessage = ref("");
const failure = ref<AccessFailure | null>(null);
const failureLabels: Record<string, string> = { MISSING_TOKEN: "缺少分享令牌", INVALID_TOKEN: "分享链接无效或已失效", REVOKED: "分享链接已撤销", EXPIRED: "分享链接已过期" };
async function redeem() {
  if (!token.value.trim()) { failure.value = { reason: "MISSING_TOKEN" }; error.value = failureLabels.MISSING_TOKEN; return; }
  loading.value = true; error.value = ""; checkMessage.value = ""; failure.value = null; result.value = null;
  try {
    const query = `token=${encodeURIComponent(token.value.trim())}${password.value ? `&password=${encodeURIComponent(password.value)}` : ""}`;
    result.value = await http.post(`/shares/redeem?${query}`) as Share;
    checkMessage.value = "服务端已重新检查当前分享状态，当前令牌仍可访问。";
  }
  catch (e: any) {
    const data = e?.response?.data as AccessFailure | undefined;
    failure.value = data || { code: "share.access.unknown" };
    error.value = failureLabels[data?.reason || ""] || data?.detail || e?.message || "分享访问失败";
    checkMessage.value = "本次访问已由服务端重新判定，未沿用之前的成功结果。";
  }
  finally { loading.value = false; }
}
</script>
<template>
  <main class="p-4 md:p-6"><div class="mb-6"><h1 class="text-2xl font-semibold">验证分享</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">每次验证都会重新读取服务端 Share Grant 状态；分享被撤销后，再次检查会立即拒绝访问。</p></div><el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4"><template #default><div>{{ error }}</div><div v-if="failure?.code" class="text-xs mt-1">错误码：{{ failure.code }}</div></template></el-alert><el-alert v-if="checkMessage" :title="checkMessage" :type="result ? 'success' : 'warning'" show-icon :closable="false" class="mb-4"/><el-card shadow="never" class="max-w-2xl"><el-form label-position="top" @submit.prevent="redeem"><el-form-item label="分享令牌" required><el-input v-model="token" clearable placeholder="粘贴 token"/></el-form-item><el-form-item label="访问密码"><el-input v-model="password" type="password" show-password placeholder="如分享设置了密码，请输入"/></el-form-item><el-button type="primary" :loading="loading" @click="redeem">验证访问</el-button><el-button v-if="result || failure" :loading="loading" @click="redeem">再次检查访问</el-button></el-form><el-descriptions v-if="result" :column="1" border class="mt-5"><el-descriptions-item label="目标类型">{{ result.targetType }}</el-descriptions-item><el-descriptions-item label="目标 ID">{{ result.targetId }}</el-descriptions-item><el-descriptions-item label="授予方式">{{ result.granteeType }}</el-descriptions-item><el-descriptions-item label="能力">{{ result.capabilities }}</el-descriptions-item><el-descriptions-item label="过期时间">{{ result.expiresAt }}</el-descriptions-item></el-descriptions></el-card></main>
</template>
