<script setup lang="ts">
import { watch, ref } from 'vue';
import {
	Subject,
	SubjectRelation,
	SubjectTypeEnum,
} from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { useRoute } from 'vue-router';
import SubjectCard from '@/components/modules/content/subject/SubjectCard.vue';
// eslint-disable-next-line no-unused-vars
import SubjectCardLink from '@/components/modules/content/subject/SubjectCardLink.vue';
import {
	ElTabs,
	ElTabPane,
	ElDescriptions,
	ElDescriptionsItem,
	ElRow,
	ElCol,
	ElButton,
} from 'element-plus';
import { onMounted } from 'vue';

const route = useRoute();
watch(route, async () => {
	if (!route.params?.id && route.params?.id === undefined) {
		return;
	}
	// console.log(route.params.id);
	await loadSubject();
});

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
	await loadSubjectRelations();
});
const subjectRelations = ref<SubjectRelation[]>([]);
const loadSubjectRelations = async () => {
	const rsp = await apiClient.subjectRelation.getSubjectRelationsById({
		subjectId: subject.value.id as number,
	});
	// console.log('subject relations rsp:', rsp);
	if (rsp) {
		console.log('subject relations data:', rsp.data);
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
watch(subjectRelations, async () => {
	await subjectRelations.value.forEach(async (subRel: SubjectRelation) => {
		const type = subRel.relation_type;
		const relSubs: Set<number> = subRel.relation_subjects;
		console.log('subRel', subRel);
		switch (type) {
			case 'ANIME': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationAnimes.value = subjects;
				break;
			}
			case 'COMIC': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				console.log('relationComics', relationComics.value);
				relationComics.value = subjects;
				break;
			}
			case 'GAME': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationGames.value = subjects;
				break;
			}
			case 'MUSIC': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationMusics.value = subjects;
				break;
			}
			case 'NOVEL': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationNovels.value = subjects;
				break;
			}
			case 'REAL': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationReals.value = subjects;
				break;
			}
			case 'BEFORE': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationBefores.value = subjects;
				break;
			}
			case 'AFTER': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationAfters.value = subjects;
				break;
			}
			case 'SAME_WORLDVIEW': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationSWs.value = subjects;
				break;
			}
			case 'ORIGINAL_SOUND_TRACK': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationOSTs.value = subjects;
				break;
			}
			case 'OTHER': {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationOthers.value = subjects;
				break;
			}
			default: {
				let subjects: Subject[] = [];
				await relSubs.forEach(async (id) => {
					let tmpSub = await findSubjectById(id);
					subjects.push(tmpSub);
				});
				relationOthers.value = subjects;
				break;
			}
		}
	});
});
const findSubjectById = async (id: number): Promise<Subject> => {
	const { data } = await apiClient.subject.searchSubjectById({ id: id });
	return data;
};

const onTabActive = (pane) => {
	console.log('pane', pane);
	console.log('subject', subject);
};

onMounted(loadSubject);
</script>

<template>
	<el-descriptions direction="vertical" :column="6" size="large" border>
		<el-descriptions-item label="ID" :span="1">
			{{ subject.id }}
		</el-descriptions-item>
		<el-descriptions-item label="名称" :span="1">
			{{ subject.name }}
		</el-descriptions-item>
		<el-descriptions-item label="中文名称" :span="1">
			{{ subject.name_cn }}
		</el-descriptions-item>
		<el-descriptions-item label="放送时间" :span="1">
			{{ subject.airTime }}
		</el-descriptions-item>
		<el-descriptions-item label="类型" :span="1">
			{{ subject.type }}
		</el-descriptions-item>
		<el-descriptions-item label="NSFW" :span="1">
			{{ subject.nsfw }}
		</el-descriptions-item>
		<el-descriptions-item label="介绍" :span="6">
			{{ subject.summary }}
		</el-descriptions-item>
	</el-descriptions>

	<br />

	<el-row>
		<el-col :span="24">
			<el-button>新增</el-button>
			<el-button type="danger">删除</el-button>
		</el-col>
	</el-row>

	<br />

	<el-tabs v-model="activeTabName" @tab-click="onTabActive">
		<el-tab-pane name="ANIME">
			<template #label> 动漫({{ relationAnimes.length }}) </template>
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
					<SubjectCard :subject="anime" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="COMIC">
			<template #label> 漫画({{ relationComics.length }}) </template>
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
					<SubjectCard :subject="comic" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="GAME">
			<template #label> 游戏({{ relationGames.length }}) </template>
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
					<SubjectCard :subject="game" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="MUSIC">
			<template #label> 音声({{ relationMusics.length }}) </template>
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
					<SubjectCard :subject="music" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="NOVEL">
			<template #label> 小说({{ relationNovels.length }}) </template>
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
					<SubjectCard :subject="novel" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="REAL">
			<template #label> 三次元({{ relationReals.length }}) </template>
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
					<SubjectCard :subject="real" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="BEFORE">
			<template #label> 前传({{ relationBefores.length }}) </template>
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
					<SubjectCard :subject="before" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="AFTER">
			<template #label> 后传({{ relationAfters.length }}) </template>
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
					<SubjectCard :subject="after" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="SAME_WORLDVIEW">
			<template #label> 相同世界观({{ relationSWs.length }}) </template>
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
					<SubjectCard :subject="sw" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="ORIGINAL_SOUND_TRACK">
			<template #label> OST({{ relationOSTs.length }}) </template>
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
					<SubjectCard :subject="ost" />
				</el-col>
			</el-row>
		</el-tab-pane>
		<el-tab-pane name="OTHER">
			<template #label> 其它({{ relationOthers.length }}) </template>
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
					<SubjectCard :subject="other" />
				</el-col>
			</el-row>
		</el-tab-pane>
	</el-tabs>
</template>

<style lang="scss" scoped></style>
