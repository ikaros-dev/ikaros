<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElButton, ElDialog, ElRow, ElCol, ElTable, ElTableColumn } from 'element-plus';
import { Attachment, AttachmentRelation } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { base64Encode } from '@/utils/string-util';
import { Plus } from '@element-plus/icons-vue';
import AttachmentMultiSelectDialog from './AttachmentMultiSelectDialog.vue';

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
	attachmentTableDatas.value = [];
	emit('close');
};


type AttachmentTableColumn = {
	id:number | undefined,
	type:string | undefined,
	masterId:number | undefined,
	relationAtt:Attachment
};

const attachmentRelations = ref<AttachmentRelation[]>([]);
const attachmentTableDatas = ref<AttachmentTableColumn[]>([]);
const fetchRelations = async () => {
	const { data } = await apiClient.attachmentRelation.findAttachmentRelations({
		attachmentId: masterAttachmentId.value,
		relationType: 'VIDEO_SUBTITLE'
	})
	attachmentRelations.value = data;
	attachmentRelations.value.forEach(async (attRel) => {
		var relationAtt:Attachment = await fetchAttComplexPathById(attRel.relation_attachment_id);
		var attTabCol:AttachmentTableColumn = {
			id: attRel.id,
			type: attRel.type,
			masterId: attRel.attachment_id,
			relationAtt:relationAtt
		}
		attachmentTableDatas.value.push(attTabCol);
	})
}

const fetchAttComplexPathById = async(id:number | undefined)=>{
	if (!id) return {};
	const {data} = await apiClient.attachment.getAttachmentById({
		id: id
	})
	return data;
}

const attachmentMultiSelectDialogVisible = ref(false);
const onAttMultiSelectDialogClose = async (attachments: Attachment[])=>{
	console.debug("onAttMultiSelectDialogClose attachments", attachments)
}
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		style="width: 80%"
		title="附件关系"
		@open="fetchRelations"
		@close="onClose"
	>
	<el-row>
		<el-col :span="24">
			<el-table
				:data="attachmentTableDatas"
				style="width: 100%"
				row-key="id"
				stripe
			>
			<el-table-column prop="id" label="ID" width="60" />
			<el-table-column prop="type" label="类型" width="150" />
			<el-table-column prop="relationAtt" label="相关附件"  >
				<template #default="scoped">
					
					<!-- {{ scoped.row.relationAtt }} -->
					<router-link
						target="_blank"
						:to="
							'/attachments?parentId=' +
							scoped.row.relationAtt.parentId +
							'&name=' +
							base64Encode(encodeURI(scoped.row.relationAtt.name as string))
						"
						>{{ scoped.row.relationAtt.name }}</router-link
					>
				</template>
			</el-table-column>
		
			</el-table>
		</el-col>
	</el-row>

		<template #footer>
			<span class="dialog-footer">
				<el-button type="primary" :icon="Plus" @click="attachmentMultiSelectDialogVisible = true">添加</el-button>
				<el-button @click="onClose">返回</el-button>
			</span>
		</template>

		<AttachmentMultiSelectDialog v-model:visible="attachmentMultiSelectDialogVisible" @close-with-attachments="onAttMultiSelectDialogClose" />
	</el-dialog>
</template>

<style lang="scss" scoped></style>
