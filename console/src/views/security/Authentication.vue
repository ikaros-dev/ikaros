<script setup lang="ts">
import { ref } from "vue";
import { http } from "@/utils/http";

type Challenge = { id: string; method?: string; purpose?: string; expiresAt?: string; status?: string };
const tab = ref("policy");
const challenge = ref<Challenge | null>(null);
const purpose = ref("LOGIN_STEP_UP");
const code = ref("");
const verificationGrant = ref("");
const loading = ref(false);
const verifying = ref(false);
const error = ref("");
const result = ref("");

async function issue() {
  loading.value = true; error.value = ""; result.value = "";
  try {
    challenge.value = purpose.value === "LOGIN_STEP_UP"
      ? await http.post<Challenge, undefined>("/security/step-up")
      : await http.post<Challenge, { purpose: string }>("/security/verification-challenges", { data: { purpose: purpose.value } });
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "验证码挑战发起失败"; }
  finally { loading.value = false; }
}
async function verify() {
  if (!challenge.value || !/^\d{6}$/.test(code.value)) { error.value = "请输入 6 位验证码"; return; }
  verifying.value = true; error.value = "";
  try {
    const verified: any = purpose.value === "LOGIN_STEP_UP"
      ? await http.post(`/security/step-up/${challenge.value.id}/verify`, { data: { code: code.value } })
      : await http.post(`/security/verification-challenges/${challenge.value.id}/verify`, { data: { code: code.value } });
    verificationGrant.value = verified?.verificationGrant || "";
    challenge.value = { ...challenge.value, status: "VERIFIED" }; code.value = ""; result.value = verificationGrant.value ? "验证成功，已获得短期用途绑定授权。" : "验证成功，已消费本次挑战。";
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "验证码验证失败"; }
  finally { verifying.value = false; }
}
async function cancel() {
  if (!challenge.value) return;
  try { await http.request("delete", `/security/verification-challenges/${challenge.value.id}`); challenge.value = null; verificationGrant.value = ""; code.value = ""; result.value = "挑战已取消。"; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "挑战取消失败"; }
}
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">认证、密钥与恢复</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">查看认证策略和安全材料状态；Secret Material 永不在此明文显示。</p></div><el-button :loading="loading" @click="issue">发起验证</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-alert v-if="result" :title="result" type="success" show-icon :closable="false" class="mb-4" />
    <el-tabs v-model="tab"><el-tab-pane label="认证策略" name="policy" /><el-tab-pane label="密钥与 Secret" name="secrets" /><el-tab-pane label="恢复" name="recovery" /><el-tab-pane label="OAuth/API 访问" name="oauth" /></el-tabs>
    <el-card shadow="never" class="mt-4">
      <template v-if="tab === 'policy'"><div class="grid grid-cols-1 md:grid-cols-2 gap-4"><el-card v-for="item in [{name:'Password / Login Policy',value:'由服务端生效',hint:'登录密码校验与失败策略'},{name:'MFA 要求',value:'由安全策略提供',hint:'高风险操作可能要求二次验证'},{name:'JWT Lifetime',value:'由认证服务提供',hint:'Access Token 过期后使用 Refresh Token'},{name:'Login Rate Limit',value:'由认证服务提供',hint:'失败次数和锁定策略'}]" :key="item.name" shadow="never"><div class="font-medium">{{ item.name }}</div><div class="text-lg mt-3">{{ item.value }}</div><div class="text-sm text-[var(--el-text-color-secondary)] mt-2">{{ item.hint }}</div></el-card></div><el-divider /><el-form label-position="top"><el-form-item label="验证用途"><el-select v-model="purpose"><el-option label="登录增强验证" value="LOGIN_STEP_UP" /><el-option label="修改安全设置" value="CHANGE_SECURITY_SETTING" /><el-option label="导出安全资料" value="EXPORT_SECURE_VAULT" /></el-select></el-form-item><div v-if="challenge" class="flex flex-wrap items-end gap-3"><el-form-item label="Challenge ID" class="flex-1 min-w-64"><el-input :model-value="challenge.id" readonly /></el-form-item><el-form-item label="验证码"><el-input v-model="code" maxlength="6" placeholder="输入邮件中的 6 位验证码" /></el-form-item><el-button type="primary" :loading="verifying" @click="verify">验证</el-button><el-button @click="cancel">取消挑战</el-button></div><el-alert v-else title="点击“发起验证”后，验证码会通过已配置的认证渠道发送；页面不会显示验证码。" type="info" show-icon :closable="false" /></el-form></template>
      <el-empty v-else-if="tab === 'secrets'" description="暂无可展示的密钥目录；仅展示状态，不展示 Secret Material" />
      <template v-else-if="tab === 'recovery'"><el-empty description="Recovery Readiness 由受保护域后端提供，不展示 Recovery Material" /></template>
      <el-empty v-else description="OAuth/API Client 由对应 Provider 管理；Token Secret 不在此展示" />
    </el-card>
  </main>
</template>
