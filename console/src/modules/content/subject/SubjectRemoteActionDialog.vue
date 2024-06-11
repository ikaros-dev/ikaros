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
// eslint-disable-next-line no-unused-vars
import { apiClient } from '@/utils/api-client';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		isPush: boolean;
		subjectId: number;
	}>(),
	{
		visible: false,
		isPush: true,
		subjectId: undefined,
	}
);

// 监听父组件传递子组件的文件ID数据变化
watch(props, (newVal) => {
	fileRemoteAction.value.subjectId = newVal.subjectId;
	fileRemoteAction.value.isPush = newVal.isPush;
});

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
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
	subjectId: props.subjectId,
	remote: 'BaiDuPan',
	isPush: false,
});

const subjectRemoteArr = ref<string[]>([]);

const { pluginModules } = usePluginModuleStore();

const actionButtonLoading = ref(false);
// eslint-disable-next-line no-unused-vars
const onConfirm = async (formEl: FormInstance | undefined) => {
	if (subjectRemoteArr.value.length === 0) {
		emit('close');
		dialogVisible.value = false;
	} else {
		if (!formEl) return;
		await formEl.validate(async (valid, fields) => {
			if (valid) {
				// console.log(fileRemoteAction.value);
				actionButtonLoading.value = true;
				if (fileRemoteAction.value.isPush) {
					// file push to remote
					// await apiClient.file
					// 	.pushSubject2Remote({
					// 		subjectId: fileRemoteAction.value.subjectId + '',
					// 		remote: fileRemoteAction.value.remote,
					// 	})
					// 	.finally(() => {
					// 		actionButtonLoading.value = false;
					// 	});
				} else {
					// file pull from remote
					// await apiClient.file
					// 	.pullSubject4Remote({
					// 		subjectId: fileRemoteAction.value.subjectId + '',
					// 		remote: fileRemoteAction.value.remote,
					// 	})
					// 	.finally(() => {
					// 		actionButtonLoading.value = false;
					// 	});
				}
				ElMessage.success(
					t(
						'module.subject.dialog.remote-action.message.operate.request-remote.success'
					)
				);
				dialogVisible.value = false;
				emit('close');
			} else {
				console.log('error submit!', fields);
				ElMessage.error(
					t(
						'module.subject.dialog.remote-action.message.operate.request-remote.validate-fail'
					)
				);
			}
		});
	}
};

const fileActionFormRef = ref();
const fileActionFormRules = reactive<FormRules>({
	remote: [
		{
			required: true,
			message: t(
				'module.subject.dialog.remote-action.form-rule.remote.required.message'
			),
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
		subjectRemoteArr.value.push(subjectPlatform);
	});
});
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		:title="t('module.subject.dialog.remote-action.title')"
	>
		<el-alert
			:title="t('module.subject.dialog.remote-action.alert.title')"
			type="warning"
			show-icon
			:closable="false"
		/>

		<br />

		<el-form
			v-if="subjectRemoteArr.length > 0"
			ref="fileActionFormRef"
			:rules="fileActionFormRules"
			:model="fileRemoteAction"
			label-width="120px"
		>
			<el-form-item
				:label="t('module.subject.dialog.remote-action.form-label.remote')"
				prop="remote"
			>
				<el-select v-model="fileRemoteAction.remote">
					<el-option
						v-for="remote in subjectRemoteArr"
						:key="remote"
						:label="remote"
						:value="remote"
					/>
				</el-select>
			</el-form-item>
			<el-form-item
				:label="t('module.subject.dialog.remote-action.form-label.subject-id')"
			>
				<el-input v-model="fileRemoteAction.subjectId" disabled />
			</el-form-item>
		</el-form>
		<span v-else>
			{{ t('module.subject.dialog.remote-action.message.form-hint-nouse') }}
		</span>
		<template #footer>
			<span>
				<el-button plain :loading="actionButtonLoading">
					{{
						subjectRemoteArr.length === 0
							? t('module.subject.dialog.remote-action.button.cancel')
							: props.isPush
							? t('module.subject.dialog.remote-action.button.push')
							: t('module.subject.dialog.remote-action.button.pull')
					}}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
