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
								'请求推送目录至远端成功，目录=' +
									folderRemoteAction.value.folderId +
									' 远端=' +
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
								'请求从远端拉取目录成功，目录=' +
									folderRemoteAction.value.folderId +
									' 远端=' +
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
				ElMessage.error('请检查所填内容是否有必要项缺失。');
			}
		});
	}
};

const folderActionFormRef = ref();
const folderActionFormRules = reactive<FormRules>({
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
		folderRemoteArr.value.push(subjectPlatform);
	});
});
</script>

<template>
	<el-dialog v-model="dialogVisible" title="目录远端操作">
		<el-alert
			title="目录包含文件越多，文件越大，操作时间越长，请耐心等待操作完成。"
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
			<el-form-item label="远端" prop="remote">
				<el-select v-model="folderRemoteAction.remote">
					<el-option
						v-for="remote in folderRemoteArr"
						:key="remote"
						:label="remote"
						:value="remote"
					/>
				</el-select>
			</el-form-item>
			<el-form-item label="目录ID">
				<el-input v-model="folderRemoteAction.folderId" disabled />
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
					@click="onConfirm(folderActionFormRef)"
				>
					{{
						folderRemoteArr.length === 0
							? '返回'
							: props.isPush
							? '推送'
							: '拉取'
					}}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
