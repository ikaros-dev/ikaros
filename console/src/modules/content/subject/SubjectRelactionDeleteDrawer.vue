<script setup lang="ts">
import {
	Subject,
	SubjectRelation,
	SubjectRelationRelationTypeEnum,
} from '@runikaros/api-client';
import { computed, ref } from 'vue';
import { ElDrawer, ElTable, ElTableColumn } from 'element-plus';
import { useSubjectStore } from '@/stores/subject';

const subjectStore = useSubjectStore();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		masterSubjectId: number;
		relationSubjects: SubjectRelation[];
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
	(event: 'selectionsChange', subjectRelations: SubjectRelation[]);
}>();

const drawerVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const handleClose = () => {
	emit('selectionsChange', selectionSubjectReactions.value);
	drawerVisible.value = false;
};

const selectionSubjectMap = new Map<string, number[]>();
const selectionSubjectReactions = ref<SubjectRelation[]>([]);
const handleSelectionChange = (selection) => {
	// console.log('selection', selection);
	selectionSubjectMap.clear();
	selection.forEach((sub) => {
		let ids: number[] = selectionSubjectMap.get(sub.type) as number[];
		if (ids) {
			ids.push(sub.id);
		} else {
			ids = [];
			ids.push(sub.id);
			selectionSubjectMap.set(sub.type, ids);
		}
	});
	// console.log('selectionSubjectIdSet', selectionSubjectIdSet);
	selectionSubjectReactions.value = [];
	selectionSubjectMap.forEach((ids, type) => {
		let subRel: SubjectRelation = {
			subject: props.masterSubjectId,
			relation_type: type as SubjectRelationRelationTypeEnum,
			relation_subjects: new Set(ids),
		};
		selectionSubjectReactions.value.push(subRel);
	});
	// console.log('selectionSubjectReactions', selectionSubjectReactions);
};

const subjectIdMapCache = new Map<number, Subject>();
const subjects = ref<Subject[]>([]);
const onOpen = async () => {
	let ids: number[] = [];
	props.relationSubjects.forEach((subRel) => {
		subRel.relation_subjects.forEach((id) => ids.push(id));
	});
	subjects.value = [];

	await ids.forEach(async (id) => {
		let sub = subjectIdMapCache.get(id);
		if (!sub) {
			sub = await subjectStore.getSubjectById(id);
			subjectIdMapCache.set(id, sub);
		}
		subjects.value.push(sub);
	});
};
</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		title="条目选择"
		direction="rtl"
		:before-close="handleClose"
		size="45%"
		@open="onOpen"
	>
		<el-table
			:data="subjects"
			style="width: 100%"
			@selection-change="handleSelectionChange"
		>
			<el-table-column type="selection" width="50" show-overflow-tooltip />
			<el-table-column prop="id" label="ID" width="100" show-overflow-tooltip />
			<el-table-column prop="name" label="名称" show-overflow-tooltip />
			<el-table-column prop="name_cn" label="中文名称" show-overflow-tooltip />
			<el-table-column
				prop="type"
				label="类型"
				width="100"
				show-overflow-tooltip
			/>
		</el-table>
	</el-drawer>
</template>

<style lang="scss" scoped></style>
