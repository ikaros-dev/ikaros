<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { apiClient } from '@/utils/api-client';
import { base64Encode } from '@/utils/string-util';
import {
	DocumentAdd,
	FolderAdd,
	KnifeFork,
	CopyDocument,
	Brush,
	FolderDelete,
} from '@element-plus/icons-vue';
import { ElRow, ElCol, ElButton, ElTable, ElTableColumn } from 'element-plus';
import { Folder } from '@runikaros/api-client';

const findFolder = ref({
	name: 'root',
	parentId: -1,
});
const folders = ref<Folder>();
const fetchFolders = async () => {
	const { data } = await apiClient.folder.findByParentIdAndName({
		name: base64Encode(findFolder.value.name),
		parentId: findFolder.value.parentId,
	});
	folders.value = data;
};

const formatTableRowKey = (row) => {
	const folderArr = row?.folders;
	if (!folderArr || folderArr.length === 0) {
		return '[]';
	}
	const rowKeyArr: number[] = [];
	folderArr.forEach((folder) => {
		rowKeyArr.push(folder.id as number);
	});
	return JSON.stringify(rowKeyArr);
};

const currentSelectFolderId = ref();
const onCurrentChange = (val) => {
	currentSelectFolderId.value = val.id;
};

onMounted(fetchFolders);
</script>

<template>
	<el-row>
		<el-col :span="24">
			<el-button :icon="DocumentAdd">添加文件</el-button>
			<el-button :icon="FolderAdd">新建目录</el-button>
			<el-button :icon="KnifeFork">剪切</el-button>
			<el-button :icon="CopyDocument">复制</el-button>
			<el-button :icon="Brush">粘贴</el-button>
			<el-button :icon="FolderDelete" type="danger">删除</el-button>
		</el-col>
	</el-row>
	<br />
	<el-row>
		<el-col :span="24">
			<el-table
				:data="folders?.folders"
				style="width: 100%"
				:row-key="formatTableRowKey"
				:default-expand-all="true"
				highlight-current-row
				@current-change="onCurrentChange"
			>
				<el-table-column type="expand">
					<template #default="props">
						<el-table :data="props.row?.files" style="width: 100%">
							<el-table-column prop="name" label="文件名" width="180" />
						</el-table>
					</template>
				</el-table-column>
				<el-table-column prop="id" label="目录ID" width="180" />
				<el-table-column prop="name" label="目录名" width="180" />
				<el-table-column prop="create_time" label="创建时间" width="180" />
				<el-table-column prop="update_time" label="更新时间" width="180" />
			</el-table>
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
