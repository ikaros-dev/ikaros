<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { http } from "@/utils/http";
import { registerUser } from "@/api/user";
import { message } from "@/utils/message";

const router = useRouter();
const { t } = useI18n();
const step = ref(0);
const accepted = ref(false);
const loading = ref(false);
const error = ref("");
const environmentStatus = ref<"UNKNOWN" | "UP" | "DOWN">("UNKNOWN");
const form = reactive({
  username: "",
  displayName: "",
  email: "",
  password: "",
  confirmPassword: ""
});
const steps = computed(() => [
  t("setup.environmentCheck"),
  t("setup.createInitialAdmin"),
  t("setup.securityAndRecovery"),
  t("setup.basicPreferences"),
  t("setup.confirmInitialization")
]);
const canNext = computed(
  () =>
    step.value !== 1 ||
    (!!form.username.trim() &&
      !!form.displayName.trim() &&
      form.password.length >= 8 &&
      form.password === form.confirmPassword)
);

async function next() {
  error.value = "";
  if (step.value === 0) {
    loading.value = true;
    try {
      const ready: any = await http.get("/health/ready");
      environmentStatus.value =
        String(ready?.status || "DOWN").toUpperCase() === "UP" ? "UP" : "DOWN";
      if (environmentStatus.value !== "UP") {
        error.value = t("setup.serviceNotReady");
        return;
      }
    } catch (e: any) {
      environmentStatus.value = "DOWN";
      error.value =
        e?.response?.data?.detail || e?.message || t("setup.cannotReadReadiness");
      return;
    } finally {
      loading.value = false;
    }
  }
  if (canNext.value && step.value < steps.value.length - 1) step.value += 1;
}

async function finish() {
  if (!accepted.value || !canNext.value) return;
  loading.value = true;
  error.value = "";
  try {
    await registerUser({
      username: form.username.trim(),
      password: form.password,
      displayName: form.displayName.trim(),
      email: form.email.trim() || undefined
    });
    message(t("setup.adminCreated"), { type: "success" });
    await router.push("/login");
  } catch (e: any) {
    error.value =
      e?.response?.data?.detail ||
      e?.message ||
      t("setup.adminCreateFailed");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main
    class="min-h-screen flex items-center justify-center p-4 bg-[var(--el-bg-color-page)]"
  >
    <el-card shadow="never" class="w-full max-w-3xl">
      <div class="text-center mb-8">
        <div class="text-2xl font-semibold">Ikaros</div>
        <h1 class="mt-3 text-2xl font-semibold">{{ t("setup.title") }}</h1>
        <p class="mt-2 text-[var(--el-text-color-secondary)]">
          {{ t("setup.description") }}
        </p>
      </div>
      <el-steps :active="step" finish-status="success" class="mb-8"
        ><el-step v-for="item in steps" :key="item" :title="item"
      /></el-steps>
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        class="mb-4"
      />
      <el-alert
        v-if="step === 0 && environmentStatus === 'UP'"
        :title="t('setup.environmentReady')"
        type="success"
        show-icon
        :closable="false"
      />
      <el-alert
        v-if="step === 0 && environmentStatus === 'UNKNOWN'"
        :title="t('setup.checkEnvironment')"
        type="info"
        show-icon
        :closable="false"
      />
      <el-form v-if="step === 1" label-position="top" class="max-w-xl mx-auto"
        ><el-form-item :label="t('login.username')" required
          ><el-input
            v-model="form.username"
            :placeholder="t('setup.initialAdminUsername')" /></el-form-item
        ><el-form-item :label="t('login.displayName')" required
          ><el-input
            v-model="form.displayName"
            :placeholder="t('login.displayName')" /></el-form-item
        ><el-form-item :label="t('login.email')"
          ><el-input
            v-model="form.email"
            type="email"
            :placeholder="t('setup.optional')" /></el-form-item
        ><el-form-item :label="t('login.password')" required
          ><el-input
            v-model="form.password"
            type="password"
            show-password /></el-form-item
        ><el-form-item :label="t('login.confirmPassword')" required
          ><el-input
            v-model="form.confirmPassword"
            type="password"
            show-password /></el-form-item
        ><el-alert
          v-if="form.password && form.password !== form.confirmPassword"
          :title="t('login.confirmPasswordMismatch')"
          type="error"
          :closable="false"
      /></el-form>
      <el-descriptions v-else-if="step === 2" :column="1" border
        ><el-descriptions-item label="Email OTP"
          >{{ t("setup.serverPolicy") }}</el-descriptions-item
        ><el-descriptions-item :label="t('login.accountRecovery')"
          >{{ t("setup.serverPolicy") }}</el-descriptions-item
        ><el-descriptions-item :label="t('setup.jwtPolicy')"
          >Access Token + Refresh Token</el-descriptions-item
        ></el-descriptions
      >
      <el-form
        v-else-if="step === 3"
        label-position="top"
        class="max-w-xl mx-auto"
        ><el-form-item :label="t('setup.defaultLanguage')"
          ><el-select class="w-full" model-value="zh-CN"
            ><el-option :label="t('setup.simplifiedChinese')" value="zh-CN" /><el-option
              label="English"
              value="en-US" /></el-select></el-form-item
        ><el-form-item :label="t('setup.defaultTimezone')"
          ><el-input model-value="Asia/Shanghai" /></el-form-item
        ><el-form-item :label="t('setup.theme')"
          ><el-radio-group
            ><el-radio label="system">{{ t("setup.followSystem") }}</el-radio
            ><el-radio label="light">{{ t("setup.light") }}</el-radio
            ><el-radio label="dark">{{ t("setup.dark") }}</el-radio></el-radio-group
          ></el-form-item
        ></el-form
      >
      <div v-else-if="step === 4" class="text-center">
        <el-alert
          :title="t('setup.initializationSummary')"
          type="info"
          :closable="false"
          class="mb-4"
        /><el-descriptions :column="1" border class="text-left mb-4"
          ><el-descriptions-item :label="t('setup.initialAdminUsername')">{{
            form.username
          }}</el-descriptions-item
          ><el-descriptions-item :label="t('login.displayName')">{{
            form.displayName
          }}</el-descriptions-item
          ><el-descriptions-item :label="t('login.email')">{{
            form.email || "—"
          }}</el-descriptions-item
          ><el-descriptions-item :label="t('login.password')"
            >{{ t("setup.passwordNotStored") }}</el-descriptions-item
        ></el-descriptions
        ><el-checkbox v-model="accepted">{{ t("setup.confirmConfiguration") }}</el-checkbox>
      </div>
      <div class="flex justify-between mt-8">
        <el-button :disabled="step === 0 || loading" @click="step--"
          >{{ t("setup.previousStep") }}</el-button
        ><el-button
          type="primary"
          :loading="loading"
          :disabled="!canNext || (step === steps.length - 1 && !accepted)"
          @click="step === steps.length - 1 ? finish() : next()"
          >{{ step === steps.length - 1 ? t("setup.finishInitialization") : t("setup.nextStep") }}</el-button
        >
      </div>
    </el-card>
  </main>
</template>
