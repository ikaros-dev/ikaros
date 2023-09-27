<script setup lang="ts">
import { taskNamePrefix } from '@/modules/common/constants';
import { usePluginModuleStore } from '@/stores/plugin';
import { apiClient } from '@/utils/api-client';
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
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const props = withDefaults(
	defineProps<{
		visible: boolean;
		isPush: boolean;
		folderId: number;
	}>(),
	{
		visible: false,
		isPush: true,
		folderId: undefined,
	}
);

// 监听父组件传递子组件的文件ID数据变化
watch(props, (newVal) => {
	folderRemoteAction.value.folderId = newVal.folderId;
	folderRemoteAction.value.isPush = newVal.isPush;
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

const folderRemoteAction = ref({
	folderId: props.folderId,
	remote: 'BaiDuPan',
	isPush: false,
});

const folderRemoteArr = ref<string[]>([]);

const { pluginModules } = usePluginModuleStore();

const actionButtonLoading = ref(false);
const onConfirm = async (formEl: FormInstance | undefined) => {
	if (folderRemoteArr.value.length === 0) {
		emit('close');
		dialogVisible.value = false;
	} else {
		if (!formEl) return;
		await formEl.validate(async (valid, fields) => {
			if (valid) {
				// console.log(folderRemoteAction.value);
				actionButtonLoading.value = true;
				let taskName;
				if (folderRemoteAction.value.isPush) {
					// folder push to remote
					await apiClient.folder
						.pushFolder2Remote({
							id: folderRemoteAction.value.folderId + '',
							remote: folderRemoteAction.value.remote,
						})
						.then(() => {
							ElMessage.success(
								t('core.folderRemoteAction.message.hint.pushSuccess.prefix') +
									folderRemoteAction.value.folderId +
									' ' +
									t(
										'core.folderRemoteAction.message.hint.pushSuccess.postfix'
									) +
									folderRemoteAction.value.remote
							);
							taskName =
								taskNamePrefix.folderRemote.push +
								folderRemoteAction.value.folderId;
						})
						.finally(() => {
							actionButtonLoading.value = false;
						});
				} else {
					// folder pull from remote
					await apiClient.folder
						.pullFolder4Remote({
							id: folderRemoteAction.value.folderId + '',
							remote: folderRemoteAction.value.remote,
						})
						.then(() => {
							ElMessage.success(
								t('core.folderRemoteAction.message.hint.pullSuccess.prefix') +
									folderRemoteAction.value.folderId +
									' 远端=' +
									t(
										'core.folderRemoteAction.message.hint.pullSuccess.postfix'
									) +
									folderRemoteAction.value.remote
							);
							taskName =
								taskNamePrefix.folderRemote.pull +
								folderRemoteAction.value.folderId;
						})
						.finally(() => {
							actionButtonLoading.value = false;
						});
				}
				dialogVisible.value = false;
				emit('closeWithTaskName', taskName);
			} else {
				console.log('error submit!', fields);
				ElMessage.error(t('core.folderRemoteAction.message.hint.submitFail'));
			}
		});
	}
};

const folderActionFormRef = ref();
const folderActionFormRules = reactive<FormRules>({
	remote: [
		{
			required: true,
			message: t('core.folderRemoteAction.message.hint.selectRemote'),
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
		folderRemoteArr.value.push(subjectPlatform);
	});
});
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		:title="t('core.folderRemoteAction.title')"
	>
		<el-alert
			:title="t('core.folderRemoteAction.alert.title')"
			type="warning"
			show-icon
			:closable="false"
		/>

		<br />

		<el-form
			v-if="folderRemoteArr.length > 0"
			ref="folderActionFormRef"
			:rules="folderActionFormRules"
			:model="folderRemoteAction"
			label-width="120px"
		>
			<el-form-item
				:label="t('core.fileRemoteAction.formItemLabel.remote')"
				prop="remote"
			>
				<el-select v-model="folderRemoteAction.remote">
					<el-option
						v-for="remote in folderRemoteArr"
						:key="remote"
						:label="remote"
						:value="remote"
					/>
				</el-select>
			</el-form-item>
			<el-form-item
				:label="t('core.folderRemoteAction.formItemLabel.folderId')"
			>
				<el-input v-model="folderRemoteAction.folderId" disabled />
			</el-form-item>
		</el-form>
		<span v-else>
			{{ t('core.folderRemoteAction.message.hint.noStartPlugin') }}
		</span>
		<template #footer>
			<span>
				<el-button
					plain
					:loading="actionButtonLoading"
					@click="onConfirm(folderActionFormRef)"
				>
					{{
						folderRemoteArr.length === 0
							? t('core.folderRemoteAction.button.cancel')
							: props.isPush
							? t('core.folderRemoteAction.button.push')
							: t('core.folderRemoteAction.button.pull')
					}}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
