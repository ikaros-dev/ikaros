<script setup lang="ts">
import SubjectCard from './SubjectCard.vue';

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
</script>

<template>
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
							/>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="7" :xl="7">
						<el-form-item label="中文名称" style="width: 95%">
							<el-input
								v-model="findSubjectsCondition.nameCn"
								placeholder="中文名称模糊匹配"
								clearable
							/>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="5" :xl="5">
						<el-form-item label="NSFW" style="width: 95%">
							<el-select v-model="findSubjectsCondition.nsfw" clearable>
								<el-option label="是" value="true" />
								<el-option label="非" value="false" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="5" :xl="5">
						<el-form-item label="所属类型" style="width: 95%">
							<el-select v-model="findSubjectsCondition.type" clearable>
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
			<el-button plain @click="toSubjectPost"> 新建条目 </el-button>
		</el-col>
	</el-row>

	<el-row :gutter="10" justify="start" align="middle">
		<el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
			<SubjectCard />
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
		/></el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
