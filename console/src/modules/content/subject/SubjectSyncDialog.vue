<script setup lang="ts">
import { usePluginModuleStore } from '@/stores/plugin';
import { apiClient } from '@/utils/api-client';
import { PluginModule } from '@runikaros/shared';
import { Subject } from '@runikaros/api-client';
import {
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
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		isMerge?: boolean;
		defineSubjectId?: number | string;
	}>(),
	{
		visible: false,
		isMerge: false,
		defineSubjectId: undefined,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'update:defineSubjectId', defineSubjectId: number | string): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'closeWithSubjectName', subject: Subject): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const subjectId = computed({
	get() {
		return props.defineSubjectId;
	},
	set(value) {
		emit('update:defineSubjectId', value as number);
	},
});

const subjectSync = ref({
	platform: '',
	platformId: '',
	subjectId: undefined,
	action: props.isMerge ? 'MERGE' : 'PULL',
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
				console.log('subjectSync', subjectSync.value);
				await apiClient.subjectSync
					.syncSubjectAndPlatform({
						// @ts-ignore
						platform: subjectSync.value.platform,
						platformId: subjectSync.value.platformId,
						subjectId: subjectId.value as number,
					})
					.finally(() => {
						syncButtonLoading.value = false;
					});
				const { data } =
					await apiClient.subjectSync.getSubjectSyncsByPlatformAndPlatformId({
						// @ts-ignore
						platform: subjectSync.value.platform,
						// @ts-ignore
						platformId: subjectSync.value.platformId as number,
					});
				if (data.length > 0) {
					const rsp = await apiClient.subject.searchSubjectById({
						id: data[0].subjectId as number,
					});
					emit('closeWithSubjectName', rsp.data);
				}
				dialogVisible.value = false;
				// emit('closeWithSubjectName', data);
			} else {
				console.log('error submit!', fields);
				ElMessage.error(t('module.subject.dialog.sync.message.validate-fail'));
			}
		});
	}
};

const subjectSyncFormRef = ref();
const subjectSyncFormRules = reactive<FormRules>({
	platform: [
		{
			required: true,
			message: t(
				'module.subject.dialog.sync.message.form-rule.platform.required'
			),
			trigger: 'change',
		},
	],
	platformId: [
		{
			required: true,
			message: t(
				'module.subject.dialog.sync.message.form-rule.platform-id.required'
			),
			trigger: 'blur',
		},
		{
			min: 1,
			max: 100,
			message: t(
				'module.subject.dialog.sync.message.form-rule.platform-id.length'
			),
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
	if (subjectPlatformArr.value.length == 1) {
		subjectSync.value.platform = subjectPlatformArr.value[0];
	}
});
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		:title="
			props.isMerge
				? t('module.subject.dialog.sync.title.update')
				: t('module.subject.dialog.sync.title.pull')
		"
	>
		<el-form
			v-if="subjectPlatformArr.length > 0"
			ref="subjectSyncFormRef"
			:rules="subjectSyncFormRules"
			:model="subjectSync"
			label-width="120px"
		>
			<el-form-item
				:label="t('module.subject.dialog.sync.label.platform')"
				prop="platform"
			>
				<el-select v-model="subjectSync.platform">
					<el-option
						v-for="platform in subjectPlatformArr"
						:key="platform"
						:label="platform"
						:value="platform"
					/>
				</el-select>
			</el-form-item>
			<el-form-item
				:label="t('module.subject.dialog.sync.label.subject-platform-id')"
				prop="platformId"
			>
				<el-input
					v-model="subjectSync.platformId"
					:placeholder="t('module.subject.dialog.sync.placeholder.platform-id')"
				/>
			</el-form-item>
		</el-form>
		<span v-else>
			{{ t('module.subject.dialog.sync.text.platform-no-available-hint-msg') }}
		</span>
		<template #footer>
			<span>
				<el-button
					plain
					:loading="syncButtonLoading"
					@click="onConfirm(subjectSyncFormRef)"
				>
					{{
						subjectPlatformArr.length === 0
							? t('module.subject.dialog.sync.footer.button.cancel')
							: t('module.subject.dialog.sync.footer.button.confirm')
					}}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
