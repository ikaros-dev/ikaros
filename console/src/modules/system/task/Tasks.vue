<script setup lang="ts">
import { ref } from 'vue';
// eslint-disable-next-line no-unused-vars
import { apiClient } from '@/utils/api-client';

// eslint-disable-next-line no-unused-vars
const findTask = ref({
	name: '',
	status: undefined,
});

// eslint-disable-next-line no-unused-vars
const tasks = ref();
// eslint-disable-next-line no-unused-vars
const fetchTasks = async () => {
	// const { data } = await apiClient.task.
};
</script>

<template>
	<div>System Task Vue</div>
</template>

<style lang="scss" scoped></style>
