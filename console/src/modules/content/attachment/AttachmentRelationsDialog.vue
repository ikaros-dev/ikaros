<script setup lang="ts">
import { computed, ref } from 'vue';
import {
	ElButton,
	ElDialog,
	ElRow,
	ElCol,
	ElTable,
	ElTableColumn,
	ElMessage,
	ElPopconfirm,
} from 'element-plus';
import { Attachment, AttachmentRelation } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { base64Encode } from '@/utils/string-util';
import { Plus } from '@element-plus/icons-vue';
import AttachmentMultiSelectDialog from './AttachmentMultiSelectDialog.vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

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
});

const onClose = () => {
	dialogVisible.value = false;
	masterAttachmentId.value = 0;
	attachmentTableDatas.value = [];
	emit('close');
};

type AttachmentTableColumn = {
	id: number | undefined;
	type: string | undefined;
	masterId: number | undefined;
	relationAtt: Attachment;
};

const attachmentRelations = ref<AttachmentRelation[]>([]);
const attachmentTableDatas = ref<AttachmentTableColumn[]>([]);
const fetchRelations = async () => {
	const { data } = await apiClient.attachmentRelation.findAttachmentRelations({
		attachmentId: masterAttachmentId.value,
		relationType: 'VIDEO_SUBTITLE',
	});
	attachmentTableDatas.value = [];
	attachmentRelations.value = data;
	attachmentRelations.value.forEach(async (attRel) => {
		var relationAtt: Attachment = await fetchAttComplexPathById(
			attRel.relation_attachment_id
		);
		var attTabCol: AttachmentTableColumn = {
			id: attRel.id,
			type: attRel.type,
			masterId: attRel.attachment_id,
			relationAtt: relationAtt,
		};
		attachmentTableDatas.value.push(attTabCol);
	});
};

const fetchAttComplexPathById = async (id: number | undefined) => {
	if (!id) return {};
	const { data } = await apiClient.attachment.getAttachmentById({
		id: id,
	});
	return data;
};

const attachmentMultiSelectDialogVisible = ref(false);
const onAttMultiSelectDialogClose = async (attachments: Attachment[]) => {
	// console.debug("onAttMultiSelectDialogClose attachments", attachments);
	var relationIds: number[] = [];
	attachments.forEach((att) => {
		relationIds.push(att.id as number);
	});
	await apiClient.attachmentRelation.postAttachmentRelations({
		postAttachmentRelationsParam: {
			masterId: masterAttachmentId.value,
			type: 'VIDEO_SUBTITLE',
			relationIds: relationIds,
		},
	});
	ElMessage.success(
		t('module.attachment.dialog.relation.message.create-success')
	);
	await fetchRelations();
};

const relationBtnDeleting = ref(false);
const onAttRelationDelateBtnConfirm = async (
	attRelation: AttachmentTableColumn
) => {
	// console.debug('onAttRelationDelateBtnConfirm attRelation', attRelation);
	await apiClient.attachmentRelation.deleteAttachmentRelation({
		masterAttachmentId: attRelation.masterId as number,
		relAttachmentId: attRelation.relationAtt.id as number,
		type: attRelation.type as 'VIDEO_SUBTITLE',
	});
	ElMessage.success(
		t('module.attachment.dialog.relation.message.delete-success')
	);

	await fetchRelations();
};
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		style="width: 80%"
		:title="t('module.attachment.dialog.relation.title')"
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
					<el-table-column
						prop="type"
						:label="t('module.attachment.dialog.relation.label.type')"
						width="150"
					/>
					<el-table-column
						prop="relationAtt"
						:label="t('module.attachment.dialog.relation.label.relationAtt')"
					>
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
					<el-table-column
						fixed="right"
						:label="t('module.attachment.dialog.relation.label.operate')"
						width="120"
					>
						<template #default="scoped">
							<el-popconfirm
								:title="t('module.attachment.dialog.relation.popconfirm.title')"
								:confirm-button-text="
									t('module.attachment.dialog.relation.popconfirm.confirm')
								"
								:cancel-button-text="
									t('module.attachment.dialog.relation.popconfirm.cancel')
								"
								confirm-button-type="danger"
								width="350px"
								@confirm="onAttRelationDelateBtnConfirm(scoped.row)"
							>
								<template #reference>
									<el-button type="danger" :loading="relationBtnDeleting">
										{{
											t('module.attachment.dialog.relation.popconfirm.submit')
										}}
									</el-button>
								</template>
							</el-popconfirm>
						</template>
					</el-table-column>
				</el-table>
			</el-col>
		</el-row>

		<template #footer>
			<span class="dialog-footer">
				<el-button
					type="primary"
					:icon="Plus"
					@click="attachmentMultiSelectDialogVisible = true"
				>
					{{ t('module.attachment.dialog.relation.footer.add') }}
				</el-button>
				<el-button @click="onClose">
					{{ t('module.attachment.dialog.relation.footer.cancel') }}
				</el-button>
			</span>
		</template>

		<AttachmentMultiSelectDialog
			v-model:visible="attachmentMultiSelectDialogVisible"
			@close-with-attachments="onAttMultiSelectDialogClose"
		/>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
