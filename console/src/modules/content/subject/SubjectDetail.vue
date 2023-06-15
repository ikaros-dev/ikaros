<script setup lang="ts">
import { apiClient } from '@/utils/api-client';

const props = withDefaults(
	defineProps<{
		subjectId: number;
	}>(),
	{
		subjectId: -1,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:subjectId', subjectId: number): void;
}>();

// eslint-disable-next-line no-unused-vars
const subjectIdRef = computed({
	get() {
		return props.subjectId;
	},
	set(value) {
		emit('update:subjectId', value);
	},
});

const subject = ref({
	name: '',
	nameCn: '',
});

// eslint-disable-next-line no-unused-vars
const fetchSubjectById = async () => {
	// eslint-disable-next-line no-unused-vars
	const { data } = await apiClient.subject.searchSubjectById({
		id: subjectIdRef.value,
	});
	// subject = data;
};
</script>

<template>
	<el-row>
		<el-col span="12">
			<el-input v-model="subject.name"></el-input>
		</el-col>
		<el-col span="12">
			<el-input v-model="subject.nameCn"></el-input>
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
