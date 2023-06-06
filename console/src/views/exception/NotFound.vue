<script setup lang="ts">
import Exception from './Exception.vue';
</script>

<template>
	<Exception code="404" :message="$t('core.common.exception.not_found')" />
</template>

<style lang="scss" scoped></style>
