<script setup lang="ts">
import {useUserStore} from '@/stores/user';

const userStore = useUserStore();
</script>
<template>
	<div>Hello, {{ userStore.currentUser?.entity?.nickname }}</div>
</template>
<style lang="scss" scoped></style>
