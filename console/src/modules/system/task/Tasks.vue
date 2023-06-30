<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { apiClient } from '@/utils/api-client';
import { TaskEntity } from '@runikaros/api-client';
import {
	ElButton,
	ElCol,
	ElForm,
	ElFormItem,
	ElInput,
	ElOption,
	ElPagination,
	ElRow,
	ElSelect,
	ElTable,
	ElTableColumn,
} from 'element-plus';
import { base64Encode } from '@/utils/string-util';
import router from '@/router';

const findTaskCondition = ref({
	page: 1,
	size: 10,
	total: 10,
	name: '',
	status: undefined,
});

const tasks = ref<TaskEntity[]>();
const fetchTasks = async () => {
	const { data } = await apiClient.task.listTasksByCondition({
		page: findTaskCondition.value.page,
		size: findTaskCondition.value.size,
		name: base64Encode(findTaskCondition.value.name),
		status: findTaskCondition.value.status,
	});
	tasks.value = data.items;
	findTaskCondition.value.size = data.size;
	findTaskCondition.value.page = data.page;
	findTaskCondition.value.total = data.total;
};

const onTaskStatusSelectChange = (val) => {
	findTaskCondition.value.status = val;
	fetchTasks();
};

const onCurrentPageChange = (val: number) => {
	findTaskCondition.value.page = val;
	fetchTasks();
};

const onSizeChange = (val: number) => {
	findTaskCondition.value.size = val;
	fetchTasks();
};

const showTaskDetails = (task) => {
	// console.log(task);
	router.push('/tasks/task/details/' + task.id);
};

onMounted(fetchTasks);
</script>

<template>
	<el-row :gutter="10">
		<el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12">
			<el-form :inline="true" :model="findTaskCondition">
				<el-form-item label="任务名称">
					<el-input
						v-model="findTaskCondition.name"
						placeholder="模糊匹配回车搜索"
						clearable
						@change="fetchTasks"
					/>
				</el-form-item>

				<el-form-item label="任务状态">
					<el-select
						v-model="findTaskCondition.status"
						clearable
						style="width: 90px"
						@change="onTaskStatusSelectChange"
					>
						<el-option label="运行" value="RUNNING" />
						<el-option label="完成" value="FINISH" />
						<el-option label="取消" value="CANCEL" />
						<el-option label="失败" value="FAIL" />
						<el-option label="新建" value="CREATE" />
					</el-select>
				</el-form-item>
			</el-form>
		</el-col>

		<el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12">
			<el-pagination
				v-model:page-size="findTaskCondition.size"
				v-model:current-page="findTaskCondition.page"
				background
				:total="findTaskCondition.total"
				layout="total, sizes, prev, pager, next, jumper"
				@current-change="onCurrentPageChange"
				@size-change="onSizeChange"
			/>
		</el-col>
	</el-row>
	<el-row>
		<el-col>
			<el-table
				:data="tasks"
				stripe
				style="width: 100%"
				@row-dblclick="showTaskDetails"
			>
				<el-table-column prop="id" label="ID" width="80" sortable />
				<el-table-column prop="name" label="名称" width="200"></el-table-column>
				<el-table-column prop="status" label="状态"></el-table-column>
				<el-table-column
					prop="createTime"
					label="创建时间"
					sortable
				></el-table-column>
				<el-table-column prop="endTime" label="结束时间"></el-table-column>
				<el-table-column prop="index" label="完成量"></el-table-column>
				<el-table-column prop="total" label="总量"></el-table-column>
				<el-table-column label="操作" width="300">
					<template #default="scoped">
						<el-button plain @click="showTaskDetails(scoped.row)"
							>详情
						</el-button>
					</template>
				</el-table-column>
			</el-table>
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
