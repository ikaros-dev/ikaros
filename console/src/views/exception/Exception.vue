<script lang="ts" setup>
import { useRouter } from 'vue-router';
import { i18n } from '@/locales';
import { ElButton } from 'element-plus';

withDefaults(
	defineProps<{
		code: number | string;
		title?: string;
		message?: string;
	}>(),
	{
		title: i18n.global.t('views.exception.exception.title'),
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
		<h1>Code: {{ code }}</h1>
		<br />

		<p>Title: {{ title }}</p>
		<br />

		<p>Message: {{ message }}</p>
		<br />

		<div>
			<ElButton @click="router.back()">
				{{ i18n.global.t('views.exception.exception.back') }}
			</ElButton>
			<ElButton @click="toDashboard">
				{{ i18n.global.t('views.exception.exception.dashboard') }}
			</ElButton>
		</div>
	</div>
</template>
