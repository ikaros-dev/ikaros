<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { Subject } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import SubjectSyncDialog from './SubjectSyncDialog.vue';
import { base64Decode, base64Encode } from '@/utils/string-util';
import { useRouter, useRoute } from 'vue-router';
import SubjectCardLink from '@/components/modules/content/subject/SubjectCardLink.vue';
import {
	ElRow,
	ElCol,
	ElForm,
	ElFormItem,
	ElInput,
	ElSelect,
	ElOption,
	ElButton,
	ElPagination,
} from 'element-plus';

const router = useRouter();
const route = useRoute();

const fetchSubjectByRouterQuery = () => {
	// console.log('route.query', route.query);

	if (route.query.name !== undefined) {
		findSubjectsCondition.value.name = base64Decode(route.query.name as string);
	}

	if (route.query.nameCn !== undefined) {
		findSubjectsCondition.value.nameCn = base64Decode(
			route.query.nameCn as string
		);
	}

	if (route.query.nsfw !== undefined) {
		findSubjectsCondition.value.nsfw = route.query.nsfw as unknown as boolean;
	}

	if (route.query.type !== undefined) {
		findSubjectsCondition.value.type = route.query.type as
			| 'ANIME'
			| 'COMIC'
			| 'GAME'
			| 'MUSIC'
			| 'NOVEL'
			| 'REAL'
			| 'OTHER';
	}

	// console.log('findSubjectsCondition', findSubjectsCondition.value);
	fetchSubjects();
};

watch(route, () => {
	fetchSubjectByRouterQuery();
});

interface SubjectsCondition {
	page: number;
	size: number;
	total: number;
	name: string;
	nameCn: string;
	nsfw: boolean;
	type?: 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL' | 'OTHER';
}

const findSubjectsCondition = ref<SubjectsCondition>({
	page: 1,
	size: 10,
	total: 10,
	name: '',
	nameCn: '',
	nsfw: false,
	type: undefined,
});

const toSubjectPost = () => {
	router.push('/subjects/subject/post');
};

const subjects = ref<Subject[]>([]);

const fetchSubjects = async () => {
	const { data } = await apiClient.subject.listSubjectsByCondition({
		page: findSubjectsCondition.value.page,
		size: findSubjectsCondition.value.size,
		name: base64Encode(findSubjectsCondition.value.name),
		nameCn: base64Encode(findSubjectsCondition.value.nameCn),
		nsfw: findSubjectsCondition.value.nsfw,
		type: findSubjectsCondition.value.type,
	});
	findSubjectsCondition.value.page = data.page;
	findSubjectsCondition.value.size = data.size;
	findSubjectsCondition.value.total = data.total;
	subjects.value = data.items as Subject[];
};

const subjectSyncDialogVisible = ref(false);
const onSubjectSyncDialogCloseWithSubjectName = (subjectName: string) => {
	findSubjectsCondition.value.name = subjectName;
	fetchSubjects();
};

watch(findSubjectsCondition.value, () => {
	// console.log('attachmentCondition.value', attachmentCondition.value);
	const query = JSON.parse(JSON.stringify(route.query));
	const name = findSubjectsCondition.value.name as string;
	if (name !== route.query.searchName) {
		query.name = base64Encode(name);
	}
	const nameCn = findSubjectsCondition.value.nameCn as string;
	if (nameCn !== route.query.nameCn) {
		query.nameCn = base64Encode(nameCn);
	}
	const nsfw = findSubjectsCondition.value.nsfw as unknown as boolean;
	if (nsfw !== (route.query.nsfw as unknown as boolean)) {
		query.nsfw = nsfw;
	}
	const type = findSubjectsCondition.value.type;
	if (type !== route.query.type) {
		query.type = type;
	}
	router.push({ path: route.path, query });
});

onMounted(fetchSubjectByRouterQuery);
</script>

<template>
	<SubjectSyncDialog
		v-model:visible="subjectSyncDialogVisible"
		@closeWithSubjectName="onSubjectSyncDialogCloseWithSubjectName"
	/>
	<el-row :gutter="10">
		<el-col :xs="24" :sm="24" :md="24" :lg="20" :xl="20">
			<el-form :inline="true" :model="findSubjectsCondition">
				<el-row :gutter="1">
					<el-col :xs="24" :sm="24" :md="24" :lg="7" :xl="7">
						<el-form-item label="原始名称" style="width: 95%">
							<el-input
								v-model="findSubjectsCondition.name"
								placeholder="原始名称模糊匹配"
								clearable
								@change="fetchSubjects"
							/>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="7" :xl="7">
						<el-form-item label="中文名称" style="width: 95%">
							<el-input
								v-model="findSubjectsCondition.nameCn"
								placeholder="中文名称模糊匹配"
								clearable
								@change="fetchSubjects"
							/>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="5" :xl="5">
						<el-form-item label="NSFW" style="width: 95%">
							<el-select
								v-model="findSubjectsCondition.nsfw"
								clearable
								@change="fetchSubjects"
							>
								<el-option label="是" :value="true" />
								<el-option label="非" :value="false" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="5" :xl="5">
						<el-form-item label="所属类型" style="width: 95%">
							<el-select
								v-model="findSubjectsCondition.type"
								clearable
								@change="fetchSubjects"
							>
								<el-option label="动漫" value="ANIME" />
								<el-option label="漫画" value="COMIC" />
								<el-option label="游戏" value="GAME" />
								<el-option label="音声" value="MUSIC" />
								<el-option label="小说" value="NOVEL" />
								<el-option label="三次元" value="REAL" />
								<el-option label="其它" value="OTHER" />
							</el-select>
						</el-form-item>
					</el-col>
				</el-row>
			</el-form>
		</el-col>

		<el-col
			:xs="24"
			:sm="24"
			:md="24"
			:lg="4"
			:xl="4"
			style="text-align: right"
		>
			<el-button plain @click="subjectSyncDialogVisible = true">
				快速新增
			</el-button>
			<el-button plain @click="toSubjectPost"> 新建条目 </el-button>
		</el-col>
	</el-row>

	<el-row :gutter="10" justify="start" align="middle">
		<el-col
			v-for="subject in subjects"
			:key="subject.id"
			:xs="24"
			:sm="12"
			:md="8"
			:lg="4"
			:xl="4"
		>
			<SubjectCardLink :subject="subject" />
		</el-col>
	</el-row>

	<el-row>
		<el-col
			><el-pagination
				v-model:page-size="findSubjectsCondition.size"
				v-model:current-page="findSubjectsCondition.page"
				background
				:total="findSubjectsCondition.total"
				layout="total, sizes, prev, pager, next, jumper"
				@current-change="fetchSubjects"
				@size-change="fetchSubjects"
		/></el-col>
	</el-row>
</template>

<style lang="scss" scoped>
.container {
	margin: 5px 0;
	border-radius: 5px;
	// border: 1px solid rebeccapurple;
	cursor: pointer;
}
</style>
