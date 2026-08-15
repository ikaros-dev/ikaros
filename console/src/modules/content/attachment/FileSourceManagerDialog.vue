<script setup lang="ts">
import {
	AttachmentDriver,
	AttachmentDriverFetcherVo,
	AttachmentDriverTypeEnum,
} from '@runikaros/api-client';
import { computed, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import {
	ElAlert,
	ElButton,
	ElDialog,
	ElForm,
	ElFormItem,
	ElInput,
	ElMessage,
	ElMessageBox,
	ElRadioButton,
	ElRadioGroup,
	ElSelect,
	ElOption,
	ElSwitch,
	ElTable,
	ElTableColumn,
	FormInstance,
	FormRules,
} from 'element-plus';
import { apiClient } from '@/utils/api-client';

const props = defineProps<{ visible: boolean }>();
const emit = defineEmits<{
	(event: 'update:visible', visible: boolean): void;
	(event: 'changed'): void;
}>();

const { t } = useI18n();
const drivers = ref<AttachmentDriver[]>([]);
const driverFetchers = ref<AttachmentDriverFetcherVo[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const editingDriver = ref<AttachmentDriver>();
const formLoading = ref(false);
const formRef = ref<FormInstance>();
const operationId = ref<string>();
type SupportedDriverType =
	| typeof AttachmentDriverTypeEnum.Local
	| typeof AttachmentDriverTypeEnum.Custom;
interface FileSourceForm {
	name: string;
	mount_name: string;
	remote_path: string;
	access_token: string;
	refresh_token: string;
	comment: string;
}
const createForm = (type: SupportedDriverType): FileSourceForm => ({
	name: type === AttachmentDriverTypeEnum.Local ? 'DISK' : '',
	mount_name: '',
	remote_path: '',
	access_token: '',
	refresh_token: '',
	comment: '',
});
const selectedType = ref<SupportedDriverType>(AttachmentDriverTypeEnum.Local);
const localForm = reactive<FileSourceForm>(
	createForm(AttachmentDriverTypeEnum.Local)
);
const customForm = reactive<FileSourceForm>(
	createForm(AttachmentDriverTypeEnum.Custom)
);
const form = computed(() =>
	selectedType.value === AttachmentDriverTypeEnum.Custom
		? customForm
		: localForm
);

const customFetchers = computed(() =>
	driverFetchers.value.filter(
		(fetcher) =>
			fetcher.type === AttachmentDriverTypeEnum.Custom && fetcher.name
	)
);
const customAvailable = computed(() => customFetchers.value.length > 0);
const customSelected = computed(
	() => selectedType.value === AttachmentDriverTypeEnum.Custom
);
const formTitle = computed(() =>
	editingDriver.value
		? t('module.attachment.file-source.edit-title')
		: t('module.attachment.file-source.create-title')
);
const submitText = computed(() =>
	editingDriver.value
		? t('module.attachment.file-source.actions.save')
		: t('module.attachment.file-source.create')
);

const formRules = computed<FormRules>(() => ({
	name: customSelected.value
		? [
				{
					required: true,
					message: t('module.attachment.file-source.validation.driver-name'),
					trigger: 'change',
				},
			]
		: [],
	mount_name: [
		{
			required: true,
			message: t('module.attachment.file-source.validation.mount-name'),
			trigger: 'blur',
		},
	],
	remote_path: [
		{
			required: true,
			message: t('module.attachment.file-source.validation.remote-path'),
			trigger: 'blur',
		},
	],
}));

const loadData = async () => {
	loading.value = true;
	errorMessage.value = '';
	try {
		const [driversResponse, fetchersResponse] = await Promise.all([
			apiClient.attachmentDriver.listDriversByCondition({
				page: 1,
				size: 1000,
			}),
			apiClient.attachmentDriver.listDriversFetchers(),
		]);
		drivers.value = (driversResponse.data?.items ?? []) as AttachmentDriver[];
		driverFetchers.value = (fetchersResponse.data ??
			[]) as AttachmentDriverFetcherVo[];
	} catch {
		errorMessage.value = t('module.attachment.file-source.errors.load');
	} finally {
		loading.value = false;
	}
};

const resetForm = () => {
	editingDriver.value = undefined;
	selectedType.value = AttachmentDriverTypeEnum.Local;
	Object.assign(localForm, createForm(AttachmentDriverTypeEnum.Local));
	Object.assign(customForm, createForm(AttachmentDriverTypeEnum.Custom));
	formRef.value?.clearValidate();
};

const openEdit = (driver: AttachmentDriver) => {
	Object.assign(localForm, createForm(AttachmentDriverTypeEnum.Local));
	Object.assign(customForm, createForm(AttachmentDriverTypeEnum.Custom));
	editingDriver.value = driver;
	selectedType.value =
		driver.type === AttachmentDriverTypeEnum.Custom
			? AttachmentDriverTypeEnum.Custom
			: AttachmentDriverTypeEnum.Local;
	Object.assign(form.value, {
		name: driver.name ?? '',
		mount_name: driver.mount_name ?? '',
		remote_path: driver.remote_path ?? '',
		access_token: driver.access_token ?? '',
		refresh_token: driver.refresh_token ?? '',
		comment: driver.comment ?? '',
	});
	formRef.value?.clearValidate();
};

const changeType = () => {
	if (
		customSelected.value &&
		!customForm.name &&
		customFetchers.value.length === 1
	) {
		customForm.name = customFetchers.value[0].name!;
	}
	formRef.value?.clearValidate();
};

const saveForm = async () => {
	if (!formRef.value) return;
	const valid = await formRef.value.validate().catch(() => false);
	if (!valid) return;
	formLoading.value = true;
	let savedDriver: AttachmentDriver;
	try {
		const driver: AttachmentDriver = {
			...(editingDriver.value ?? {}),
			type: selectedType.value,
			name: customSelected.value ? form.value.name : 'DISK',
			mount_name: form.value.mount_name,
			remote_path: form.value.remote_path,
			access_token: customSelected.value ? form.value.access_token : undefined,
			refresh_token: customSelected.value
				? form.value.refresh_token
				: undefined,
			comment: form.value.comment,
		};
		const { data } = await apiClient.attachmentDriver.saveAttachmentDriver({
			attachmentDriver: driver,
		});
		savedDriver = data;
	} catch {
		ElMessage.error(t('module.attachment.file-source.errors.save'));
		formLoading.value = false;
		return;
	}
	if (!editingDriver.value) {
		if (!savedDriver.id) {
			ElMessage.error(t('module.attachment.file-source.errors.missing-id'));
			formLoading.value = false;
			return;
		}
		try {
			await apiClient.attachmentDriver.enableDriver1({ id: savedDriver.id });
		} catch {
			await loadData();
			ElMessage.error(
				t('module.attachment.file-source.errors.enable-after-save')
			);
			formLoading.value = false;
			return;
		}
	}
	try {
		resetForm();
		await loadData();
		ElMessage.success(t('module.attachment.file-source.messages.save-success'));
		emit('changed');
	} finally {
		formLoading.value = false;
	}
};

const changeEnabled = async (
	driver: AttachmentDriver,
	enabled: string | number | boolean
) => {
	if (!driver.id) return;
	operationId.value = driver.id;
	try {
		if (enabled) {
			await apiClient.attachmentDriver.enableDriver1({ id: driver.id });
		} else {
			await apiClient.attachmentDriver.enableDriver({ id: driver.id });
		}
		await loadData();
		emit('changed');
	} catch {
		ElMessage.error(t('module.attachment.file-source.errors.toggle'));
		await loadData();
	} finally {
		operationId.value = undefined;
	}
};

const deleteDriver = async (driver: AttachmentDriver) => {
	if (!driver.id) return;
	try {
		await ElMessageBox.confirm(
			t('module.attachment.file-source.delete-confirm', {
				name: driver.mount_name,
			}),
			t('module.attachment.file-source.delete-title'),
			{
				confirmButtonText: t('common.button.confirm'),
				cancelButtonText: t('common.button.cancel'),
				type: 'warning',
			}
		);
	} catch {
		return;
	}
	operationId.value = driver.id;
	try {
		await apiClient.attachmentDriver.deleteAttachmentDriverById({
			id: driver.id,
		});
		if (editingDriver.value?.id === driver.id) resetForm();
		await loadData();
		emit('changed');
		ElMessage.success(
			t('module.attachment.file-source.messages.delete-success')
		);
	} catch {
		ElMessage.error(t('module.attachment.file-source.errors.delete'));
	} finally {
		operationId.value = undefined;
	}
};

watch(
	() => props.visible,
	(visible) => {
		if (visible) {
			resetForm();
			loadData();
		}
	},
	{ immediate: true }
);
</script>

<template>
	<el-dialog
		:model-value="visible"
		:title="t('module.attachment.file-source.title')"
		width="min(1100px, calc(100vw - 32px))"
		class="file-source-dialog"
		@update:model-value="emit('update:visible', $event)"
	>
		<div class="manager-layout">
			<section class="source-form-panel">
				<h3 class="panel-title">{{ formTitle }}</h3>
				<el-form
					ref="formRef"
					:model="form"
					:rules="formRules"
					label-position="top"
				>
					<el-form-item
						v-if="customAvailable"
						class="type-form-item"
						:label="t('module.attachment.file-source.fields.type')"
					>
						<el-radio-group v-model="selectedType" @change="changeType">
							<el-radio-button :value="AttachmentDriverTypeEnum.Local">
								LOCAL
							</el-radio-button>
							<el-radio-button :value="AttachmentDriverTypeEnum.Custom">
								CUSTOM
							</el-radio-button>
						</el-radio-group>
					</el-form-item>
					<el-form-item
						v-if="customSelected"
						:label="t('module.attachment.file-source.fields.driver-name')"
						prop="name"
					>
						<el-select v-model="form.name">
							<el-option
								v-for="fetcher in customFetchers"
								:key="fetcher.name"
								:label="fetcher.name"
								:value="fetcher.name"
							/>
						</el-select>
					</el-form-item>
					<el-form-item
						:label="t('module.attachment.file-source.fields.name')"
						prop="mount_name"
					>
						<el-input v-model="form.mount_name" />
					</el-form-item>
					<el-form-item
						:label="t('module.attachment.file-source.fields.path')"
						prop="remote_path"
					>
						<el-input v-model="form.remote_path" />
					</el-form-item>
					<template v-if="customSelected">
						<el-form-item
							:label="t('module.attachment.file-source.fields.access-token')"
						>
							<el-input v-model="form.access_token" />
						</el-form-item>
						<el-form-item
							:label="t('module.attachment.file-source.fields.refresh-token')"
						>
							<el-input v-model="form.refresh_token" />
						</el-form-item>
					</template>
					<el-form-item
						:label="t('module.attachment.file-source.fields.comment')"
					>
						<el-input v-model="form.comment" type="textarea" />
					</el-form-item>
				</el-form>
				<div class="form-actions">
					<el-button
						v-if="editingDriver"
						:disabled="formLoading"
						@click="resetForm"
					>
						{{ t('common.button.cancel') }}
					</el-button>
					<el-button type="primary" :loading="formLoading" @click="saveForm">
						{{ submitText }}
					</el-button>
				</div>
			</section>

			<section class="source-list-panel">
				<h3 class="panel-title">
					{{ t('module.attachment.file-source.title') }}
				</h3>
				<el-alert
					v-if="errorMessage"
					:title="errorMessage"
					type="error"
					show-icon
				>
					<template #default>
						<el-button link type="danger" @click="loadData">
							{{ t('module.attachment.file-source.actions.retry') }}
						</el-button>
					</template>
				</el-alert>
				<div class="table-scroll">
					<el-table v-loading="loading" :data="drivers" class="source-table">
						<el-table-column
							:label="t('module.attachment.file-source.columns.name')"
							min-width="140"
						>
							<template #default="{ row }">
								<div class="source-cell-text">{{ row.mount_name }}</div>
							</template>
						</el-table-column>
						<el-table-column
							:label="t('module.attachment.file-source.columns.path')"
							min-width="190"
						>
							<template #default="{ row }">
								<div class="source-cell-text">{{ row.remote_path }}</div>
							</template>
						</el-table-column>
						<el-table-column
							:label="t('module.attachment.file-source.columns.enabled')"
							width="100"
						>
							<template #default="{ row }">
								<el-switch
									v-model="row.enable"
									:loading="operationId === row.id"
									:disabled="operationId !== undefined"
									@change="changeEnabled(row, $event)"
								/>
							</template>
						</el-table-column>
						<el-table-column
							:label="t('module.attachment.file-source.columns.operations')"
							width="150"
						>
							<template #default="{ row }">
								<el-button link type="primary" @click="openEdit(row)">
									{{ t('common.button.edit') }}
								</el-button>
								<el-button
									link
									type="danger"
									:loading="operationId === row.id"
									@click="deleteDriver(row)"
								>
									{{ t('common.button.delete') }}
								</el-button>
							</template>
						</el-table-column>
					</el-table>
				</div>
			</section>
		</div>
	</el-dialog>
</template>

<style scoped>
:deep(.file-source-dialog .el-dialog__body) {
	max-height: calc(100vh - 140px);
	overflow: auto;
}

.manager-layout {
	display: grid;
	grid-template-columns: minmax(300px, 340px) minmax(0, 1fr);
	gap: 24px;
	align-items: stretch;
}

.source-form-panel,
.source-list-panel {
	min-width: 0;
	padding: 20px;
	border: 1px solid var(--el-border-color-lighter);
	border-radius: 8px;
}

.source-form-panel {
	display: flex;
	flex-direction: column;
}

.panel-title {
	margin: 0 0 20px;
	font-size: 16px;
}

.type-form-item :deep(.el-form-item__content) {
	justify-content: center;
}

.form-actions {
	display: flex;
	justify-content: flex-end;
	gap: 8px;
	margin-top: auto;
	padding-top: 8px;
}

.table-scroll {
	width: 100%;
	overflow-x: auto;
}

.source-table {
	min-width: 580px;
}

.source-cell-text {
	min-width: 0;
	white-space: normal;
	overflow-wrap: anywhere;
	word-break: break-word;
}

@media (max-width: 767px) {
	:deep(.file-source-dialog) {
		margin-top: 16px;
		margin-bottom: 16px;
	}

	:deep(.file-source-dialog .el-dialog__body) {
		max-height: calc(100vh - 96px);
		padding: 12px;
	}

	.manager-layout {
		grid-template-columns: minmax(0, 1fr);
		gap: 16px;
	}

	.source-form-panel,
	.source-list-panel {
		padding: 16px;
	}

	.form-actions .el-button {
		flex: 1;
	}
}
</style>
