<script setup lang="ts">
import { ref } from "vue";
const tab = ref("sources"); const query = ref("");
</script>
<template><main class="p-4 md:p-6"><div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">内容导入</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">管理导入来源、扫描任务和失败项目；导入过程由后台任务执行。</p></div><el-button>刷新</el-button></div><el-tabs v-model="tab"><el-tab-pane label="来源" name="sources"/><el-tab-pane label="扫描任务" name="scans"/><el-tab-pane label="失败项目" name="failed"/></el-tabs><el-card shadow="never"><template #header><div class="flex justify-between"><span>{{ tab === 'sources' ? '导入来源' : tab === 'scans' ? '扫描任务' : '失败项目' }}</span><el-input v-model="query" clearable placeholder="搜索名称或任务 ID" class="w-64"/></div></template><el-empty description="导入接口尚未接入；页面保留来源、扫描和失败队列布局，不生成虚假状态"/><el-alert title="实际导入执行结果请在后台任务中查看。" type="info" show-icon :closable="false" class="mt-4"/></el-card></main></template>
