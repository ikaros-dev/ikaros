<script setup lang="ts">
import {ElCard, ElProgress} from 'element-plus';
import {computed, onMounted, ref, watch} from 'vue';

const props = withDefaults(
	defineProps<{
		name: string;
		nameCn?: string;
		cover: string;
		percentage: number;
	}>(),
	{
		name: '',
		nameCn: '',
		cover: '',
		percentage: 0,
	}
);

watch(
	() => props.percentage,
	(newValue, oldValue) => {
		if (newValue !== oldValue) {
			changeStyleByPercentage(newValue);
		}
	}
);

const cardStyle = ref({
	border: '1px solid #FF0000',
});
const progressColor = ref('#FF0000');

const changeStyleByPercentage = (percentage: number) => {
	if (0 < percentage && percentage <= 10) {
		const color = '#FF0000';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else if (10 < percentage && percentage <= 20) {
		const color = '#E12C27';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else if (20 < percentage && percentage <= 30) {
		const color = '#C34B4F';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else if (30 < percentage && percentage <= 40) {
		const color = '#A56A76';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else if (40 < percentage && percentage <= 50) {
		const color = '#877A9E';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else if (50 < percentage && percentage <= 60) {
		const color = '#6A89C6';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else if (60 < percentage && percentage <= 70) {
		const color = '#4C99EE';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else if (70 < percentage && percentage <= 80) {
		const color = '#35A9FF';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else if (80 < percentage && percentage <= 90) {
		const color = '#1DBAFF';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else if (90 < percentage && percentage <= 100) {
		const color = '#00CCFF';
		cardStyle.value.border = '1px solid ' + color;
		progressColor.value = color;
	} else {
		cardStyle.value.border = '1px solid #FF0000';
		progressColor.value = '#FF0000';
	}
};

const isPercentageExists = computed({
	get() {
		return props.percentage && props.percentage >= 0;
	},
	set() {},
});

onMounted(() => {
	if (isPercentageExists.value) {
		changeStyleByPercentage(props.percentage);
	} else {
		cardStyle.value = { border: '' };
	}
});
</script>

<template>
	<el-card
		shadow="hover"
		class="container"
		:body-style="{ padding: '0px' }"
		:style="cardStyle"
	>
		<template #header>
			<div class="card-header">
				<span>{{ props.name }} </span>
				<span class="grey">{{ props.nameCn }}</span>
			</div>
		</template>
		<el-progress
			v-if="isPercentageExists"
			:percentage="props.percentage"
			:color="progressColor"
			:show-text="false"
		/>
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
