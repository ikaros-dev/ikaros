<script setup lang="ts">
import { Episode } from '@runikaros/api-client';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		episode: Episode;
	}>(),
	{
		visible: false,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});
</script>

<template>
	<el-dialog v-model="dialogVisible">
		<el-descriptions border :column="1">
			<el-descriptions-item label="原始名称">
				{{ episode.name }}
			</el-descriptions-item>
			<el-descriptions-item label="中文名称">
				{{ episode.name_cn }}
			</el-descriptions-item>
			<el-descriptions-item label="放送时间">
				{{ episode.air_time }}
			</el-descriptions-item>
			<el-descriptions-item label="序列号">
				{{ episode.sequence }}
			</el-descriptions-item>
			<el-descriptions-item label="描述">
				{{ episode.description }}
			</el-descriptions-item>
		</el-descriptions>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
