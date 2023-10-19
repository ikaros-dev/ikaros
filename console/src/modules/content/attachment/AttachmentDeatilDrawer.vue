<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Attachment } from '@runikaros/api-client';
import { formatFileSize } from '@/utils/string-util';
import { computed, nextTick, ref } from 'vue';
import {
	ElButton,
	ElCol,
	ElDescriptions,
	ElDescriptionsItem,
	ElDrawer,
	ElInput,
	ElMessage,
	ElPopconfirm,
	ElRow,
} from 'element-plus';
import { useI18n } from 'vue-i18n';
import { isImage, isVideo, isVoice } from '@/utils/file';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		defineFile: Attachment;
	}>(),
	{
		visible: true,
		defineFile: () => ({}),
	}
);

const editable = ref(false);
const deleting = ref(false);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'update:defineFile', file: Attachment): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'delete', file: Attachment): void;
}>();

const drawerVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const file = computed({
	get() {
		return props.defineFile;
	},
	set(value) {
		emit('update:defineFile', value);
	},
});

const handleDelete = async () => {
	try {
		deleting.value = true;
		await apiClient.attachment
			.deleteAttachment({
				id: file.value.id as number,
			})
			.then(() => {
				ElMessage.success(
					t('core.fileDetail.message.event.delete') + ' ' + file.value.name
				);
				emit('delete', file.value);
				drawerVisible.value = false;
			});
	} catch (err) {
		console.error(err);
	} finally {
		setTimeout(() => {
			deleting.value = false;
		}, 400);
	}
};

const nameInput = ref(null);
// eslint-disable-next-line no-unused-vars
const handleEditName = () => {
	editable.value = !editable.value;
	if (editable.value) {
		nextTick(() => {
			// @ts-ignore
			nameInput.value.focus();
		});
	}
};

const handleUpdateName = async () => {
	if (!file.value.name) {
		ElMessage.error(t('core.fileDetail.message.hint.name'));
		window.location.reload();
		return;
	}
	try {
		await apiClient.attachment
			.updateAttachment({
				attachment: file.value,
			})
			.then(() => {
				ElMessage.success(t('core.fileDetail.message.event.updateName'));
			});
	} catch (error) {
		console.error(error);
	} finally {
		editable.value = false;
	}
};

const getCompleteFileUrl = (reactiveUrl: string | undefined): string => {
	var curPageUrl = window.location.href;
	var pathName = window.location.pathname;
	var localhostPath = curPageUrl.substring(0, curPageUrl.indexOf(pathName));
	return localhostPath + reactiveUrl;
};

const handleClose = (done: () => void) => {
	done();
	drawerVisible.value = false;
};
</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		:title="t('core.fileDetail.title')"
		direction="rtl"
		:before-close="handleClose"
		size="40%"
	>
		<el-row>
			<el-col :lg="24" :md="24" :sm="24" :xl="24" :xs="24">
				<div class="attach-detail-img pb-3">
					<a
						v-if="isImage(file.url as string)"
						:href="getCompleteFileUrl(file.url)"
						target="_blank"
					>
						<img
							:src="file.url"
							class="file-detail-preview-img"
							loading="lazy"
						/>
					</a>
					<video
						v-else-if="isVideo(file.url as string)"
						style="width: 100%"
						:src="getCompleteFileUrl(file.url)"
						controls
						preload="metadata"
					>
						{{ t('core.fileDetail.message.hint.videoFormat') }}
					</video>
					<audio
						v-else-if="isVoice(file.url as string)"
						controls
						:src="getCompleteFileUrl(file.url)"
					>
						{{ t('core.fileDetail.message.hint.audioFormat') }}
					</audio>
					<div v-else>{{ t('core.fileDetail.message.hint.preview') }}</div>
				</div>
			</el-col>
		</el-row>

		<br />
		<br />

		<el-row :gutter="24" type="flex">
			<el-col :lg="24" :md="24" :sm="24" :xl="24" :xs="24">
				<el-descriptions
					:title="t('core.fileDetail.descTitle')"
					:column="1"
					size="large"
					border
					direction="vertical"
				>
					<el-descriptions-item label="ID">
						{{ file.id }}
					</el-descriptions-item>
					<el-descriptions-item
						:label="t('core.fileDetail.descItemLabel.name')"
					>
						<span v-if="editable">
							<el-input
								ref="nameInput"
								v-model="file.name"
								@blur="handleUpdateName"
								@pressEnter="handleUpdateName"
							/>
						</span>
						<span v-else @dblclick="handleEditName">
							{{ file.name }}
						</span>
					</el-descriptions-item>
					<el-descriptions-item
						:label="t('core.fileDetail.descItemLabel.size')"
					>
						{{ formatFileSize(file.size) }}
					</el-descriptions-item>
					<el-descriptions-item
						:label="t('core.fileDetail.descItemLabel.updateTime')"
					>
						{{ file.updateTime }}
					</el-descriptions-item>
					<el-descriptions-item v-if="file.url" label="URL">
						<a :href="file.url" target="_blank">{{ file.url }}</a>
					</el-descriptions-item>
					<el-descriptions-item
						v-if="file.fsPath"
						:label="t('core.fileDetail.descItemLabel.fsPath')"
					>
						{{ file.fsPath }}
					</el-descriptions-item>
				</el-descriptions>
			</el-col>
		</el-row>

		<template #footer>
			<el-popconfirm
				title="你确定要删除该文件？"
				confirm-button-text="确定"
				cancel-button-text="取消"
				confirm-button-type="danger"
				@confirm="handleDelete"
			>
				<template #reference>
					<el-button type="danger" :loading="deleting">删除</el-button>
				</template>
			</el-popconfirm>
		</template>
	</el-drawer>
</template>

<style lang="scss" scoped>
.file-detail-preview-img {
	width: 100%;
	height: 100%;
	border-radius: 5px;
}
</style>
