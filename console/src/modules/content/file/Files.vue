<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import type { FileEntity } from '@runikaros/api-client';
import FileFragmentUploadDrawer from './FileFragmentUploadDrawer.vue';
import FileRemoteActionDialog from './FileRemoteActionDialog.vue';
import FileDeatilDrawer from './FileDeatilDrawer.vue';
import {
	ElButton,
	ElCol,
	ElForm,
	ElFormItem,
	ElIcon,
	ElInput,
	ElMessage,
	ElOption,
	ElPagination,
	ElPopconfirm,
	ElRow,
	ElSelect,
	ElTable,
	ElTableColumn,
} from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import { base64Encode } from '@/utils/string-util';
import { useRoute } from 'vue-router';
import { onMounted, ref, watch } from 'vue';
import router from '@/router';

const fileUploadDrawerVisible = ref(false);

// eslint-disable-next-line no-unused-vars
const onFileUploadDrawerClose = (firstFile) => {
	fileUploadDrawerVisible.value = false;
	// console.log('receive firstFile:', firstFile);
	fetchFiles();
};

const findFilesCondition = ref({
	page: 1,
	size: 10,
	total: 10,
	fileName: '',
	type: undefined,
});

const files = ref<FileEntity[]>([]);

const fetchFiles = async () => {
	const { data } = await apiClient.file.listFilesByCondition({
		page: findFilesCondition.value.page,
		size: findFilesCondition.value.size,
		fileName: base64Encode(findFilesCondition.value.fileName),
		type: findFilesCondition.value.type,
	});
	findFilesCondition.value.page = data.page;
	findFilesCondition.value.size = data.size;
	findFilesCondition.value.total = data.total;
	files.value = data.items;
};

const currentShowDetailFile = ref<FileEntity>({});
const fileDetailDrawerVisible = ref(false);
const showFileDetails = (file) => {
	currentShowDetailFile.value = file;
	fileDetailDrawerVisible.value = true;
};

const handleFileDetailDrawerDelete = () => {
	fetchFiles();
};

const onCurrentPageChange = (val: number) => {
	findFilesCondition.value.page = val;
	fetchFiles();
};

const onSizeChange = (val: number) => {
	findFilesCondition.value.size = val;
	fetchFiles();
};

const deleteButtonLoading = ref(false);
const handleDelete = async (file: FileEntity) => {
	deleteButtonLoading.value = true;
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
		})
		.finally(() => {
			deleteButtonLoading.value = false;
		});
};

const onFileTypeSelectChange = (val) => {
	findFilesCondition.value.type = val;
	fetchFiles();
};

const updateFile = async (file: FileEntity) => {
	await apiClient.file
		.updateFile({
			fileEntity: file,
		})
		.then(() => {
			ElMessage.success('更新文件成功，文件名称：' + file.name);
			fetchFiles();
		});
};

const route = useRoute();
watch(
	() => route.query,
	(newValue) => {
		// console.log(newValue);
		if (newValue) {
			findFilesCondition.value.fileName = newValue.searchFileName as string;
		}
	},
	{ immediate: true }
);

const fileRemoteActionDialogVisible = ref(false);
const fileRemoteFileId = ref(-1);
const fileRemoteIsPush = ref(true);
const openFileRemoteActionDialog = (file: FileEntity) => {
	fileRemoteFileId.value = file.id as number;
	fileRemoteIsPush.value = file.canRead as boolean;
	fileRemoteActionDialogVisible.value = true;
};
const onFileRemoteActionDialogCloseWithTaskName = (taskName) => {
	router.push('/tasks/task/details/' + taskName);
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

	<FileRemoteActionDialog
		v-model:visible="fileRemoteActionDialogVisible"
		v-model:file-id="fileRemoteFileId"
		v-model:is-push="fileRemoteIsPush"
		@closeWithTaskName="onFileRemoteActionDialogCloseWithTaskName"
	/>

	<el-row :gutter="10">
		<el-col :xs="24" :sm="24" :md="24" :lg="10" :xl="10">
			<el-form :inline="true" :model="findFilesCondition">
				<el-form-item label="文件名称">
					<el-input
						v-model="findFilesCondition.fileName"
						placeholder="模糊匹配回车搜索"
						clearable
						@change="fetchFiles"
					/>
				</el-form-item>

				<el-form-item label="文件类型">
					<el-select
						v-model="findFilesCondition.type"
						clearable
						style="width: 90px"
						@change="onFileTypeSelectChange"
					>
						<el-option label="图片" value="IMAGE" />
						<el-option label="视频" value="VIDEO" />
						<el-option label="文档" value="DOCUMENT" />
						<el-option label="音声" value="VOICE" />
						<el-option label="未知" value="UNKNOWN" />
					</el-select>
				</el-form-item>
			</el-form>
		</el-col>

		<el-col :xs="24" :sm="24" :md="24" :lg="10" :xl="10">
			<el-pagination
				v-model:page-size="findFilesCondition.size"
				v-model:current-page="findFilesCondition.page"
				background
				:total="findFilesCondition.total"
				layout="total, sizes, prev, pager, next, jumper"
				@current-change="onCurrentPageChange"
				@size-change="onSizeChange"
			/>
		</el-col>

		<el-col
			:xs="24"
			:sm="24"
			:md="24"
			:lg="4"
			:xl="4"
			style="text-align: right"
		>
			<el-button plain @click="fileUploadDrawerVisible = true">
				<el-icon>
					<Upload />
				</el-icon>
				上传文件
			</el-button>
		</el-col>
	</el-row>

	<el-table
		:data="files"
		stripe
		style="width: 100%"
		@row-dblclick="showFileDetails"
	>
		<el-table-column prop="id" label="文件ID" width="80" sortable />
		<el-table-column prop="name" label="文件名称">
			<template #default="scope">
				<el-input
					v-model="scope.row.name"
					@keydown.enter="updateFile(scope.row)"
				>
				</el-input>
			</template>
		</el-table-column>
		<el-table-column prop="originalName" label="原始名称"></el-table-column>
		<el-table-column prop="url" label="文件URL" />
		<el-table-column label="操作" width="300">
			<template #default="scoped">
				<el-button plain @click="showFileDetails(scoped.row)">详情</el-button>

				<el-button plain @click="openFileRemoteActionDialog(scoped.row)">
					<span v-if="scoped.row.canRead"> 推送 </span>
					<span v-else> 拉取 </span>
				</el-button>
				<el-popconfirm
					title="你确定要删除该文件？"
					confirm-button-text="确定"
					cancel-button-text="取消"
					confirm-button-type="danger"
					@confirm="handleDelete(scoped.row)"
				>
					<template #reference>
						<el-button :loading="deleteButtonLoading" type="danger">
							删除
						</el-button>
					</template>
				</el-popconfirm>
			</template>
		</el-table-column>
	</el-table>
</template>

<style lang="scss" scoped></style>
