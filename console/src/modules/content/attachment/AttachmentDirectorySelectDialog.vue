<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Attachment } from '@runikaros/api-client';
import { ref, computed } from 'vue';
import { ElButton, ElDialog, ElTreeSelect } from 'element-plus';

const props = withDefaults(
	defineProps<{
		visible: boolean;
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
	// eslint-disable-next-line no-unused-vars
	(event: 'closeWithTargetDirId', targetDirid: number): void;
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

const targetDirectoryId = ref(0);
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
	console.log('node', node);
	console.log('node.data.value', node.data.value);
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

const onDirectorySelectDialogButtonClick = async () => {
	if (!targetDirectoryId.value) {
		return;
	}
	emit('closeWithTargetDirId', targetDirectoryId.value);
	dialogVisible.value = false;
};
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		style="width: 50%"
		title="选择目录"
		@close="onClose"
	>
		<el-tree-select
			v-model="targetDirectoryId"
			lazy
			style="width: 100%"
			check-strictly
			:load="loadDirectoryNodes"
			:props="dirTreeProps"
		/>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="dialogVisible = false">返回</el-button>
				<el-button type="primary" @click="onDirectorySelectDialogButtonClick">
					提交
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
