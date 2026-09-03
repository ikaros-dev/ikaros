<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElAlert, ElButton, ElEmpty, ElInput, ElSkeleton, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { api, unwrapPage, type ResourceRecord } from './services/api'
const route = useRoute(); const router = useRouter(); const query = ref(String(route.query.q || '')); const loading = ref(true); const error = ref(''); const results = ref<ResourceRecord[]>([])
const demo: ResourceRecord[] = [{ id: 'demo-resource', title: '《星际穿越》', resource_type: '电影', lifecycle: '正常' }, { id: 'demo-document', title: 'Ikaros V2 产品设计', resource_type: '文档', lifecycle: '正常' }, { id: 'demo-collection', title: '2026 年读书计划', resource_type: '集合', lifecycle: '草稿' }]
const filtered = computed(() => results.value.filter(item => `${item.title || ''} ${item.resource_type || ''} ${item.lifecycle || ''}`.toLowerCase().includes(query.value.toLowerCase())))
async function load() { loading.value = true; error.value = ''; try { const result = await api.listResources('?limit=100'); results.value = unwrapPage(result); if (!results.value.length) results.value = demo } catch { results.value = demo; error.value = '搜索 API 暂不可用，当前显示演示结果' } finally { loading.value = false } }
function open(item: ResourceRecord) { router.push(`/console/resources/${item.id}`) }
function submit() { router.replace({ query: query.value ? { q: query.value } : {} }) }
onMounted(load)
</script>
<template><main class="search-workspace"><header class="dashboard-header"><div><p class="eyebrow">工作台</p><h1>全局搜索</h1><p>搜索资源、文档、任务和活动。</p></div><el-button :loading="loading" @click="load">刷新</el-button></header><el-alert v-if="error" :title="error" type="info" show-icon :closable="false" /><el-input v-model="query" clearable autofocus placeholder="搜索资源、文档、任务和活动…" style="max-width:620px;margin:20px 0" @keyup.enter="submit" /><el-skeleton v-if="loading" :rows="6" animated /><el-table v-else :data="filtered" stripe empty-text="没有匹配结果"><el-table-column prop="title" label="名称" min-width="300" /><el-table-column prop="resource_type" label="类型" width="140"><template #default="{ row }"><el-tag type="info">{{ row.resource_type || '资源' }}</el-tag></template></el-table-column><el-table-column prop="lifecycle" label="状态" width="140"><template #default="{ row }"><el-tag :type="row.lifecycle === '正常' ? 'success' : 'warning'">{{ row.lifecycle || '未知' }}</el-tag></template></el-table-column><el-table-column label="操作" width="110"><template #default="{ row }"><el-button link type="primary" @click="open(row)">打开</el-button></template></el-table-column></el-table><el-empty v-if="!loading && filtered.length === 0" description="没有匹配结果" /></main></template>
