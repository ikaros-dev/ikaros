<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElAlert, ElButton, ElEmpty, ElInput, ElSkeleton, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { api, type ResourceActivityRecord } from './services/api'
const actorId = String(import.meta.env.VITE_ACTOR_ID || '')
const loading = ref(true); const error = ref(''); const query = ref(''); const records = ref<ResourceActivityRecord[]>([])
const demo: ResourceActivityRecord[] = [{ id: 'demo-1', type: 'VIEW', details: '《星际穿越》', occurred_at: '12 分钟前' }, { id: 'demo-2', type: 'FAVORITE', details: 'Material Design 3', occurred_at: '昨天 15:08' }]
const visible = computed(() => records.value.filter(item => `${item.type || ''} ${item.details || ''} ${item.resourceId || item.resource_id || ''}`.toLowerCase().includes(query.value.toLowerCase())))
function label(type?: string) { const value = (type || '').toUpperCase(); return value.includes('FAVOR') ? '收藏' : value.includes('DOWNLOAD') ? '下载' : value.includes('PLAY') ? '播放' : value.includes('READ') || value.includes('VIEW') ? '访问' : '更新' }
async function load() { loading.value = true; error.value = ''; if (!actorId) { records.value = demo; loading.value = false; return } try { records.value = await api.listRecentActivity(actorId, 50) } catch { error.value = '活动 API 暂不可用，当前显示演示数据'; records.value = demo } finally { loading.value = false } }
onMounted(load)
</script>
<template><main class="activity-workspace"><header class="dashboard-header"><div><p class="eyebrow">工作台</p><h1>我的活动</h1><p>只显示当前用户授权范围内的 Resource Activity。</p></div><el-button :loading="loading" @click="load">刷新</el-button></header><el-alert v-if="error" :title="error" type="info" show-icon :closable="false" /><el-input v-model="query" clearable placeholder="搜索活动类型、详情或资源 ID" style="max-width:420px;margin:20px 0" /><el-skeleton v-if="loading" :rows="5" animated /><el-table v-else :data="visible" stripe empty-text="暂无活动"><el-table-column label="类型" width="120"><template #default="{ row }"><el-tag type="info">{{ label(row.type) }}</el-tag></template></el-table-column><el-table-column label="详情" min-width="260"><template #default="{ row }">{{ row.details || row.resourceId || row.resource_id || '资源活动' }}</template></el-table-column><el-table-column label="发生时间" width="220"><template #default="{ row }">{{ row.occurredAt || row.occurred_at || '时间不可用' }}</template></el-table-column><el-table-column prop="id" label="记录 ID" min-width="260" /></el-table><el-empty v-if="!loading && visible.length === 0" description="暂无匹配活动" /></main></template>
