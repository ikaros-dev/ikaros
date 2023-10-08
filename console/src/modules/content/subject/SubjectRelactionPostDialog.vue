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
} from 'element-plus';
import { Search } from '@element-plus/icons-vue';

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
	emit('close');
};

const slaveSubjectId = ref();
const selectSubjectReactionType = ref();
</script>

<template>
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
				<el-input v-model="slaveSubjectId">
					<template #append>
						<el-button :icon="Search" />
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
				<el-button type="primary" @click="dialogVisible = false">
					关联
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
