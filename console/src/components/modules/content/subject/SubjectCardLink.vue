<script setup lang="ts">
import { Subject } from '@runikaros/api-client';
import SubjectCard from './SubjectCard.vue';

const props = withDefaults(
	defineProps<{
		subject: Subject;
	}>(),
	{
		subject: undefined,
	}
);
</script>

<template>
	<router-link :to="'/subjects/subject/details/' + props.subject.id">
		<SubjectCard :subject="props.subject" />
	</router-link>
</template>

<style lang="scss" scoped></style>
