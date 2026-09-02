<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const route = useRoute(); const router = useRouter(); const capability = computed(() => String(route.query.capability || 'console.read'))
</script>
<template><div class="access-denied"><div class="access-denied-card surface-card"><span class="access-denied-icon">♢</span><p class="eyebrow">错误 403</p><h1>你没有权限访问此页面</h1><p>当前账号缺少进入该页面所需的能力：</p><code>{{ capability }}</code><div class="access-denied-actions"><button class="outlined-button" @click="router.back()">返回上一页</button><button class="filled-button" @click="router.push('/console/dashboard')">返回概览</button></div></div></div></template>
