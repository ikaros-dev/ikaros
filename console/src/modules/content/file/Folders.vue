<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { apiClient } from '@/utils/api-client';
import { Folder } from '@runikaros/api-client';
import { base64Encode } from '@/utils/string-util';
import { fileTypeMap } from '@/modules/common/constants';
import FileFragmentUploadDrawer from './FileFragmentUploadDrawer.vue';
import FileDeatilDrawer from './FileDeatilDrawer.vue';
import { formatFileSize } from '@/utils/string-util';
import {
	DocumentAdd,
	FolderAdd,
	// eslint-disable-next-line no-unused-vars
	KnifeFork,
	// eslint-disable-next-line no-unused-vars
	CopyDocument,
	// eslint-disable-next-line no-unused-vars
	Brush,
	FolderDelete,
} from '@element-plus/icons-vue';
import {
	ElRow,
	ElCol,
	ElButton,
	ElTable,
	ElTableColumn,
	ElDialog,
	ElForm,
	ElFormItem,
	ElInput,
	ElMessage,
	ElBreadcrumb,
	ElBreadcrumbItem,
	ElPopconfirm,
} from 'element-plus';
import { computed } from 'vue';

const findFolder = ref({
	name: 'root',
	parentId: -1,
});
const folder = ref<Folder>();
const fetchFolders = async () => {
	const { data } = await apiClient.folder.findByParentIdAndName({
		name: base64Encode(findFolder.value.name),
		parentId: findFolder.value.parentId,
	});
	folder.value = data;
};

const formatTableRowKey = (row) => {
	return row.id;
};

// eslint-disable-next-line no-unused-vars
const formatTableExpendRowKey = computed(() => {
	const folderArr = folder.value?.folders;
	if (!folderArr || folderArr.length === 0) {
		return [];
	}
	const rowKeyArr: number[] = [];
	folderArr.forEach((folder) => {
		if (folder.files && folder.files.length > 0) {
			rowKeyArr.push(folder.id as number);
		}
	});
	// console.log(rowKeyArr);
	return rowKeyArr;
});

const currentSelectFolder = ref<Folder>({
	id: 0,
	name: 'root',
	parent_id: -1,
});
const onCurrentChange = (val) => {
	currentSelectFolder.value = val;
	// console.log(currentSelectFolder.value);
};
const currentSelectFile = ref();
const onCurrentFileChange = (val) => {
	currentSelectFile.value = val;
};

const dialogFormVisible = ref(false);
const createFolder = ref({
	name: '',
});
const onCreateFolderButtonClick = async () => {
	await apiClient.folder.createFolder({
		name: base64Encode(createFolder.value.name),
		parentId: currentSelectFolder.value.id,
	});
	ElMessage.success('创建目录成功：' + createFolder.value);
	dialogFormVisible.value = false;
	fetchFolders();
};

// eslint-disable-next-line no-unused-vars
const moveFile2Folder = async () => {
	await apiClient.file.moveFileToAppointFolder({
		id: currentSelectFile.value.id,
		folderId: currentSelectFolder.value.id + '',
	});
	ElMessage.success('移动文件成功');
	fetchFolders();
};

const fileUploadDrawerVisible = ref(false);
const onFileUploadDrawerClose = () => {
	fetchFolders();
};

const currentShowDetailFile = ref();
const fileDetailDrawerVisible = ref(false);
const handleFileDetailDrawerDelete = () => {
	fetchFolders();
};
const showFileDetails = (file) => {
	currentShowDetailFile.value = file;
	fileDetailDrawerVisible.value = true;
};

interface Path {
	name: string;
	parentId: number;
}

const paths = ref<Path[]>([
	{
		name: 'root',
		parentId: -1,
	},
]);

const enrtyFolder = (folder) => {
	// console.log(folder);
	findFolder.value.name = folder.name;
	findFolder.value.parentId = folder.parent_id;
	paths.value.push({
		name: folder.name,
		parentId: folder.parent_id,
	});
	fetchFolders();
};
const onBreadcrumbClick = (path) => {
	// console.log(path);
	var index = paths.value.indexOf(path);
	if (index !== -1) {
		paths.value.splice(index + 1);
	}
	findFolder.value.name = path.name;
	findFolder.value.parentId = path.parentId;
	fetchFolders();
};

const onDeleteButtonClick = async () => {
	let needDeteFolderId = -1;
	if (currentSelectFile.value) {
		needDeteFolderId = currentSelectFile.value.id as number;
	} else {
		needDeteFolderId = folder.value?.id as number;
	}
	await apiClient.folder.deleteFolder({
		id: needDeteFolderId,
		allowDeleteWhenChildExists: false,
	});
	ElMessage.success('删除目录成功, ID：' + needDeteFolderId);
	if (needDeteFolderId === folder.value?.id) {
		findFolder.value.parentId = -1;
		findFolder.value.name = 'root';
		paths.value = [
			{
				name: 'root',
				parentId: -1,
			},
		];
	}
	fetchFolders();
};

const selectFiles = ref<any[]>([]);
const onSelectionChange = (files: any) => {
	// console.log(files);
	files.forEach((file) => {
		selectFiles.value.push(file);
	});
	// console.log(selectFiles.value);
};

const pasteFiles = async () => {
	let needPasteFolderId = -1;
	if (currentSelectFile.value) {
		needPasteFolderId = currentSelectFile.value.id as number;
	} else {
		needPasteFolderId = folder.value?.id as number;
	}
	selectFiles.value.forEach(async (file) => {
		await apiClient.file.moveFileToAppointFolder({
			id: file.id,
			folderId: needPasteFolderId + '',
		});
	});
	ElMessage.success('粘贴成功');
	selectFiles.value = [];
	fetchFolders();
};

onMounted(fetchFolders);
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

	<el-dialog v-model="dialogFormVisible" title="新建文件夹">
		<el-form :model="createFolder">
			<el-form-item label="父文件夹ID" :label-width="100">
				<el-input
					v-model="currentSelectFolder.id"
					disabled
					style="max-width: 600px"
					size="large"
				/>
			</el-form-item>
			<el-form-item label="父文件夹名称" :label-width="100">
				<el-input
					v-model="currentSelectFolder.name"
					disabled
					style="max-width: 600px"
					size="large"
				/>
			</el-form-item>
			<el-form-item label="文件夹名称" :label-width="100">
				<el-input
					v-model="createFolder.name"
					autocomplete="off"
					style="max-width: 600px"
					size="large"
				/>
			</el-form-item>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="dialogFormVisible = false">返回</el-button>
				<el-button type="primary" @click="onCreateFolderButtonClick">
					提交
				</el-button>
			</span>
		</template>
	</el-dialog>

	<el-row>
		<el-col :span="24">
			<el-button :icon="DocumentAdd" @click="fileUploadDrawerVisible = true">
				添加文件
			</el-button>
			<el-button :icon="FolderAdd" @click="dialogFormVisible = true">
				新建目录
			</el-button>
			<!-- <el-button :icon="KnifeFork" @click="handleSelect">剪切</el-button> -->
			<!-- <el-button :icon="CopyDocument">复制</el-button> -->
			<el-button :icon="Brush" @click="pasteFiles">粘贴</el-button>
			<el-popconfirm
				title="您确定删除选中目录吗?"
				width="200"
				@confirm="onDeleteButtonClick"
			>
				<template #reference>
					<el-button :icon="FolderDelete" type="danger">删除</el-button>
				</template>
			</el-popconfirm>
		</el-col>
	</el-row>
	<br />
	<el-row>
		<el-col :span="24">
			<el-breadcrumb separator="/">
				<el-breadcrumb-item v-for="(path, index) in paths" :key="index">
					<span style="cursor: pointer" @click="onBreadcrumbClick(path)">
						{{ path.name }}
					</span>
				</el-breadcrumb-item>
			</el-breadcrumb>
		</el-col>
	</el-row>
	<br />
	<el-row style="width: 100%">
		<el-col :span="24">
			<el-table
				:data="folder?.folders"
				style="width: 100%"
				:row-key="formatTableRowKey"
				highlight-current-row
				@current-change="onCurrentChange"
				@row-dblclick="enrtyFolder"
			>
				<el-table-column prop="id" label="ID" width="60" />
				<el-table-column prop="name" label="目录名" width="180" />
				<el-table-column prop="create_time" label="创建时间" />
				<el-table-column prop="update_time" label="更新时间" />
			</el-table>
			<el-table
				:data="folder?.files"
				style="width: 100%"
				:highlight-current-row="false"
				@current-change="onCurrentFileChange"
				@row-dblclick="showFileDetails"
				@selection-change="onSelectionChange"
			>
				<el-table-column type="selection" width="55" />
				<el-table-column prop="id" label="ID" width="60" />
				<el-table-column prop="name" label="文件名" width="180" />
				<el-table-column prop="originalName" label="原始名" width="250" />
				<el-table-column prop="type" label="类型" width="180">
					<template #default="scoped">
						{{ fileTypeMap.get(scoped.row.type as string) }}
					</template>
				</el-table-column>
				<el-table-column prop="size" label="大小" width="180">
					<template #default="scoped">
						{{ formatFileSize(scoped.row.size) }}
					</template>
				</el-table-column>
				<el-table-column prop="url" label="URL">
					<template #default="scoped">
						<a :href="scoped.row.url" target="_blank">
							{{ scoped.row.url }}
						</a>
					</template>
				</el-table-column>
			</el-table>
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
