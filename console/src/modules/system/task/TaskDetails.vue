<script setup lang="ts">
import { useRoute } from 'vue-router';
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { TaskEntity } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { ElDescriptions, ElDescriptionsItem, ElProgress } from 'element-plus';

const route = useRoute();

watch(route, () => {
	onTaskNameUpdate();
});

const task = ref<TaskEntity>({});
const fetchTaskByName = async () => {
	const { data } = await apiClient.task.findTaskByName({
		name: task.value.name,
	});
	task.value = data;
};

const taskProcessPercentage = ref(0);
const fetchTaskProcess = async () => {
	const { data } = await apiClient.task.findTaskProcess({
		name: task.value.name,
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
					fetchTaskByName();
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

const onTaskNameUpdate = () => {
	if (route.params.name == undefined) {
		return;
	}
	task.value.name = route.params.name as string;
	fetchTaskByName();
	fetchTaskProcess();
};

onMounted(() => {
	onTaskNameUpdate();
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
