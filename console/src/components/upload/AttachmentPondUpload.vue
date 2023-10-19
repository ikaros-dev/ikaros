<script setup lang="ts">
// @ts-ignore
import VueFilePond, { setOptions } from 'vue-filepond';
import 'filepond/dist/filepond.min.css';

// Plugins
import FilePondPluginImageExifOrientation from 'filepond-plugin-image-exif-orientation';
import FilePondPluginImagePreview from 'filepond-plugin-image-preview';
import 'filepond-plugin-image-preview/dist/filepond-plugin-image-preview.min.css';
import FilePondPluginFileValidateType from 'filepond-plugin-file-validate-type';
import FilePondPluginFileRename from 'filepond-plugin-file-rename';
import Utf8 from 'crypto-js/enc-utf8';
import Base64 from 'crypto-js/enc-base64';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

const filePondRef = ref(null);
const { t } = useI18n();

// Create component and register plugins
const filePond = VueFilePond(
	FilePondPluginImageExifOrientation,
	FilePondPluginImagePreview,
	FilePondPluginFileValidateType,
	FilePondPluginFileRename
);

setOptions({
	fileRenameFunction: (file) => {
		const word = Utf8.parse(file.name);
		return Base64.stringify(word);
	},
});

const props = withDefaults(
	defineProps<{
		name?: string;
		field?: string;
		multiple?: boolean;
		accepts?: string[];
		uploadHandler: any;
		enableChunkUploads?: boolean;
		enableChunkForce?: boolean;
		chunkSize?: number;
		parentId: number;
	}>(),
	{
		name: 'file',
		field: '',
		multiple: true,
		accepts: undefined,
		// 是否开启分片上传
		enableChunkUploads: false,
		// 是否强制分片上传
		enableChunkForce: false,
		// 分片上传单片字节大小，默认 1MB
		chunkSize: 1000000,
		parentId: undefined,
	}
);

const server = {
	url: '/',
	process: {
		url: './api/v1alpha1/attachment/fragment/unique',
		withCredentials: true,
	},
	patch: {
		url: './api/v1alpha1/attachment/fragment/patch/',
		withCredentials: true,
		headers: {
			'PARENT-ID': props.parentId,
		},
	},
	revert: {
		url: './api/v1alpha1/attachment/fragment/revert',
		withCredentials: true,
	},
};

const fileList = ref([]);

const handleFilePondInit = () => {};
// eslint-disable-next-line no-unused-vars
const handleClearFileList = () => {
	// @ts-ignore
	filePondRef.value.removeFiles();
};
// eslint-disable-next-line no-unused-vars
const getFirstFile = () => {
	// console.log('filePondRef.value', filePondRef.value);
	// @ts-ignore
	return filePondRef.value.getFile(0);
};

defineExpose({
	handleClearFileList,
	getFirstFile,
});
</script>

<template>
	<file-pond
		ref="filePondRef"
		style="height: 100%"
		:accepted-file-types="props.accepts"
		:allow-multiple="props.multiple"
		:allowImagePreview="true"
		:allowRevert="false"
		:files="fileList"
		:label-idle="t('components.upload.file-pond-upload.mainLabel')"
		:maxFiles="100"
		:maxParallelUploads="5"
		:name="props.name"
		:server="server"
		:chunkUploads="props.enableChunkUploads"
		:chunkSize="props.chunkSize"
		:chunkForce="props.enableChunkForce"
		fileValidateTypeLabelExpectedTypes="请选择 {lastType} 格式的文件"
		:labelFileProcessing="t('components.upload.file-pond-upload.uploadding')"
		:labelFileProcessingAborted="
			t('components.upload.file-pond-upload.cancelUpload')
		"
		:labelFileProcessingComplete="
			t('components.upload.file-pond-upload.uploadFinish')
		"
		:labelFileProcessingError="
			t('components.upload.file-pond-upload.uploadException')
		"
		:labelFileTypeNotAllowed="
			t('components.upload.file-pond-upload.notSupportFileFormat')
		"
		:labelTapToCancel="t('components.upload.file-pond-upload.clickCancel')"
		:labelTapToRetry="t('components.upload.file-pond-upload.clickRetry')"
		@init="handleFilePondInit"
	>
	</file-pond>
</template>

<style lang="scss" scoped></style>