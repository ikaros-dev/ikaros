<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { http } from "@/utils/http";
import { registerUser } from "@/api/user";
import { message } from "@/utils/message";

const router = useRouter();
const step = ref(0);
const accepted = ref(false);
const loading = ref(false);
const error = ref("");
const environmentStatus = ref<"UNKNOWN" | "UP" | "DOWN">("UNKNOWN");
const form = reactive({ username: "", displayName: "", email: "", password: "", confirmPassword: "" });
const steps = ["环境检查", "创建初始管理员", "安全与恢复", "基础偏好", "确认初始化"];
const canNext = computed(() => step.value !== 1 || (!!form.username.trim() && !!form.displayName.trim() && form.password.length >= 8 && form.password === form.confirmPassword));

async function next() {
  error.value = "";
  if (step.value === 0) {
    loading.value = true;
    try { const ready: any = await http.get("/health/ready"); environmentStatus.value = String(ready?.status || "DOWN").toUpperCase() === "UP" ? "UP" : "DOWN"; if (environmentStatus.value !== "UP") { error.value = "服务尚未就绪，请检查 application 和数据库"; return; } }
    catch (e: any) { environmentStatus.value = "DOWN"; error.value = e?.response?.data?.detail || e?.message || "无法读取服务就绪状态"; return; }
    finally { loading.value = false; }
  }
  if (canNext.value && step.value < steps.length - 1) step.value += 1;
}

async function finish() {
  if (!accepted.value || !canNext.value) return;
  loading.value = true; error.value = "";
  try { await registerUser({ username: form.username.trim(), password: form.password, displayName: form.displayName.trim(), email: form.email.trim() || undefined }); message("初始管理员创建成功，请登录", { type: "success" }); await router.push("/login"); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "初始化管理员失败，请检查输入后重试"; }
  finally { loading.value = false; }
}
</script>

<template>
  <main class="min-h-screen flex items-center justify-center p-4 bg-[var(--el-bg-color-page)]">
    <el-card shadow="never" class="w-full max-w-3xl">
      <div class="text-center mb-8"><div class="text-2xl font-semibold">Ikaros</div><h1 class="mt-3 text-2xl font-semibold">初始化 Ikaros</h1><p class="mt-2 text-[var(--el-text-color-secondary)]">完成首次部署配置后即可进入登录页。</p></div>
      <el-steps :active="step" finish-status="success" class="mb-8"><el-step v-for="item in steps" :key="item" :title="item" /></el-steps>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
      <el-alert v-if="step === 0 && environmentStatus === 'UP'" title="环境检查通过" type="success" show-icon :closable="false" />
      <el-alert v-if="step === 0 && environmentStatus === 'UNKNOWN'" title="点击下一步检查服务就绪状态" type="info" show-icon :closable="false" />
      <el-form v-if="step === 1" label-position="top" class="max-w-xl mx-auto"><el-form-item label="用户名" required><el-input v-model="form.username" placeholder="请输入初始管理员用户名" /></el-form-item><el-form-item label="显示名称" required><el-input v-model="form.displayName" placeholder="请输入显示名称" /></el-form-item><el-form-item label="邮箱"><el-input v-model="form.email" type="email" placeholder="可选" /></el-form-item><el-form-item label="密码" required><el-input v-model="form.password" type="password" show-password /></el-form-item><el-form-item label="确认密码" required><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item><el-alert v-if="form.password && form.password !== form.confirmPassword" title="两次密码输入不一致" type="error" :closable="false" /></el-form>
      <el-descriptions v-else-if="step === 2" :column="1" border><el-descriptions-item label="Email OTP">由服务端策略决定</el-descriptions-item><el-descriptions-item label="恢复方式">由服务端策略决定</el-descriptions-item><el-descriptions-item label="JWT 策略">Access Token + Refresh Token</el-descriptions-item></el-descriptions>
      <el-form v-else-if="step === 3" label-position="top" class="max-w-xl mx-auto"><el-form-item label="默认语言"><el-select class="w-full" model-value="zh-CN"><el-option label="简体中文" value="zh-CN" /><el-option label="English" value="en-US" /></el-select></el-form-item><el-form-item label="默认时区"><el-input model-value="Asia/Shanghai" /></el-form-item><el-form-item label="主题"><el-radio-group><el-radio label="system">跟随系统</el-radio><el-radio label="light">浅色</el-radio><el-radio label="dark">深色</el-radio></el-radio-group></el-form-item></el-form>
      <div v-else-if="step === 4" class="text-center"><el-alert title="请确认初始化摘要" type="info" :closable="false" class="mb-4" /><el-descriptions :column="1" border class="text-left mb-4"><el-descriptions-item label="管理员用户名">{{ form.username }}</el-descriptions-item><el-descriptions-item label="显示名称">{{ form.displayName }}</el-descriptions-item><el-descriptions-item label="邮箱">{{ form.email || "—" }}</el-descriptions-item><el-descriptions-item label="密码">不会展示或保存明文</el-descriptions-item></el-descriptions><el-checkbox v-model="accepted">我已确认以上配置</el-checkbox></div>
      <div class="flex justify-between mt-8"><el-button :disabled="step === 0 || loading" @click="step--">上一步</el-button><el-button type="primary" :loading="loading" :disabled="!canNext || (step === steps.length - 1 && !accepted)" @click="step === steps.length - 1 ? finish() : next()">{{ step === steps.length - 1 ? "完成初始化" : "下一步" }}</el-button></div>
    </el-card>
  </main>
</template>
