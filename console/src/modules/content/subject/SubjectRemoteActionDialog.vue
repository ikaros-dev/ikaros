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
				ElMessage.success('提交成功');
				dialogVisible.value = false;
				emit('close');
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
		subjectRemoteArr.value.push(subjectPlatform);
	});
});
</script>

<template>
	<el-dialog v-model="dialogVisible" title="条目远端操作">
		<el-alert
			title="条目剧集越多，剧集文件越大，操作时间越长，请耐心等待操作完成。"
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
			<el-form-item label="远端" prop="remote">
				<el-select v-model="fileRemoteAction.remote">
					<el-option
						v-for="remote in subjectRemoteArr"
						:key="remote"
						:label="remote"
						:value="remote"
					/>
				</el-select>
			</el-form-item>
			<el-form-item label="条目ID">
				<el-input v-model="fileRemoteAction.subjectId" disabled />
			</el-form-item>
		</el-form>
		<span v-else>
			暂无可用的远端，请开启相关的插件并启动，比如百度网盘的插件。
		</span>
		<template #footer>
			<span>
				<el-button plain :loading="actionButtonLoading">
					{{
						subjectRemoteArr.length === 0
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
