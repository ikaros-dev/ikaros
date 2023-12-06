<script setup lang="ts">
import { ref, watch } from 'vue';
import { Subject, SubjectTypeEnum } from '@runikaros/api-client';
import { useRoute } from 'vue-router';
import { useSubjectStore } from '@/stores/subject';
import { onMounted } from 'vue';

const route = useRoute();
const subjectStore = useSubjectStore();

watch(route, async () => {
	if (!route.params?.id && route.params?.id === undefined) {
		return;
	}
	//@ts-ignore
	subject.value.id = route.params.id as number;
	await fetchSubjectById();
	// console.log(route.params.id);
});

// eslint-disable-next-line no-unused-vars
const subject = ref<Subject>({
	id: -1,
	name: '',
	type: SubjectTypeEnum.Other,
	nsfw: true,
	name_cn: '',
});

const fetchSubjectById = async () => {
	if (subject.value.id) {
		subject.value = await subjectStore.fetchSubjectById(
			subject.value.id as number
		);
	}
};

const fetchDatas = async () => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	await fetchSubjectById();
};

onMounted(fetchDatas);
</script>

<template>
	<div>Comic Subject Details page</div>
	<div>{{ subject }}</div>
</template>

<style lang="scss" scoped></style>
