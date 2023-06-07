<script lang="ts" setup>
import { useRouter } from 'vue-router';
import { i18n } from '@/locales';

withDefaults(
	defineProps<{
		code: number | string;
		title?: string;
		message?: string;
	}>(),
	{
		title: i18n.global.t('core.common.exception.title'),
		message: undefined,
	}
);

const router = useRouter();
const toDashboard = () => {
	router.push('/');
};
</script>

<template>
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
			<ElButton @click="toDashboard"> Dashboard </ElButton>
		</div>
	</div>
</template>
