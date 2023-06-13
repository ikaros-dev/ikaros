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

const filePondRef = ref(null);

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
		label?: string;
		uploadHandler: any;
		enableChunkUploads?: boolean;
		enableChunkForce?: boolean;
		chunkSize?: number;
	}>(),
	{
		name: 'file',
		field: '',
		multiple: true,
		accepts: undefined,
		label: '点击选择文件或将文件拖拽到此处',
		// 是否开启分片上传
		enableChunkUploads: false,
		// 是否强制分片上传
		enableChunkForce: false,
		// 分片上传单片字节大小，默认 1MB
		chunkSize: 1000000,
	}
);

const server = {
	url: '/',
	process: {
		url: './api/v1alpha1/file/fragment/unique',
		withCredentials: true,
	},
	patch: {
		url: './api/v1alpha1/file/fragment/patch/',
		withCredentials: true,
	},
	revert: {
		url: './api/v1alpha1/file/fragment/revert',
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
		:label-idle="props.label"
		:maxFiles="20"
		:maxParallelUploads="5"
		:name="props.name"
		:server="server"
		:chunkUploads="props.enableChunkUploads"
		:chunkSize="props.chunkSize"
		:chunkForce="props.enableChunkForce"
		fileValidateTypeLabelExpectedTypes="请选择 {lastType} 格式的文件"
		labelFileProcessing="上传中"
		labelFileProcessingAborted="取消上传"
		labelFileProcessingComplete="上传完成"
		labelFileProcessingError="上传错误"
		labelFileTypeNotAllowed="不支持当前文件格式"
		labelTapToCancel="点击取消"
		labelTapToRetry="点击重试"
		@init="handleFilePondInit"
	>
	</file-pond>
</template>

<style lang="scss" scoped></style>
