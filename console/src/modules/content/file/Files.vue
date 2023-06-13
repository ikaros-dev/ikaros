<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import type { FileEntity } from '@runikaros/api-client';
import FileFragmentUploadDrawer from './FileFragmentUploadDrawer.vue';
import FileDeatilDrawer from './FileDeatilDrawer.vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const fileUploadDrawerVisible = ref(false);

// eslint-disable-next-line no-unused-vars
const onFileUploadDrawerClose = (firstFile) => {
	fileUploadDrawerVisible.value = false;
	// console.log('receive firstFile:', firstFile);
	ElMessageBox.confirm(
		'你需要刷新下页面获取最新的文件列表吗？注意刷新页面后需要重新上传文件，如果有文件正在上传，请选择不刷新。',
		'温馨提示',
		{
			confirmButtonText: '刷新',
			cancelButtonText: '不刷新',
			type: 'warning',
		}
	)
		.then(() => {
			window.location.reload();
		})
		.catch(() => {
			ElMessage.warning(
				'已取消刷新，您需要在文件上传完毕后手动进行刷新页面获取最新数据。'
			);
		});
};

const findFilesCondition = ref({
	page: 1,
	size: 8,
});

const files = ref<FileEntity[]>([]);

const fetchFiles = async () => {
	const { data } = await apiClient.file.listFilesByCondition({
		page: findFilesCondition.value.page + '',
		size: findFilesCondition.value.size + '',
	});
	findFilesCondition.value.page = data.page;
	findFilesCondition.value.size = data.size;
	files.value = data.items;
};

const currentShowDetailFile = ref<FileEntity>({});
const fileDetailDrawerVisible = ref(false);
const showFileDeatil = (file) => {
	currentShowDetailFile.value = file;
	fileDetailDrawerVisible.value = true;
};

const handleFileDetailDrawerDelete = () => {
	window.location.reload();
};

onMounted(fetchFiles);
</script>

<template>
	<FileDeatilDrawer
		v-model:visible="fileDetailDrawerVisible"
		:defineFile="currentShowDetailFile"
		@delete="handleFileDetailDrawerDelete"
	/>

	<FileFragmentUploadDrawer
		v-model:visible="fileUploadDrawerVisible"
		@fileUploadDrawerCloes="onFileUploadDrawerClose"
	/>

	<el-button plain @click="fileUploadDrawerVisible = true">上传文件</el-button>

	<el-table
		:data="files"
		stripe
		style="width: 100%"
		@row-dblclick="showFileDeatil"
	>
		<el-table-column prop="id" label="文件ID" width="80" />
		<el-table-column prop="name" label="文件名称" width="180" />
		<el-table-column prop="url" label="文件URL" />
		<el-table-column fixed="right" label="操作" width="120">
			<template #default="scoped">
				<el-button plain @click="showFileDeatil(scoped.row)">详情</el-button>
			</template>
		</el-table-column>
	</el-table>
</template>

<style lang="scss" scoped></style>
