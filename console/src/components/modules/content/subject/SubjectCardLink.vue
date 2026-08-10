<script setup lang="ts">
import SubjectCard from './SubjectCard.vue';

const props = withDefaults(
	defineProps<{
		id: string;
		name: string;
		nameCn?: string;
		cover: string;
		percentage: number;
		to?: string;
	}>(),
	{
		id: '',
		name: '',
		nameCn: '',
		cover: '',
		percentage: 0,
		to: '',
	}
);
</script>

<template>
	<router-link :to="props.to || '/subjects/subject/details/' + props.id">
		<SubjectCard
			:name="props.name"
			:name-cn="props.nameCn"
			:cover="props.cover"
			:percentage="props.percentage"
		/>
	</router-link>
</template>

<style lang="scss" scoped></style>
