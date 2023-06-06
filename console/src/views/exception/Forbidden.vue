<script setup lang="ts">
import Exception from './Exception.vue';
</script>

<template>
	<Exception code="403" :message="$t('core.common.exception.forbidden')" />
</template>

<style lang="scss" scoped></style>
