<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElAlert, ElButton, ElCard, ElEmpty, ElProgress, ElSkeleton, ElStatistic, ElTag } from 'element-plus'
import { api, unwrapPage } from './services/api'

const router = useRouter()
const actorId = String(import.meta.env.VITE_ACTOR_ID || '')
const loading = ref(true)
const error = ref('')
const resourceCount = ref(12486)
const storageUsed = ref(68.4)
const storageQuota = ref(100)
const todayTotal = ref(12)
const todayDone = ref(8)
const taskTotal = ref(3)
const taskFailed = ref(1)

async function load() {
  loading.value = true; error.value = ''
  if (!actorId) { loading.value = false; return }
  const results = await Promise.allSettled([api.listResources('?limit=1'), api.listDriveSpaces(actorId), api.listTodayTasks(actorId), api.listBackgroundTasks()])
  const resources = results[0]; const spaces = results[1]; const today = results[2]; const tasks = results[3]
  if (resources.status === 'fulfilled') resourceCount.value = Array.isArray(resources.value) ? resources.value.length : resources.value.total || unwrapPage(resources.value).length
  if (spaces.status === 'fulfilled' && spaces.value[0]) { const space = spaces.value[0]; storageUsed.value = Number(((space.usedBytes || space.used_bytes || 0) / 1024 ** 3).toFixed(1)); storageQuota.value = Math.max(1, Number(((space.quotaBytes || space.quota_bytes || 0) / 1024 ** 3).toFixed(1))) }
  if (today.status === 'fulfilled') { todayTotal.value = today.value.length; todayDone.value = today.value.filter(item => ['DONE', 'COMPLETED'].includes(String(item.status || '').toUpperCase())).length }
  if (tasks.status === 'fulfilled') { taskTotal.value = tasks.value.length; taskFailed.value = tasks.value.filter(item => ['FAILED', 'ERROR'].includes(String(item.status || item.state || '').toUpperCase())).length }
  if (results.every(item => item.status === 'rejected')) error.value = '后端暂不可用，当前显示演示统计'
  loading.value = false
}
onMounted(load)
</script>

<template>
  <main class="dashboard-workspace">
    <header class="dashboard-header"><div><p class="eyebrow">工作台</p><h1>概览</h1><p>快速了解你的 Ikaros 工作空间。</p></div><el-button type="primary" :loading="loading" @click="load">刷新数据</el-button></header>
    <el-alert v-if="error" :title="error" type="info" show-icon :closable="false" />
    <el-skeleton v-if="loading && !actorId" :rows="3" animated />
    <section class="dashboard-stat-grid">
      <el-card shadow="never"><el-statistic title="资源" :value="resourceCount" /><el-tag type="info">当前授权范围</el-tag></el-card>
      <el-card shadow="never"><el-statistic title="存储（GB）" :value="storageUsed" :precision="1" /><el-progress :percentage="Math.min(100, Math.round(storageUsed / storageQuota * 100))" :show-text="false" /></el-card>
      <el-card shadow="never"><el-statistic title="今天完成" :value="todayDone" /><span class="dashboard-muted">/ {{ todayTotal }} 项任务</span></el-card>
      <el-card shadow="never"><el-statistic title="后台任务" :value="taskTotal" /><el-tag :type="taskFailed ? 'danger' : 'success'">{{ taskFailed }} 项失败</el-tag></el-card>
    </section>
    <section class="dashboard-content-grid"><el-card shadow="never"><template #header><div class="dashboard-card-header"><span>继续处理</span><el-button link type="primary" @click="router.push('/console/resources')">查看资源库</el-button></div></template><el-empty v-if="!actorId" description="配置 VITE_ACTOR_ID 后显示实时资源" /><div v-else class="dashboard-action-list"><el-button text @click="router.push('/console/resources')">统一资源库 →</el-button><el-button text @click="router.push('/console/documents')">文章与文档 →</el-button><el-button text @click="router.push('/console/planning/today')">今天的任务 →</el-button></div></el-card><el-card shadow="never"><template #header><div class="dashboard-card-header"><span>需要关注</span><el-tag type="warning">{{ taskFailed }} 项</el-tag></div></template><el-alert v-if="taskFailed" title="存在失败的后台任务" type="warning" :closable="false" /><el-empty v-else description="当前没有需要处理的事项" /></el-card></section>
  </main>
</template>
