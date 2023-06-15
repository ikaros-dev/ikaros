<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Subject, SubjectTypeEnum } from '@runikaros/api-client';

const route = useRoute();

watch(route, () => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	fetchSubjectById();
});

const subject = ref<Subject>({
	id: -1,
	name: '',
	type: SubjectTypeEnum.Other,
	nsfw: true,
	name_cn: '',
});

// eslint-disable-next-line no-unused-vars
const fetchSubjectById = async () => {
	const { data } = await apiClient.subject.searchSubjectById({
		id: subject.value.id as number,
	});
	subject.value = data;
};

onMounted(() => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	fetchSubjectById();
});
</script>

<template>
	{{ subject }}
	<hr />
	<el-row>
		<el-col :span="12">
			<el-input v-model="subject.name"></el-input>
		</el-col>
		<el-col :span="12">
			<el-input v-model="subject.name_cn"></el-input>
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
