<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError, api, unwrapPage } from './services/api'

type Item = { label: string; path: string; icon: string; capability?: string }
type Group = { label: string; icon: string; items: Item[] }
const router = useRouter(); const route = useRoute()
const groups: Group[] = [
  { label: '工作台', icon: '⌂', items: [{ label: '概览', path: 'dashboard', icon: '▦' }, { label: '全局搜索', path: 'search', icon: '⌕' }, { label: '我的活动与收藏', path: 'activity', icon: '◷' }] },
  { label: '内容与创作', icon: '▤', items: [{ label: '统一资源库', path: 'library', icon: '◈' }, { label: '集合与标签', path: 'collections', icon: '▧' }, { label: '文章与文档', path: 'documents', icon: '□' }, { label: '媒体消费', path: 'media', icon: '▶' }, { label: '分享与协作', path: 'sharing', icon: '♧' }] },
  { label: '个人网盘', icon: '☁', items: [{ label: '文件空间', path: 'drive', icon: '▰' }, { label: '传输与同步', path: 'drive/transfers', icon: '⇄' }, { label: '冲突处理', path: 'drive/conflicts', icon: '⚠' }, { label: '配额与策略', path: 'drive/quota', icon: '◫' }] },
  { label: '附件与存储', icon: '▣', items: [{ label: '附件与 Blob', path: 'attachments', icon: '⊙' }, { label: '存储层', path: 'storage/tiers', icon: '▥' }, { label: '归档与恢复', path: 'storage/archive', icon: '↶' }, { label: '备份与恢复', path: 'storage/backup', icon: '⟳' }] },
  { label: '效率与计划', icon: '✓', items: [{ label: '收集箱与今天', path: 'planning/today', icon: '☀' }, { label: '项目与任务', path: 'planning/projects', icon: '▤' }, { label: '日历与时间块', path: 'planning/calendar', icon: '▦' }, { label: '目标与 OKR', path: 'planning/goals', icon: '↗' }, { label: '习惯与专注', path: 'planning/focus', icon: '◉' }] },
  { label: '个人记账', icon: '¥', items: [{ label: '账本总览', path: 'finance', icon: '◉' }, { label: '账户', path: 'finance/accounts', icon: '▣' }, { label: '交易', path: 'finance/transactions', icon: '⇄' }, { label: '预算与对账', path: 'finance/budgets', icon: '▤' }] },
  { label: '私密笔记', icon: '▤', items: [{ label: '保险库', path: 'private-notes', icon: '▣' }, { label: '版本与冲突', path: 'private-notes/conflicts', icon: '↻' }, { label: '恢复与导出', path: 'private-notes/recovery', icon: '↶' }] },
  { label: '密码管理', icon: '♢', items: [{ label: '密码保险库', path: 'passwords', icon: '▣' }, { label: '生成器', path: 'passwords/generator', icon: '✦' }, { label: '健康与安全发送', path: 'passwords/health', icon: '♥' }, { label: '设备与访问', path: 'passwords/devices', icon: '⌁' }] },
  { label: 'AI 智能', icon: '✦', items: [{ label: '助手', path: 'ai/assistant', icon: '✦' }, { label: '模型与提供方', path: 'ai/models', icon: '◇' }, { label: '人格', path: 'ai/personas', icon: '♙' }, { label: '作业与用量', path: 'ai/jobs', icon: '◷' }] },
  { label: '数据分析', icon: '⌁', items: [{ label: '个人概览', path: 'analytics', icon: '◒' }, { label: '内容分析', path: 'analytics/content', icon: '▥' }, { label: '存储分析', path: 'analytics/storage', icon: '▥' }, { label: '报表与重建', path: 'analytics/reports', icon: '▤' }] },
  { label: '集成与自动化', icon: '⚙', items: [{ label: '自动化规则', path: 'integration/automation', icon: '⟳' }, { label: '执行与 Trace', path: 'integration/executions', icon: '▷' }, { label: '插件与连接器', path: 'integration/plugins', icon: '⊕' }] },
  { label: '身份与安全', icon: '♢', items: [{ label: '用户与角色', path: 'security/users', icon: '♙' }, { label: '权限矩阵', path: 'security/permissions', icon: '▦' }, { label: '活跃会话', path: 'security/sessions', icon: '⌁' }, { label: '认证、密钥与恢复', path: 'security/authentication', icon: '▣' }] },
  { label: '平台配置', icon: '⚙', items: [{ label: '参数', path: 'platform/parameters', icon: '≡' }, { label: '字典', path: 'platform/dictionaries', icon: '▤' }, { label: '菜单', path: 'platform/menus', icon: '☷' }] },
  { label: '沟通与审计', icon: '✉', items: [{ label: '公告', path: 'communications/announcements', icon: '▰' }, { label: '通知中心', path: 'communications/notifications', icon: '♧' }, { label: '审计日志', path: 'communications/audit', icon: '≡' }] },
  { label: '系统运维', icon: '♨', items: [{ label: '系统健康与告警', path: 'ops/health', icon: '♥' }, { label: '定时任务', path: 'ops/jobs', icon: '◷' }, { label: '后台任务', path: 'ops/background', icon: '⇄' }] }
]
const preferenceKey = 'ikaros-console-preferences'
const storedPreferences = (() => { try { return JSON.parse(localStorage.getItem(preferenceKey) || '{}') as { expanded?: string[]; theme?: Theme } } catch { return {} } })()
type Theme = 'system' | 'light' | 'dark'
const expanded = ref<string[]>(storedPreferences.expanded || []); const theme = ref<Theme>(storedPreferences.theme || 'system'); const drawer = ref(false); const dialog = ref(''); const toast = ref(''); const query = ref(String(route.query.q || '')); const statusFilter = ref('全部状态'); const selectedRows = ref<string[]>([]); const selectedTab = ref('概览'); const loading = ref(false); const apiConnected = ref(false); const errorState = ref<{ status: number; title: string; detail: string } | null>(null)
const currentPath = computed(() => Array.isArray(route.params.pathMatch) ? route.params.pathMatch.join('/') : String(route.params.pathMatch || 'dashboard'))
const current = computed(() => groups.flatMap(g => g.items).find(i => i.path === currentPath.value) || groups[0].items[0])
const currentGroup = computed(() => groups.find(g => g.items.some(i => i.path === currentPath.value)) || groups[0])
const pageTitle = computed(() => current.value.label)
function go(path: string) { router.push('/console/' + path); drawer.value = false }
function toggle(label: string) { expanded.value = expanded.value.includes(label) ? expanded.value.filter(v => v !== label) : [...expanded.value, label] }
function notify(message: string) { toast.value = message; setTimeout(() => toast.value = '', 2800) }
function openDialog(kind: string) { dialog.value = kind }
function applyTheme(value: Theme) { document.documentElement.dataset.theme = value; document.documentElement.classList.toggle('dark', value === 'dark'); localStorage.setItem(preferenceKey, JSON.stringify({ expanded: expanded.value, theme: value })) }
function cycleTheme() { theme.value = theme.value === 'system' ? 'light' : theme.value === 'light' ? 'dark' : 'system'; applyTheme(theme.value); notify(`主题：${theme.value === 'system' ? '跟随系统' : theme.value === 'light' ? '浅色' : '深色'}`) }
function submitSearch() { if (!isSearch.value) go('search'); router.replace({ query: query.value ? { q: query.value } : {} }); notify(query.value ? `已搜索：${query.value}` : '已恢复全部结果') }
function handleShortcut(event: KeyboardEvent) { if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); go('search'); requestAnimationFrame(() => document.querySelector<HTMLInputElement>('.search-box input')?.focus()) } }
const filteredRows = computed(() => rows.value.filter(row => { const matchesQuery = !query.value || `${row.name} ${row.owner} ${row.status}`.toLowerCase().includes(query.value.toLowerCase()); const matchesStatus = statusFilter.value === '全部状态' || row.status === statusFilter.value; return matchesQuery && matchesStatus }))
function setStatusFilter(value: string) { statusFilter.value = statusFilter.value === value ? '全部状态' : value }
const allVisibleSelected = computed(() => filteredRows.value.length > 0 && filteredRows.value.every(row => selectedRows.value.includes(row.name)))
function toggleRow(name: string) { selectedRows.value = selectedRows.value.includes(name) ? selectedRows.value.filter(item => item !== name) : [...selectedRows.value, name] }
function toggleAll() { selectedRows.value = allVisibleSelected.value ? selectedRows.value.filter(name => !filteredRows.value.some(row => row.name === name)) : [...new Set([...selectedRows.value, ...filteredRows.value.map(row => row.name)])] }
function clearSelection() { selectedRows.value = [] }
const isDashboard = computed(() => currentPath.value === 'dashboard'); const isSearch = computed(() => currentPath.value === 'search')
const kpis = [{ label: '资源', value: '12,486', trend: '+8.4%', icon: '◈', tone: 'primary' }, { label: '存储', value: '68.4 GB', trend: '已配置 100 GB', icon: '▥', tone: 'teal' }, { label: '今天', value: '8 / 12', trend: '4 项待完成', icon: '✓', tone: 'orange' }, { label: '后台任务', value: '3', trend: '1 项失败', icon: '⇄', tone: 'purple' }, { label: '通知', value: '6', trend: '2 条重要', icon: '✉', tone: 'pink' }]
const activities = [{ icon: '✦', text: '完成了媒体资源的元数据同步', target: '《星际穿越》', time: '12 分钟前', color: 'purple' }, { icon: '✓', text: '完成任务', target: '整理本周阅读清单', time: '1 小时前', color: 'teal' }, { icon: '↗', text: '更新了项目', target: 'Ikaros V2 产品设计', time: '昨天 18:24', color: 'orange' }, { icon: '♧', text: '收藏了资源', target: 'Material Design 3', time: '昨天 15:08', color: 'blue' }]
const resources = [{ name: '《星际穿越》', type: '电影', tags: ['科幻', '收藏'], status: '进行中', progress: 72, updated: '12 分钟前' }, { name: 'Ikaros V2 产品设计', type: '文档', tags: ['项目'], status: '已更新', progress: 100, updated: '昨天' }, { name: 'Material Design 3', type: '网页', tags: ['设计系统'], status: '收藏', progress: 34, updated: '3 天前' }, { name: '2026 年读书计划', type: '集合', tags: ['计划'], status: '草稿', progress: 18, updated: '5 天前' }]
const rows = ref([{ name: '媒体资源索引', owner: '系统', status: '已启用', updated: '刚刚' }, { name: '每周资料备份', owner: '你', status: '运行中', updated: '8 分钟前' }, { name: '阅读进度同步', owner: '你', status: '已暂停', updated: '昨天' }, { name: '存储健康检查', owner: '系统', status: '已启用', updated: '昨天' }, { name: '活动周报', owner: '你', status: '失败', updated: '2 天前' }])
const demoRows = rows.value
async function loadResources() {
  if (isDashboard.value) return
  loading.value = true
  errorState.value = null
  try {
    if (currentPath.value === 'library') {
      const result = unwrapPage(await api.listResources('?limit=5'))
      if (result.length) rows.value = result.map(item => ({ name: item.title || `Resource ${item.id.slice(0, 8)}`, owner: '当前用户', status: item.lifecycle || '已启用', updated: item.updated_at ? new Date(item.updated_at).toLocaleString('zh-CN') : '刚刚' }))
    } else if (currentPath.value === 'security/users') {
      const result = unwrapPage(await api.listUsers('?page=0&size=5'))
      if (result.length) rows.value = result.map(item => ({ name: item.display_name || item.username || item.id.slice(0, 8), owner: item.username || '平台用户', status: item.status || 'ACTIVE', updated: item.last_active_at ? new Date(item.last_active_at).toLocaleString('zh-CN') : '暂无记录' }))
    } else if (currentPath.value === 'ops/background') {
      const result = await api.listBackgroundTasks()
      if (result.length) rows.value = result.slice(0, 5).map(item => ({ name: item.task_type || item.id.slice(0, 8), owner: item.owning_subsystem || '系统', status: item.state || '排队中', updated: item.created_at ? new Date(item.created_at).toLocaleString('zh-CN') : '刚刚' }))
    } else return
    apiConnected.value = true
  } catch (error) {
    apiConnected.value = false
    if (error instanceof ApiError && [401, 403, 404, 409, 412].includes(error.status)) {
      errorState.value = { status: error.status, title: error.status === 401 ? '会话已失效' : error.status === 403 ? '你没有权限访问此页面' : error.status === 404 ? '页面或资源不存在' : '数据发生并发冲突', detail: error.problem?.detail || error.message }
    } else { rows.value = demoRows; notify('后端暂不可用，已保留演示数据') }
  } finally { loading.value = false }
}
onMounted(loadResources); watch(currentPath, loadResources)
onMounted(() => applyTheme(theme.value)); watch(expanded, () => applyTheme(theme.value), { deep: true })
watch(() => route.query.q, value => { query.value = String(value || '') })
onMounted(() => window.addEventListener('keydown', handleShortcut)); onBeforeUnmount(() => window.removeEventListener('keydown', handleShortcut))
let refreshTimer: number | undefined
watch(currentPath, value => {
  if (refreshTimer) window.clearInterval(refreshTimer)
  if (value === 'ops/background') refreshTimer = window.setInterval(loadResources, 30000)
}, { immediate: true })
onBeforeUnmount(() => { if (refreshTimer) window.clearInterval(refreshTimer) })
function genericTitle() { return current.value.label }
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ open: drawer }">
      <div class="brand" @click="go('dashboard')"><div class="brand-mark">i</div><div><strong>Ikaros</strong><span>Console</span></div><span class="env-chip">本地</span></div>
      <div class="quick-actions"><button class="tonal-button" @click="go('search')"><span>⌕</span> 全局搜索 <kbd>⌘ K</kbd></button><button class="icon-button" aria-label="创建" @click="openDialog('create')">＋</button></div>
      <nav class="nav-scroll">
        <div v-for="group in groups" :key="group.label" class="nav-group">
          <button class="group-toggle" @click="toggle(group.label)"><span class="nav-icon">{{ group.icon }}</span><span>{{ group.label }}</span><span class="chevron" :class="{ rotated: expanded.includes(group.label) || currentGroup.label === group.label }">⌄</span></button>
          <div v-if="expanded.includes(group.label) || currentGroup.label === group.label" class="nav-items">
            <button v-for="item in group.items" :key="item.path" class="nav-item" :class="{ active: currentPath === item.path }" @click="go(item.path)"><span class="nav-icon">{{ item.icon }}</span>{{ item.label }}</button>
          </div>
        </div>
      </nav>
      <div class="sidebar-bottom"><button class="bottom-link" @click="notify('帮助中心即将开放')">? <span>帮助中心</span></button><button class="bottom-link" @click="cycleTheme">◐ <span>主题：{{ theme === 'system' ? '跟随系统' : theme === 'light' ? '浅色' : '深色' }}</span></button><div class="profile" @click="openDialog('profile')"><div class="avatar">陈</div><div><b>陈昊</b><small>管理员</small></div><span>•••</span></div></div>
    </aside>
    <main class="main-content">
      <header class="topbar"><button class="menu-button icon-button" @click="drawer = !drawer">☰</button><div class="breadcrumbs"><span>Ikaros Console</span><span>/</span><b>{{ currentGroup.label }}</b><span>/</span><strong>{{ pageTitle }}</strong></div><div class="top-actions"><button class="icon-button" @click="openDialog('tasks')" aria-label="后台任务">⇄<i class="badge">3</i></button><button class="icon-button" @click="openDialog('notifications')" aria-label="通知">♧<i class="badge pink">6</i></button><button class="icon-button" @click="notify('这是当前页面的帮助提示')">?</button><div class="mini-avatar">陈</div></div></header>
      <div class="page-wrap">
        <section v-if="errorState" class="error-state surface-card"><div class="error-state-icon">{{ errorState.status === 403 ? '♢' : errorState.status === 401 ? '⌁' : errorState.status === 409 || errorState.status === 412 ? '↻' : '!' }}</div><p class="eyebrow">错误 {{ errorState.status }}</p><h1>{{ errorState.title }}</h1><p>{{ errorState.detail }}</p><div class="error-actions"><button class="outlined-button" @click="go('dashboard')">返回概览</button><button v-if="errorState.status !== 403" class="filled-button" @click="loadResources">重新加载</button></div></section>
        <div v-else-if="isDashboard" class="dashboard">
          <section class="hero-row"><div><p class="eyebrow">星期三，2026 年 9 月 2 日</p><h1>概览</h1><p class="subtitle">快速了解你的 Ikaros 工作空间。</p></div><div class="title-actions"><button class="outlined-button" @click="openDialog('customize')">⊞ 自定义</button><button class="icon-button elevated" @click="notify('工作台已刷新')">⟳</button></div></section>
          <section class="welcome-card"><div><h2>下午好，陈昊 <span class="wave">✦</span></h2><p>你的空间运行良好，今天也有一些值得继续的事情。</p><div class="chips"><span class="status-chip success">● 系统健康</span><span class="status-chip neutral">本地环境</span><span class="muted-text">上次刷新：刚刚</span></div></div><div class="welcome-art"><div class="orb orb-a"></div><div class="orb orb-b"></div><div class="spark">✦</div></div></section>
          <section class="kpi-grid"><article v-for="item in kpis" :key="item.label" class="kpi-card" @click="notify(item.label + '详情已准备')"><div class="kpi-top"><span class="kpi-icon" :class="item.tone">{{ item.icon }}</span><span class="more">•••</span></div><p>{{ item.label }}</p><strong>{{ item.value }}</strong><small :class="{ positive: item.tone === 'primary' }">{{ item.trend }}</small><div v-if="item.label === '存储'" class="progress"><span style="width:68%"></span></div></article></section>
          <section class="content-grid"><article class="surface-card continue-card"><div class="card-heading"><div><h3>继续处理</h3><p>从你上次离开的地方继续</p></div><button class="text-button" @click="go('library')">查看全部</button></div><div v-for="item in resources.slice(0, 3)" :key="item.name" class="resource-row"><div class="resource-thumb" :class="item.type === '电影' ? 'cover' : ''">{{ item.type === '电影' ? '✦' : item.type === '文档' ? '□' : '◈' }}</div><div class="resource-info"><b>{{ item.name }}</b><div><span class="small-chip">{{ item.type }}</span><span class="muted-text">{{ item.updated }}</span></div><div class="tiny-progress"><span :style="{ width: item.progress + '%' }"></span></div></div><span class="progress-label">{{ item.progress }}%</span><button class="icon-button compact" @click="notify('正在打开 ' + item.name)">›</button></div></article><article class="surface-card activity-card"><div class="card-heading"><div><h3>最近活动</h3><p>你的空间动态</p></div><button class="text-button" @click="go('activity')">查看全部</button></div><div v-for="item in activities" :key="item.target" class="activity-row"><span class="activity-icon" :class="item.color">{{ item.icon }}</span><div><span>{{ item.text }}</span><b>{{ item.target }}</b><small>{{ item.time }}</small></div></div></article></section>
          <section class="surface-card attention-card"><div class="card-heading"><div><h3>待处理事项 <span class="count-badge">4</span></h3><p>需要你关注的空间状态</p></div><button class="text-button" @click="go('ops/background')">查看任务中心</button></div><div class="attention-grid"><div class="attention-item"><span class="attention-dot error"></span><div><b>备份任务执行失败</b><small>系统运维 · 2 小时前</small></div><button class="outlined-button small" @click="go('ops/background')">处理</button></div><div class="attention-item"><span class="attention-dot warning"></span><div><b>3 个元数据冲突</b><small>内容与创作 · 昨天</small></div><button class="outlined-button small" @click="go('library')">处理</button></div><div class="attention-item"><span class="attention-dot info"></span><div><b>分享链接即将过期</b><small>分享与协作 · 3 天内</small></div><button class="outlined-button small" @click="go('sharing')">处理</button></div></div></section>
        </div>
        <div v-else class="generic-page">
          <section class="hero-row"><div><p class="eyebrow">{{ currentGroup.label }}</p><h1>{{ genericTitle() }}</h1><p class="subtitle">管理和查看你的{{ genericTitle() }}，所有变更都会记录并支持恢复。</p></div><div class="title-actions"><button class="filled-button" @click="openDialog('create')">＋ 新建</button><button class="icon-button elevated" :disabled="loading" @click="loadResources">{{ loading ? '…' : '⟳' }}</button></div></section>
          <div v-if="isSearch" class="search-hero"><div class="search-box"><span>⌕</span><input v-model="query" placeholder="搜索资源、文档、任务和活动…" @keyup.enter="submitSearch"/><button v-if="query" @click="query = ''; submitSearch()">×</button><kbd>⌘ K</kbd></div><div class="filter-row"><span class="filter-chip active">全部类型</span><span class="filter-chip">最近更新</span><span class="filter-chip">仅收藏</span><button class="filter-more" @click="openDialog('filters')">＋ 更多筛选</button></div></div>
          <section class="mini-kpi-row"><div class="mini-kpi"><span>总条目</span><b>1,284</b><small>较上月 +12%</small></div><div class="mini-kpi"><span>活跃中</span><b>86</b><small>7 天内更新</small></div><div class="mini-kpi"><span>待处理</span><b class="warning-text">12</b><small>需要关注</small></div><div class="mini-kpi"><span>同步状态</span><b class="success-text">正常</b><small>刚刚检查</small></div></section>
          <section class="surface-card table-card"><div class="table-toolbar"><div class="search-inline"><span>⌕</span><input placeholder="筛选当前列表" v-model="query"/></div><div class="filter-row"><button class="filter-chip" :class="{ active: statusFilter === '全部状态' }" @click="setStatusFilter('全部状态')">全部状态</button><button class="filter-chip" :class="{ active: statusFilter === '运行中' }" @click="setStatusFilter('运行中')">运行中</button><button class="filter-chip" :class="{ active: statusFilter === '失败' }" @click="setStatusFilter('失败')">失败</button><button class="outlined-button small" @click="openDialog('filters')">筛选</button><button class="icon-button compact">⋮</button></div></div><div class="bulk-bar" v-if="selectedRows.length"><b>已选择 {{ selectedRows.length }} 项</b><button class="text-button" @click="openDialog('bulk')">批量操作</button><button class="text-button" @click="clearSelection">清除选择</button></div><div class="bulk-bar" v-if="!selectedRows.length && (query || statusFilter !== '全部状态')">正在筛选 <b>{{ query ? `“${query}”` : statusFilter }}</b><button class="text-button" @click="query = ''; statusFilter = '全部状态'">清除</button></div><table><thead><tr><th class="check"><input type="checkbox" :checked="allVisibleSelected" @change="toggleAll" /></th><th>名称</th><th>所有者</th><th>状态</th><th>最近更新</th><th>操作</th></tr></thead><tbody><tr v-for="row in filteredRows" :key="row.name" @click="openDialog('detail')"><td class="check"><input type="checkbox" :checked="selectedRows.includes(row.name)" @click.stop @change="toggleRow(row.name)" /></td><td><div class="table-name"><span class="row-icon">◈</span><b>{{ row.name }}</b></div></td><td>{{ row.owner }}</td><td><span class="status-chip" :class="row.status === '失败' ? 'danger' : row.status === '运行中' ? 'warning' : row.status === '已暂停' ? 'neutral' : 'success'">● {{ row.status }}</span></td><td class="muted-text">{{ row.updated }}</td><td><button class="icon-button compact" @click.stop="openDialog('detail')">⋮</button></td></tr><tr v-if="filteredRows.length === 0"><td colspan="6" class="no-results">没有匹配结果，请清除筛选后重试。</td></tr></tbody></table><div class="table-footer"><span>显示 {{ filteredRows.length }} 项，共 {{ rows.length }} 项</span><div><button class="icon-button compact">‹</button><button class="icon-button compact">›</button></div></div></section>
          <section class="empty-hint"><span>✦</span><div><b>需要了解更多？</b><p>查看该模块的帮助文档，了解状态、权限和操作规则。</p></div><button class="text-button" @click="notify('帮助文档即将开放')">查看文档 →</button></section>
        </div>
      </div>
    </main>
    <div v-if="drawer" class="scrim" @click="drawer = false"></div>
    <div v-if="dialog" class="dialog-scrim" @click.self="dialog = ''"><section class="dialog"><div class="dialog-title"><div><p class="eyebrow">{{ dialog === 'create' ? '创建入口' : 'Ikaros Console' }}</p><h2>{{ dialog === 'create' ? '创建新内容' : dialog === 'customize' ? '自定义工作台' : dialog === 'filters' ? '更多筛选' : dialog === 'profile' ? '个人账户' : dialog === 'tasks' ? '后台任务' : dialog === 'notifications' ? '通知中心' : '详情' }}</h2></div><button class="icon-button" @click="dialog = ''">×</button></div><div v-if="dialog === 'create'" class="create-options"><button @click="notify('已打开资源创建器'); dialog = ''"><span>◈</span><div><b>Resource</b><small>创建统一资源</small></div>›</button><button @click="notify('已打开文档编辑器'); dialog = ''"><span>□</span><div><b>文章 / 文档</b><small>开始一篇新的内容</small></div>›</button><button @click="notify('已打开任务创建器'); dialog = ''"><span>✓</span><div><b>任务</b><small>加入今天的计划</small></div>›</button><button @click="notify('已打开导入流程'); dialog = ''"><span>⇩</span><div><b>导入</b><small>从本地文件或连接器导入</small></div>›</button></div><div v-else-if="dialog === 'tasks'" class="dialog-list"><div class="task-line"><span class="loader"></span><div><b>每周资料备份</b><small>存储与备份 · 68%</small></div><span class="status-chip warning">运行中</span></div><div class="task-line"><span class="loader"></span><div><b>资源缩略图生成</b><small>内容与创作 · 排队中</small></div><span class="status-chip neutral">排队中</span></div><div class="task-line"><span class="error-dot"></span><div><b>活动周报</b><small>数据分析 · 失败</small></div><span class="status-chip danger">失败</span></div></div><div v-else class="dialog-body"><p>这里展示符合权限策略的安全信息。所有高风险修改都会先进行影响评估、确认和审计记录。</p><label>备注（可选）<textarea placeholder="输入你的备注…"></textarea></label><div class="dialog-actions"><button class="outlined-button" @click="dialog = ''">取消</button><button class="filled-button" @click="dialog = ''; notify('操作已完成')">确认</button></div></div></section></div>
    <div v-if="toast" class="snackbar">✓ {{ toast }}<button @click="toast = ''">×</button></div>
  </div>
</template>
