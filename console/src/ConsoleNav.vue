<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute(); const expanded = ref<string[]>(['内容与创作', '个人网盘', '身份与安全'])
const groups = [
  { label: '工作台', items: [{ label: '概览', path: '/console/dashboard' }, { label: '全局搜索', path: '/console/search' }, { label: '我的活动', path: '/console/activity' }] },
  { label: '内容与创作', items: [{ label: '统一资源库', path: '/console/resources' }, { label: '集合与标签', path: '/console/collections' }, { label: '文章与文档', path: '/console/documents' }, { label: '媒体消费', path: '/console/media' }, { label: '分享与协作', path: '/console/sharing' }] },
  { label: '个人网盘', items: [{ label: '文件空间', path: '/console/drive' }, { label: '传输中心', path: '/console/drive/transfers' }, { label: '同步关系', path: '/console/drive/sync' }, { label: '冲突处理', path: '/console/drive/conflicts' }, { label: '回收站', path: '/console/drive/trash' }, { label: '配额与策略', path: '/console/drive/quota' }] },
  { label: '效率与生活', items: [{ label: '今天与任务', path: '/console/planning/today' }, { label: '日历与时间块', path: '/console/planning/calendar' }, { label: '个人记账', path: '/console/finance' }, { label: '数据分析', path: '/console/analytics' }] },
  { label: '身份与安全', items: [{ label: '用户与角色', path: '/console/security/users' }, { label: '权限矩阵', path: '/console/security/permissions' }, { label: '活跃会话', path: '/console/security/sessions' }, { label: '认证与恢复', path: '/console/security/authentication' }] },
]
function toggle(label: string) { expanded.value = expanded.value.includes(label) ? expanded.value.filter(item => item !== label) : [...expanded.value, label] }
const active = computed(() => route.path)
</script>
<template><aside class="console-nav"><RouterLink to="/console/dashboard" class="console-nav-brand"><span class="brand-mark">i</span><span><strong>Ikaros</strong><small>Console</small></span></RouterLink><div class="console-nav-groups"><section v-for="group in groups" :key="group.label"><button class="console-nav-group" @click="toggle(group.label)"><span>{{ group.label }}</span><span>{{ expanded.includes(group.label) ? '⌃' : '⌄' }}</span></button><div v-if="expanded.includes(group.label)" class="console-nav-items"><RouterLink v-for="item in group.items" :key="item.path" :to="item.path" :class="{ active: active === item.path || active.startsWith(item.path + '/') }">{{ item.label }}</RouterLink></div></section></div><div class="console-nav-foot"><span class="mini-avatar">陈</span><div><b>陈昊</b><small>管理员</small></div></div></aside></template>
