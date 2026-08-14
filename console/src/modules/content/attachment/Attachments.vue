<script setup lang="ts">
import { computed, ref, watch, onMounted, h, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import {
	Attachment,
	AttachmentDriver,
	AttachmentDriverTypeEnum,
	AttachmentTypeEnum,
	DirectoryBindingWorkflowEntity,
} from '@runikaros/api-client';
import moment from 'moment';
import { apiClient } from '@/utils/api-client';
import {
	loadMediaFileFormatLookup,
	type MediaFileFormatLookup,
} from '@/utils/media-file-format';
import ContentBrowser from '@/components/modules/content/ContentBrowser.vue';
import RefreshButton from '@/components/common/RefreshButton.vue';
import AttachmentFragmentUploadDrawer from './AttachmentFragmentUploadDrawer.vue';
import AttachmentDeatilDrawer from './AttachmentDeatilDrawer.vue';
import AttachmentDirectorySelectDialog from './AttachmentDirectorySelectDialog.vue';
import FileSourceManagerDialog from './FileSourceManagerDialog.vue';
import LocalDirectoryBindingDialog from './LocalDirectoryBindingDialog.vue';
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
	Search,
	Setting,
} from '@element-plus/icons-vue';
import {
	ElInput,
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
	ElAlert,
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
const attachmentsLoading = ref(false);
const attachmentsError = ref<string | null>(null);
const fileSourceManagerVisible = ref(false);
const fileSourcesLoaded = ref(false);
const fileSources = ref<AttachmentDriver[]>([]);
const mediaFileFormatLookup = ref<MediaFileFormatLookup>();
const mediaFileCategory = (fileName?: string) =>
	fileName ? mediaFileFormatLookup.value?.categoryOf(fileName) : undefined;
let attachmentRequestId = 0;

type AttachmentSortProperty = 'name' | 'updateTime' | 'size';
const englishNameCollator = new Intl.Collator('en', {
	numeric: true,
	sensitivity: 'base',
});
const chineseNameCollator = new Intl.Collator('zh-CN', {
	numeric: true,
	sensitivity: 'base',
});

const isEnglishName = (name: string) => /^[A-Za-z]/.test(name);

const attachmentSortableColumns = computed<
	Record<AttachmentSortProperty, boolean>
>(() => ({
	name: attachments.value.some((attachment) => Boolean(attachment.name)),
	updateTime: attachments.value.some((attachment) =>
		Boolean(attachment.updateTime)
	),
	size: attachments.value.some(
		(attachment) =>
			attachment.type !== 'Directory' &&
			attachment.type !== 'Driver_Directory' &&
			attachment.size !== undefined &&
			attachment.size !== null
	),
}));

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

const compareAttachmentByName = (first: Attachment, second: Attachment) =>
	compareAttachmentName(first.name || '', second.name || '');

const compareAttachmentByUpdateTime = (first: Attachment, second: Attachment) =>
	new Date(first.updateTime || 0).getTime() -
	new Date(second.updateTime || 0).getTime();

const compareAttachmentBySize = (first: Attachment, second: Attachment) =>
	Number(first.size || 0) - Number(second.size || 0);

const applyAttachmentPage = async (
	data,
	requestId: number,
	parentId: string
) => {
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
	attachmentsLoading.value = true;
	attachmentsError.value = null;
	try {
		const { data } = await apiClient.attachment.listAttachmentsByCondition1({
			page: attachmentCondition.value.page,
			size: attachmentCondition.value.size,
			name: base64Encode(attachmentCondition.value.name),
			parentId,
		});
		await applyAttachmentPage(data, requestId, parentId);
	} catch (error) {
		if (requestId === attachmentRequestId) {
			attachmentsError.value =
				(error as Error)?.message || t('module.attachment.browser.error');
		}
	} finally {
		if (requestId === attachmentRequestId) {
			attachmentsLoading.value = false;
		}
	}
};
const fetchDriverAttachments = async () => {
	const requestId = ++attachmentRequestId;
	const parentId = attachmentCondition.value.parentId as any as string;
	attachmentsLoading.value = true;
	attachmentsError.value = null;
	try {
		const { data } =
			await apiClient.attachmentDriver.listAttachmentsByCondition({
				page: attachmentCondition.value.page,
				size: attachmentCondition.value.size,
				name: base64Encode(attachmentCondition.value.name),
				parentId,
				refresh: true,
			});
		await applyAttachmentPage(data, requestId, parentId);
	} catch (error) {
		if (requestId === attachmentRequestId) {
			attachmentsError.value =
				(error as Error)?.message || t('module.attachment.browser.error');
		}
	} finally {
		if (requestId === attachmentRequestId) {
			attachmentsLoading.value = false;
		}
	}
};

const fetchFileSources = async () => {
	try {
		const { data } = await apiClient.attachmentDriver.listDriversByCondition({
			page: 1,
			size: 1000,
		});
		fileSources.value = (data.items || []) as AttachmentDriver[];
	} catch {
		attachmentsError.value = t('module.attachment.browser.source-error');
	} finally {
		fileSourcesLoaded.value = true;
	}
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
		.filter((att): att is Attachment & { id: string; parentId: string } =>
			Boolean(att.id && att.parentId)
		)
		.map((att) => {
			const isSystemRoot = att.id === attachmentRootId;
			const isFileSourceRoot =
				att.type === AttachmentTypeEnum.DriverDirectory &&
				att.parentId === attachmentRootId;
			const path: Path = {
				name: isSystemRoot
					? '根目录'
					: isFileSourceRoot
						? `${(att.driverId || '').slice(0, 8)} 根目录`
						: (att.name as string),
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

const searchCurrentDirectory = async () => {
	attachmentCondition.value.page = 1;
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
		name: '根目录',
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
const hasReadOnlyAttachmentSelected = computed(() =>
	selectionAttachments.value.some(
		(attachment) =>
			attachment.type === AttachmentTypeEnum.DriverDirectory ||
			attachment.type === AttachmentTypeEnum.DriverFile
	)
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
		hasReadOnlyAttachmentSelected.value ||
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
									simpleName = simpleName.substring(
										0,
										simpleName.lastIndexOf('.')
									);
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
									t('module.attachment.message.operate.copy_url', {
										name: name,
									})
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
				(!isSystemInternalAttachment(currentSelectionAttachment.value) &&
					currentSelectionAttachment.value.type !==
						AttachmentTypeEnum.DriverDirectory &&
					currentSelectionAttachment.value.type !==
						AttachmentTypeEnum.DriverFile) ||
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

const isRootDirectory = computed(
	() => attachmentCondition.value.parentId === attachmentRootId
);
const isDriverDirectory = computed(
	() =>
		currentParentAttachment.value.type === AttachmentTypeEnum.DriverDirectory
);
const canWriteCurrentDirectory = computed(() => !isDriverDirectory.value);
const canUploadCurrentDirectory = computed(
	() => canWriteCurrentDirectory.value && !isRootDirectory.value
);
const canScanCurrentDirectory = computed(
	() =>
		isDriverDirectory.value &&
		fileSources.value.some(
			(driver) =>
				driver.id === currentParentAttachment.value.driverId &&
				driver.type === AttachmentDriverTypeEnum.Local
		)
);
const hasFileSources = computed(() => fileSources.value.length > 0);
const browserTotal = computed(() =>
	fileSourcesLoaded.value &&
	isRootDirectory.value &&
	!attachmentCondition.value.name &&
	!hasFileSources.value
		? 0
		: attachmentCondition.value.total
);
const browserEmptyTitle = computed(() => {
	if (isRootDirectory.value && !hasFileSources.value) {
		return t('module.attachment.browser.no-sources.title');
	}
	if (attachmentCondition.value.name) {
		return t('module.attachment.browser.no-results.title');
	}
	return t('module.attachment.browser.empty.title');
});
const browserEmptyDescription = computed(() => {
	if (isRootDirectory.value && !hasFileSources.value) {
		return t('module.attachment.browser.no-sources.description');
	}
	if (attachmentCondition.value.name) {
		return t('module.attachment.browser.no-results.description');
	}
	return isDriverDirectory.value
		? t('module.attachment.browser.empty.driver-description')
		: t('module.attachment.browser.empty.directory-description');
});

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
		attachmentsError.value =
			(error as Error)?.message || t('module.attachment.browser.error');
	} finally {
		refreshButtonLoading.value = false;
	}
};

const onFileSourcesChanged = async () => {
	await fetchFileSources();
	attachmentCondition.value.parentId = attachmentRootId;
	attachmentCondition.value.page = 1;
	await fetchCurrentParentAttachment();
	await fetchAttachments();
};

const localBindingDialogVisible = ref(false);
const localBindingWorkflow = ref<DirectoryBindingWorkflowEntity>();
const currentLocalBindingWorkflow = computed(() =>
	localBindingWorkflow.value?.directoryId === attachmentCondition.value.parentId
		? localBindingWorkflow.value
		: undefined
);

onMounted(() => {
	loadMediaFileFormatLookup()
		.then((lookup) => {
			mediaFileFormatLookup.value = lookup;
		})
		.catch(() => {
			mediaFileFormatLookup.value = undefined;
		});
	fetchFileSources();
});

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

const openScanDialog = () => {
	if (!canScanCurrentDirectory.value) {
		return;
	}
	localBindingDialogVisible.value = true;
};

watch(
	() => route.query,
	async (newValue) => {
		// console.log(newValue);
		if (newValue) {
			attachmentCondition.value.name = decodeURI(
				base64Decode(newValue.name as string)
			);
			if (newValue.parentId) {
				attachmentCondition.value.parentId = newValue.parentId as string;
			}
			try {
				await fetchCurrentParentAttachment();
				await fetchAttachments();
			} catch (error) {
				attachmentsError.value =
					(error as Error)?.message || t('module.attachment.browser.error');
			}
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

	<FileSourceManagerDialog
		v-model:visible="fileSourceManagerVisible"
		@changed="onFileSourcesChanged"
	/>

	<ContentBrowser
		:title="t('module.attachment.title')"
		:search-model-value="attachmentCondition.name"
		:search-placeholder="t('module.attachment.search_input.placeholder')"
		:loading="attachmentsLoading"
		:error="attachmentsError"
		:empty-title="browserEmptyTitle"
		:empty-description="browserEmptyDescription"
		:page="attachmentCondition.page"
		:size="attachmentCondition.size"
		:total="browserTotal"
		:page-sizes="[10, 20, 50, 100]"
		@update:search-model-value="attachmentCondition.name = $event"
		@search="searchCurrentDirectory"
		@retry="refreshCurrentDir"
		@update:page="onCurrentPageChange"
		@update:size="onSizeChange"
	>
		<template #actions>
			<el-button
				v-if="canUploadCurrentDirectory"
				type="primary"
				:icon="Upload"
				@click="attachmentUploadDrawerVisible = true"
			>
				{{ t('module.attachment.btn.upload') }}
			</el-button>
			<el-button
				v-if="canWriteCurrentDirectory"
				:icon="FolderAdd"
				@click="dialogFolderVisible = true"
			>
				{{ t('module.attachment.btn.mkdir') }}
			</el-button>
			<el-button :icon="Setting" @click="fileSourceManagerVisible = true">
				{{ t('module.attachment.btn.manage-sources') }}
			</el-button>
			<el-button
				v-if="canScanCurrentDirectory"
				:icon="Search"
				@click="openScanDialog"
			>
				{{ t('module.attachment.btn.scan') }}
			</el-button>
			<RefreshButton :loading="refreshButtonLoading" @click="refreshCurrentDir">
				{{ t('module.attachment.btn.refresh') }}
			</RefreshButton>
			<el-button
				v-if="
					selectionAttachments.length > 0 &&
					canWriteCurrentDirectory &&
					!hasReadOnlyAttachmentSelected
				"
				:icon="Position"
				@click="directorySelectDialogVisible = true"
			>
				{{ t('module.attachment.btn.move_atts') }}
			</el-button>
			<el-tooltip
				v-if="
					selectionAttachments.length > 0 &&
					canWriteCurrentDirectory &&
					!hasReadOnlyAttachmentSelected
				"
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
		</template>

		<template #breadcrumb>
			<div class="attachment-navigation">
				<div class="attachment-breadcrumb">
					<span>{{ t('module.attachment.breadcrumb.label') }}</span>
					<el-breadcrumb separator=">">
						<el-breadcrumb-item v-for="path in paths" :key="path.id">
							<el-button link @click="onBreadcrumbClick(path)">
								{{ path.name }}
							</el-button>
						</el-breadcrumb-item>
					</el-breadcrumb>
				</div>
				<el-alert
					:title="t('module.attachment.browser.refresh-hint')"
					type="info"
					show-icon
					:closable="false"
					class="attachment-refresh-hint"
				/>
			</div>
		</template>

		<template #empty-actions>
			<el-button
				v-if="isRootDirectory && !hasFileSources"
				type="primary"
				:icon="Setting"
				@click="fileSourceManagerVisible = true"
			>
				{{ t('module.attachment.btn.add-source') }}
			</el-button>
			<template v-else-if="isDriverDirectory">
				<RefreshButton @click="refreshCurrentDir">
					{{ t('module.attachment.btn.refresh') }}
				</RefreshButton>
				<el-button
					v-if="canScanCurrentDirectory"
					type="primary"
					:icon="Search"
					@click="openScanDialog"
				>
					{{ t('module.attachment.btn.scan') }}
				</el-button>
			</template>
			<template v-else>
				<el-button
					v-if="canUploadCurrentDirectory"
					type="primary"
					:icon="Upload"
					@click="attachmentUploadDrawerVisible = true"
				>
					{{ t('module.attachment.btn.upload') }}
				</el-button>
				<el-button
					v-if="canWriteCurrentDirectory"
					:icon="FolderAdd"
					@click="dialogFolderVisible = true"
				>
					{{ t('module.attachment.btn.mkdir') }}
				</el-button>
			</template>
		</template>

		<el-table
			:data="attachments"
			:default-sort="{ prop: 'updateTime', order: 'descending' }"
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
				:sortable="canSortAttachmentColumn('name')"
				:sort-method="compareAttachmentByName"
			>
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
								v-if="mediaFileCategory(scoped.row.name) === 'IMAGE'"
								:color="
									scoped.row.type === 'Driver_File' ? 'skyblue' : 'default'
								"
							/>
							<Headset
								v-else-if="mediaFileCategory(scoped.row.name) === 'AUDIO'"
								:color="
									scoped.row.type === 'Driver_File' ? 'skyblue' : 'default'
								"
							/>
							<Film
								v-else-if="mediaFileCategory(scoped.row.name) === 'VIDEO'"
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
					<span class="attachment-name">
						{{ scoped.row.name }}
					</span>
				</template>
			</el-table-column>
			<el-table-column
				prop="updateTime"
				width="160"
				:label="t('module.attachment.table.colum.label.update_time')"
				:formatter="dateFormat"
				:sortable="canSortAttachmentColumn('updateTime')"
				:sort-method="compareAttachmentByUpdateTime"
			/>
			<el-table-column
				prop="size"
				width="130"
				:label="t('module.attachment.table.colum.label.size')"
				:sortable="canSortAttachmentColumn('size')"
				:sort-method="compareAttachmentBySize"
			>
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
	</ContentBrowser>
</template>

<style lang="scss" scoped>
.attachment-navigation {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.attachment-breadcrumb {
	display: flex;
	align-items: baseline;
	gap: 12px;
	line-height: 24px;
}

.attachment-breadcrumb > span {
	flex: none;
}

:deep(.attachment-breadcrumb .el-breadcrumb) {
	display: flex;
	flex-wrap: wrap;
	align-items: baseline;
	min-width: 0;
}

:deep(.attachment-breadcrumb .el-breadcrumb__item) {
	display: flex;
	align-items: baseline;
	min-width: 0;
	max-width: 100%;
}

:deep(.attachment-breadcrumb .el-breadcrumb__inner) {
	display: block;
	min-width: 0;
	max-width: 100%;
	line-height: 24px;
}

:deep(.attachment-breadcrumb .el-breadcrumb__separator) {
	display: inline-block;
	line-height: 24px;
}

.attachment-name {
	line-height: 24px;
}

:deep(.attachment-breadcrumb .el-button) {
	display: block;
	max-width: 100%;
	height: auto;
	min-height: 24px;
	padding: 0;
	border: 0;
	font: inherit;
	line-height: 24px;
	white-space: normal;
	text-align: left;
	overflow-wrap: anywhere;
	word-break: break-word;
}

:deep(.attachment-breadcrumb .el-button > span) {
	display: block;
	line-height: 24px;
}

.attachment-name {
	white-space: normal;
	overflow-wrap: anywhere;
	word-break: break-word;
}

.attachment-refresh-hint {
	border-radius: 6px;
}

.batch-delete-button-wrapper {
	display: inline-flex;
	margin-left: 12px;
}

.ik-attachment-breadcrumb-item {
	width: 20px;
	cursor: pointer;
}
</style>
