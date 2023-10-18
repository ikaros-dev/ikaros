<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { Attachment } from '@runikaros/api-client';
import moment from 'moment';
import {
	Upload,
	Search,
	Folder,
	Document,
	FolderDelete,
	FolderAdd,
} from '@element-plus/icons-vue';
import {
	ElRow,
	ElCol,
	ElInput,
	ElForm,
	ElFormItem,
	ElPagination,
	ElButton,
	ElIcon,
	ElBreadcrumb,
	ElBreadcrumbItem,
	ElTable,
	ElTableColumn,
	ElDialog,
	ElMessage,
	ElPopconfirm,
	ElMessageBox,
} from 'element-plus';
import { onMounted } from 'vue';
import { apiClient } from '@/utils/api-client';
import { base64Encode, formatFileSize } from '@/utils/string-util';
import AttachmentFragmentUploadDrawer from './AttachmentFragmentUploadDrawer.vue';

// eslint-disable-next-line no-unused-vars
const { t } = useI18n();

const attachmentCondition = ref({
	page: 1,
	size: 10,
	total: 10,
	parentId: 0,
	name: '',
	type: undefined,
});

const attachments = ref<Attachment[]>([]);
const fetchAttachments = async () => {
	const { data } = await apiClient.attachment.listAttachmentsByCondition({
		page: attachmentCondition.value.page,
		size: attachmentCondition.value.size,
		name: base64Encode(attachmentCondition.value.name),
		parentId: attachmentCondition.value.parentId as any as string,
	});
	attachments.value = data.items;
	attachmentCondition.value.page = data.page;
	attachmentCondition.value.size = data.size;
	attachmentCondition.value.total = data.total;
};

const onCurrentPageChange = (val: number) => {
	attachmentCondition.value.page = val;
	fetchAttachments();
};

const onSizeChange = (val: number) => {
	attachmentCondition.value.size = val;
	fetchAttachments();
};

const attachmentUploadDrawerVisible = ref(false);
const onFileUploadDrawerClose = () => {
	fetchAttachments();
};

const onBreadcrumbClick = (path) => {
	// console.log('path', path);
	var index = paths.value.indexOf(path);
	if (index !== -1) {
		paths.value.splice(index + 1);
	}
	attachmentCondition.value.parentId = path.id;
	fetchAttachments();
};
interface Path {
	name: string;
	parentId: number;
	id: number;
}

const paths = ref<Path[]>([
	{
		name: '/',
		parentId: 0,
		id: 0,
	},
]);

const entryAttachment = (attachment) => {
	// console.log('attachment', attachment);
	if ('Directory' === attachment.type) {
		attachmentCondition.value.parentId = attachment.id;
		paths.value.push({
			name: attachment.name,
			parentId: attachment.parent_id,
			id: attachment.id,
		});
		fetchAttachments();
	}
};

const dateFormat = (row, column) => {
	var date = row[column.property];

	if (date == undefined) {
		return '';
	}

	return moment(date).format('YYYY-MM-DD HH:mm:ss');
};

onMounted(fetchAttachments);

const dialogFolderVisible = ref(false);
const createFolderName = ref('');
const onCreateFolderButtonClick = async () => {
	await apiClient.attachment.createDirectory({
		parentId: attachmentCondition.value.parentId as any as string,
		name: base64Encode(createFolderName.value),
	});
	ElMessage.success('创建目录成功，目录名：' + createFolderName.value);
	dialogFolderVisible.value = false;
	await fetchAttachments();
};

const currentSelectionAttachment = ref<Attachment>();
const onCurrentChange = (val: Attachment | undefined) => {
	if (val !== undefined) {
		currentSelectionAttachment.value = val;
	}
};

const onDeleteButtonClick = async () => {
	if (
		!currentSelectionAttachment.value ||
		!currentSelectionAttachment.value?.id
	) {
		return;
	}

	if (currentSelectionAttachment.value?.type === 'Directory') {
		ElMessageBox.confirm(
			'您当前在删除目录，系统默认会删除目录里的所有内容，您确定要删除吗？',
			'警告',
			{
				confirmButtonText: '确认',
				cancelButtonText: '取消',
				type: 'warning',
			}
		)
			.then(async () => {
				await apiClient.attachment.deleteAttachment({
					id: currentSelectionAttachment.value?.id as number,
				});
				ElMessage.success(
					'删除目录【' + currentSelectionAttachment.value?.name + '】成功。'
				);
			})
			.catch(() => {
				ElMessage({
					type: 'info',
					message: '删除目录取消',
				});
			});
	} else {
		await apiClient.attachment.deleteAttachment({
			id: currentSelectionAttachment.value.id as number,
		});
		ElMessage.success(
			'删除文件【' + currentSelectionAttachment.value.name + '】成功'
		);
	}
	fetchAttachments();
};
</script>

<template>
	<AttachmentFragmentUploadDrawer
		v-model:visible="attachmentUploadDrawerVisible"
		v-model:parentId="attachmentCondition.parentId"
		@fileUploadDrawerCloes="onFileUploadDrawerClose"
	/>

	<el-dialog v-model="dialogFolderVisible" title="新建目录">
		<el-input
			v-model="createFolderName"
			autocomplete="off"
			size="large"
			placeholder="请输入目录名称，提交后会在当前目录创造子目录。"
		/>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="dialogFolderVisible = false">{{
					t('core.folder.createDialog.cancel')
				}}</el-button>
				<el-button type="primary" @click="onCreateFolderButtonClick">
					{{ t('core.folder.createDialog.confirm') }}
				</el-button>
			</span>
		</template>
	</el-dialog>

	<el-row>
		<el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
			<el-button plain @click="attachmentUploadDrawerVisible = true">
				<el-icon>
					<Upload />
				</el-icon>
				上传附件
			</el-button>
			<el-button :icon="FolderAdd" @click="dialogFolderVisible = true">
				新建目录
			</el-button>

			<el-popconfirm
				v-if="currentSelectionAttachment && currentSelectionAttachment.id"
				:title="
					'您确定删除' +
					(currentSelectionAttachment.type === 'Directory' ? '目录' : '附件') +
					'【' +
					currentSelectionAttachment.name +
					'】' +
					'吗？'
				"
				width="300"
				@confirm="onDeleteButtonClick"
			>
				<template #reference>
					<el-button :icon="FolderDelete" type="danger"> 删除 </el-button>
				</template>
			</el-popconfirm>
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
			<el-input
				v-model="attachmentCondition.name"
				placeholder="搜索附件，模糊匹配，回车搜查"
				clearable
				@change="fetchAttachments"
			>
				<template #append>
					<el-button :icon="Search" @click="fetchAttachments" />
				</template>
			</el-input>
		</el-col>
	</el-row>

	<br />

	<el-row>
		<el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
			<el-pagination
				v-model:page-size="attachmentCondition.size"
				v-model:current-page="attachmentCondition.page"
				background
				:total="attachmentCondition.total"
				:pager-count="5"
				layout="total, sizes, prev, pager, next, jumper"
				@current-change="onCurrentPageChange"
				@size-change="onSizeChange"
			/>
		</el-col>
	</el-row>

	<br />

	<el-row>
		<el-col :span="24">
			<el-form :inline="true">
				<el-form-item label="路径" style="width: 100%">
					<el-breadcrumb separator=">">
						<el-breadcrumb-item v-for="(path, index) in paths" :key="index">
							<el-button @click="onBreadcrumbClick(path)">
								{{ path.name }}
							</el-button>
						</el-breadcrumb-item>
					</el-breadcrumb>
				</el-form-item>
			</el-form>
		</el-col>
	</el-row>

	<el-row>
		<el-col :span="24">
			<el-table
				:data="attachments"
				style="width: 100%"
				row-key="id"
				highlight-current-row
				@current-change="onCurrentChange"
				@row-dblclick="entryAttachment"
			>
				<!-- <el-table-column type="selection" width="60" /> -->
				<!-- <el-table-column prop="id" label="ID" width="60" /> -->
				<el-table-column prop="name" label="名称" show-overflow-tooltip>
					<template #default="scoped">
						<el-icon
							size="25"
							style="position: relative; top: 7px; margin: 0 5px 0 0px"
						>
							<Folder v-if="'Directory' === scoped.row.type" />
							<Document v-else />
						</el-icon>
						<!-- &nbsp;&nbsp; -->
						<span>
							{{ scoped.row.name }}
						</span>
					</template>
				</el-table-column>
				<el-table-column
					prop="updateTime"
					label="更新时间"
					width="160"
					:formatter="dateFormat"
				/>
				<el-table-column prop="size" label="大小" width="100">
					<template #default="scoped">
						<span v-if="scoped.row.type === 'File'">
							{{ formatFileSize(scoped.row.size) }}
						</span>
					</template>
				</el-table-column>
			</el-table>
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped>
.ik-attachment-breadcrumb-item {
	width: 20px;
	cursor: pointer;
}

// .ik-attachment-breadcrumb-item:hover {
// 	border: 1px red solid;
// }
</style>
