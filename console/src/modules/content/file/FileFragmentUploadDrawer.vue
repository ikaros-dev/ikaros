<script setup lang="ts">
import FilePondUpload from '@/components/upload/FilePondUpload.vue';
const props = withDefaults(
	defineProps<{
		visible: boolean;
		allowMultiple?: boolean;
	}>(),
	{
		visible: false,
		allowMultiple: true,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'fileUploadDrawerCloes', file?: any): void;
}>();

const uploadVisible = ref(false);
const drawerVisible = ref(false);

const handleVisibleChange = (visible: boolean) => {
	emit('update:visible', visible);
	if (!visible) {
		emit('close');
	}
};

watch(
	() => props.visible,
	(newValue) => {
		if (newValue) {
			uploadVisible.value = true;
			drawerVisible.value = props.visible;
		} else {
			const uploadVisibleTimer = setTimeout(() => {
				uploadVisible.value = false;
				drawerVisible.value = false;
				clearTimeout(uploadVisibleTimer);
			}, 200);
		}
	}
);

const filePondUploadRef = ref(null);

const handleClose = (done: () => void) => {
	// @ts-ignore
	const firstFile = filePondUploadRef.value.getFirstFile();
	// @ts-ignore
	filePondUploadRef.value.handleClearFileList();
	// console.log('firstFile', firstFile);
	emit('fileUploadDrawerCloes', firstFile);
	done();
	handleVisibleChange(false);
};

// eslint-disable-next-line no-unused-vars
const uploadHandler = (file, onUploadProgress) => {
	console.log('file', file);
	console.log('onUploadProgress', onUploadProgress);
};
</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		title="上传文件"
		direction="rtl"
		:before-close="handleClose"
		size="40%"
	>
		<template #header>
			<div align="center">
				<h4>上传文件</h4>
			</div>
		</template>
		<template #default>
			<div align="center">
				<FilePondUpload
					ref="filePondUploadRef"
					:uploadHandler="uploadHandler"
					:enableChunkForce="true"
					:enableChunkUploads="true"
					:multiple="props.allowMultiple"
				/>
			</div>
		</template>
	</el-drawer>
</template>

<style lang="scss" scoped></style>
