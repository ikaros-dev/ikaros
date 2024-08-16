<script setup lang="ts">
import {Subject, SubjectRelation} from '@runikaros/api-client';
import {computed, ref} from 'vue';
import {ElDrawer, ElTable, ElTableColumn} from 'element-plus';
import {useSubjectStore} from '@/stores/subject';
import {useI18n} from 'vue-i18n';

const subjectStore = useSubjectStore();
const { t } = useI18n();

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
	// console.debug('selection', selection);
	// console.debug('props.relationSubjects', props.relationSubjects);
	selectionSubjectMap.clear();
	selectionSubjectReactions.value = [];
	selection.forEach((sub) => {
		let ids: number[] = selectionSubjectMap.get(sub.type) as number[];
		if (ids) {
			ids.push(sub.id);
		} else {
			ids = [];
			ids.push(sub.id);
			selectionSubjectMap.set(sub.type, ids);
		}
		var subjectRelation = props.relationSubjects.find((rel) => {
			var result = false;
			rel.relation_subjects.forEach((relId) => {
				if (relId === sub.id) result = true;
			});
			return result;
		}) as SubjectRelation;
		// console.debug('subjectRelation', subjectRelation);
		selectionSubjectReactions.value.push(subjectRelation);
	});
	// console.debug('selectionSubjectReactions', selectionSubjectReactions);
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
		:title="t('module.subject.relaction.drawer.delete.title')"
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
			<el-table-column
				prop="id"
				:label="t('module.subject.relaction.drawer.delete.table.label.id')"
				width="100"
				show-overflow-tooltip
			/>
			<el-table-column
				prop="name"
				:label="t('module.subject.relaction.drawer.delete.table.label.name')"
				show-overflow-tooltip
			/>
			<el-table-column
				prop="name_cn"
				:label="t('module.subject.relaction.drawer.delete.table.label.name_cn')"
				show-overflow-tooltip
			/>
			<el-table-column
				prop="type"
				:label="t('module.subject.relaction.drawer.delete.table.label.type')"
				width="100"
				show-overflow-tooltip
			/>
		</el-table>
	</el-drawer>
</template>

<style lang="scss" scoped></style>
