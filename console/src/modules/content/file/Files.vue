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
import { base64Encode, formatFileSize } from '@/utils/string-util';
import { useRoute } from 'vue-router';
import { onMounted, ref, watch } from 'vue';
import router from '@/router';
import moment from 'moment';
import { useSettingStore } from '@/stores/setting';
import { useI18n } from 'vue-i18n';

// eslint-disable-next-line no-unused-vars
const settingStore = useSettingStore();

const fileUploadDrawerVisible = ref(false);

const { t } = useI18n();

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

const handleDelete = async (file: FileEntity) => {
	await apiClient.file
		.deleteFile({
			id: file.id as number,
		})
		.then(() => {
			ElMessage.success(
				t('core.file.message.delete.success') + file.id + '-' + file.name
			);
			fetchFiles();
		})
		.catch((err) => {
			console.error(err);
			ElMessage.error(t('core.file.message.delete.fail') + err.message);
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
			ElMessage.success(t('core.file.message.update.success') + file.name);
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
	router.push('/tasks?name=' + taskName.substring(0, taskName.indexOf('-')));
};

const dateFormat = (row, column) => {
	var date = row[column.property];

	if (date == undefined) {
		return '';
	}

	return moment(date).format('YYYY-MM-DD HH:mm:ss');
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
		<el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
			<el-form :inline="true" :model="findFilesCondition">
				<el-form-item
					:label="t('core.file.form.item.label.search.type')"
					style="width: 15%"
				>
					<el-select
						v-model="findFilesCondition.type"
						clearable
						@change="onFileTypeSelectChange"
					>
						<el-option label="图片" value="IMAGE" />
						<el-option label="视频" value="VIDEO" />
						<el-option label="文档" value="DOCUMENT" />
						<el-option label="音声" value="VOICE" />
						<el-option label="未知" value="UNKNOWN" />
					</el-select>
				</el-form-item>

				<el-form-item
					:label="t('core.file.form.item.label.search.name')"
					style="width: 80%"
				>
					<el-input
						v-model="findFilesCondition.fileName"
						:placeholder="t('core.file.form.item.label.search.namePlaceHolder')"
						clearable
						@change="fetchFiles"
					/>
				</el-form-item>
			</el-form>
		</el-col>

		<el-col :xs="24" :sm="24" :md="24" :lg="20" :xl="20">
			<el-pagination
				v-model:page-size="findFilesCondition.size"
				v-model:current-page="findFilesCondition.page"
				background
				:total="findFilesCondition.total"
				:pager-count="5"
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
				{{ t('core.file.button.label.upload') }}
			</el-button>
		</el-col>
	</el-row>

	<el-table
		:data="files"
		stripe
		style="width: 100%"
		@row-dblclick="showFileDetails"
	>
		<el-table-column
			prop="id"
			:label="t('core.file.table.column.label.id')"
			width="80"
			sortable
		/>
		<el-table-column
			prop="name"
			:label="t('core.file.table.column.label.name')"
		>
			<template #default="scope">
				<el-input
					v-model="scope.row.name"
					@keydown.enter="updateFile(scope.row)"
				>
				</el-input>
			</template>
		</el-table-column>
		<el-table-column
			prop="updateTime"
			:label="t('core.file.table.column.label.updateTime')"
			:formatter="dateFormat"
			width="160"
		/>
		<el-table-column
			:label="t('core.file.table.column.label.size')"
			width="100"
		>
			<template #default="scoped">
				{{ formatFileSize(scoped.row.size) }}
			</template>
		</el-table-column>
		<el-table-column
			:label="t('core.file.table.column.label.operators')"
			width="300"
		>
			<template #default="scoped">
				<el-button plain @click="showFileDetails(scoped.row)">
					{{ t('core.file.table.column.operations.details') }}
				</el-button>

				<el-button
					v-if="settingStore.remoteEnable"
					plain
					@click="openFileRemoteActionDialog(scoped.row)"
				>
					<span v-if="scoped.row.canRead">
						{{ t('core.file.table.column.operations.push') }}
					</span>
					<span v-else>
						{{ t('core.file.table.column.operations.pull') }}
					</span>
				</el-button>
				<el-popconfirm
					:title="
						t('core.file.table.column.operations.delete.popconfirm.title')
					"
					:confirm-button-text="
						t('core.file.table.column.operations.delete.popconfirm.confirm')
					"
					:cancel-button-text="
						t('core.file.table.column.operations.delete.popconfirm.cancel')
					"
					confirm-button-type="danger"
					@confirm="handleDelete(scoped.row)"
				>
					<template #reference>
						<el-button type="danger">
							{{ t('core.file.table.column.operations.delete.button') }}
						</el-button>
					</template>
				</el-popconfirm>
			</template>
		</el-table-column>
	</el-table>
</template>

<style lang="scss" scoped></style>
