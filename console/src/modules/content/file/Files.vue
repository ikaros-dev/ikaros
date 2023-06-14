<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import type { FileEntity } from '@runikaros/api-client';
import FileFragmentUploadDrawer from './FileFragmentUploadDrawer.vue';
import FileDeatilDrawer from './FileDeatilDrawer.vue';
import { ElMessage } from 'element-plus';

const fileUploadDrawerVisible = ref(false);

// eslint-disable-next-line no-unused-vars
const onFileUploadDrawerClose = (firstFile) => {
	fileUploadDrawerVisible.value = false;
	// console.log('receive firstFile:', firstFile);
	window.location.reload();
};

const findFilesCondition = ref({
	page: 1,
	size: 10,
	total: 10,
});

const files = ref<FileEntity[]>([]);

const fetchFiles = async () => {
	const { data } = await apiClient.file.listFilesByCondition({
		page: findFilesCondition.value.page + '',
		size: findFilesCondition.value.size + '',
	});
	findFilesCondition.value.page = data.page;
	findFilesCondition.value.size = data.size;
	findFilesCondition.value.total = data.total;
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

const onCurrentPageChange = (val: number) => {
	findFilesCondition.value.page = val;
	fetchFiles();
};

const onSizeChange = (val: number) => {
	findFilesCondition.value.size = val;
	fetchFiles();
};

const handleDelete = async (file: FileEntity) => {
	await apiClient.file
		.deleteFile({
			id: file.id as number,
		})
		.then(() => {
			ElMessage.success('删除文件成功，文件：' + file.id + '-' + file.name);
			window.location.reload();
		})
		.catch((err) => {
			console.error(err);
			ElMessage.error('删除文件失败，异常：' + err.message);
		});
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

	<el-pagination
		v-model:page-size="findFilesCondition.size"
		v-model:current-page="findFilesCondition.page"
		background
		:total="findFilesCondition.total"
		layout="total, sizes, prev, pager, next, jumper"
		style="vertical-align: middle; line-height: 40px; height: 40px"
		@current-change="onCurrentPageChange"
		@size-change="onSizeChange"
	/>

	<el-table
		:data="files"
		stripe
		style="width: 100%"
		@row-dblclick="showFileDeatil"
	>
		<el-table-column prop="id" label="文件ID" width="80" />
		<el-table-column prop="name" label="文件名称" width="180" />
		<el-table-column prop="url" label="文件URL" />
		<el-table-column label="操作" width="200">
			<template #default="scoped">
				<el-button plain @click="showFileDeatil(scoped.row)">详情</el-button>

				<el-popconfirm
					title="你确定要删除该文件？"
					confirm-button-text="确定"
					cancel-button-text="取消"
					confirm-button-type="danger"
					@confirm="handleDelete(scoped.row)"
				>
					<template #reference>
						<el-button type="danger">删除</el-button>
					</template>
				</el-popconfirm>
			</template>
		</el-table-column>
	</el-table>
</template>

<style lang="scss" scoped></style>
