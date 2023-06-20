<script setup lang="ts">
import { usePluginModuleStore } from '@/stores/plugin';
import { apiClient } from '@/utils/api-client';
import { PluginModule } from '@runikaros/shared';
import { ElMessage, FormInstance, FormRules } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import {
	ElDialog,
	ElForm,
	ElFormItem,
	ElSelect,
	ElOption,
	ElInput,
	ElButton,
} from 'element-plus';

const props = withDefaults(
	defineProps<{
		visible: boolean;
	}>(),
	{
		visible: false,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'closeWithSubjectName', subjectName: string): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const subjectSync = ref({
	platform: '',
	platformId: '',
	subjectId: undefined,
});

const subjectPlatformArr = ref<string[]>([]);

const { pluginModules } = usePluginModuleStore();

const syncButtonLoading = ref(false);
const onConfirm = async (formEl: FormInstance | undefined) => {
	if (subjectPlatformArr.value.length === 0) {
		emit('close');
		dialogVisible.value = false;
	} else {
		if (!formEl) return;
		await formEl.validate(async (valid, fields) => {
			if (valid) {
				syncButtonLoading.value = true;
				const { data } = await apiClient.subjectSyncPlatform
					.syncSubjectAndPlatform({
						// @ts-ignore
						platform: subjectSync.value.platform,
						platformId: subjectSync.value.platformId,
					})
					.finally(() => {
						syncButtonLoading.value = false;
					});
				dialogVisible.value = false;
				emit('closeWithSubjectName', data.name);
			} else {
				console.log('error submit!', fields);
				ElMessage.error('请检查所填内容是否有必要项缺失。');
			}
		});
	}
};

const subjectSyncFormRef = ref();
const subjectSyncFormRules = reactive<FormRules>({
	platform: [
		{
			required: true,
			message: '请选择同步的平台',
			trigger: 'change',
		},
	],
	platformId: [
		{ required: true, message: '请输入同步平台的条目ID', trigger: 'blur' },
		{
			min: 1,
			max: 10,
			message: '长度应该在 1 到 10 字符之间',
			trigger: 'blur',
		},
	],
});

onMounted(() => {
	pluginModules.forEach((pluginModule: PluginModule) => {
		const { extensionPoints } = pluginModule;
		if (!extensionPoints?.['subject:sync:platform']) {
			return;
		}
		const subjectPlatform = extensionPoints[
			'subject:sync:platform'
		] as unknown as string;
		subjectPlatformArr.value.push(subjectPlatform);
	});
});
</script>

<template>
	<el-dialog v-model="dialogVisible" title="条目同步">
		<el-form
			v-if="subjectPlatformArr.length > 0"
			ref="subjectSyncFormRef"
			:rules="subjectSyncFormRules"
			:model="subjectSync"
			label-width="120px"
		>
			<el-form-item label="平台" prop="platform">
				<el-select v-model="subjectSync.platform">
					<el-option
						v-for="platform in subjectPlatformArr"
						:key="platform"
						:label="platform"
						:value="platform"
					/>
				</el-select>
			</el-form-item>
			<el-form-item label="平台的条目ID" prop="platformId">
				<el-input
					v-model="subjectSync.platformId"
					placeholder="请输入同步平台的条目ID"
				/>
			</el-form-item>
		</el-form>
		<span v-else
			>暂无可用的同步平台，请开启相关的插件并启动，比如番组计划的插件。</span
		>
		<template #footer>
			<span>
				<el-button
					plain
					:loading="syncButtonLoading"
					@click="onConfirm(subjectSyncFormRef)"
				>
					{{ subjectPlatformArr.length === 0 ? '返回' : '同步' }}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
