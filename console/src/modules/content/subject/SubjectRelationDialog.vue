<script setup lang="ts">
import { computed, watch, ref } from 'vue';
import {
	Subject,
	SubjectRelation,
	SubjectTypeEnum,
} from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { useRoute } from 'vue-router';
import SubjectCardLink from '@/components/modules/content/subject/SubjectCardLink.vue';
import {
	ElDialog,
	ElTabs,
	ElTabPane,
	ElDescriptions,
	ElDescriptionsItem,
	ElRow,
	ElCol,
	ElButton,
} from 'element-plus';
import { onMounted } from 'vue';
import SubjectRelationPostDialog from './SubjectRelationPostDialog.vue';
import SubjectRelationDeleteDialog from './SubjectRelationDeleteDialog.vue';
import { useSubjectStore } from '@/stores/subject';
import { useI18n } from 'vue-i18n';

const subjectStore = useSubjectStore();
const { t } = useI18n();
const route = useRoute();
// watch(route, async () => {
// 	if (!route.params?.id && route.params?.id === undefined) {
// 		return;
// 	}
// 	// console.log(route.params.id);
// 	await loadSubject();
// });

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
};
watch(subject, async () => {
	subjectRelations.value = [];
	await loadSubjectRelations();
});
const subjectRelations = ref<SubjectRelation[]>([]);
const loadSubjectRelations = async () => {
	const rsp = await apiClient.subjectRelation.getSubjectRelationsById({
		subjectId: subject.value.id as number,
	});
	// console.log('subject relations rsp:', rsp);
	if (rsp) {
		// console.log('subject relations data:', rsp.data);
		subjectRelations.value = rsp.data as never;
	}
};
const relationAnimes = ref<Subject[]>([]);
const relationComics = ref<Subject[]>([]);
const relationGames = ref<Subject[]>([]);
const relationMusics = ref<Subject[]>([]);
const relationNovels = ref<Subject[]>([]);
const relationReals = ref<Subject[]>([]);
const relationBefores = ref<Subject[]>([]);
const relationAfters = ref<Subject[]>([]);
const relationSWs = ref<Subject[]>([]);
const relationOSTs = ref<Subject[]>([]);
const relationOthers = ref<Subject[]>([]);
watch(subjectRelations, async (newSubjectRelations) => {
	if (!newSubjectRelations || newSubjectRelations.length === 0) {
		relationAnimes.value = [];
		relationComics.value = [];
		relationGames.value = [];
		relationMusics.value = [];
		relationNovels.value = [];
		relationReals.value = [];
		relationBefores.value = [];
		relationAfters.value = [];
		relationSWs.value = [];
		relationOSTs.value = [];
		relationOthers.value = [];
	}
	await newSubjectRelations.forEach(async (subRel: SubjectRelation) => {
		const type = subRel.relation_type;
		const relSubs: Set<number> = subRel.relation_subjects;
		switch (type) {
			case 'ANIME': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationAnimes.value = subjects;
				break;
			}
			case 'COMIC': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationComics.value = subjects;
				break;
			}
			case 'GAME': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationGames.value = subjects;
				break;
			}
			case 'MUSIC': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationMusics.value = subjects;
				break;
			}
			case 'NOVEL': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationNovels.value = subjects;
				break;
			}
			case 'REAL': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationReals.value = subjects;
				break;
			}
			case 'BEFORE': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationBefores.value = subjects;
				break;
			}
			case 'AFTER': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationAfters.value = subjects;
				break;
			}
			case 'SAME_WORLDVIEW': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationSWs.value = subjects;
				break;
			}
			case 'ORIGINAL_SOUND_TRACK': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationOSTs.value = subjects;
				break;
			}
			case 'OTHER': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationOthers.value = subjects;
				break;
			}
			default: {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await subjectStore.getSubjectById(id);
					subjects.push(tmpSub);
				});
				relationOthers.value = subjects;
				break;
			}
		}
	});
});

const subjectRelationPostDialogVisible = ref(false);

const onSubjectRelationPostDialogClose = async () => {
	await loadSubjectRelations();
};
const onSubjectRelationDeleteDialogClose = async () => {
	await loadSubjectRelations();
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
onMounted(loadSubject);
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

		<el-tabs v-model="activeTabName">
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.anime', {
						length: relationAnimes.length,
					})
				"
				name="ANIME"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="anime in relationAnimes"
						:key="anime.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="anime.id"
							:cover="anime.cover"
							:name="anime.name"
							:name-cn="anime.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.comic', {
						length: relationComics.length,
					})
				"
				name="COMIC"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="comic in relationComics"
						:key="comic.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="comic.id"
							:cover="comic.cover"
							:name="comic.name"
							:name-cn="comic.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.game', {
						length: relationGames.length,
					})
				"
				name="GAME"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="game in relationGames"
						:key="game.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="game.id"
							:cover="game.cover"
							:name="game.name"
							:name-cn="game.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.music', {
						length: relationMusics.length,
					})
				"
				name="MUSIC"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="music in relationMusics"
						:key="music.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="music.id"
							:cover="music.cover"
							:name="music.name"
							:name-cn="music.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.novel', {
						length: relationNovels.length,
					})
				"
				name="NOVEL"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="novel in relationNovels"
						:key="novel.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="novel.id"
							:cover="novel.cover"
							:name="novel.name"
							:name-cn="novel.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.real', {
						length: relationReals.length,
					})
				"
				name="REAL"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="real in relationReals"
						:key="real.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="real.id"
							:cover="real.cover"
							:name="real.name"
							:name-cn="real.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.before', {
						length: relationBefores.length,
					})
				"
				name="BEFORE"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="before in relationBefores"
						:key="before.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="before.id"
							:cover="before.cover"
							:name="before.name"
							:name-cn="before.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.after', {
						length: relationAfters.length,
					})
				"
				name="AFTER"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="after in relationAfters"
						:key="after.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="after.id"
							:cover="after.cover"
							:name="after.name"
							:name-cn="after.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.same-worldview', {
						length: relationSWs.length,
					})
				"
				name="SAME_WORLDVIEW"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="sw in relationSWs"
						:key="sw.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="sw.id"
							:cover="sw.cover"
							:name="sw.name"
							:name-cn="sw.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.ost', {
						length: relationOSTs.length,
					})
				"
				name="ORIGINAL_SOUND_TRACK"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="ost in relationOSTs"
						:key="ost.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="ost.id"
							:cover="ost.cover"
							:name="ost.name"
							:name-cn="ost.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
			<el-tab-pane
				:label="
					t('module.subject.relaction.dialog.main.tab.label.other', {
						length: relationOthers.length,
					})
				"
				name="OTHER"
			>
				<el-row :gutter="10" justify="start" align="middle">
					<el-col
						v-for="other in relationOthers"
						:key="other.id"
						:xs="24"
						:sm="12"
						:md="8"
						:lg="4"
						:xl="4"
					>
						<SubjectCardLink
							:id="other.id"
							:cover="other.cover"
							:name="other.name"
							:name-cn="other.name_cn"
						/>
					</el-col>
				</el-row>
			</el-tab-pane>
		</el-tabs>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
