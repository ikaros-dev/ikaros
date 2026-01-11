<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Attachment } from '@runikaros/api-client';
import { computed } from 'vue';
import { ElTreeSelect } from 'element-plus';
import { attachmentRootId } from '@/modules/common/constants';

const props = withDefaults(
	defineProps<{
		targetDirid: string;
	}>(),
	{
		targetDirid: undefined,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:targetDirid', targetDirid: string | undefined): void;
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
		parentId = attachmentRootId;
	}
	if (node.isLeaf) return resolve([]);
	const { data } = await apiClient.attachment.listAttachmentsByCondition1({
		parentId: parentId  as string,
		page: 1,
		size: 999999,
	});
	const attachments: Attachment[] = data.items;
	const dirNodes: DirNode[] = attachments
		.filter(att => att.type == 'Directory' || att?.type == 'Driver_Directory')	
		.map((attachment) => {
			let node: DirNode = {
				value: attachment.id as number,
				label: attachment.name as string,
			};
			return node;
		});
	resolve(dirNodes);
};

const onClear = () => {
	emit('update:targetDirid', undefined);
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
