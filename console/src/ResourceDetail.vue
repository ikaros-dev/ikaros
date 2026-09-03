<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError, api } from './services/api'

const route = useRoute(); const router = useRouter(); const tab = ref('概览'); const loading = ref(false); const toast = ref(''); const favorite = ref(false); const lifecycle = ref('正常')
const resourceId = computed(() => String(route.params.id || '')); const actorId = String(import.meta.env.VITE_ACTOR_ID || ''); const title = ref(''); const type = ref(''); const description = ref('')
const metadata = ref([{ key: '标题', value: '《星际穿越》', source: '人工', locked: true }, { key: '上映年份', value: '2014', source: '提供方', locked: false }, { key: '导演', value: 'Christopher Nolan', source: '提供方', locked: false }])
const activity = ref<Array<{ text: string; time: string; actor: string }>>([])
function notify(message: string) { toast.value = message; window.setTimeout(() => toast.value = '', 2400) }
async function load() { if (!resourceId.value) { notify('资源 ID 不存在'); return }; if (!actorId) { notify('未配置当前用户身份，无法加载资源'); return }; loading.value = true; try { const result = await api.getResource(resourceId.value, actorId); title.value = result.primaryTitle || result.title || result.id; description.value = result.summary || ''; type.value = result.type || result.resource_type || '其他'; lifecycle.value = result.lifecycle || '未知' } catch (error) { if (!(error instanceof ApiError && import.meta.env.DEV && error.status === 404)) notify('资源详情加载失败，请检查登录状态或后端服务') } finally { loading.value = false } }
async function toggleFavorite() { if (!actorId || !resourceId.value) { notify('未配置当前用户身份，无法修改收藏'); return }; const next = !favorite.value; try { if (next) { await api.addFavorite(resourceId.value, actorId) } else { await api.removeFavorite(resourceId.value, actorId) }; favorite.value = next; notify(next ? '资源已收藏' : '已取消收藏') } catch { notify('收藏状态更新失败，请稍后重试') } }
async function loadFavorite() { if (!actorId || !resourceId.value) return; try { const result = await api.getFavorite(resourceId.value, actorId); favorite.value = Boolean(result.favorite ?? result.favorited) } catch { /* 收藏状态不可用时不影响资源详情 */ } }
async function loadActivity() { if (!actorId || !resourceId.value) return; try { const result = await api.listRecentActivity(actorId, 50); activity.value = result.filter(item => (item.resourceId || item.resource_id) === resourceId.value).map(item => ({ text: item.details || item.type || '资源活动', time: item.occurredAt || item.occurred_at || '时间不可用', actor: actorId })) } catch { activity.value = [] } }
async function archive() { if (lifecycle.value === '已归档') return; if (!actorId || !resourceId.value) { notify('未配置当前用户身份，无法归档资源'); return } loading.value = true; try { await api.archiveResource(resourceId.value, actorId); lifecycle.value = '已归档'; notify('资源已归档，操作已写入审计') } catch (error) { notify(error instanceof ApiError && error.status === 409 ? '资源当前状态不允许归档' : '资源归档失败，请稍后重试') } finally { loading.value = false } }
load(); loadFavorite(); loadActivity()
</script>

<template>
  <div class="resource-shell">
    <header class="catalog-topbar">
      <el-button text class="resource-back" @click="router.push('/console/resources')">← 资源库</el-button>
      <div class="catalog-brand"><span class="brand-mark">i</span><strong>Ikaros <small>Console</small></strong></div>
      <span class="catalog-context">资源详情 · {{ resourceId.slice(0, 8) }}</span><span class="mini-avatar">陈</span>
    </header>
    <main class="resource-main">
      <div class="resource-heading"><div><p class="eyebrow">内容与创作 · 资源详情</p><div class="resource-title-line"><h1>{{ title }}</h1><el-button text class="favorite-button" :class="{ active: favorite }" @click="toggleFavorite">{{ favorite ? '★' : '☆' }}</el-button></div><div class="resource-chips"><el-tag type="info">{{ type }}</el-tag><el-tag :type="lifecycle === '正常' ? 'success' : 'warning'">● {{ lifecycle }}</el-tag><span v-if="loading" class="muted-text">正在同步…</span></div></div><div class="resource-actions"><el-button @click="notify('编辑器即将打开')">编辑</el-button><el-button @click="notify('分享流程已打开')">分享</el-button><el-button type="primary" :disabled="lifecycle === '已归档'" @click="archive">{{ lifecycle === '已归档' ? '已归档' : '归档' }}</el-button></div></div>
      <section class="resource-overview"><article class="cover-card surface-card"><div class="cover-art">✦</div><div class="progress-label">消费进度 <b>72%</b></div><el-progress :percentage="72" :show-text="false" /><el-button type="primary" size="small" @click="notify('继续播放：从上次位置开始')">继续消费</el-button></article><article class="surface-card resource-info-card"><h2>基础信息</h2><dl><div><dt>内部 ID</dt><dd><code>{{ resourceId }}</code></dd></div><div><dt>所有者</dt><dd>陈昊</dd></div><div><dt>创建时间</dt><dd>2026-08-20 15:08</dd></div><div><dt>最近更新</dt><dd>刚刚</dd></div><div><dt>描述</dt><dd>{{ description }}</dd></div></dl></article></section>
      <el-button-group class="resource-tabs"><el-button v-for="name in ['概览','元数据','附件','关系','集合与标签','活动']" :key="name" :type="tab === name ? 'primary' : 'default'" @click="tab = name">{{ name }}</el-button></el-button-group>
      <section v-if="tab === '概览'" class="resource-panels"><article class="surface-card"><div class="card-heading"><div><h2>外部身份</h2><p>来自已配置 Provider 的关联标识</p></div><el-button link type="primary" @click="notify('外部身份管理即将开放')">管理</el-button></div><div class="external-row"><span>TMDB</span><code>movie/157336</code><el-tag type="success">● 已同步</el-tag></div></article><article class="surface-card"><div class="card-heading"><div><h2>相关资源</h2><p>与当前资源存在关系的内容</p></div></div><div class="related-row"><span class="row-icon">□</span><div><b>《星际穿越》阅读笔记</b><small>文档 · 昨天更新</small></div><el-button link>打开 →</el-button></div></article></section>
      <section v-else-if="tab === '元数据'" class="surface-card metadata-panel"><div class="card-heading"><div><h2>元数据字段</h2><p>人工确认字段不会被自动同步静默覆盖。</p></div><el-button @click="notify('元数据同步已请求')">重新同步</el-button></div><div v-for="item in metadata" :key="item.key" class="metadata-row"><div><b>{{ item.key }}</b><small>{{ item.source }}<span v-if="item.locked"> · 已锁定</span></small></div><strong>{{ item.value }}</strong><el-button v-if="!item.locked" link @click="notify('字段编辑器已打开')">编辑</el-button><span v-else class="lock-icon">▣</span></div></section>
      <section v-else-if="tab === '活动'" class="surface-card activity-panel"><div v-for="item in activity" :key="item.text" class="activity-row"><span class="activity-icon teal">✓</span><div><b>{{ item.text }}</b><small>{{ item.actor }} · {{ item.time }}</small></div></div></section><section v-else class="surface-card placeholder-panel"><span>✦</span><h2>{{ tab }}</h2><p>该关系管理面板已预留安全边界，将根据后端 Schema 展示受授权的数据。</p><el-button @click="notify(tab + '面板即将开放')">了解更多</el-button></section>
    </main><div v-if="toast" class="snackbar">✓ {{ toast }}<el-button text circle @click="toast = ''">×</el-button></div>
  </div>
</template>
