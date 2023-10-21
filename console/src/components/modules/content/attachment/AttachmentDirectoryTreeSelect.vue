<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Attachment } from '@runikaros/api-client';
import { computed } from 'vue';
import { ElTreeSelect } from 'element-plus';

const props = withDefaults(
	defineProps<{
		targetDirid: number;
	}>(),
	{
		targetDirid: 0,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:targetDirid', targetDirid: number): void;
}>();

const targetDirectoryId = computed({
	get() {
		return props.targetDirid;
	},
	set(value) {
		emit('update:targetDirid', value);
	},
});
const dirTreeProps = {
	label: 'label',
	children: 'children',
	isLeaf: 'isLeaf',
};
interface DirNode {
	value: number;
	label: string;
	isLeaf?: boolean;
}
const loadDirectoryNodes = async (node, resolve) => {
	// console.log('node', node);
	// console.log('node.data.value', node.data.value);
	let parentId = node.data.value;
	if (!parentId) {
		parentId = 0;
	}
	if (node.isLeaf) return resolve([]);
	const { data } = await apiClient.attachment.listAttachmentsByCondition({
		type: 'Directory',
		parentId: parentId as any as string,
	});
	const attachments: Attachment[] = data.items;
	const dirNodes: DirNode[] = attachments.map((attachment) => {
		let node: DirNode = {
			value: attachment.id as number,
			label: attachment.name as string,
		};
		return node;
	});
	resolve(dirNodes);
};

const onClear = () => {
	emit('update:targetDirid', 0);
};
</script>

<template>
	<el-tree-select
		v-model="targetDirectoryId"
		lazy
		clearable
		style="width: 100%"
		check-strictly
		:load="loadDirectoryNodes"
		:props="dirTreeProps"
		@clear="onClear"
	/>
</template>

<style lang="scss" scoped></style>
