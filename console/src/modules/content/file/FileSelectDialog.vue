<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import type { FileEntity } from '@runikaros/api-client';
import FileFragmentUploadDrawer from './FileFragmentUploadDrawer.vue';
import FileDeatilDrawer from './FileDeatilDrawer.vue';
import { ElMessage } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import Utf8 from 'crypto-js/enc-utf8';
import Base64 from 'crypto-js/enc-base64';

const fileUploadDrawerVisible = ref(false);

// eslint-disable-next-line no-unused-vars
const onFileUploadDrawerClose = (firstFile) => {
	fileUploadDrawerVisible.value = false;
	// console.log('receive firstFile:', firstFile);
	fetchFiles();
};

const findFilesCondition = ref({
	page: 1,
	size: 10,
	total: 10,
	fileName: undefined,
	place: undefined,
	type: undefined,
});

const base64Encode = (raw: string | undefined): string => {
	if (raw === undefined) {
		return '';
	}
	const word = Utf8.parse(raw);
	return Base64.stringify(word);
};

const files = ref<FileEntity[]>([]);

const fetchFiles = async () => {
	const { data } = await apiClient.file.listFilesByCondition({
		page: findFilesCondition.value.page,
		size: findFilesCondition.value.size,
		fileName: base64Encode(findFilesCondition.value.fileName),
		place: findFilesCondition.value.place,
		type: findFilesCondition.value.type,
	});
	findFilesCondition.value.page = data.page;
	findFilesCondition.value.size = data.size;
	findFilesCondition.value.total = data.total;
	files.value = data.items;
};

const currentShowDetailFile = ref<FileEntity>({});
const fileDetailDrawerVisible = ref(false);
const showFileDeatil = (file) => {
	currentShowDetailFile.value = file;
	fileDetailDrawerVisible.value = true;
};

const handleFileDetailDrawerDelete = () => {
	fetchFiles();
};

const onCurrentPageChange = (val: number) => {
	findFilesCondition.value.page = val;
	fetchFiles();
};

const onSizeChange = (val: number) => {
	findFilesCondition.value.size = val;
	fetchFiles();
};

const handleDelete = async (file: FileEntity) => {
	await apiClient.file
		.deleteFile({
			id: file.id as number,
		})
		.then(() => {
			ElMessage.success('删除文件成功，文件：' + file.id + '-' + file.name);
			window.location.reload();
		})
		.catch((err) => {
			console.error(err);
			ElMessage.error('删除文件失败，异常：' + err.message);
		});
};

const onFilePlaceSelectChange = (val) => {
	findFilesCondition.value.place = val;
	fetchFiles();
};

const onFileTypeSelectChange = (val) => {
	findFilesCondition.value.type = val;
	fetchFiles();
};

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
	(event: 'closeWithUrl', file: FileEntity): void;
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

// eslint-disable-next-line no-unused-vars
const currentSelectFile = ref<FileEntity>({});
const onCurrentSelectChange = (currentRow) => {
	// console.log('currentRow', currentRow);
	// console.log('oldCurrentRow', oldCurrentRow);
	currentSelectFile.value = currentRow;
};

const onConfirm = () => {
	// console.log('currentSelectFile', currentSelectFile.value);
	emit('closeWithUrl', currentSelectFile.value as FileEntity);
	dialogVisible.value = false;
};

onMounted(fetchFiles);
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		title="文件选择器"
		width="85%"
		@close="onClose"
	>
		<FileDeatilDrawer
			v-model:visible="fileDetailDrawerVisible"
			:defineFile="currentShowDetailFile"
			@delete="handleFileDetailDrawerDelete"
		/>

		<FileFragmentUploadDrawer
			v-model:visible="fileUploadDrawerVisible"
			@fileUploadDrawerCloes="onFileUploadDrawerClose"
		/>

		<el-row :gutter="10">
			<el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12">
				<el-form :inline="true" :model="findFilesCondition">
					<el-form-item label="文件名称">
						<el-input
							v-model="findFilesCondition.fileName"
							placeholder="输入文件名称模糊匹配"
							clearable
							@change="fetchFiles"
						/>
					</el-form-item>
					<el-form-item label="文件位置">
						<el-select
							v-model="findFilesCondition.place"
							clearable
							style="width: 90px"
							@change="onFilePlaceSelectChange"
						>
							<el-option label="本地" value="LOCAL" />
						</el-select>
					</el-form-item>
					<el-form-item label="文件类型">
						<el-select
							v-model="findFilesCondition.type"
							clearable
							style="width: 90px"
							@change="onFileTypeSelectChange"
						>
							<el-option label="图片" value="IMAGE" />
							<el-option label="视频" value="VIDEO" />
							<el-option label="文档" value="DOCUMENT" />
							<el-option label="音声" value="VOICE" />
							<el-option label="未知" value="UNKNOWN" />
						</el-select>
					</el-form-item>
				</el-form>
			</el-col>

			<el-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
				<el-pagination
					v-model:page-size="findFilesCondition.size"
					v-model:current-page="findFilesCondition.page"
					background
					:total="findFilesCondition.total"
					layout="total, sizes, prev, pager, next, jumper"
					@current-change="onCurrentPageChange"
					@size-change="onSizeChange"
				/>
			</el-col>

			<el-col
				:xs="24"
				:sm="24"
				:md="24"
				:lg="4"
				:xl="4"
				style="text-align: right"
			>
				<el-button plain @click="fileUploadDrawerVisible = true">
					<el-icon><Upload /></el-icon>
					上传文件
				</el-button>
			</el-col>
		</el-row>

		<el-row>
			<el-col :span="24">
				<el-table
					:data="files"
					style="width: 100%"
					highlight-current-row
					@current-change="onCurrentSelectChange"
					@row-dblclick="showFileDeatil"
				>
					<el-table-column prop="id" label="文件ID" width="80" />
					<el-table-column prop="name" label="文件名称" />
					<el-table-column prop="url" label="文件URL" />
					<el-table-column label="操作" width="200">
						<template #default="scoped">
							<el-button plain @click="showFileDeatil(scoped.row)"
								>详情</el-button
							>

							<el-popconfirm
								title="你确定要删除该文件？"
								confirm-button-text="确定"
								cancel-button-text="取消"
								confirm-button-type="danger"
								@confirm="handleDelete(scoped.row)"
							>
								<template #reference>
									<el-button type="danger">删除</el-button>
								</template>
							</el-popconfirm>
						</template>
					</el-table-column>
				</el-table>
			</el-col>
		</el-row>

		<template #footer>
			<span class="dialog-footer">
				<el-button plain type="info" @click="dialogVisible = false">
					返回
				</el-button>
				<el-button plain @click="onConfirm"> 确认 </el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
