<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, h, ref, watch } from 'vue';
import { ElDrawer } from 'element-plus';
import { useI18n } from 'vue-i18n';
import DialogMessage from '@/components/dialog/DialogMessage.vue';
import AttachmentPondUpload from '@/components/upload/AttachmentPondUpload.vue';
import { loadMediaFileFormatLookup } from '@/utils/media-file-format';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		allowMultiple?: boolean;
		parentId?: string;
	}>(),
	{
		visible: false,
		allowMultiple: true,
		parentId: undefined,
	}
);

const emit = defineEmits<{
	(event: 'update:visible', visible: boolean): void;

	(event: 'update:parentId', parentId: string): void;

	(event: 'close'): void;

	(event: 'fileUploadDrawerCloes', file?: any): void;
}>();

const uploadVisible = ref(false);
const drawerVisible = ref(false);
const accepts = ref<string[] | undefined>();
const uploadParentId = computed({
	get() {
		return props.parentId || '';
	},
	set(value) {
		emit('update:parentId', value as string);
	},
});

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
			loadMediaFileFormatLookup()
				.then((lookup) => {
					accepts.value = [...lookup.accepts];
				})
				.catch(() => {
					accepts.value = undefined;
				});
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

const closeDrawer = (done: () => void) => {
	// @ts-expect-error
	const firstFile = filePondUploadRef.value?.getFirstFile();
	// @ts-expect-error
	filePondUploadRef.value?.handleClearFileList();
	emit('fileUploadDrawerCloes', firstFile);
	done();
	handleVisibleChange(false);
};

const handleClose = (done: () => void) => {
	// @ts-expect-error
	if (!filePondUploadRef.value?.hasIncompleteFiles()) {
		closeDrawer(done);
		return;
	}

	ElMessageBox.confirm(
		h(DialogMessage, {
			message: t('module.attachment.drawer.fragment-upload.confirm.message'),
		}),
		t('module.attachment.drawer.fragment-upload.confirm.title'),
		{
			confirmButtonText: t(
				'module.attachment.drawer.fragment-upload.confirm.confirm'
			),
			cancelButtonText: t(
				'module.attachment.drawer.fragment-upload.confirm.cancel'
			),
			confirmButtonClass: 'el-button--danger',
			type: 'warning',
		}
	)
		.then(() => {
			closeDrawer(done);
		})
		.catch(() => {
			ElMessage.warning(
				t('module.attachment.drawer.fragment-upload.confirm.hintMsg')
			);
		});
};

const uploadHandler = (file, onUploadProgress) => {
	console.log('file', file);
	console.log('onUploadProgress', onUploadProgress);
};
</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		:title="t('module.attachment.drawer.fragment-upload.title')"
		direction="rtl"
		:before-close="handleClose"
		size="40%"
	>
		<template #header>
			<div align="center">
				<h4>{{ t('module.attachment.drawer.fragment-upload.title') }}</h4>
			</div>
		</template>
		<template #default>
			<div align="center">
				<AttachmentPondUpload
					ref="filePondUploadRef"
					v-model:parentId="uploadParentId"
					:uploadHandler="uploadHandler"
					:enableChunkForce="true"
					:enableChunkUploads="true"
					:multiple="props.allowMultiple"
					:accepts="accepts"
				/>
			</div>
		</template>
	</el-drawer>
</template>

<style lang="scss" scoped></style>
