<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

const subsystems = [
  { title: "资源中心", description: "资源、文档、集合与活动", path: "/resources", icon: "ep:files" },
  { title: "内容与媒体", description: "媒体、阅读、音乐与照片", path: "/media", icon: "ep:video-camera" },
  { title: "存储与云盘", description: "云盘空间、存储与备份", path: "/drive", icon: "ep:box" },
  { title: "身份与安全", description: "用户、角色、权限与会话", path: "/account", icon: "ep:lock" }
];

const today = computed(() => new Intl.DateTimeFormat("zh-CN", {
  dateStyle: "full"
}).format(new Date()));
</script>

<template>
  <main class="p-4 md:p-6">
    <section class="mb-6">
      <p class="text-sm text-[var(--el-text-color-secondary)]">{{ today }}</p>
      <h1 class="mt-2 text-3xl font-semibold">仪表盘</h1>
      <p class="mt-2 text-[var(--el-text-color-secondary)]">Ikaros 管理控制台概览</p>
    </section>

    <el-row :gutter="16" class="mb-6">
      <el-col :xs="24" :sm="12" :lg="6" class="mb-4">
        <el-card shadow="never"><p class="text-sm text-[var(--el-text-color-secondary)]">资源中心</p><strong class="mt-2 block text-2xl">资源管理</strong><span class="mt-1 block text-xs text-[var(--el-text-color-secondary)]">统一管理核心资源</span></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6" class="mb-4">
        <el-card shadow="never"><p class="text-sm text-[var(--el-text-color-secondary)]">系统状态</p><strong class="mt-2 block text-2xl text-green-600">运行中</strong><span class="mt-1 block text-xs text-[var(--el-text-color-secondary)]">服务可正常访问</span></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6" class="mb-4">
        <el-card shadow="never"><p class="text-sm text-[var(--el-text-color-secondary)]">后台任务</p><strong class="mt-2 block text-2xl">任务中心</strong><span class="mt-1 block text-xs text-[var(--el-text-color-secondary)]">查看处理进度</span></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6" class="mb-4">
        <el-card shadow="never"><p class="text-sm text-[var(--el-text-color-secondary)]">安全中心</p><strong class="mt-2 block text-2xl">身份安全</strong><span class="mt-1 block text-xs text-[var(--el-text-color-secondary)]">管理访问控制</span></el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header><span class="font-semibold">子系统快捷入口</span></template>
      <el-row :gutter="16">
        <el-col v-for="item in subsystems" :key="item.path" :xs="24" :sm="12" :lg="6" class="mb-4">
          <el-card shadow="hover" class="cursor-pointer" @click="router.push(item.path)">
            <div class="flex items-center gap-3">
              <IconifyIconOnline :icon="item.icon" width="28" height="28" />
              <div><strong>{{ item.title }}</strong><p class="mt-1 text-xs text-[var(--el-text-color-secondary)]">{{ item.description }}</p></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </main>
</template>
