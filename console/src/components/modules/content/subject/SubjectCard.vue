<script setup lang="ts">
import { ElCard } from 'element-plus';

const props = withDefaults(
	defineProps<{
		name: string;
		nameCn?: string;
		cover: string;
	}>(),
	{
		name: '',
		nameCn: '',
		cover: '',
	}
);
</script>

<template>
	<el-card shadow="hover" class="container" :body-style="{ padding: '0px' }">
		<template #header>
			<div class="card-header">
				<span>{{ props.name }} </span>
				<span class="grey">{{ props.nameCn }}</span>
			</div>
		</template>
		<span class="card-body">
			<img :src="props.cover" />
		</span>
	</el-card>
</template>

<style lang="scss" scoped>
.container {
	// overflow: hidden;
	width: 100%;
	height: auto;
	aspect-ratio: 24/39;
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

	.card-body {
		img {
			width: 100%;
			height: auto;
			border-radius: 5px;
			object-fit: contain;
		}
	}
}
</style>
