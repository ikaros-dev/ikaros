<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, watch } from 'vue';
import { ElDrawer } from 'element-plus';
import { useI18n } from 'vue-i18n';
import AttachmentPondUpload from '@/components/upload/AttachmentPondUpload.vue';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		allowMultiple?: boolean;
		parentId?: number;
	}>(),
	{
		visible: false,
		allowMultiple: true,
		parentId: undefined,
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
	// console.log('firstFile', firstFile);
	ElMessageBox.confirm(
		t('core.file.drawer.upload.closeConfirm.message'),
		t('core.file.drawer.upload.closeConfirm.title'),
		{
			confirmButtonText: t('core.file.drawer.upload.closeConfirm.confirm'),
			cancelButtonText: t('core.file.drawer.upload.closeConfirm.cancel'),
			type: 'warning',
		}
	)
		.then(() => {
			// @ts-ignore
			const firstFile = filePondUploadRef.value.getFirstFile();
			// @ts-ignore
			filePondUploadRef.value.handleClearFileList();
			emit('fileUploadDrawerCloes', firstFile);
			done();
			handleVisibleChange(false);
		})
		.catch(() => {
			ElMessage.warning(t('core.file.drawer.upload.closeConfirm.hintMsg'));
		});
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
				<h4>{{ t('core.file.drawer.upload.title') }}</h4>
			</div>
		</template>
		<template #default>
			<div align="center">
				<AttachmentPondUpload
					ref="filePondUploadRef"
					:parentId="props.parentId"
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
