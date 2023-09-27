<script setup lang="ts">
import { usePluginModuleStore } from '@/stores/plugin';
import { PluginModule } from '@runikaros/shared';
import {
	ElAlert,
	ElButton,
	ElDialog,
	ElForm,
	ElFormItem,
	ElInput,
	ElMessage,
	ElOption,
	ElSelect,
	FormInstance,
	FormRules,
} from 'element-plus';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { apiClient } from '@/utils/api-client';
import { taskNamePrefix } from '@/modules/common/constants';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		isPush: boolean;
		fileId: number;
	}>(),
	{
		visible: false,
		isPush: true,
		fileId: undefined,
	}
);

// 监听父组件传递子组件的文件ID数据变化
watch(props, (newVal) => {
	fileRemoteAction.value.fileId = newVal.fileId;
	fileRemoteAction.value.isPush = newVal.isPush;
});

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'closeWithTaskName', taskName: string): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const fileRemoteAction = ref({
	fileId: props.fileId,
	remote: 'BaiDuPan',
	isPush: false,
});

const fileRemoteArr = ref<string[]>([]);

const { pluginModules } = usePluginModuleStore();

const actionButtonLoading = ref(false);
const onConfirm = async (formEl: FormInstance | undefined) => {
	if (fileRemoteArr.value.length === 0) {
		emit('close');
		dialogVisible.value = false;
	} else {
		if (!formEl) return;
		await formEl.validate(async (valid, fields) => {
			if (valid) {
				// console.log(fileRemoteAction.value);
				actionButtonLoading.value = true;
				let taskName;
				if (fileRemoteAction.value.isPush) {
					// file push to remote
					await apiClient.file
						.pushFile2Remote({
							id: fileRemoteAction.value.fileId + '',
							remote: fileRemoteAction.value.remote,
						})
						.then(() => {
							taskName =
								taskNamePrefix.fileRemote.push + fileRemoteAction.value.fileId;
						})
						.finally(() => {
							actionButtonLoading.value = false;
						});
				} else {
					// file pull from remote
					await apiClient.file
						.pullFile4Remote({
							// @ts-ignore
							id: fileRemoteAction.value.fileId + '',
							remote: fileRemoteAction.value.remote,
						})
						.then(() => {
							taskName =
								taskNamePrefix.fileRemote.pull + fileRemoteAction.value.fileId;
						})
						.finally(() => {
							actionButtonLoading.value = false;
						});
				}
				dialogVisible.value = false;
				emit('closeWithTaskName', taskName);
			} else {
				console.log('error submit!', fields);
				ElMessage.error(t('core.fileRemoteAction.message.hint.submitFail'));
			}
		});
	}
};

const fileActionFormRef = ref();
const fileActionFormRules = reactive<FormRules>({
	remote: [
		{
			required: true,
			message: t('core.fileRemoteAction.message.hint.selectRemote'),
			trigger: 'change',
		},
	],
});

onMounted(() => {
	pluginModules.forEach((pluginModule: PluginModule) => {
		const { extensionPoints } = pluginModule;
		if (!extensionPoints?.['file:remote']) {
			return;
		}
		const subjectPlatform = extensionPoints['file:remote'] as unknown as string;
		fileRemoteArr.value.push(subjectPlatform);
	});
});
</script>

<template>
	<el-dialog v-model="dialogVisible" :title="t('core.fileRemoteAction.title')">
		<el-alert
			:title="t('core.fileRemoteAction.alert.title')"
			type="warning"
			show-icon
			:closable="false"
		/>

		<br />

		<el-form
			v-if="fileRemoteArr.length > 0"
			ref="fileActionFormRef"
			:rules="fileActionFormRules"
			:model="fileRemoteAction"
			label-width="120px"
		>
			<el-form-item
				:label="t('core.fileRemoteAction.formItemLabel.remote')"
				prop="remote"
			>
				<el-select v-model="fileRemoteAction.remote">
					<el-option
						v-for="remote in fileRemoteArr"
						:key="remote"
						:label="remote"
						:value="remote"
					/>
				</el-select>
			</el-form-item>
			<el-form-item :label="t('core.fileRemoteAction.formItemLabel.fileId')">
				<el-input v-model="fileRemoteAction.fileId" disabled />
			</el-form-item>
		</el-form>
		<span v-else>
			{{ t('core.fileRemoteAction.message.hint.noStartPlugin') }}
		</span>
		<template #footer>
			<span>
				<el-button
					plain
					:loading="actionButtonLoading"
					@click="onConfirm(fileActionFormRef)"
				>
					{{
						fileRemoteArr.length === 0
							? t('core.fileRemoteAction.button.cancel')
							: props.isPush
							? t('core.fileRemoteAction.button.push')
							: t('core.fileRemoteAction.button.pull')
					}}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
