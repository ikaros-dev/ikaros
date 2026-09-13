<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { http } from "@/utils/http";
import { hasPerms } from "@/utils/auth";

type AppCard = { title: string; path: string; description: string; source: "core" | "plugin"; capability?: string };
type Plugin = { lifecycle?: string; manifest?: { name?: string; capabilities?: string[]; app?: { path?: string; description?: string } } };
const router = useRouter();
const plugins = ref<Plugin[]>([]);
const coreApps: AppCard[] = [
  ["Drive", "/apps/drive", "个人文件与同步", "drive.space.read"], ["Documents", "/apps/documents", "文档与编辑", "document.read"], ["Media", "/apps/media", "媒体浏览与播放", "media.read"],
  ["Planning", "/apps/planning", "项目、任务与日历", "planning.read"], ["Finance", "/apps/finance", "个人财务与账本", "finance.read"], ["Private Notes", "/apps/private-notes", "受保护的私密笔记", "private_note.read"],
  ["Passwords", "/apps/passwords", "密码与秘密管理", "password.read"], ["AI", "/apps/ai", "AI 助手与 Persona", "ai.read"], ["Sharing", "/apps/sharing", "分享与协作房间", "share.read"],
  ["Analytics", "/apps/analytics", "个人数据洞察", "analytics.read"], ["Automation", "/apps/automation", "自动化规则", "automation.read"]
].map(([title, path, description, capability]) => ({ title, path, description, capability, source: "core" as const })) as AppCard[];
const visibleApps = computed(() => coreApps.filter(app => hasPerms(app.capability || "")));
const pluginApps = computed<AppCard[]>(() => plugins.value.filter(plugin => plugin.lifecycle === "ENABLED" && plugin.manifest?.app?.path?.startsWith("/apps/")).map(plugin => ({
  title: plugin.manifest?.name || "Plugin App", path: plugin.manifest!.app!.path!, description: plugin.manifest?.app?.description || "由插件提供的业务 App", source: "plugin" as const,
  capability: plugin.manifest?.capabilities?.find(capability => capability.endsWith(".read"))
})).filter(app => !app.capability || hasPerms(app.capability)));
const apps = computed(() => [...visibleApps.value, ...pluginApps.value]);
onMounted(async () => { try { const result = await http.get<unknown, unknown>("/plugins"); plugins.value = Array.isArray(result) ? result as Plugin[] : []; } catch { plugins.value = []; } });
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="mb-6"><h1 class="text-2xl font-semibold">Apps</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">只显示当前部署已启用且你有权访问的业务产品。</p></div>
    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
      <el-card v-for="app in apps" :key="app.path" shadow="never" class="cursor-pointer" @click="router.push(app.path)">
        <div class="flex items-start justify-between gap-3"><div><h2 class="text-lg font-medium">{{ app.title }}</h2><p class="mt-2 text-sm text-[var(--el-text-color-secondary)]">{{ app.description }}</p></div><el-tag size="small" :type="app.source === 'plugin' ? 'warning' : 'info'">{{ app.source === 'plugin' ? '插件 App' : '内置 App' }}</el-tag></div>
      </el-card>
    </div>
    <el-empty v-if="!apps.length" description="暂无可用 App" />
  </main>
</template>
