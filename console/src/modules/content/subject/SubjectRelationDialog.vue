<script setup lang="ts">
import {computed, onMounted, ref} from 'vue';
import {Subject, SubjectRelation, SubjectRelationRelationTypeEnum, SubjectTypeEnum,} from '@runikaros/api-client';
import {apiClient} from '@/utils/api-client';
import {useRoute} from 'vue-router';
import SubjectCardLink from '@/components/modules/content/subject/SubjectCardLink.vue';
import {ElButton, ElCol, ElDescriptions, ElDescriptionsItem, ElDialog, ElRow, ElTabPane, ElTabs,} from 'element-plus';
import SubjectRelationPostDialog from './SubjectRelationPostDialog.vue';
import SubjectRelationDeleteDialog from './SubjectRelationDeleteDialog.vue';
import {useSubjectStore} from '@/stores/subject';
import {useI18n} from 'vue-i18n';

const subjectStore = useSubjectStore();
const { t } = useI18n();
const route = useRoute();

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

const activeTabName = ref('AFTER');
const subject = ref<Subject>({
	id: -1,
	name: '',
	type: SubjectTypeEnum.Other,
	nsfw: true,
	name_cn: '',
});
const loadSubject = async () => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	const { data } = await apiClient.subject.searchSubjectById({
		id: subject.value.id,
	});
	subject.value = data;
	await loadSubjectRelations();
};
const subjectRelations = ref<SubjectRelation[]>([]);
const loadSubjectRelations = async () => {
	const { data } = await apiClient.subjectRelation.getSubjectRelationsById({
		subjectId: subject.value.id as number,
	});
	// console.log('subject relations rsp:', rsp);
	if (data instanceof Array) {
		subjectRelations.value = data;
		loadSubjectRelationTabItems();
		loadTypeRelSubjectMap();
	}
};
interface SubjectRelationTabItem {
	type:SubjectRelationRelationTypeEnum,
	count:number,
	label:string
}
const subjectRelationTabItems = ref<SubjectRelationTabItem[]>([]);
const loadSubjectRelationTabItems = ()=>{
	if (!(subjectRelations.value instanceof Array)) return;
	// convert count map
	const countMap = new Map<SubjectRelationRelationTypeEnum, number>();
	subjectRelations.value.forEach(e => {
		const type = e.relation_type;
		if (countMap.has(type)) {
			let count = countMap.get(type) as number;
			count++;
			countMap.set(type, count);
		} else {
			countMap.set(type, 1);
		}
	});
	subjectRelationTabItems.value = [];
	countMap.forEach((val, key) => {
		subjectRelationTabItems.value.push({
			type: key,
			label: t('module.subject.relaction.type.' + key)
			+ '(' + val + ')',
			count: val,
		});
	});
	console.debug('subjectRelationTabItems', subjectRelationTabItems.value);
	if (subjectRelationTabItems.value.length > 0) {
		activeTabName.value = subjectRelationTabItems.value[0].label
	}
}
const typeRelSubjectMap = ref<Map<SubjectRelationRelationTypeEnum, Subject[]>>(new Map())
const loadTypeRelSubjectMap = async ()=>{
	if (!(subjectRelations.value instanceof Array)) return;
	typeRelSubjectMap.value.clear();
	await subjectRelations.value.forEach(async (subRel) => {
		const type = subRel.relation_type;
		const relSubs: Set<number> = subRel.relation_subjects;
		let subjects: Subject[] = [];
		await relSubs.forEach(async (id) => {
			let tmpSub = await subjectStore.getSubjectById(id);
			subjects.push(tmpSub);
		});
		typeRelSubjectMap.value.set(type, subjects);
	});
}

const subjectRelationPostDialogVisible = ref(false);

const onSubjectRelationPostDialogClose = async () => {
	await loadSubjectRelations();
	setTimeout(() => {
		window.location.reload();
	}, 500);
};
const onSubjectRelationDeleteDialogClose = async () => {
	await loadSubjectRelations();
	setTimeout(() => {
		window.location.reload();
	}, 500);
};

const subjectRelationDeleteDialogVisible = ref(false);
const subjectId = computed({
	get() {
		return parseInt(subject.value.id + '');
	},
	set(val) {
		subject.value.id = val;
	},
});
onMounted(() => {
	loadSubject();
	loadSubjectRelations();
});
</script>

<template>
	<SubjectRelationPostDialog
		v-model:visible="subjectRelationPostDialogVisible"
		v-model:masterSubjectId="subjectId"
		@close="onSubjectRelationPostDialogClose"
	/>
	<SubjectRelationDeleteDialog
		v-model:visible="subjectRelationDeleteDialogVisible"
		v-model:masterSubjectId="subjectId"
		v-model:relationSubjects="subjectRelations"
		@close="onSubjectRelationDeleteDialogClose"
	/>

	<el-dialog
		v-model="dialogVisible"
		:title="t('module.subject.relaction.dialog.main.title')"
		fullscreen
		@close="onClose"
	>
		<el-descriptions direction="vertical" :column="6" size="large" border>
			<el-descriptions-item
				:label="t('module.subject.relaction.dialog.main.label.id')"
				:span="1"
			>
				{{ subject.id }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.relaction.dialog.main.label.name')"
				:span="1"
			>
				{{ subject.name }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.relaction.dialog.main.label.name_cn')"
				:span="1"
			>
				{{ subject.name_cn }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.relaction.dialog.main.label.air_time')"
				:span="1"
			>
				{{ subject.airTime }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.relaction.dialog.main.label.type')"
				:span="1"
			>
				{{ subject.type }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.relaction.dialog.main.label.nsfw')"
				:span="1"
			>
				{{ subject.nsfw }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.relaction.dialog.main.label.summary')"
				:span="6"
			>
				{{ subject.summary }}
			</el-descriptions-item>
		</el-descriptions>

		<br />

		<el-row>
			<el-col :span="24">
				<el-button @click="subjectRelationPostDialogVisible = true">
					{{ t('module.subject.relaction.dialog.main.button.add') }}
				</el-button>
				<el-button
					type="danger"
					@click="subjectRelationDeleteDialogVisible = true"
				>
					{{ t('module.subject.relaction.dialog.main.button.delete') }}
				</el-button>
			</el-col>
		</el-row>

		<br />

		<el-tabs v-if="subjectRelationTabItems && subjectRelationTabItems.length > 0" v-model="activeTabName" type="border-card" >
			<el-tab-pane
				v-for="item in subjectRelationTabItems"
				:key="item.type"
				:label="item.label"
				:name="item.label"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="sub in (typeRelSubjectMap?.get(item.type))"
						:key="sub.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="sub.id"
							:cover="sub.cover"
							:name="sub.name"
							:name-cn="sub.name_cn"
							:percentage="0"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
		</el-tabs>

	</el-dialog>
</template>

<style lang="scss" scoped></style>
