<script setup lang="ts">
import { reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { registerUser } from "@/api/user";
import type { FormInstance, FormRules } from "element-plus";

const router = useRouter();
const { t } = useI18n();
const formRef = ref<FormInstance>();
const loading = ref(false);
const error = ref("");
const form = reactive({
  username: "",
  displayName: "",
  email: "",
  password: "",
  confirmPassword: ""
});
const rules: FormRules = {
  username: [
    { required: true, message: t("login.pureUsernameReg"), trigger: "blur" },
    {
      pattern: /^[A-Za-z0-9][A-Za-z0-9_.-]*$/,
      message: t("login.usernameHint"),
      trigger: "blur"
    }
  ],
  displayName: [{ required: true, message: t("login.displayName"), trigger: "blur" }],
  email: [
    {
      required: true,
      type: "email",
      message: t("login.validEmail"),
      trigger: "blur"
    }
  ],
  password: [
    {
      required: true,
      min: 8,
      max: 128,
      message: t("login.passwordLength"),
      trigger: "blur"
    }
  ],
  confirmPassword: [
    {
      required: true,
      validator: (_rule, value, callback) =>
        value === form.password
          ? callback()
          : callback(new Error(t("login.confirmPasswordMismatch"))),
      trigger: "blur"
    }
  ]
};

async function submit() {
  if (!(await formRef.value?.validate())) return;
  loading.value = true;
  error.value = "";
  try {
    await registerUser({
      username: form.username,
      password: form.password,
      displayName: form.displayName,
      email: form.email
    });
    message(t("login.registerSuccess"), { type: "success" });
    router.push("/login");
  } catch (e: any) {
    error.value =
      e?.response?.data?.detail || e?.message || t("login.registerFail");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="flex items-center justify-center min-h-screen p-6">
    <el-card class="w-full max-w-[440px]">
      <h2 class="text-2xl font-semibold mb-6">{{ t("login.createAccount") }}</h2>
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        class="mb-4"
      />
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="submit"
      >
        <el-form-item :label="t('login.username')" prop="username"
          ><el-input v-model="form.username" :placeholder="t('login.usernamePlaceholderExample')" />
          <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
            {{ t("login.usernameHint") }}
          </div></el-form-item
        >
        <el-form-item :label="t('login.displayName')" prop="displayName"
          ><el-input v-model="form.displayName"
        /></el-form-item>
        <el-form-item :label="t('login.email')" prop="email"
          ><el-input v-model="form.email"
        /></el-form-item>
        <el-form-item :label="t('login.password')" prop="password"
          ><el-input v-model="form.password" type="password" show-password
        /></el-form-item>
        <el-form-item :label="t('login.confirmPassword')" prop="confirmPassword"
          ><el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
        /></el-form-item>
        <el-button
          type="primary"
          class="w-full"
          :loading="loading"
          @click="submit"
          >{{ t("login.register") }}</el-button
        >
        <div class="text-center mt-4 text-sm">
          <router-link class="text-primary" to="/login">{{ t("login.backToLogin") }}</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>
