<script setup lang="ts">
import Exception from './Exception.vue';
import { i18n } from '@/locales';
</script>

<template>
	<Exception
		code="404"
		:message="i18n.global.t('views.exception.notfound.message')"
	/>
</template>

<style lang="scss" scoped></style>
