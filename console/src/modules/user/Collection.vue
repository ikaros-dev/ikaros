<script setup lang="ts">
import { ref } from 'vue';

// eslint-disable-next-line no-unused-vars
const findSubjectCollection = ref({
	page: 1,
	size: 12,
	type: undefined,
	isPrivate: undefined,
});
</script>

<template>
	<div>Subject Collection Page</div>
</template>

<style lang="scss" scoped></style>
