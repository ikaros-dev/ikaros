<script setup lang="ts">
import { SubjectCollectionTypeEnum } from '@runikaros/api-client';
import { computed, ref } from 'vue';
import {
	scoreColors,
	scoreTexts,
	subjectCollectTypeAliasMap,
} from '@/modules/common/constants';
import {
	ElButton,
	ElDialog,
	ElMessage,
	ElRadioButton,
	ElRadioGroup,
	ElRate,
	ElInput,
} from 'element-plus';
import { apiClient } from '@/utils/api-client';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		subjectId: string;
	}>(),
	{
		visible: false,
		subjectId: undefined,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

// const collect = ref<SubjectCollection>();
const collectType = ref<SubjectCollectionTypeEnum>();
const score = ref(0);
const comment = ref('');

const onSubjectCollectionSubmit = async () => {
	await apiClient.collectionSubject.collectSubject({
		subjectId: props.subjectId as string,
		type: collectType.value as 'WISH' | 'DOING' | 'DONE' | 'SHELVE' | 'DISCARD',
		score: score.value,
		comment: comment.value,
	});
	ElMessage.success('收藏成功');
	dialogVisible.value = false;
};
</script>

<template>
	<el-dialog v-model="dialogVisible" title="条目收藏盒子" width="30%">
		<el-radio-group v-model="collectType">
			<el-radio-button
				v-for="type in Object.values(SubjectCollectionTypeEnum)"
				:key="type"
				:label="type"
				border
			>
				{{ subjectCollectTypeAliasMap.get(type) }}
			</el-radio-button>
		</el-radio-group>

		<br />
		<el-rate
			v-model="score"
			clearable
			show-text
			:max="10"
			:colors="scoreColors"
			:texts="scoreTexts"
		/>

		<br />
		<el-input
			v-model="comment"
			type="textarea"
			rows="3"
			:autosize="{ minRows: 3 }"
			placeholder="输入您的对条目的评论"
		/>

		<template #footer>
			<el-button @click="dialogVisible = false">返回</el-button>
			<el-button type="primary" @click="onSubjectCollectionSubmit">
				提交
			</el-button>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
