<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElButton, ElDialog, ElRow, ElCol, ElTable, ElTableColumn } from 'element-plus';
import { AttachmentRelation } from 'packages/api-client/dist.ts';
import { apiClient } from '@/utils/api-client';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		attachmentId?: number;
	}>(),
	{
		visible: false,
		attachmentId: 0,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'update:attachmentId', attacmentId: number): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'closeWithMasterAttachmentId', attacmentId: number): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const masterAttachmentId = computed({
	get() {
		return props.attachmentId;
	},
	set(value) {
		emit('update:attachmentId', value);
	},
});;

const onClose = () => {
	dialogVisible.value = false;
	masterAttachmentId.value = 0;
	emit('close');
};

const onRelationsSelectDialogButtonClick = async () => {
	if (!masterAttachmentId.value) {
		masterAttachmentId.value = 0;
	}
	emit('closeWithMasterAttachmentId', masterAttachmentId.value);
	dialogVisible.value = false;
};

const attachmentRelations = ref<AttachmentRelation[]>([]);
const fetchRelations = async () => {
	const { data } = await apiClient.attachmentRelation.findAttachmentRelations({
		attachmentId: masterAttachmentId.value,
		relationType: 'VIDEO_SUBTITLE'
	})
	attachmentRelations.value = data;
}
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		style="width: 50%"
		title="附件关系"
		@open="fetchRelations"
		@close="onClose"
	>
	<el-row>
		<el-col :span="24">
			<el-table
				:data="attachmentRelations"
				style="width: 100%"
				row-key="id"
			>
			<el-table-column prop="id" label="ID" width="60" />
			<el-table-column prop="type" label="类型" width="150" />
			<el-table-column prop="relation_attachment_id" label="相关附件"  >
				<template #default="scoped">
					
					{{ scoped.row.relation_attachment_id }}
				</template>
			</el-table-column>
		
			</el-table>
		</el-col>
	</el-row>

		<template #footer>
			<span class="dialog-footer">
				<el-button @click="onClose">返回</el-button>
				<el-button type="primary" @click="onRelationsSelectDialogButtonClick">
					确定
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
