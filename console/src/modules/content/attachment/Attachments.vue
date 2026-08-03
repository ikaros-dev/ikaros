<script setup lang="ts">
import { computed, ref, watch, onMounted, h, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import {
	Attachment,
	AttachmentTypeEnum,
	DirectoryBindingWorkflowEntity,
} from '@runikaros/api-client';
import { isImage, isVideo, isVoice } from '@/utils/file';
import moment from 'moment';
import { apiClient } from '@/utils/api-client';
import { usePluginModuleStore } from '@/stores/plugin';
import { PluginModule } from '@runikaros/shared';
import AttachmentFragmentUploadDrawer from './AttachmentFragmentUploadDrawer.vue';
import AttachmentDeatilDrawer from './AttachmentDeatilDrawer.vue';
import AttachmentDirectorySelectDialog from './AttachmentDirectorySelectDialog.vue';
import LocalDirectoryBindingDialog from './LocalDirectoryBindingDialog.vue';
import DialogMessage from '@/components/dialog/DialogMessage.vue';
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
	MostlyCloudy,
	Link,
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
	ElOption,
	ElSelect,
} from 'element-plus';
import router from '@/router';
import { getCompleteFileUrl } from '@/utils/url-tuils';
import { attachmentRootId } from '@/modules/common/constants';

const { t } = useI18n();

const systemInternalAttachmentIds = new Set([
	'019b715b-5cb5-7407-b571-6688c9e61e5a',
	'019b715b-97dc-72dd-9e5a-0f714efc89d9',
]);

const isSystemInternalAttachment = (attachment: Attachment) =>
	systemInternalAttachmentIds.has(attachment.id as string);
const route = useRoute();

const attachmentCondition = ref({
	page: 1,
	size: 10,
	total: 10,
	parentId: attachmentRootId,
	name: '',
	type: undefined,
});

const attachments = ref<Attachment[]>([]);
let attachmentRequestId = 0;

type AttachmentSortProperty = 'name' | 'updateTime' | 'size';
type AttachmentSortOrder = 'ascending' | 'descending';

const attachmentSortProperty = ref<AttachmentSortProperty>('updateTime');
const attachmentSortOrder = ref<AttachmentSortOrder>('descending');
const englishNameCollator = new Intl.Collator('en', {
	numeric: true,
	sensitivity: 'base',
});
const chineseNameCollator = new Intl.Collator('zh-CN', {
	numeric: true,
	sensitivity: 'base',
});

const isEnglishName = (name: string) => /^[A-Za-z]/.test(name);

const attachmentSortableColumns = computed<Record<AttachmentSortProperty, boolean>>(
	() => ({
		name: attachments.value.some((attachment) => Boolean(attachment.name)),
		updateTime: attachments.value.some((attachment) => Boolean(attachment.updateTime)),
		size: attachments.value.some(
			(attachment) =>
				attachment.type !== 'Directory' &&
				attachment.type !== 'Driver_Directory' &&
				attachment.size !== undefined &&
				attachment.size !== null
		),
	})
);

const canSortAttachmentColumn = (property: AttachmentSortProperty) =>
	attachmentSortableColumns.value[property];

const compareAttachmentName = (firstName: string, secondName: string) => {
	const firstIsEnglish = isEnglishName(firstName);
	const secondIsEnglish = isEnglishName(secondName);
	if (firstIsEnglish !== secondIsEnglish) {
		return firstIsEnglish ? -1 : 1;
	}
	return (firstIsEnglish ? englishNameCollator : chineseNameCollator).compare(
		firstName,
		secondName
	);
};

const sortedAttachments = computed(() =>
	!canSortAttachmentColumn(attachmentSortProperty.value)
		? attachments.value
		: attachments.value
		.map((attachment, index) => ({ attachment, index }))
		.sort((first, second) => {
			let comparison = 0;
			if (attachmentSortProperty.value === 'name') {
				comparison = compareAttachmentName(
					first.attachment.name || '',
					second.attachment.name || ''
				);
			} else if (attachmentSortProperty.value === 'updateTime') {
				comparison =
					new Date(first.attachment.updateTime || 0).getTime() -
					new Date(second.attachment.updateTime || 0).getTime();
			} else {
				comparison = Number(first.attachment.size || 0) - Number(second.attachment.size || 0);
			}
			if (comparison === 0) {
				return first.index - second.index;
			}
			return attachmentSortOrder.value === 'ascending' ? comparison : -comparison;
		})
		.map(({ attachment }) => attachment)
);

const toggleAttachmentSort = (property: AttachmentSortProperty) => {
	if (!canSortAttachmentColumn(property)) {
		return;
	}
	if (attachmentSortProperty.value === property) {
		attachmentSortOrder.value =
			attachmentSortOrder.value === 'ascending' ? 'descending' : 'ascending';
		return;
	}
	attachmentSortProperty.value = property;
	attachmentSortOrder.value = property === 'name' ? 'ascending' : 'descending';
};

const attachmentSortSymbol = (property: AttachmentSortProperty) => {
	if (
		attachmentSortProperty.value !== property ||
		!canSortAttachmentColumn(property)
	) {
		return '';
	}
	return attachmentSortOrder.value === 'descending' ? '▼' : '▲';
};

const applyAttachmentPage = async (data, requestId: number, parentId: string) => {
	if (
		requestId !== attachmentRequestId ||
		attachmentCondition.value.parentId !== parentId
	) {
		return;
	}
	attachments.value = data.items;
	attachmentCondition.value.page = data.page;
	attachmentCondition.value.size = data.size;
	attachmentCondition.value.total = data.total;
	await updateBreadcrumbByParentPath(requestId, parentId);
};

const fetchAttachments = async () => {
	const requestId = ++attachmentRequestId;
	const parentId = attachmentCondition.value.parentId as any as string;
	const { data } = await apiClient.attachment.listAttachmentsByCondition1({
		page: attachmentCondition.value.page,
		size: attachmentCondition.value.size,
		name: base64Encode(attachmentCondition.value.name),
		parentId,
	});
	await applyAttachmentPage(data, requestId, parentId);
};
const fetchDriverAttachments = async () => {
	const requestId = ++attachmentRequestId;
	const parentId = attachmentCondition.value.parentId as any as string;
	const { data } = await apiClient.attachmentDriver.listAttachmentsByCondition({
		page: attachmentCondition.value.page,
		size: attachmentCondition.value.size,
		name: base64Encode(attachmentCondition.value.name),
		parentId,
		refresh: true,
	});
	await applyAttachmentPage(data, requestId, parentId);
};

async function updateBreadcrumbByParentPath(
	requestId: number,
	parentId: string
) {
	const { data } = await apiClient.attachment.getAttachmentPathDirsById({
		id: parentId,
	});
	if (
		requestId !== attachmentRequestId ||
		attachmentCondition.value.parentId !== parentId
	) {
		return;
	}
	paths.value = data
		.filter(
			(att): att is Attachment & { id: string; parentId: string } =>
				Boolean(att.id && att.parentId)
		)
		.map((att) => {
			const path: Path = {
				name: att.name as string,
				id: att.id,
				parentId: att.parentId,
			};
			return path;
		});
}

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
	parentId: string;
	id: string;
}

const paths = ref<Path[]>([
	{
		name: '/',
		parentId: attachmentRootId,
		id: attachmentRootId,
	},
]);

const onBreadcrumbClick = (path) => {
	attachmentCondition.value.parentId = path.id;
};

const entryAttachment = (attachment) => {
	if (
		'Directory' === attachment.type ||
		'Driver_Directory' == attachment.type
	) {
		if (attachmentCondition.value.parentId === attachment.id) {
			return;
		}
		attachmentCondition.value.parentId = attachment.id;
	} else {
		currentSelectionAttachment.value = attachment;
		attachmentDetailDrawerVisible.value = true;
	}
};

const dateFormat = (row, column) => {
	const date = row[column.property];

	if (date == undefined) {
		return '';
	}

	return moment(date).format('YYYY-MM-DD HH:mm:ss');
};

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

const currentSelectionAttachment = ref<Attachment>({} as Attachment);
const onCurrentChange = (val: Attachment | undefined) => {
	if (val) {
		currentSelectionAttachment.value = val;
	}
};

const selectionAttachments = ref<Attachment[]>([]);

const hasSystemInternalAttachmentSelected = computed(() =>
	selectionAttachments.value.some(isSystemInternalAttachment)
);

const onSelectionChange = (selections) => {
	// console.log('selections', selections);
	selectionAttachments.value = selections;
};

const deleteAttachment = async (attachment: Attachment) => {
	const attachmentId = attachment.id;
	if (!attachmentId) return;
	await apiClient.attachment
		.deleteAttachment({
			id: attachmentId,
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
	if (
		hasSystemInternalAttachmentSelected.value ||
		!selectionAttachments.value ||
		selectionAttachments.value.length === 0
	) {
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

const isDirectory = (attachment: Attachment) =>
	attachment.type === AttachmentTypeEnum.Directory ||
	attachment.type === AttachmentTypeEnum.DriverDirectory;

const isTopLevelDirectory = (attachment: Attachment) =>
	isDirectory(attachment) && attachment.parentId === attachmentRootId;

const attachmentDetailDrawerVisible = ref(false);

const onRowContextmenu = (row, column, event) => {
	currentSelectionAttachment.value = row;
	const attachment = currentSelectionAttachment.value;
	const directory = isDirectory(attachment);
	const topLevelDirectory = isTopLevelDirectory(attachment);
	event.preventDefault();
	ContextMenu.showContextMenu({
		x: event.x,
		y: event.y,
		minWidth: 320,
		items: [
			{
				label:
					currentSelectionAttachment.value?.type === 'Directory' ||
					currentSelectionAttachment.value?.type == 'Driver_Directory'
						? t('module.attachment.contextmenu.entry')
						: t('module.attachment.contextmenu.details'),
				divided: 'down',
				icon: h(Pointer, { style: 'height: 14px' }),
				onClick: () => {
					entryAttachment(currentSelectionAttachment.value);
				},
			},
			...(topLevelDirectory
				? [
						{
							label: t('module.attachment.contextmenu.copy_name'),
							icon: h(CopyDocument, { style: 'height: 14px' }),
							onClick: async () => {
								const name = attachment.name as string;
								await copyValue(name);
								ElMessage.success(
									t('module.attachment.message.operate.copy_name', { name })
								);
							},
						},
					]
				: [
			{
				label: t('module.attachment.contextmenu.copy_short_name'),
				icon: h(CopyDocument, { style: 'height: 14px' }),
				onClick: async () => {
					const name = attachment.name as string;
					let simpleName = name;
					if (!directory) {
						simpleName = name.replace(/\[.*?\]/g, '');
						simpleName = simpleName.substring(0, simpleName.lastIndexOf('.'));
					}
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
					const name = attachment.name as string;
					const value = directory ? (attachment.path as string) : name;
					await copyValue(value);
					ElMessage.success(
						t(
							directory
								? 'module.attachment.message.operate.copy_path'
								: 'module.attachment.message.operate.copy_integrally_name',
							{ name }
						)
					);
				},
			},
				]),
			...(!directory
				? [
			{
				label: t('module.attachment.contextmenu.copy_url'),
				divided: 'down',
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
				]
				: []),
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
		].filter(
			(item) =>
				!isSystemInternalAttachment(currentSelectionAttachment.value) ||
				item.label !== t('module.attachment.contextmenu.delete.value')
		) as Parameters<typeof ContextMenu.showContextMenu>[0]['items'],
	});
};

const directorySelectDialogVisible = ref(false);
const onDirSelected = async (targetDirid: string) => {
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

const currentParentAttachment = ref<Attachment>({});
const fetchCurrentParentAttachment = async () => {
	if (!attachmentCondition.value.parentId) return;
	const attId = attachmentCondition.value.parentId;
	const { data } = await apiClient.attachment.getAttachmentById({ id: attId });
	currentParentAttachment.value = data;
};

const refreshButtonLoading = ref(false);
const refreshCurrentDir = async () => {
	try {
		refreshButtonLoading.value = true;
		await fetchCurrentParentAttachment();
		const type = currentParentAttachment.value.type;
		if (type && type === 'Driver_Directory') {
			await fetchDriverAttachments();
		} else {
			await fetchAttachments();
		}
	} catch (error) {
		console.error(error);
	} finally {
		refreshButtonLoading.value = false;
	}
};

const toAttachmentDrivers = () => {
	router.push('/attachment/drivers');
};

const bindDialogVisible = ref(false);
const localBindingDialogVisible = ref(false);
const localBindingWorkflow = ref<DirectoryBindingWorkflowEntity>();
const bindPlatform = ref('');
const bindPlatformId = ref('');
const bindSearchKeyword = ref('');
const bindPlatformArr = ref<string[]>([]);

const { pluginModules } = usePluginModuleStore();
const bindPlatformOptions = computed(() => [
	{
		value: 'local',
		label: t('module.attachment.bind.local.option'),
	},
	...bindPlatformArr.value.map((platform) => ({
		value: platform,
		label: platform,
	})),
]);
const currentLocalBindingWorkflow = computed(() =>
	localBindingWorkflow.value?.directoryId === attachmentCondition.value.parentId
		? localBindingWorkflow.value
		: undefined
);

onMounted(() => {
	pluginModules.forEach((pluginModule: PluginModule) => {
		const { extensionPoints } = pluginModule;
		if (!extensionPoints?.['subject:sync:platform']) {
			return;
		}
		const subjectPlatform = extensionPoints[
			'subject:sync:platform'
		] as unknown as string;
		if (subjectPlatform) {
			bindPlatformArr.value.push(subjectPlatform);
		}
	});
});

const onBindDirectoryClick = async () => {
	if (bindPlatformArr.value.length === 0) {
		bindPlatform.value = 'local';
	} else if (bindPlatformArr.value.length == 1) {
		bindPlatform.value = bindPlatformArr.value[0];
	} else {
		bindPlatform.value = '';
	}
	bindPlatformId.value = '';
	bindSearchKeyword.value = '';
	bindDialogVisible.value = true;
};

const onBindPlatformChange = (platform: string) => {
	if (platform === 'local') {
		bindDialogVisible.value = false;
		localBindingDialogVisible.value = true;
	}
};

const onBindDirectoryConfirm = async () => {
	if (bindPlatform.value === 'local') {
		bindDialogVisible.value = false;
		localBindingDialogVisible.value = true;
		return;
	}

	try {
		await ElMessageBox.confirm(
			h(DialogMessage, {
				message: t('module.attachment.bind.confirm.content'),
			}),
			t('module.attachment.bind.confirm.title'),
			{
				confirmButtonText: t('module.attachment.bind.confirm.btn.confirm'),
				cancelButtonText: t('module.attachment.bind.confirm.btn.cancel'),
				type: 'info',
			}
		);
		await apiClient.binding.bindDirectory({
			directoryId: attachmentCondition.value.parentId,
			platform: bindPlatform.value as
				| 'BGM_TV'
				| 'TMDB'
				| 'AniDB'
				| 'TVDB'
				| 'VNDB'
				| 'DOU_BAN'
				| 'OTHER',
			platformId: bindPlatformId.value || undefined,
			keyword: bindSearchKeyword.value || undefined,
		});
		ElMessage.success(t('module.attachment.bind.success'));
		bindDialogVisible.value = false;
	} catch (e) {
		if (e === 'cancel' || e === 'close') {
			return;
		}
		console.error('bind directory error', e);
		ElMessage.error(t('module.attachment.bind.error'));
	}
};

const onLocalBindingConfirmed = (workflow: DirectoryBindingWorkflowEntity) => {
	localBindingWorkflow.value = workflow;
	ElMessage.success(
		t('module.attachment.bind.local.success', {
			workflowId: workflow.id || '-',
			taskId: workflow.taskId || '-',
		})
	);
};

const onLocalBindingRescanned = (workflow: DirectoryBindingWorkflowEntity) => {
	localBindingWorkflow.value = workflow;
};

const onLocalRescanClick = () => {
	localBindingDialogVisible.value = true;
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
				attachmentCondition.value.parentId = newValue.parentId as string;
				fetchCurrentParentAttachment();
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
	if (parentId !== (route.query.parentId as string)) {
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

	<LocalDirectoryBindingDialog
		v-model:visible="localBindingDialogVisible"
		:directory-id="attachmentCondition.parentId as string"
		:workflow="currentLocalBindingWorkflow"
		@confirmed="onLocalBindingConfirmed"
		@rescanned="onLocalBindingRescanned"
	/>

	<el-dialog
		v-model="bindDialogVisible"
		:title="t('module.attachment.bind.confirm.title')"
		width="400px"
	>
		<el-form>
			<el-form-item
				:label="t('module.attachment.bind.platform.title')"
			>
				<el-select v-model="bindPlatform" @change="onBindPlatformChange">
					<el-option
						v-for="platform in bindPlatformOptions"
						:key="platform.value"
						:label="platform.label"
						:value="platform.value"
					/>
				</el-select>
			</el-form-item>
			<el-form-item
				v-if="bindPlatform !== 'local'"
				:label="t('module.attachment.bind.platformId.label')"
			>
				<el-input
					v-model="bindPlatformId"
					:placeholder="t('module.attachment.bind.platformId.placeholder')"
				/>
			</el-form-item>
			<el-form-item
				v-if="bindPlatform !== 'local'"
				:label="t('module.attachment.bind.keyword.label')"
			>
				<el-input
					v-model="bindSearchKeyword"
					:placeholder="t('module.attachment.bind.keyword.placeholder')"
				/>
			</el-form-item>
		</el-form>
		<template #footer>
			<span>
				<el-button @click="bindDialogVisible = false">
					{{ t('module.attachment.bind.confirm.btn.cancel') }}
				</el-button>
				<el-button
					type="primary"
					:disabled="!bindPlatform"
					@click="onBindDirectoryConfirm"
				>
					{{ t('module.attachment.bind.confirm.btn.confirm') }}
				</el-button>
			</span>
		</template>
	</el-dialog>

	<div class="attachment-toolbar">
		<div class="attachment-toolbar-actions">
			<el-button
				plain
				:disabled="
					currentParentAttachment.type &&
					currentParentAttachment.type === 'Driver_Directory'
				"
				@click="attachmentUploadDrawerVisible = true"
			>
				<el-icon>
					<Upload />
				</el-icon>
				{{ t('module.attachment.btn.upload') }}
			</el-button>
			<el-button :icon="FolderAdd" @click="dialogFolderVisible = true">
				{{ t('module.attachment.btn.mkdir') }}
			</el-button>
			<el-button
				:icon="Refresh"
				:loading="refreshButtonLoading"
				@click="refreshCurrentDir"
			>
				{{ t('module.attachment.btn.refresh') }}
			</el-button>
			<el-button :icon="MostlyCloudy" @click="toAttachmentDrivers">
				{{ t('module.attachment.btn.driver') }}
			</el-button>
			<el-button :icon="Link" @click="onBindDirectoryClick">
				{{ t('module.attachment.btn.bind') }}
			</el-button>
			<el-button
				v-if="currentLocalBindingWorkflow"
				:icon="Refresh"
				@click="onLocalRescanClick"
			>
				{{ t('module.attachment.bind.local.rescan.entry') }}
			</el-button>
			<el-button
				v-if="selectionAttachments && selectionAttachments.length > 0"
				:icon="Position"
				@click="directorySelectDialogVisible = true"
			>
				{{ t('module.attachment.btn.move_atts') }}
			</el-button>

			<el-tooltip
				v-if="selectionAttachments && selectionAttachments.length > 0"
				:disabled="!hasSystemInternalAttachmentSelected"
				:content="t('module.attachment.popconfirm.system_internal_forbidden')"
				placement="top"
			>
				<span class="batch-delete-button-wrapper">
					<el-popconfirm
						:title="t('module.attachment.popconfirm.title')"
						:disabled="hasSystemInternalAttachmentSelected"
						width="300"
						@confirm="onDeleteButtonClick"
					>
						<template #reference>
							<el-button
								:icon="FolderDelete"
								:disabled="hasSystemInternalAttachmentSelected"
								:type="hasSystemInternalAttachmentSelected ? '' : 'danger'"
							>
								{{ t('module.attachment.popconfirm.btn') }}
							</el-button>
						</template>
					</el-popconfirm>
				</span>
			</el-tooltip>
		</div>
		<el-input
			v-model="attachmentCondition.name"
			class="attachment-search-input"
			:placeholder="t('module.attachment.search_input.placeholder')"
			clearable
			@change="fetchAttachments"
		>
			<template #append>
				<el-button :icon="Search" @click="fetchAttachments" />
			</template>
		</el-input>
	</div>

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
				:data="sortedAttachments"
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
					show-overflow-tooltip
				>
					<template #header>
						<button
							type="button"
							class="attachment-sort-header"
							:disabled="!canSortAttachmentColumn('name')"
							@click.stop="toggleAttachmentSort('name')"
						>
							{{ t('module.attachment.table.colum.label.name') }}
							<span class="attachment-sort-symbol">{{ attachmentSortSymbol('name') }}</span>
						</button>
					</template>
					<template #default="scoped">
						<el-icon
							size="25"
							style="position: relative; top: 7px; margin: 0 5px 0 0px"
						>
							<Folder
								v-if="
									'Directory' === scoped.row.type ||
									'Driver_Directory' === scoped.row.type
								"
								:color="
									scoped.row.type === 'Driver_Directory' ? 'skyblue' : 'default'
								"
							/>
							<span v-else>
								<Picture
									v-if="isImage(scoped.row.name)"
									:color="
										scoped.row.type === 'Driver_File' ? 'skyblue' : 'default'
									"
								/>
								<Headset
									v-else-if="isVoice(scoped.row.name)"
									:color="
										scoped.row.type === 'Driver_File' ? 'skyblue' : 'default'
									"
								/>
								<Film
									v-else-if="isVideo(scoped.row.name)"
									:color="
										scoped.row.type === 'Driver_File' ? 'skyblue' : 'default'
									"
								/>
								<Document
									v-else
									:color="
										scoped.row.type === 'Driver_File' ? 'skyblue' : 'default'
									"
								/>
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
					width="160"
					:formatter="dateFormat"
				>
					<template #header>
						<button
							type="button"
							class="attachment-sort-header"
							:disabled="!canSortAttachmentColumn('updateTime')"
							@click.stop="toggleAttachmentSort('updateTime')"
						>
							{{ t('module.attachment.table.colum.label.update_time') }}
							<span class="attachment-sort-symbol">{{ attachmentSortSymbol('updateTime') }}</span>
						</button>
					</template>
				</el-table-column>
				<el-table-column
					prop="size"
					width="130"
				>
					<template #header>
						<button
							type="button"
							class="attachment-sort-header"
							:disabled="!canSortAttachmentColumn('size')"
							@click.stop="toggleAttachmentSort('size')"
						>
							{{ t('module.attachment.table.colum.label.size') }}
							<span class="attachment-sort-symbol">{{ attachmentSortSymbol('size') }}</span>
						</button>
					</template>
					<template #default="scoped">
						<span
							v-if="
								scoped.row.type !== 'Directory' &&
								scoped.row.type !== 'Driver_Directory'
							"
						>
							{{ formatFileSize(scoped.row.size) }}
						</span>
					</template>
				</el-table-column>
			</el-table>
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped>
.attachment-toolbar {
	display: flex;
	flex-wrap: nowrap;
	gap: 12px;
}

.attachment-toolbar-actions {
	display: inline-flex;
	flex-shrink: 0;
}

.attachment-search-input {
	flex: 1 1 auto;
	min-width: 0;
}

.batch-delete-button-wrapper {
	display: inline-flex;
	margin-left: 12px;
}

.ik-attachment-breadcrumb-item {
	width: 20px;
	cursor: pointer;
}

.attachment-sort-header {
	padding: 0;
	border: 0;
	color: inherit;
	font: inherit;
	background: transparent;
	cursor: pointer;

	&:disabled {
		cursor: default;
	}
}

.attachment-sort-symbol {
	margin-left: 0.25em;
	font-size: 0.875em;
}
</style>
