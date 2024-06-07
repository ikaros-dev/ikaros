<script setup lang="ts">
import Exception from './Exception.vue';
import { useI18n } from 'vue-i18n';
const { t } = useI18n();
</script>

<template>
	<Exception code="403" :message="t('views.exception.forbidden.message')" />
</template>

<style lang="scss" scoped></style>
