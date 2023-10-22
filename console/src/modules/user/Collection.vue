<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { SubjectCollection } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { useUserStore } from '@/stores/user';
import {
	ElRow,
	ElCol,
	ElFormItem,
	ElSelect,
	ElOption,
	ElPagination,
	ElForm,
} from 'element-plus';
import SubjectCardLink from '@/components/modules/content/subject/SubjectCardLink.vue';

const userStore = useUserStore();

// eslint-disable-next-line no-unused-vars
const findSubjectCollection = ref({
	page: 1,
	size: 12,
	total: 0,
	type: undefined,
	isPrivate: undefined,
});

const subjectCollections = ref<SubjectCollection[]>([]);

const fetchCollections = async () => {
	const { data } = await apiClient.subjectCollection.findSubjectCollections({
		userId: userStore.currentUser?.entity?.id as number,
		page: findSubjectCollection.value.page,
		size: findSubjectCollection.value.size,
		type: findSubjectCollection.value.type,
		isPrivate: findSubjectCollection.value.isPrivate,
	});
	findSubjectCollection.value.page = data.page;
	findSubjectCollection.value.size = data.size;
	findSubjectCollection.value.total = data.total;
	subjectCollections.value = data.items as SubjectCollection[];
};

onMounted(fetchCollections);
</script>

<template>
	<el-row :gutter="10">
		<el-col :xs="24" :sm="24" :md="24" :lg="20" :xl="20">
			<el-form :inline="true" :model="findSubjectCollection">
				<el-row :gutter="1">
					<el-col :xs="24" :sm="24" :md="24" :lg="5" :xl="5">
						<el-form-item label="私有" style="width: 95%">
							<el-select
								v-model="findSubjectCollection.isPrivate"
								clearable
								@change="fetchCollections"
							>
								<el-option label="是" value="true" />
								<el-option label="非" value="false" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="5" :xl="5">
						<el-form-item label="收藏类型" style="width: 95%">
							<el-select
								v-model="findSubjectCollection.type"
								clearable
								@change="fetchCollections"
							>
								<el-option label="想看" value="WISH" />
								<el-option label="在看" value="DOING" />
								<el-option label="看完" value="DONE" />
								<el-option label="搁置" value="SHELVE" />
								<el-option label="抛弃" value="DISCARD" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="14" :xl="14">
						<el-pagination
							v-model:page-size="findSubjectCollection.size"
							v-model:current-page="findSubjectCollection.page"
							background
							:total="findSubjectCollection.total"
							layout="total, sizes, prev, pager, next, jumper"
							@current-change="fetchCollections"
							@size-change="fetchCollections"
						/>
					</el-col>
				</el-row>
			</el-form>
		</el-col>
	</el-row>
	<!-- <el-row>
		<p>{{ subjectCollections }}</p>
	</el-row> -->

	<el-row :gutter="10" justify="start" align="middle">
		<el-col
			v-for="subjectCollection in subjectCollections"
			:key="subjectCollection.id"
			:xs="24"
			:sm="12"
			:md="8"
			:lg="4"
			:xl="4"
		>
			<subject-card-link
				:id="subjectCollection.subject_id"
				:name="subjectCollection?.name"
				:name-cn="subjectCollection?.name_cn"
				:cover="subjectCollection?.cover"
			/>
		</el-col>
	</el-row>

	<br />

	<el-row>
		<el-col>
			<el-pagination
				v-model:page-size="findSubjectCollection.size"
				v-model:current-page="findSubjectCollection.page"
				background
				:total="findSubjectCollection.total"
				layout="total, sizes, prev, pager, next, jumper"
				@current-change="fetchCollections"
				@size-change="fetchCollections"
			/>
		</el-col>
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
