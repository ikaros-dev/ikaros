<script setup lang="ts">
import {
	AttachmentDriver,
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
	ElTable,
	ElTableColumn,
	ElSwitch,
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
const loading = ref(false);
const errorMessage = ref('');
const formVisible = ref(false);
const editingDriver = ref<AttachmentDriver>();
const formLoading = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ mount_name: '', remote_path: '', comment: '' });
const operationId = ref<string>();

const formTitle = computed(() =>
	editingDriver.value
		? t('module.attachment.file-source.edit-title')
		: t('module.attachment.file-source.create-title')
);

const formRules = computed<FormRules>(() => ({
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

const loadDrivers = async () => {
	loading.value = true;
	errorMessage.value = '';
	try {
		const { data } = await apiClient.attachmentDriver.listDriversByCondition({
			page: 1,
			size: 1000,
		});
		drivers.value = ((data?.items ?? []) as AttachmentDriver[]).filter(
			(driver) =>
				driver.type === AttachmentDriverTypeEnum.Local && driver.name === 'DISK'
		);
	} catch {
		errorMessage.value = t('module.attachment.file-source.errors.load');
	} finally {
		loading.value = false;
	}
};

const resetForm = () => {
	form.mount_name = '';
	form.remote_path = '';
	form.comment = '';
	formRef.value?.clearValidate();
};

const openCreate = () => {
	editingDriver.value = undefined;
	resetForm();
	formVisible.value = true;
};

const openEdit = (driver: AttachmentDriver) => {
	editingDriver.value = driver;
	form.mount_name = driver.mount_name ?? '';
	form.remote_path = driver.remote_path ?? '';
	form.comment = driver.comment ?? '';
	formVisible.value = true;
};

const closeForm = () => {
	if (!formLoading.value) formVisible.value = false;
};

const saveForm = async () => {
	if (!formRef.value) return;
	const valid = await formRef.value.validate().catch(() => false);
	if (!valid) return;
	formLoading.value = true;
	let savedDriver: AttachmentDriver;
	try {
		const driver: AttachmentDriver = editingDriver.value
			? {
					...editingDriver.value,
					mount_name: form.mount_name,
					remote_path: form.remote_path,
					comment: form.comment,
				}
			: {
					type: AttachmentDriverTypeEnum.Local,
					name: 'DISK',
					mount_name: form.mount_name,
					remote_path: form.remote_path,
					comment: form.comment,
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
			await loadDrivers();
			ElMessage.error(
				t('module.attachment.file-source.errors.enable-after-save')
			);
			formLoading.value = false;
			return;
		}
	}
	try {
		formVisible.value = false;
		await loadDrivers();
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
		await loadDrivers();
		emit('changed');
	} catch {
		ElMessage.error(t('module.attachment.file-source.errors.toggle'));
		await loadDrivers();
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
		await loadDrivers();
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
		if (visible) loadDrivers();
	},
	{ immediate: true }
);
</script>

<template>
	<el-dialog
		:model-value="visible"
		:title="t('module.attachment.file-source.title')"
		width="760px"
		@update:model-value="emit('update:visible', $event)"
	>
		<el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon>
			<template #default>
				<el-button link type="danger" @click="loadDrivers">
					{{ t('module.attachment.file-source.actions.retry') }}
				</el-button>
			</template>
		</el-alert>
		<el-button type="primary" :loading="loading" @click="openCreate">
			{{ t('module.attachment.file-source.create') }}
		</el-button>
		<el-table v-loading="loading" :data="drivers" class="source-table">
			<el-table-column
				prop="mount_name"
				:label="t('module.attachment.file-source.columns.name')"
			/>
			<el-table-column
				prop="remote_path"
				:label="t('module.attachment.file-source.columns.path')"
			/>
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
				width="180"
			>
				<template #default="{ row }">
					<el-button link type="primary" @click="openEdit(row)">{{
						t('common.button.edit')
					}}</el-button>
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

		<el-dialog
			v-model="formVisible"
			:title="formTitle"
			width="520px"
			append-to-body
		>
			<el-form
				ref="formRef"
				:model="form"
				:rules="formRules"
				label-width="100px"
			>
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
				<el-form-item
					:label="t('module.attachment.file-source.fields.comment')"
				>
					<el-input v-model="form.comment" type="textarea" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="closeForm">{{
					t('common.button.cancel')
				}}</el-button>
				<el-button type="primary" :loading="formLoading" @click="saveForm">
					{{ t('module.attachment.file-source.actions.save') }}
				</el-button>
			</template>
		</el-dialog>
	</el-dialog>
</template>

<style scoped>
.source-table {
	margin-top: 16px;
}
</style>
