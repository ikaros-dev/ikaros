<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const code = ref("");
const email = ref("");
const password = ref("");
const sent = ref(false);
const submitted = ref(false);
const mode = computed(() =>
  route.path.includes("/verify")
    ? "verify"
    : route.path.includes("/reset")
      ? "reset"
      : "request"
);
function next() {
  if (mode.value === "request") {
    sent.value = true;
  } else if (mode.value === "verify")
    router.push({
      path: "/login/recovery/reset",
      query: { transaction: "pending" }
    });
  else submitted.value = true;
}
</script>
<template>
  <main
    class="min-h-screen flex items-center justify-center bg-[var(--el-bg-color-page)] p-6"
  >
    <el-card shadow="never" class="w-full max-w-md"
      ><div class="mb-6">
        <h1 class="text-2xl font-semibold">
          {{
            mode === "request"
              ? t("login.accountRecovery")
              : mode === "verify"
                ? t("login.recoveryVerification")
                : t("login.resetPassword")
          }}
        </h1>
        <p class="mt-2 text-sm text-[var(--el-text-color-secondary)]">
          {{
            mode === "request"
              ? t("login.recoveryRequestDescription")
              : mode === "verify"
                ? t("login.recoveryVerifyDescription")
                : t("login.recoveryResetDescription")
          }}
        </p>
      </div>
      <el-alert
        v-if="submitted"
        :title="t('login.passwordResetSuccess')"
        type="success"
        show-icon
        :closable="false"
      /><el-form v-else label-position="top" @submit.prevent="next"
        ><el-form-item v-if="mode === 'request'" :label="t('login.email')" required
          ><el-input
            v-model="email"
            type="email"
            autocomplete="email" /></el-form-item
        ><el-form-item v-if="mode === 'verify'" :label="t('login.verificationCode')" required
          ><el-input
            v-model="code"
            maxlength="8"
            autocomplete="one-time-code" /></el-form-item
        ><template v-if="mode === 'reset'"
          ><el-form-item :label="t('login.newPassword')" required
            ><el-input
              v-model="password"
              type="password"
              show-password
              autocomplete="new-password" /></el-form-item
          ><el-form-item :label="t('login.confirmNewPassword')" required
            ><el-input
              type="password"
              show-password
              autocomplete="new-password" /></el-form-item></template
        ><el-alert
          v-if="sent"
          :title="t('login.recoveryCodeSent')"
          type="info"
          :closable="false"
          class="mb-4"
        /><el-button type="primary" class="w-full" native-type="submit">{{
          mode === "request"
            ? t("login.sendVerificationCode")
            : mode === "verify"
              ? t("login.verifyAndContinue")
              : t("login.confirmReset")
        }}</el-button></el-form
      ><el-button text class="mt-4 w-full" @click="router.push('/login')"
        >{{ t("login.backToLogin") }}</el-button
      ></el-card
    >
  </main>
</template>
