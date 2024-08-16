<script setup lang="ts">
import {Episode} from '@runikaros/api-client';
import {
  ElButton,
  ElDatePicker,
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
import {computed, reactive, ref} from 'vue';
import {episodeGroupLabelMap, episodeGroups,} from '@/modules/common/constants';
import {useI18n} from 'vue-i18n';

const { t } = useI18n();

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
	(event: 'closeWithEpsiode', episode: Episode): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const onClose = () => {
	emit('close');
};

const episode = ref<Episode>({
	group: 'MAIN',
});
const onConfirm = async (formEl: FormInstance | undefined) => {
	if (!formEl) return;
	await formEl.validate((valid, fields) => {
		if (valid) {
			if (episode.value.name === undefined) {
				dialogVisible.value = false;
				return;
			}
			emit('closeWithEpsiode', episode.value);
			episode.value = {};
			dialogVisible.value = false;
		} else {
			console.log('error submit!', fields);
			ElMessage.error(t('module.subject.episode.post.message.hint.validate'));
		}
	});
};

const formLabelWidth = '85px';

const episodeRuleFormRules = reactive<FormRules>({
	sequence: [
		{
			required: true,
			message: t(
				'module.subject.episode.post.message.episode.form-rule.sequence.required'
			),
			trigger: 'blur',
		},
		{
			type: 'number',
			message: t(
				'module.subject.episode.post.message.episode.form-rule.sequence.type'
			),
			trigger: 'blur',
		},
	],
	air_time: [
		{
			type: 'date',
			required: true,
			message: t(
				'module.subject.episode.post.message.episode.form-rule.air_time.required'
			),
			trigger: 'change',
		},
	],
	group: [{ required: true }],
	name: [
		{
			required: true,
			message: t(
				'module.subject.episode.post.message.episode.form-rule.name.required'
			),
			trigger: 'blur',
		},
		{
			min: 1,
			max: 100,
			message: t(
				'module.subject.episode.post.message.episode.form-rule.name.length'
			),
			trigger: 'blur',
		},
	],
});

const episodeElFormRef = ref<FormInstance>();
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		:title="t('module.subject.episode.post.title')"
		@close="onClose"
	>
		<el-form
			ref="episodeElFormRef"
			:rules="episodeRuleFormRules"
			:model="episode"
		>
			<el-form-item
				:label="t('module.subject.episode.post.label.air_time')"
				prop="air_time"
				:label-width="formLabelWidth"
			>
				<el-date-picker
					v-model="episode.air_time"
					type="date"
					:placeholder="
						t('module.subject.episode.post.date-picker.placeholder')
					"
				/>
			</el-form-item>
			<el-form-item
				:label="t('module.subject.episode.post.label.group')"
				prop="group"
				:label-width="formLabelWidth"
				width="110px"
				show-overflow-tooltip
			>
				<el-select
					v-model="episode.group"
					clearable
					:placeholder="t('module.subject.episode.post.select.placeholder')"
				>
					<el-option
						v-for="item in episodeGroups"
						:key="item"
						:label="episodeGroupLabelMap.get(item)"
						:value="item"
					/>
				</el-select>
			</el-form-item>
			<el-form-item
				:label="t('module.subject.episode.post.label.name')"
				:label-width="formLabelWidth"
				prop="name"
			>
				<el-input v-model="episode.name" />
			</el-form-item>
			<el-form-item
				:label="t('module.subject.episode.post.label.name_cn')"
				:label-width="formLabelWidth"
			>
				<el-input v-model="episode.name_cn" />
			</el-form-item>
			<el-form-item
				:label="t('module.subject.episode.post.label.sequence')"
				prop="sequence"
				:label-width="formLabelWidth"
			>
				<el-input v-model.number="episode.sequence" type="text" />
			</el-form-item>
			<el-form-item
				:label="t('module.subject.episode.post.label.description')"
				:label-width="formLabelWidth"
			>
				<el-input
					v-model="episode.description"
					:autosize="{ minRows: 3 }"
					maxlength="10000"
					rows="3"
					show-word-limit
					type="textarea"
				/>
			</el-form-item>
		</el-form>
		<template #footer>
			<span>
				<el-button plain @click="onConfirm(episodeElFormRef)">
					{{ t('module.subject.episode.post.footer.button.confirm') }}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
