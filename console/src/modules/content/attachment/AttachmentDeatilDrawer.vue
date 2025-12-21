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
import { Edit } from '@element-plus/icons-vue';
import Artplayer from '@/components/video/Artplayer.vue';
import AttachmentRelationsDialog from './AttachmentRelationsDialog.vue';
import { getCompleteFileUrl } from '@/utils/url-tuils';

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
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
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
					t('module.attachment.details.message.event.delete') +
						' ' +
						file.value.name
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
		ElMessage.error(t('module.attachment.details.message.hint.name'));
		window.location.reload();
		return;
	}
	try {
		await apiClient.attachment
			.updateAttachment({
				attachment: file.value,
			})
			.then(() => {
				ElMessage.success(
					t('module.attachment.details.message.event.updateName')
				);
			});
	} catch (error) {
		console.error(error);
	} finally {
		editable.value = false;
	}
};


const handleClose = (done: () => void) => {
	done();
	drawerVisible.value = false;
};

const attachmentRelationsDialogVisible = ref(false);
const onAttachmentRelationsDialogClose = async () => {
	// artplayerRef.value.getVideoSubtitles();
	// artplayerRef.value.reloadArtplayer();
	window.location.reload();
};
const onClose = async () => {
	drawerVisible.value = false;
	emit('close');
};

const artplayer = ref<Artplayer>();
const artplayerRef = ref();
const getArtplayerInstance = (art: Artplayer) => {
	artplayer.value = art;
};

</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		:title="t('module.attachment.details.title')"
		direction="rtl"
		:before-close="handleClose"
		size="88%"
		@close="onClose"
	>
		<el-row>
			<el-col :lg="24" :md="24" :sm="24" :xl="24" :xs="24">
				<div class="attach-detail-img pb-3">
					<a
						v-if="isImage(file.name as string)"
						:href="getCompleteFileUrl(file.url)"
						target="_blank"
					>
						<img
							:src="file.url"
							class="file-detail-preview-img"
							loading="lazy"
						/>
					</a>
					<artplayer
						v-else-if="isVideo(file.name as string)"
						ref="artplayerRef"
						v-model:attachmentId="file.id"
						style="width: 100%"
						@getInstance="getArtplayerInstance"
					/>
					<!-- <video
						
						:src="getCompleteFileUrl(file.url)"
						controls
						preload="metadata"
					>
						{{ t('module.attachment.details.message.hint.videoFormat') }}
					</video> -->
					<audio
						v-else-if="isVoice(file.name as string)"
						controls
						:volume="0.3"
						:src="getCompleteFileUrl(file.url)"
					>
						{{ t('module.attachment.details.message.hint.audioFormat') }}
					</audio>
					<div v-else>
						{{ t('module.attachment.details.message.hint.preview') }}
					</div>
				</div>
			</el-col>
		</el-row>

		<br />
		<br />

		<el-row :gutter="24" type="flex">
			<el-col :lg="24" :md="24" :sm="24" :xl="24" :xs="24">
				<el-descriptions
					:title="t('module.attachment.details.descTitle')"
					:column="1"
					size="large"
					border
					direction="vertical"
				>
					<el-descriptions-item label="ID">
						{{ file.id }}
					</el-descriptions-item>
					<el-descriptions-item
						:label="t('module.attachment.details.descItemLabel.name')"
					>
						<span v-if="editable">
							<el-input
								ref="nameInput"
								v-model="file.name"
								@blur="handleUpdateName"
							>
								<template #append>
									<el-button :icon="Edit" @click="handleUpdateName" />
								</template>
							</el-input>
						</span>
						<span v-else @dblclick="handleEditName">
							{{ file.name }}
						</span>
					</el-descriptions-item>
					<el-descriptions-item
						:label="t('module.attachment.details.descItemLabel.size')"
					>
						{{ formatFileSize(file.size) }}
					</el-descriptions-item>
					<el-descriptions-item
						label="SHA1"
					>
						{{ file.sha1 }}
					</el-descriptions-item>
					<el-descriptions-item
						:label="t('module.attachment.details.descItemLabel.updateTime')"
					>
						{{ file.updateTime }}
					</el-descriptions-item>
					<el-descriptions-item
						v-if="file.path"
						:label="t('module.attachment.details.descItemLabel.path')"
					>
						{{ file.path }}
					</el-descriptions-item>
					<el-descriptions-item v-if="file.url" label="URL">
						<a :href="encodeURI(file.url)" target="_blank">{{ file.url }}</a>
					</el-descriptions-item>
					<el-descriptions-item
						v-if="file.fsPath"
						:label="t('module.attachment.details.descItemLabel.fsPath')"
					>
						{{ file.fsPath }}
					</el-descriptions-item>
				</el-descriptions>
			</el-col>
		</el-row>

		<template #footer>
			<el-button @click="attachmentRelationsDialogVisible = true">
				{{ t('module.attachment.details.button.relation') }}
			</el-button>
			<el-popconfirm
				:title="t('module.attachment.details.popconfirm.title')"
				:confirm-button-text="t('module.attachment.details.popconfirm.confirm')"
				:cancel-button-text="t('module.attachment.details.popconfirm.cancel')"
				confirm-button-type="danger"
				width="350px"
				@confirm="handleDelete"
			>
				<template #reference>
					<el-button type="danger" :loading="deleting">
						{{ t('module.attachment.details.button.delete') }}
					</el-button>
				</template>
			</el-popconfirm>
		</template>

		<AttachmentRelationsDialog
			v-model:visible="attachmentRelationsDialogVisible"
			:attachmentId="file.id"
			@close="onAttachmentRelationsDialogClose"
		/>
	</el-drawer>
</template>

<style lang="scss" scoped>
.file-detail-preview-img {
	width: 100%;
	height: 100%;
	border-radius: 5px;
}
</style>
