<script setup lang="ts">
import { useRoute } from 'vue-router';
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { TaskEntity } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { ElDescriptions, ElDescriptionsItem, ElProgress } from 'element-plus';

const route = useRoute();

watch(route, () => {
	onTaskIdUpdate();
});

const task = ref<TaskEntity>({});
const fetchTask = async () => {
	const { data } = await apiClient.task.findTaskById({
		id: task.value.id,
	});
	task.value = data;
};

const taskProcessPercentage = ref(0);
const fetchTaskProcess = async () => {
	const { data } = await apiClient.task.findTaskProcessById({
		id: task.value.id,
	});
	taskProcessPercentage.value = data;
};

let timer;
watch(task, (newVal) => {
	const newStatus = newVal.status;
	if ('RUNNING' === newStatus) {
		// 开启定时任务，每隔1000ms拉取进度
		if (timer == undefined) {
			timer = setInterval(() => {
				fetchTaskProcess();
				if (taskProcessPercentage.value === 100) {
					fetchTask();
				}
			}, 1000);
		}
	} else {
		// 关闭定时拉取任务
		if (timer !== undefined) clearInterval(timer);
	}
});

onUnmounted(() => {
	if (timer !== undefined) clearInterval(timer);
});

const onTaskIdUpdate = () => {
	if (route.params.id == undefined) {
		return;
	}
	task.value.id = route.params.id as number;
	fetchTask();
	fetchTaskProcess();
};

onMounted(() => {
	onTaskIdUpdate();
});
</script>

<template>
	<el-descriptions
		title="任务详情"
		direction="vertical"
		:column="5"
		size="large"
		border
	>
		<el-descriptions-item label="ID">{{ task.id }}</el-descriptions-item>
		<el-descriptions-item label="名称">{{ task.name }}</el-descriptions-item>
		<el-descriptions-item label="状态">{{ task.status }}</el-descriptions-item>
		<el-descriptions-item label="创建时间">
			{{ task.createTime }}
		</el-descriptions-item>
		<el-descriptions-item label="结束时间">
			{{ task.endTime }}
		</el-descriptions-item>
		<el-descriptions-item label="进度" :span="5">
			<el-progress
				:text-inside="true"
				:stroke-width="24"
				:percentage="taskProcessPercentage"
				status="success"
			/>
		</el-descriptions-item>
		<el-descriptions-item v-if="task.failMessage" label="失败消息" :span="5">
			{{ task.failMessage }}
		</el-descriptions-item>
	</el-descriptions>
</template>

<style scoped lang="scss"></style>
