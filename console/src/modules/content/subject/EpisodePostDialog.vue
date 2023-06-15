<script setup lang="ts">
import { Episode } from '@runikaros/api-client';
import { ElMessage, FormInstance, FormRules } from 'element-plus';

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

const episode = ref<Episode>({});
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
			ElMessage.error('请检查所填内容是否有必要项缺失。');
		}
	});
};

const formLabelWidth = '85px';

const episodeRuleFormRules = reactive<FormRules>({
	sequence: [
		{ required: true, message: '请指定剧集的序列号', trigger: 'blur' },
		{ type: 'number', message: '序列号必须是一个数字', trigger: 'blur' },
	],
	air_time: [
		{
			type: 'date',
			required: true,
			message: '请选择一个时间',
			trigger: 'change',
		},
	],
	name: [
		{ required: true, message: '请输入剧集原始名称', trigger: 'blur' },
		{ min: 1, max: 100, message: '长度应该在 1 到 100 之间', trigger: 'blur' },
	],
	description: [
		{ required: true, message: '请输入条目介绍', trigger: 'blur' },
		{ min: 5, message: '长度应该大于5', trigger: 'blur' },
	],
});

const episodeElFormRef = ref<FormInstance>();
</script>

<template>
	<el-dialog v-model="dialogVisible" title="新建剧集" @close="onClose">
		<el-form
			ref="episodeElFormRef"
			:rules="episodeRuleFormRules"
			:model="episode"
		>
			<el-form-item
				label="发布时间"
				prop="air_time"
				:label-width="formLabelWidth"
			>
				<el-date-picker
					v-model="episode.air_time"
					type="date"
					placeholder="请选择一天"
				/>
			</el-form-item>
			<el-form-item label="剧集名称" :label-width="formLabelWidth" prop="name">
				<el-input v-model="episode.name" />
			</el-form-item>
			<el-form-item label="中文名称" :label-width="formLabelWidth">
				<el-input v-model="episode.name_cn" />
			</el-form-item>
			<el-form-item label="序列" prop="sequence" :label-width="formLabelWidth">
				<el-input v-model.number="episode.sequence" type="text" />
			</el-form-item>
			<el-form-item
				label="介绍"
				:label-width="formLabelWidth"
				prop="description"
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
				<el-button plain @click="onConfirm(episodeElFormRef)"> 确认 </el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
