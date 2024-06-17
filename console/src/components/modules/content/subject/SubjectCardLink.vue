<script setup lang="ts">
import SubjectCard from './SubjectCard.vue';

const props = withDefaults(
	defineProps<{
		id: number;
		name: string;
		nameCn?: string;
		cover: string;
		percentage: number;
	}>(),
	{
		id: -1,
		name: '',
		nameCn: '',
		cover: '',
		percentage: 0,
	}
);
</script>

<template>
	<router-link :to="'/subjects/subject/details/' + props.id">
		<SubjectCard
			:name="props.name"
			:name-cn="props.nameCn"
			:cover="props.cover"
			:percentage="props.percentage"
		/>
	</router-link>
</template>

<style lang="scss" scoped></style>
