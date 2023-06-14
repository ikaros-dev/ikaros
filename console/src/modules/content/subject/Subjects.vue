<script setup lang="ts">
import SubjectCard from './SubjectCard.vue';
</script>

<template>
	<el-row :gutter="10">
		<el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12"> 左边的 </el-col>

		<el-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8"> 中间的 </el-col>

		<el-col
			:xs="24"
			:sm="24"
			:md="24"
			:lg="4"
			:xl="4"
			style="text-align: right"
		>
			<el-button plain> 新建条目 </el-button>
		</el-col>
	</el-row>

	<el-row :gutter="10" justify="start" align="middle">
		<el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="4">
			<SubjectCard />
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="4">
			<SubjectCard />
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
