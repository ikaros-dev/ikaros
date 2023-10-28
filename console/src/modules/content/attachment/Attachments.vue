<script setup lang="ts">
import { ref, watch, onMounted, h, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import { Attachment } from '@runikaros/api-client';
import { isImage, isVideo, isVoice } from '@/utils/file';
import moment from 'moment';
import { apiClient } from '@/utils/api-client';
import AttachmentFragmentUploadDrawer from './AttachmentFragmentUploadDrawer.vue';
import AttachmentDeatilDrawer from './AttachmentDeatilDrawer.vue';
import AttachmentDirectorySelectDialog from './AttachmentDirectorySelectDialog.vue';
import { useRoute } from 'vue-router';

import '@imengyu/vue3-context-menu/lib/vue3-context-menu.css';
import ContextMenu from '@imengyu/vue3-context-menu';

import {
	base64Encode,
	base64Decode,
	formatFileSize,
} from '@/utils/string-util';
import {
	Upload,
	Search,
	Folder,
	Document,
	FolderDelete,
	FolderAdd,
	Picture,
	Headset,
	Film,
	Pointer,
	Delete,
	Position,
	CopyDocument,
	Download,
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
import router from '@/router';

// eslint-disable-next-line no-unused-vars
const { t } = useI18n();
const route = useRoute();

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
	await updateBreadcrumbByParentPath();
};

const updateBreadcrumbByParentPath = async () => {
	const { data } = await apiClient.attachment.getAttachmentPathDirsById({
		id: attachmentCondition.value.parentId as number,
	});
	paths.value = data.map((att) => {
		const path: Path = {
			name: att.name as string,
			id: att.id as number,
			parentId: att.parentId as number,
		};
		return path;
	});
};

const onCurrentPageChange = async (val: number) => {
	attachmentCondition.value.page = val;
	await fetchAttachments();
};

const onSizeChange = async (val: number) => {
	attachmentCondition.value.size = val;
	await fetchAttachments();
};

const attachmentUploadDrawerVisible = ref(false);
const onFileUploadDrawerClose = async () => {
	await fetchAttachments();
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

const onBreadcrumbClick = async (path) => {
	// console.log('path', path);
	var index = paths.value.indexOf(path);
	if (index !== -1) {
		paths.value.splice(index + 1);
	}
	attachmentCondition.value.parentId = path.id;
	await fetchAttachments();
	// console.log('parentId', attachmentCondition.value.parentId);
};

const entryAttachment = async (attachment) => {
	// console.log('attachment', attachment);
	// console.log('attachment id:', attachment.id);
	// console.log('attachment name:', attachment.name);
	if ('Directory' === attachment.type) {
		attachmentCondition.value.parentId = attachment.id;
		paths.value.push({
			name: attachment.name,
			parentId: attachment.parentId,
			id: attachment.id,
		});
		await fetchAttachments();
	} else {
		currentSelectionAttachment.value = attachment;
		attachmentDetailDrawerVisible.value = true;
	}
	// console.log('parentId', attachmentCondition.value.parentId);
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
const createFolderInputRef = ref();
const onCreateFolderButtonClick = async () => {
	await apiClient.attachment.createDirectory({
		parentId: attachmentCondition.value.parentId as any as string,
		name: base64Encode(createFolderName.value),
	});
	ElMessage.success('创建目录成功，目录名：' + createFolderName.value);
	createFolderName.value = '';
	await fetchAttachments();
	dialogFolderVisible.value = false;
};
const onCreateFolderDialogOpen = () => {
	nextTick(() => {
		createFolderInputRef.value.focus();
	});
};

const currentSelectionAttachment = ref<Attachment>();
const onCurrentChange = (val: Attachment | undefined) => {
	if (val) {
		currentSelectionAttachment.value = val;
	}
};

const selectionAttachments = ref<Attachment[]>([]);

const onSelectionChange = (selections) => {
	// console.log('selections', selections);
	selectionAttachments.value = selections;
};

const deleteAttachment = async (attachment: Attachment) => {
	await apiClient.attachment
		.deleteAttachment({
			id: attachment?.id as number,
		})
		.then(() => {
			ElMessage.success(
				'删除' +
					(attachment.type === 'Directory' ? '目录' : '文件') +
					'【' +
					attachment.name +
					'】' +
					'成功。'
			);
		})
		.catch((e) => {
			// @ts-ignore
			let msg = e?.response?.data?.message;
			if (!msg) {
				msg = e.message;
			}
			console.log('error', msg, e);
			ElMessage.error(
				'删除' +
					(attachment.type === 'Directory' ? '目录' : '文件') +
					'【' +
					attachment.name +
					'】' +
					'失败：' +
					msg
			);
		});

	await fetchAttachments();
};

const deleteAttachments = async () => {
	currentSelectionAttachment.value?.type === 'Directory';
	await selectionAttachments.value.forEach(async (a) => {
		await deleteAttachment(a);
	});
	// ElMessage.success('批量删除目录成功。');
	await fetchAttachments();
};

const onDeleteButtonClick = async () => {
	if (!selectionAttachments.value || selectionAttachments.value.length === 0) {
		return;
	}

	// 检测选中的附件里是否有目录，如果有则进行二次提示确认
	let hasDir: boolean = false;
	selectionAttachments.value.forEach((a) => {
		if (a.type === 'Directory') {
			hasDir = true;
			return;
		}
	});

	if (hasDir) {
		ElMessageBox.confirm(
			'伊卡洛斯检测到您当前待删除的附件（选中的）包含目录类型，系统默认会删除目录里的所有内容，您确定要删除吗？',
			'警告',
			{
				confirmButtonText: '确认',
				cancelButtonText: '取消',
				type: 'warning',
			}
		)
			.then(async () => {
				await deleteAttachments();
			})
			.catch(() => {
				ElMessage({
					type: 'info',
					message: '批量删除目录取消',
				});
			});
	} else {
		await deleteAttachments();
	}
};

const copyValue = async (val: string) => {
	if (navigator.clipboard && window.isSecureContext) {
		return navigator.clipboard.writeText(val);
	} else {
		const textArea = document.createElement('textarea');
		textArea.value = val;
		document.body.appendChild(textArea);
		textArea.focus();
		textArea.select();
		return new Promise((res, rej) => {
			document.execCommand('copy') ? res(val) : rej();
			textArea.remove();
		});
	}
};

const attachmentDetailDrawerVisible = ref(false);

const onRowContextmenu = (row, column, event) => {
	currentSelectionAttachment.value = row;
	event.preventDefault();
	ContextMenu.showContextMenu({
		x: event.x,
		y: event.y,
		minWidth: 320,
		items: [
			{
				label:
					currentSelectionAttachment.value?.type === 'Directory'
						? '进入'
						: '详情',
				divided: 'down',
				icon: h(Pointer, { style: 'height: 14px' }),
				onClick: () => {
					entryAttachment(currentSelectionAttachment.value);
				},
			},
			{
				label: '复制简短名称',
				icon: h(CopyDocument, { style: 'height: 14px' }),
				onClick: async () => {
					const name = currentSelectionAttachment.value?.name as string;
					var simpleName = name.replace(/\[.*?\]/g, '');
					simpleName = simpleName.substring(0, simpleName.lastIndexOf('.'));
					await copyValue(simpleName);
					ElMessage.success('已复制附件【' + name + '】的简短名称到剪贴板');
				},
			},
			{
				label: '复制完整名称',
				icon: h(CopyDocument, { style: 'height: 14px' }),
				onClick: async () => {
					const name = currentSelectionAttachment.value?.name as string;
					await copyValue(name);
					ElMessage.success('已复制附件【' + name + '】的完整名称到剪贴板');
				},
			},
			{
				label: '复制URL',
				divided: 'down',
				disabled: currentSelectionAttachment.value?.type !== 'File',
				icon: h(CopyDocument, { style: 'height: 14px' }),
				onClick: async () => {
					const name = currentSelectionAttachment.value?.name as string;
					const url = currentSelectionAttachment.value?.url as string;
					await copyValue(url);
					ElMessage.success('已复制附件【' + name + '】的URL到剪贴板');
				},
			},
			{
				label: '下载',
				disabled: currentSelectionAttachment.value?.type !== 'File',
				icon: h(Download, { style: 'height: 14px' }),
				onClick: async () => {
					const url = currentSelectionAttachment.value?.url as string;
					window.open(url);
				},
			},
			{
				label: '删除',
				icon: h(Delete, { style: 'height: 14px; color: red;' }),
				onClick: async () => {
					if (currentSelectionAttachment.value?.type === 'Directory') {
						await ElMessageBox.confirm(
							'您当前删除的附件【' +
								currentSelectionAttachment.value.name +
								'】为目录类型，系统默认会删除目录里的所有内容，您确定要删除吗？',
							'警告',
							{
								confirmButtonText: '确认',
								cancelButtonText: '取消',
								type: 'warning',
							}
						)
							.then(async () => {
								await deleteAttachment(
									currentSelectionAttachment.value as Attachment
								);
							})
							.catch(() => {
								ElMessage({
									type: 'info',
									message: '批量删除目录取消',
								});
							});
					} else {
						await ElMessageBox.confirm(
							'您当前待删除的附件为【' +
								currentSelectionAttachment.value?.name +
								'】您确定要删除吗？',
							'警告',
							{
								confirmButtonText: '确认',
								cancelButtonText: '取消',
								type: 'warning',
							}
						)
							.then(async () => {
								await deleteAttachment(
									currentSelectionAttachment.value as Attachment
								);
							})
							.catch(() => {
								ElMessage({
									type: 'info',
									message:
										'删除目录【' +
										currentSelectionAttachment.value?.name +
										'】取消',
								});
							});
					}
					await fetchAttachments();
				},
			},
		],
	});
};

const directorySelectDialogVisible = ref(false);
const onDirSelected = async (targetDirid: number) => {
	for (const attachment of selectionAttachments.value.filter(
		(attachment) => targetDirid !== attachment.id
	)) {
		attachment.parentId = targetDirid;
		await apiClient.attachment.updateAttachment({
			attachment: attachment,
		});
	}
	await ElMessage.success('批量移动附件成功');
	await fetchAttachments();
};

watch(
	() => route.query,
	(newValue) => {
		// console.log(newValue);
		if (newValue) {
			attachmentCondition.value.name = decodeURI(
				base64Decode(newValue.name as string)
			);
			if (newValue.parentId) {
				attachmentCondition.value.parentId = parseInt(
					newValue.parentId as string
				);
			}
			fetchAttachments();
		}
	},
	{ immediate: true }
);
watch(attachmentCondition.value, () => {
	// console.log('attachmentCondition.value', attachmentCondition.value);
	const name = attachmentCondition.value.name;
	const parentId = attachmentCondition.value.parentId;
	const query = JSON.parse(JSON.stringify(route.query));
	if (name !== route.query.name) {
		query.name = base64Encode(encodeURI(name));
	}
	if (parentId !== parseInt(route.query.parentId as string)) {
		query.parentId = parentId + '';
	}
	router.push({ path: route.path, query });
});
</script>

<template>
	<AttachmentFragmentUploadDrawer
		v-model:visible="attachmentUploadDrawerVisible"
		v-model:parentId="attachmentCondition.parentId"
		@fileUploadDrawerCloes="onFileUploadDrawerClose"
	/>

	<AttachmentDeatilDrawer
		v-model:visible="attachmentDetailDrawerVisible"
		v-model:define-file="currentSelectionAttachment"
		@delete="fetchAttachments"
	/>

	<el-dialog
		v-model="dialogFolderVisible"
		title="新建目录"
		@open="onCreateFolderDialogOpen"
	>
		<el-input
			ref="createFolderInputRef"
			v-model="createFolderName"
			autocomplete="off"
			size="large"
			placeholder="请输入目录名称，提交后会在当前目录创造子目录。"
			@keydown.enter="onCreateFolderButtonClick"
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

	<AttachmentDirectorySelectDialog
		v-model:visible="directorySelectDialogVisible"
		@close-with-target-dir-id="onDirSelected"
	/>

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

			<el-button
				v-if="selectionAttachments && selectionAttachments.length > 0"
				:icon="Position"
				@click="directorySelectDialogVisible = true"
			>
				批量移动
			</el-button>

			<el-popconfirm
				v-if="selectionAttachments && selectionAttachments.length > 0"
				:title="'您确定批量删除附件吗？'"
				width="300"
				@confirm="onDeleteButtonClick"
			>
				<template #reference>
					<el-button :icon="FolderDelete" type="danger"> 批量删除 </el-button>
				</template>
			</el-popconfirm>
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
			<el-input
				v-model="attachmentCondition.name"
				placeholder="搜索当前目录下的所有附件，模糊匹配，空格多个关键词，回车搜查"
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

	<el-row v-if="attachmentCondition.total > 10 || attachmentCondition.page > 1">
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
						<el-breadcrumb-item v-for="path in paths" :key="path.id">
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
				@current-change="onCurrentChange"
				@row-dblclick="entryAttachment"
				@row-contextmenu="onRowContextmenu"
				@selection-change="onSelectionChange"
			>
				<el-table-column type="selection" width="60" />
				<!-- <el-table-column prop="id" label="ID" width="60" /> -->
				<el-table-column prop="name" label="名称" show-overflow-tooltip>
					<template #default="scoped">
						<el-icon
							size="25"
							style="position: relative; top: 7px; margin: 0 5px 0 0px"
						>
							<Folder v-if="'Directory' === scoped.row.type" />
							<span v-else>
								<Picture v-if="isImage(scoped.row.name)" />
								<Headset v-else-if="isVoice(scoped.row.name)" />
								<Film v-else-if="isVideo(scoped.row.name)" />
								<Document v-else />
							</span>
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
				<el-table-column prop="size" label="大小" width="130">
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
