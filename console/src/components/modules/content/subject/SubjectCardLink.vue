<script setup lang="ts">
import SubjectCard from './SubjectCard.vue';

const props = withDefaults(
	defineProps<{
		id: number;
		name: string;
		nameCn?: string;
		cover: string;
	}>(),
	{
		id: -1,
		name: '',
		nameCn: '',
		cover: '',
	}
);
</script>

<template>
	<router-link :to="'/subjects/subject/details/' + props.id">
		<SubjectCard
			:name="props.name"
			:name-cn="props.nameCn"
			:cover="props.cover"
		/>
	</router-link>
</template>

<style lang="scss" scoped></style>
