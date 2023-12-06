<script setup lang="ts">
import router from '@/router';
import SubjectCard from './SubjectCard.vue';
import { SubjectTypeEnum } from '@runikaros/api-client';

const props = withDefaults(
	defineProps<{
		id: number;
		name: string;
		type: SubjectTypeEnum;
		nameCn?: string;
		cover: string;
	}>(),
	{
		id: -1,
		name: '',
		type: SubjectTypeEnum.Other,
		nameCn: '',
		cover: '',
	}
);

const toSubjectDetailsPage = () => {
	const subjectId = props.id;
	if (!subjectId) return;
	console.debug('subject type', props.type);
	if (SubjectTypeEnum.Comic === props.type) {
		router.push('/subjects/subject/comic/details/' + subjectId);
	} else {
		router.push('/subjects/subject/details/' + subjectId);
	}
};
</script>

<template>
	<div style="cursor: pointer" @click="toSubjectDetailsPage">
		<SubjectCard
			:name="props.name"
			:name-cn="props.nameCn"
			:cover="props.cover"
		/>
	</div>
</template>

<style lang="scss" scoped></style>
