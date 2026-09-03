<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { registerUser } from "@/api/user";
import type { FormInstance, FormRules } from "element-plus";

const router = useRouter();
const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({ username: "", displayName: "", email: "", password: "", confirmPassword: "" });
const rules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }, { pattern: /^[A-Za-z0-9][A-Za-z0-9_.-]*$/, message: "用户名只能使用字母、数字、下划线、点和连字符", trigger: "blur" }],
  displayName: [{ required: true, message: "请输入显示名称", trigger: "blur" }],
  email: [{ required: true, type: "email", message: "请输入有效邮箱", trigger: "blur" }],
  password: [{ required: true, min: 8, max: 128, message: "密码长度需为 8-128 位", trigger: "blur" }],
  confirmPassword: [{ required: true, validator: (_rule, value, callback) => value === form.password ? callback() : callback(new Error("两次密码不一致")), trigger: "blur" }]
};

async function submit() {
  if (!(await formRef.value?.validate())) return;
  loading.value = true;
  try {
    await registerUser({ username: form.username, password: form.password, displayName: form.displayName, email: form.email });
    message("注册成功，请登录", { type: "success" });
    router.push("/login");
  } finally { loading.value = false; }
}
</script>

<template>
  <div class="flex items-center justify-center min-h-screen p-6">
    <el-card class="w-full max-w-[440px]">
      <h2 class="text-2xl font-semibold mb-6">创建账号</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" placeholder="如 chivehao" /></el-form-item>
        <el-form-item label="显示名称" prop="displayName"><el-input v-model="form.displayName" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="submit">注册</el-button>
        <div class="text-center mt-4 text-sm"><router-link class="text-primary" to="/login">返回登录</router-link></div>
      </el-form>
    </el-card>
  </div>
</template>
