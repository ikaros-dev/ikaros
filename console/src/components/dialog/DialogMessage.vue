<script setup lang="ts">
defineProps<{
	message: string;
}>();
</script>

<template>
	<p class="dialog-message">{{ message }}</p>
</template>

<style lang="scss" scoped>
.dialog-message {
	margin: 0;
	color: var(--el-text-color-regular);
	line-height: 1.6;
	white-space: pre-line;
}
</style>
