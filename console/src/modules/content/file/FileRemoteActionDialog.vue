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
				ElMessage.error('请检查所填内容是否有必要项缺失。');
			}
		});
	}
};

const fileActionFormRef = ref();
const fileActionFormRules = reactive<FormRules>({
	remote: [
		{
			required: true,
			message: '请选择远端的名称',
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
	<el-dialog v-model="dialogVisible" title="文件远端操作">
		<el-alert
			title="文件越大，操作时间越长，请耐心等待操作完成。"
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
			<el-form-item label="远端" prop="remote">
				<el-select v-model="fileRemoteAction.remote">
					<el-option
						v-for="remote in fileRemoteArr"
						:key="remote"
						:label="remote"
						:value="remote"
					/>
				</el-select>
			</el-form-item>
			<el-form-item label="文件ID">
				<el-input v-model="fileRemoteAction.fileId" disabled />
			</el-form-item>
		</el-form>
		<span v-else>
			暂无可用的远端，请开启相关的插件并启动，比如百度网盘的插件。
		</span>
		<template #footer>
			<span>
				<el-button
					plain
					:loading="actionButtonLoading"
					@click="onConfirm(fileActionFormRef)"
				>
					{{
						fileRemoteArr.length === 0 ? '返回' : props.isPush ? '推送' : '拉取'
					}}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
