<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Subject } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import SubjectSyncDialog from './SubjectSyncDialog.vue';
import { base64Encode } from '@/utils/string-util';
import { useRouter } from 'vue-router';

const router = useRouter();

const findSubjectsCondition = ref({
	page: 1,
	size: 10,
	total: 10,
	name: '',
	nameCn: '',
	nsfw: undefined,
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

onMounted(fetchSubjects);
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
								<el-option label="是" value="true" />
								<el-option label="非" value="false" />
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
			<router-link :to="'/subjects/subject/details/' + subject.id">
				<el-card
					shadow="hover"
					class="container"
					:body-style="{ padding: '0px' }"
				>
					<template #header>
						<div class="card-header">
							<span>{{ subject?.name }} </span>
							<span class="grey">{{ subject?.name_cn }}</span>
						</div>
					</template>
					<span>
						<img
							:src="subject?.cover"
							style="width: 100%; border-radius: 5px"
						/>
					</span>
				</el-card>
			</router-link>
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

.card-header {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	height: 15px;
	.grey {
		font-size: 10px;
		color: #999;
	}
}
</style>
