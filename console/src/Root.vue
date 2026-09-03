<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import ConsoleNav from './ConsoleNav.vue'

const route = useRoute()
const theme = ref<'system' | 'light' | 'dark'>('system')
const systemTheme = window.matchMedia('(prefers-color-scheme: dark)')
function applyTheme() { const dark = theme.value === 'dark' || (theme.value === 'system' && systemTheme.matches); document.documentElement.dataset.theme = theme.value; document.documentElement.classList.toggle('dark', dark) }
function syncTheme() { try { const stored = JSON.parse(localStorage.getItem('ikaros-console-preferences') || '{}'); if (stored.theme === 'light' || stored.theme === 'dark' || stored.theme === 'system') theme.value = stored.theme } catch { theme.value = 'system' } applyTheme() }
const standalone = computed(() => route.path.startsWith('/console/') && !['/console/search', '/console/activity', '/console/security/users', '/console/security/permissions', '/console/security/sessions'].includes(route.path))
onMounted(() => { syncTheme(); systemTheme.addEventListener('change', applyTheme) }); onBeforeUnmount(() => systemTheme.removeEventListener('change', applyTheme))
</script>
<template><div :class="{ 'with-console-nav': standalone }"><ConsoleNav v-if="standalone" /><router-view /></div></template>
