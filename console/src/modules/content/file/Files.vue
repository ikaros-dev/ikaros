<script setup lang="ts">
import FileFragmentUploadDrawer from './FileFragmentUploadDrawer.vue';

const fileUploadDrawerVisible = ref(false);

// eslint-disable-next-line no-unused-vars
const onFileUploadDrawerClose = (firstFile) => {
	fileUploadDrawerVisible.value = false;
	// window.location.reload();
	// console.log('receive firstFile:', firstFile);
};
</script>

<template>
	<h1>Core module files.</h1>
	<el-button plain @click="fileUploadDrawerVisible = true">打开</el-button>
	<FileFragmentUploadDrawer
		v-model:visible="fileUploadDrawerVisible"
		@fileUploadDrawerCloes="onFileUploadDrawerClose"
	/>
</template>

<style lang="scss" scoped></style>
