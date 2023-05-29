<script lang="ts" setup>
import { useRouter } from 'vue-router';

withDefaults(
	defineProps<{
		code: number | string;
		title?: string;
		message?: string;
	}>(),
	{
		title: undefined,
		message: undefined,
	}
);

const router = useRouter();
</script>

<template>
	<div style="align-content: center">
		<div>
			<h1>
				{{ code }}
			</h1>

			<p>
				{{ title }}
			</p>

			<p>{{ message }}</p>

			<div>
				<ElButton @click="router.back()"> Back </ElButton>
				<ElButton :route="{ name: 'Dashboard' }"> Dashboard </ElButton>
			</div>
		</div>
	</div>
</template>
