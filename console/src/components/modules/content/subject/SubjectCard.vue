<script setup lang="ts">
import { Subject } from '@runikaros/api-client';
import { ElCard } from 'element-plus';

const props = withDefaults(
	defineProps<{
		subject: Subject;
	}>(),
	{
		subject: undefined,
	}
);
</script>

<template>
	<el-card shadow="hover" class="container" :body-style="{ padding: '0px' }">
		<template #header>
			<div class="card-header">
				<span>{{ props.subject?.name }} </span>
				<span class="grey">{{ props.subject?.name_cn }}</span>
			</div>
		</template>
		<span>
			<img
				:src="props.subject?.cover"
				style="width: 100%; border-radius: 5px"
			/>
		</span>
	</el-card>
</template>

<style lang="scss" scoped>
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
