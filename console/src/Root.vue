<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ConsoleNav from './ConsoleNav.vue'

const route = useRoute()
const standalone = computed(() => route.path.startsWith('/console/') && !['/console/dashboard', '/console/search', '/console/activity', '/console/security/users', '/console/security/permissions', '/console/security/sessions'].includes(route.path))
</script>
<template><div :class="{ 'with-console-nav': standalone }"><ConsoleNav v-if="standalone" /><router-view /></div></template>
