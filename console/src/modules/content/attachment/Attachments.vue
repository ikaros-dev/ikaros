<script setup lang="ts">
import { ref, watch, onMounted, h, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import { Attachment, AttachmentTypeEnum } from '@runikaros/api-client';
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
	Refresh,
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
import { getCompleteFileUrl } from '@/utils/url-tuils';

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
const fetchDriverAttachments = async () => {
	const { data } = await apiClient.attachmentDriver.listAttachmentsByCondition1({
		page: attachmentCondition.value.page,
		size: attachmentCondition.value.size,
		name: base64Encode(attachmentCondition.value.name),
		parentId: attachmentCondition.value.parentId as any as string,
		refresh: true,
	});
	attachments.value = data.items;
	attachmentCondition.value.page = data.page;
	attachmentCondition.value.size = data.size;
	attachmentCondition.value.total = data.total;
	await updateBreadcrumbByParentPath();
}

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
	} else if (attachment.type === 'Driver_Directory') {
		attachmentCondition.value.parentId = attachment.id;
		paths.value.push({
			name: attachment.name,
			parentId: attachment.parentId,
			id: attachment.id,
		});
		await fetchDriverAttachments();
	}
	 else {
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
	ElMessage.success(
		t('module.attachment.message.operate.create_att_dir', {
			name: createFolderName.value,
		})
	);
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
				t('module.attachment.message.operate.delete_att.success', {
					type:
						attachment.type === 'Directory'
							? t('module.attachment.message.directory')
							: t('module.attachment.message.file'),
					name: attachment.name,
				})
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
				t('module.attachment.message.operate.delete_att.fail', {
					type:
						attachment.type === 'Directory'
							? t('module.attachment.message.directory')
							: t('module.attachment.message.file'),
					name: attachment.name,
				})
			);
		});

	await fetchAttachments();
};

const deleteAttachments = async () => {
	currentSelectionAttachment.value?.type === 'Directory';
	await selectionAttachments.value.forEach(async (a) => {
		await deleteAttachment(a);
	});
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
			t('module.attachment.confirm.content'),
			t('module.attachment.confirm.warning'),
			{
				confirmButtonText: t('module.attachment.confirm.btn.confirm'),
				cancelButtonText: t('module.attachment.confirm.btn.cancel'),
				type: 'warning',
			}
		)
			.then(async () => {
				await deleteAttachments();
			})
			.catch(() => {
				ElMessage({
					type: 'info',
					message: t('module.attachment.message.operate.delete_atts.cancel'),
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
					(currentSelectionAttachment.value?.type === 'Directory' || currentSelectionAttachment.value?.type == 'Driver_Directory')
						? t('module.attachment.contextmenu.entry')
						: t('module.attachment.contextmenu.details'),
				divided: 'down',
				icon: h(Pointer, { style: 'height: 14px' }),
				onClick: () => {
					entryAttachment(currentSelectionAttachment.value);
				},
			},
			{
				label: t('module.attachment.contextmenu.copy_short_name'),
				icon: h(CopyDocument, { style: 'height: 14px' }),
				onClick: async () => {
					const name = currentSelectionAttachment.value?.name as string;
					var simpleName = name.replace(/\[.*?\]/g, '');
					simpleName = simpleName.substring(0, simpleName.lastIndexOf('.'));
					await copyValue(simpleName);
					ElMessage.success(
						t('module.attachment.message.operate.copy_short_name', {
							name: name,
						})
					);
				},
			},
			{
				label: t('module.attachment.contextmenu.copy_integrally_name'),
				icon: h(CopyDocument, { style: 'height: 14px' }),
				onClick: async () => {
					const name = currentSelectionAttachment.value?.name as string;
					await copyValue(name);
					ElMessage.success(
						t('module.attachment.message.operate.copy_integrally_name', {
							name: name,
						})
					);
				},
			},
			{
				label: t('module.attachment.contextmenu.copy_url'),
				divided: 'down',
				disabled: (currentSelectionAttachment.value?.type !== AttachmentTypeEnum.File) && (AttachmentTypeEnum.DriverFile !== currentSelectionAttachment.value?.type),
				icon: h(CopyDocument, { style: 'height: 14px' }),
				onClick: async () => {
					const name = currentSelectionAttachment.value?.name as string;
					const url = currentSelectionAttachment.value?.url as string;
					await copyValue(encodeURI(getCompleteFileUrl(url)));
					ElMessage.success(
						t('module.attachment.message.operate.copy_url', { name: name })
					);
				},
			},
			{
				label: t('module.attachment.contextmenu.download'),
				disabled: currentSelectionAttachment.value?.type !== 'File',
				icon: h(Download, { style: 'height: 14px' }),
				onClick: async () => {
					const url = currentSelectionAttachment.value?.url as string;
					window.open(url);
				},
			},
			{
				label: t('module.attachment.contextmenu.delete.value'),
				icon: h(Delete, { style: 'height: 14px; color: red;' }),
				onClick: async () => {
					if (currentSelectionAttachment.value?.type === 'Directory') {
						await ElMessageBox.confirm(
							t('module.attachment.contextmenu.delete.confirm', {
								name: currentSelectionAttachment.value.name,
							}),
							t('module.attachment.confirm.warning'),
							{
								confirmButtonText: t('module.attachment.confirm.btn.confirm'),
								cancelButtonText: t('module.attachment.confirm.btn.cancel'),
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
									message: t(
										'module.attachment.message.operate.delete_atts.cancel'
									),
								});
							});
					} else {
						await ElMessageBox.confirm(
							t('module.attachment.contextmenu.delete.confirm', {
								name: currentSelectionAttachment.value?.name,
							}),
							t('module.attachment.confirm.warning'),
							{
								confirmButtonText: t('module.attachment.confirm.btn.confirm'),
								cancelButtonText: t('module.attachment.confirm.btn.cancel'),
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
									message: t(
										'module.attachment.message.operate.delete_att.cancel',
										{ name: currentSelectionAttachment.value?.name }
									),
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
	await ElMessage.success(t('module.attachment.message.operate.move_atts'));
	await fetchAttachments();
};

const currentParentAttachment = ref<Attachment>({})
const fetchCurrentParentAttachment = async () => {
	if (!(attachmentCondition.value.parentId)) return
	var attId = attachmentCondition.value.parentId
	const {data} = await apiClient.attachment.getAttachmentById({id: attId})
	currentParentAttachment.value = data
}

const refreshButtonLoading = ref(false)
const refreshCurrentDir = async () => {
	try {
		refreshButtonLoading.value = true
		await fetchCurrentParentAttachment()
		var type = currentParentAttachment.value.type
		if (type && type === 'Driver_Directory') {
			await fetchDriverAttachments()
		} else {
			await fetchAttachments()
		}
	} catch (error) {
		console.error(error)
	} finally {
		refreshButtonLoading.value = false
	}
	
}

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
				fetchCurrentParentAttachment()
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
const onAttachmentDetailDrawerClose = () => {
	window.location.reload();
};
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
		@close="onAttachmentDetailDrawerClose"
	/>

	<el-dialog
		v-model="dialogFolderVisible"
		:title="t('module.attachment.dialog.mkdir.title')"
		@open="onCreateFolderDialogOpen"
	>
		<el-input
			ref="createFolderInputRef"
			v-model="createFolderName"
			autocomplete="off"
			size="large"
			:placeholder="t('module.attachment.dialog.mkdir.placeholder')"
			@keydown.enter="onCreateFolderButtonClick"
		/>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="dialogFolderVisible = false">
					{{ t('module.attachment.dialog.mkdir.btn.cancel') }}
				</el-button>
				<el-button type="primary" @click="onCreateFolderButtonClick">
					{{ t('module.attachment.dialog.mkdir.btn.confirm') }}
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
				{{ t('module.attachment.btn.upload') }}
			</el-button>
			<el-button :icon="FolderAdd" @click="dialogFolderVisible = true">
				{{ t('module.attachment.btn.mkdir') }}
			</el-button>
			<el-button :icon="Refresh"  :loading="refreshButtonLoading" @click="refreshCurrentDir">
				{{ t('module.attachment.btn.refresh') }}
			</el-button>

			<el-button
				v-if="selectionAttachments && selectionAttachments.length > 0"
				:icon="Position"
				@click="directorySelectDialogVisible = true"
			>
				{{ t('module.attachment.btn.move_atts') }}
			</el-button>

			<el-popconfirm
				v-if="selectionAttachments && selectionAttachments.length > 0"
				:title="t('module.attachment.popconfirm.title')"
				width="300"
				@confirm="onDeleteButtonClick"
			>
				<template #reference>
					<el-button :icon="FolderDelete" type="danger">
						{{ t('module.attachment.popconfirm.btn') }}
					</el-button>
				</template>
			</el-popconfirm>
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
			<el-input
				v-model="attachmentCondition.name"
				:placeholder="t('module.attachment.search_input.placeholder')"
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
				<el-form-item
					:label="t('module.attachment.breadcrumb.label')"
					style="width: 100%"
				>
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
				<el-table-column
					prop="name"
					:label="t('module.attachment.table.colum.label.name')"
					show-overflow-tooltip
				>
					<template #default="scoped">
						<el-icon
							size="25"
							style="position: relative; top: 7px; margin: 0 5px 0 0px"
						>
							<Folder v-if="('Directory' === scoped.row.type) || ('Driver_Directory' === scoped.row.type)" :color="scoped.row.type === 'Driver_Directory' ? 'skyblue': 'default'"/>
							<span v-else>
								<Picture v-if="isImage(scoped.row.name)" :color="scoped.row.type === 'Driver_File' ? 'skyblue': 'default'" />
								<Headset v-else-if="isVoice(scoped.row.name)" :color="scoped.row.type === 'Driver_File' ? 'skyblue': 'default'" />
								<Film v-else-if="isVideo(scoped.row.name)" :color="scoped.row.type === 'Driver_File' ? 'skyblue': 'default'" />
								<Document v-else  :color="scoped.row.type === 'Driver_File' ? 'skyblue': 'default'" />
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
					:label="t('module.attachment.table.colum.label.update_time')"
					width="160"
					:formatter="dateFormat"
				/>
				<el-table-column
					prop="size"
					:label="t('module.attachment.table.colum.label.size')"
					width="130"
				>
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
</style>
