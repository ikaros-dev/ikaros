<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElButton, ElResult } from 'element-plus'
const router = useRouter()
</script>
<template><main class="not-found-workspace"><el-result icon="404" title="页面不存在" sub-title="请从 Console 导航中选择一个可用页面"><template #extra><el-button type="primary" @click="router.push('/console/dashboard')">返回概览</el-button></template></el-result></main></template>
