<script setup lang="ts">
import { ref } from "vue";
const tab = ref("audit"); const query = ref(""); const range = ref("7d");
</script>
<template><main class="p-4 md:p-6"><div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">沟通审计与安全事件</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">只展示公告、通知投递和安全事件的技术审计元数据。</p></div><el-button>刷新</el-button></div><el-tabs v-model="tab"><el-tab-pane label="审计日志" name="audit"/><el-tab-pane label="安全事件" name="security"/></el-tabs><div class="flex flex-wrap gap-3 mb-4"><el-input v-model="query" clearable placeholder="搜索操作、Actor 或资源 ID" class="max-w-sm"/><el-select v-model="range" class="w-32"><el-option label="近 7 天" value="7d"/><el-option label="近 30 天" value="30d"/></el-select></div><el-card shadow="never"><el-empty description="审计查询接口尚未接入；不在前端伪造操作记录或安全事件"/><el-alert title="日志内容应由服务端按权限过滤并脱敏。" type="info" show-icon :closable="false"/></el-card></main></template>
