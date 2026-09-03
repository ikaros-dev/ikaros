<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton, ElResult, ElTag } from 'element-plus'
const route = useRoute(); const router = useRouter(); const capability = computed(() => String(route.query.capability || 'console.read'))
</script>
<template><div class="access-denied"><el-result icon="error" title="你没有权限访问此页面" sub-title="当前账号缺少进入该页面所需的能力"><template #extra><el-tag type="danger">{{ capability }}</el-tag><div class="access-denied-actions"><el-button @click="router.back()">返回上一页</el-button><el-button type="primary" @click="router.push('/console/dashboard')">返回概览</el-button></div></template></el-result></div></template>
