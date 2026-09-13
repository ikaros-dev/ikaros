<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const title = computed(() => String(route.meta.title ?? ""));
const subtitle = computed(() => String(route.meta.description ?? ""));
</script>

<template>
  <main class="p-4 md:p-6">
    <el-card shadow="never">
      <h1 class="text-2xl font-semibold">{{ title }}</h1>
      <p class="mt-2 text-[var(--el-text-color-secondary)]">{{ subtitle }}</p>
    </el-card>
  </main>
</template>
