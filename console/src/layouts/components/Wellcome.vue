<script setup lang="ts">
import { useUserStore } from '@/stores/user';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const userStore = useUserStore();
</script>
<template>
	<div>{{ t('common.greeting') }} {{ userStore.currentUser?.entity?.nickname }}</div>
</template>
<style lang="scss" scoped></style>
