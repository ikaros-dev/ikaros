<script setup lang="ts">
import { computed, ref } from 'vue';
import {
	ElDialog,
	ElForm,
	ElFormItem,
	ElButton,
	ElInput,
	ElSelect,
	ElOption,
	ElMessage,
} from 'element-plus';
import { Tickets } from '@element-plus/icons-vue';
import SubjectSelectDrawer from './SubjectSelectDrawer.vue';
import { apiClient } from '@/utils/api-client';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		masterSubjectId: number;
	}>(),
	{
		visible: false,
		masterSubjectId: -1,
	}
);

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

const onClose = () => {
	dialogVisible.value = false;
	emit('close');
};
const slaveSubjectIdsStr = ref('[]');
const selectSubjectReactionType = ref();

const subjectSelectDrawerVisible = ref(false);

const onSelectionsChange = (set) => {
	let arr: number[] = [];
	set.forEach((v) => arr.push(v));
	slaveSubjectIdsStr.value = JSON.stringify(arr);
};

const reqCreateRelactionBtnLoading = ref(false);
const reqCreateRelaction = async () => {
	if (slaveSubjectIdsStr.value === '[]') {
		return;
	}
	reqCreateRelactionBtnLoading.value = true;
	await apiClient.subjectRelation.createSubjectRelation({
		subjectRelation: {
			subject: props.masterSubjectId as number,
			relation_subjects: JSON.parse(slaveSubjectIdsStr.value),
			relation_type: selectSubjectReactionType.value,
		},
	});
	ElMessage.success('添加条目关系成功');
	reqCreateRelactionBtnLoading.value = false;
	onClose();
};
</script>

<template>
	<subject-select-drawer
		v-model:visible="subjectSelectDrawerVisible"
		@selections-change="onSelectionsChange"
	/>
	<el-dialog
		v-model="dialogVisible"
		style="width: 40%"
		title="条目关系新增"
		@close="onClose"
	>
		<el-form label-width="100px" style="max-width: 460px">
			<el-form-item label="主条目">
				<el-input disabled :value="props.masterSubjectId" />
			</el-form-item>
			<el-form-item label="副条目">
				<el-input
					v-model="slaveSubjectIdsStr"
					disabled
					placeholder="副条目ID，英文逗号隔开"
				>
					<template #append>
						<el-button
							:icon="Tickets"
							@click="subjectSelectDrawerVisible = true"
						/>
					</template>
				</el-input>
			</el-form-item>
			<el-form-item label="关系类型">
				<el-select v-model="selectSubjectReactionType" clearable>
					<el-option label="后传" value="AFTER" />
					<el-option label="前传" value="BEFORE" />
					<el-option label="相同世界观" value="SAME_WORLDVIEW" />
					<el-option label="OST" value="ORIGINAL_SOUND_TRACK" />
					<el-option label="动漫" value="ANIME" />
					<el-option label="漫画" value="COMIC" />
					<el-option label="游戏" value="GAME" />
					<el-option label="音声" value="MUSIC" />
					<el-option label="小说" value="NOVEL" />
					<el-option label="三次元" value="REAL" />
					<el-option label="其它" value="OTHER" />
				</el-select>
			</el-form-item>
		</el-form>
		<template #footer>
			<span>
				<el-button @click="dialogVisible = false">取消</el-button>
				<el-button
					type="primary"
					:loading="reqCreateRelactionBtnLoading"
					@click="reqCreateRelaction"
				>
					关联
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
