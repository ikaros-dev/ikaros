<script setup lang="ts">
import {
	ElAlert,
	ElButton,
	ElDrawer,
	ElMessage,
	ElMessageBox,
} from 'element-plus';
import { computed, h, ref, watch } from 'vue';
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
const formatLookupLoading = ref(false);
const formatLookupFailed = ref(false);
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

const loadAcceptedFileTypes = async () => {
	formatLookupLoading.value = true;
	formatLookupFailed.value = false;
	accepts.value = undefined;
	try {
		const lookup = await loadMediaFileFormatLookup();
		if (lookup.accepts.length === 0) {
			throw new Error('Empty media file format whitelist');
		}
		accepts.value = [...lookup.accepts];
	} catch {
		formatLookupFailed.value = true;
	} finally {
		formatLookupLoading.value = false;
	}
};

watch(
	() => props.visible,
	(newValue) => {
		if (newValue) {
			uploadVisible.value = true;
			drawerVisible.value = props.visible;
			void loadAcceptedFileTypes();
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
				<div v-if="formatLookupLoading" class="format-lookup-status">
					{{ t('module.attachment.drawer.fragment-upload.formatLoading') }}
				</div>
				<div v-else-if="formatLookupFailed" class="format-lookup-status">
					<ElAlert
						:closable="false"
						:title="
							t('module.attachment.drawer.fragment-upload.formatLoadFailed')
						"
						type="error"
					/>
					<ElButton class="retry-button" @click="loadAcceptedFileTypes">
						{{ t('module.attachment.drawer.fragment-upload.retryFormatLoad') }}
					</ElButton>
				</div>
				<AttachmentPondUpload
					v-else-if="accepts"
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

<style lang="scss" scoped>
.format-lookup-status {
	width: 100%;
}

.retry-button {
	margin-top: 16px;
}
</style>
