<script setup lang="ts">
import {
	ElButton,
	ElDialog,
	ElForm,
	ElFormItem,
	ElInput,
	ElInputNumber,
	ElMessage,
	ElOption,
	ElSelect,
	ElSwitch,
	FormInstance,
	FormRules,
} from 'element-plus';
import { computed, reactive, ref, watch } from 'vue';
import { apiClient } from '@/utils/api-client';
import { episodeGroupLabelMap, episodeGroups } from '@/modules/common/constants';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		rule: any;
	}>(),
	{
		visible: false,
		rule: null,
	}
);

const emit = defineEmits<{
	(event: 'update:visible', visible: boolean): void;
	(event: 'close'): void;
	(event: 'success'): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const isEdit = computed(() => !!props.rule?.id);

const formRef = ref<FormInstance>();

const form = reactive<any>({
	name: '',
	regex: '',
	epGroup: 'MAIN',
	sequence: null,
	priority: 100001,
	description: '',
	enabled: true,
});

const formRules = reactive<FormRules>({
	name: [
		{
			required: true,
			message: t('module.regular.dialog.form-rule.name.required'),
			trigger: 'blur',
		},
	],
	regex: [
		{
			required: true,
			message: t('module.regular.dialog.form-rule.regex.required'),
			trigger: 'blur',
		},
	],
	epGroup: [
		{
			required: true,
			message: t('module.regular.dialog.form-rule.epGroup.required'),
			trigger: 'change',
		},
	],
	priority: [
		{
			required: true,
			message: t('module.regular.dialog.form-rule.priority.required'),
			trigger: 'blur',
		},
	],
});

const submitting = ref(false);

const resetForm = () => {
	form.name = '';
	form.regex = '';
	form.epGroup = 'MAIN';
	form.sequence = null;
	form.priority = 100001;
	form.description = '';
	form.enabled = true;
};

watch(
	() => props.rule,
	(val) => {
		if (val) {
			form.name = val.name || '';
			form.regex = val.regex || '';
			form.epGroup = val.epGroup || 'MAIN';
			form.sequence = val.sequence ?? null;
			form.priority = val.priority ?? 100001;
			form.description = val.description || '';
			form.enabled = val.enabled ?? true;
		} else {
			resetForm();
		}
	},
	{ immediate: true }
);

const onSubmit = async (formEl: FormInstance | undefined) => {
	if (!formEl) return;
	await formEl.validate(async (valid) => {
		if (!valid) return;

		submitting.value = true;
		try {
			const payload: any = {
				name: form.name,
				regex: form.regex,
				epGroup: form.epGroup,
				sequence: form.sequence,
				priority: form.priority,
				description: form.description,
				enabled: form.enabled,
			};

			if (isEdit.value) {
				payload.id = props.rule.id;
				await apiClient.episode.updateEpisodeSequenceRegular({
					episodeSequenceRegular: payload,
				});
				ElMessage.success(
					t('module.regular.dialog.message.update-success', {
						name: form.name,
					})
				);
			} else {
				await apiClient.episode.createEpisodeSequenceRegular({
					episodeSequenceRegular: payload,
				});
				ElMessage.success(
					t('module.regular.dialog.message.create-success', {
						name: form.name,
					})
				);
			}

			resetForm();
			emit('success');
			dialogVisible.value = false;
		} catch (e) {
			console.error('Failed to save rule', e);
		} finally {
			submitting.value = false;
		}
	});
};

const onClose = () => {
	resetForm();
	emit('close');
};
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		:title="
			isEdit
				? t('module.regular.dialog.title.edit')
				: t('module.regular.dialog.title.create')
		"
		:close-on-click-modal="false"
		width="600px"
		@close="onClose"
	>
		<el-form
			ref="formRef"
			:model="form"
			:rules="formRules"
			label-width="100px"
		>
			<el-form-item
				:label="t('module.regular.dialog.label.name')"
				prop="name"
			>
				<el-input
					v-model="form.name"
					:placeholder="t('module.regular.dialog.placeholder.name')"
				/>
			</el-form-item>
			<el-form-item
				:label="t('module.regular.dialog.label.regex')"
				prop="regex"
			>
				<el-input
					v-model="form.regex"
					:placeholder="t('module.regular.dialog.placeholder.regex')"
				/>
				<div class="form-item-hint">
					{{ t('module.regular.dialog.hint.regex') }}
				</div>
			</el-form-item>
			<el-form-item
				:label="t('module.regular.dialog.label.epGroup')"
				prop="epGroup"
			>
				<el-select v-model="form.epGroup" style="width: 100%">
					<el-option
						v-for="group in episodeGroups"
						:key="group"
						:label="episodeGroupLabelMap.get(group) || group"
						:value="group"
					/>
				</el-select>
			</el-form-item>
			<el-form-item
				:label="t('module.regular.dialog.label.sequence')"
				prop="sequence"
			>
				<el-input-number
					v-model="form.sequence"
					:min="0"
					:max="999"
					:step="0.1"
					:precision="1"
					:placeholder="t('module.regular.dialog.placeholder.sequence')"
					clearable
					style="width: 100%"
				/>
				<div class="form-item-hint">
					{{ t('module.regular.dialog.hint.sequence') }}
				</div>
			</el-form-item>
			<el-form-item
				:label="t('module.regular.dialog.label.priority')"
				prop="priority"
			>
				<el-input-number
					v-model="form.priority"
					:min="0"
					:max="2147483647"
					:step="1"
					style="width: 100%"
				/>
			</el-form-item>
			<el-form-item
				:label="t('module.regular.dialog.label.description')"
				prop="description"
			>
				<el-input
					v-model="form.description"
					type="textarea"
					:rows="2"
					:placeholder="t('module.regular.dialog.placeholder.description')"
				/>
			</el-form-item>
			<el-form-item
				:label="t('module.regular.dialog.label.enabled')"
				prop="enabled"
			>
				<el-switch v-model="form.enabled" />
			</el-form-item>
		</el-form>
		<template #footer>
			<el-button @click="onClose">
				{{ t('common.button.cancel') }}
			</el-button>
			<el-button type="primary" :loading="submitting" @click="onSubmit(formRef)">
				{{ t('common.button.submit') }}
			</el-button>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped>
.form-item-hint {
	font-size: 12px;
	color: var(--el-text-color-secondary);
	line-height: 1.4;
	margin-top: 4px;
}
</style>
