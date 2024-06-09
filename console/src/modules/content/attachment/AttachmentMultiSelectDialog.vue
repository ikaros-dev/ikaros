<script setup lang="ts">
import { Attachment } from '@runikaros/api-client';
import { computed, ref, onMounted } from 'vue';
import { apiClient } from '@/utils/api-client';
import { base64Encode, formatFileSize } from '@/utils/string-util';
import AttachmentFragmentUploadDrawer from './AttachmentFragmentUploadDrawer.vue';
import AttachmentDirectoryTreeSelect from '@/components/modules/content/attachment/AttachmentDirectoryTreeSelect.vue';
import moment from 'moment';
import { isImage, isVideo, isVoice } from '@/utils/file';

import {
	Upload,
	Search,
	Folder,
	Document,
	Picture,
	Headset,
	Film,
} from '@element-plus/icons-vue';
import {
	ElRow,
	ElCol,
	ElInput,
	ElPagination,
	ElButton,
	ElIcon,
	ElTable,
	ElTableColumn,
	ElDialog,
	ElFormItem,
} from 'element-plus';
import { useI18n } from 'vue-i18n';

const {t} = useI18n();

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
	(event: 'closeWithAttachments', attachments: Attachment[]): void;
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

const attachmentCondition = ref({
	page: 1,
	size: 10,
	total: 10,
	parentId: undefined,
	name: '',
	type: 'File',
});

const attachments = ref<Attachment[]>([]);
const fetchAttachments = async () => {
	const { data } = await apiClient.attachment.listAttachmentsByCondition({
		page: attachmentCondition.value.page,
		size: attachmentCondition.value.size,
		name: base64Encode(attachmentCondition.value.name),
		parentId: attachmentCondition.value.parentId as any as string,
		type: attachmentCondition.value.type as 'File' | 'Directory',
	});
	attachments.value = data.items;
	attachmentCondition.value.page = data.page;
	attachmentCondition.value.size = data.size;
	attachmentCondition.value.total = data.total;
};

const onCurrentPageChange = (val: number) => {
	attachmentCondition.value.page = val;
	fetchAttachments();
};

const onSizeChange = (val: number) => {
	attachmentCondition.value.size = val;
	fetchAttachments();
};

const attachmentUploadDrawerVisible = ref(false);
const onFileUploadDrawerClose = () => {
	fetchAttachments();
};

const dateFormat = (row, column) => {
	var date = row[column.property];

	if (date == undefined) {
		return '';
	}

	return moment(date).format('YYYY-MM-DD HH:mm:ss');
};

const selectionAttachments = ref<Attachment[]>([]);

const onSelectionChange = (selections) => {
	// console.log('selections', selections);
	selectionAttachments.value = selections;
};

const onConfirm = () => {
	emit('closeWithAttachments', selectionAttachments.value as Attachment[]);
	dialogVisible.value = false;
};

const onParentDirSelected = async (val) => {
	console.log('val', val);
	if (!val || val === '') {
		attachmentCondition.value.parentId = undefined;
	}
	attachmentCondition.value.page = 1;
	await fetchAttachments();
};
const onSearchNameChange = async () => {
	attachmentCondition.value.page = 1;
	await fetchAttachments();
};

onMounted(fetchAttachments);
</script>

<template>
	<AttachmentFragmentUploadDrawer
		v-model:visible="attachmentUploadDrawerVisible"
		v-model:parentId="attachmentCondition.parentId"
		@fileUploadDrawerCloes="onFileUploadDrawerClose"
	/>

	<el-dialog
		v-model="dialogVisible"
		:title="t('module.attachment.dialog.multi-select.title')"
		width="85%"
		@close="onClose"
	>
		<el-row>
			<el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
				<el-row>
					<el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<el-button plain @click="attachmentUploadDrawerVisible = true">
							<el-icon>
								<Upload />
							</el-icon>
							{{ t('module.attachment.dialog.multi-select.operate.upload') }}
						</el-button>
					</el-col>
					<el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<el-form-item :label="t('module.attachment.dialog.multi-select.parent-dir')">
							<AttachmentDirectoryTreeSelect
								v-model:target-dirid="attachmentCondition.parentId"
								@change="onParentDirSelected"
							/>
						</el-form-item>
					</el-col>
				</el-row>
			</el-col>
			<el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
				<el-input
					v-model="attachmentCondition.name"
					:placeholder="t('module.attachment.dialog.multi-select.search.placeholder')"
					clearable
					@change="onSearchNameChange"
				>
					<template #append>
						<el-button :icon="Search" @click="fetchAttachments" />
					</template>
				</el-input>
			</el-col>
		</el-row>

		<br />

		<el-row
			v-if="attachmentCondition.total > 10 || attachmentCondition.page > 1"
		>
			<el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
				<el-pagination
					v-model:page-size="attachmentCondition.size"
					v-model:current-page="attachmentCondition.page"
					background
					:total="attachmentCondition.total"
					:pager-count="5"
					layout="total, sizes, prev, pager, next, jumper"
					@current-change="onCurrentPageChange"
					@size-change="onSizeChange"
				/>
			</el-col>
		</el-row>

		<br />
		<el-row>
			<el-col :span="24">
				<el-table
					:data="attachments"
					style="width: 100%"
					row-key="id"
					@selection-change="onSelectionChange"
				>
					<el-table-column type="selection" width="60" />
					<!-- <el-table-column prop="id" label="ID" width="60" /> -->
					<el-table-column
						prop="name"
						:label="t('module.attachment.dialog.multi-select.table.colum.label.name')"
						show-overflow-tooltip
						sortable
					>
						<template #default="scoped">
							<el-icon
								size="25"
								style="position: relative; top: 7px; margin: 0 5px 0 0px"
							>
								<Folder v-if="'Directory' === scoped.row.type" />
								<span v-else>
									<Picture v-if="isImage(scoped.row.name)" />
									<Headset v-else-if="isVoice(scoped.row.name)" />
									<Film v-else-if="isVideo(scoped.row.name)" />
									<Document v-else />
								</span>
							</el-icon>
							<!-- &nbsp;&nbsp; -->
							<span>
								{{ scoped.row.name }}
							</span>
						</template>
					</el-table-column>
					<el-table-column prop="path" :label="t('module.attachment.dialog.multi-select.table.colum.label.path')" show-overflow-tooltip />
					<el-table-column
						prop="updateTime"
						:label="t('module.attachment.dialog.multi-select.table.colum.label.update-time')"
						sortable
						width="160"
						:formatter="dateFormat"
					/>
					<el-table-column prop="size" :label="t('module.attachment.dialog.multi-select.table.colum.label.size')" width="130" sortable>
						<template #default="scoped">
							<span v-if="scoped.row.type === 'File'">
								{{ formatFileSize(scoped.row.size) }}
							</span>
						</template>
					</el-table-column>
				</el-table>
			</el-col>
		</el-row>

		<template #footer>
			<span class="dialog-footer">
				<el-button plain type="info" @click="dialogVisible = false">
					{{t('common.button.cancel')}}
				</el-button>
				<el-button plain @click="onConfirm"> 
					{{t('common.button.confirm')}}	
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
