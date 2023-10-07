<script setup lang="ts">
import { computed } from 'vue';
import { Subject } from '@runikaros/api-client';
import {
	ElDialog,
	ElTabs,
	ElTabPane,
	ElDescriptions,
	ElDescriptionsItem,
} from 'element-plus';
import { ref } from 'vue';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		subject: Subject;
	}>(),
	{
		visible: false,
		subject: undefined,
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

const onClose = () => {
	emit('close');
};

const activeTabName = ref('AFTER');
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		title="条目关系"
		fullscreen
		@close="onClose"
	>
		<el-descriptions direction="vertical" :column="5" size="large" border>
			<el-descriptions-item label="ID" :span="1">
				{{ props.subject.id }}
			</el-descriptions-item>
			<el-descriptions-item label="名称" :span="1">
				{{ props.subject.name }}
			</el-descriptions-item>
			<el-descriptions-item label="中文名称" :span="1">
				{{ props.subject.name_cn }}
			</el-descriptions-item>
			<el-descriptions-item label="放送时间" :span="1">
				{{ props.subject.airTime }}
			</el-descriptions-item>
			<el-descriptions-item label="NSFW" :span="1">
				{{ props.subject.nsfw }}
			</el-descriptions-item>
			<el-descriptions-item label="介绍" :span="5">
				{{ props.subject.summary }}
			</el-descriptions-item>
		</el-descriptions>

		<br />

		<el-tabs v-model="activeTabName">
			<el-tab-pane label="动漫" name="ANIME">动漫</el-tab-pane>
			<el-tab-pane label="漫画" name="COMIC">漫画</el-tab-pane>
			<el-tab-pane label="游戏" name="GAME">Role</el-tab-pane>
			<el-tab-pane label="音声" name="MUSIC">Task</el-tab-pane>
			<el-tab-pane label="小说" name="NOVEL">Task</el-tab-pane>
			<el-tab-pane label="三次元" name="REAL">Task</el-tab-pane>
			<el-tab-pane label="前传" name="BEFORE">Task</el-tab-pane>
			<el-tab-pane label="后传" name="AFTER">Task</el-tab-pane>
			<el-tab-pane label="相同世界观" name="SAME_WORLDVIEW">Task</el-tab-pane>
			<el-tab-pane label="OST" name="ORIGINAL_SOUND_TRACK">Task</el-tab-pane>
			<el-tab-pane label="其它" name="OTHER">Task</el-tab-pane>
		</el-tabs>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
