<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Attachment } from '@runikaros/api-client';
import { formatFileSize } from '@/utils/string-util';
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
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
import { Edit } from '@element-plus/icons-vue';
import Artplayer from '@/components/video/Artplayer.vue';
import AttachmentRelationsDialog from './AttachmentRelationsDialog.vue';
import { getCompleteFileUrl } from '@/utils/url-tuils';
import {
	loadMediaFileFormatLookup,
	type MediaFileFormatLookup,
} from '@/utils/media-file-format';

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
	(event: 'update:visible', visible: boolean): void;

	(event: 'update:defineFile', file: Attachment): void;

	(event: 'delete', file: Attachment): void;

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

const mediaFileFormatLookup = ref<MediaFileFormatLookup>();
const mediaFileCategory = computed(() =>
	file.value.name
		? mediaFileFormatLookup.value?.categoryOf(file.value.name)
		: undefined
);
const isSvgFile = computed(
	() =>
		Boolean(file.value.name) &&
		mediaFileFormatLookup.value?.formatOf(file.value.name as string) === 'SVG'
);
const svgPreviewUrl = ref('');
const svgPreviewLoading = ref(false);
const svgPreviewFailed = ref(false);

const revokeSvgPreviewUrl = () => {
	if (svgPreviewUrl.value) {
		URL.revokeObjectURL(svgPreviewUrl.value);
		svgPreviewUrl.value = '';
	}
};

const loadSvgPreview = async () => {
	revokeSvgPreviewUrl();
	svgPreviewFailed.value = false;
	if (!drawerVisible.value || !isSvgFile.value || !file.value.id) return;
	svgPreviewLoading.value = true;
	try {
		const response = await apiClient.attachment.getSvgPreviewById(
			{ id: file.value.id },
			{ responseType: 'blob' }
		);
		const data = response.data as unknown;
		const blob = data instanceof Blob ? data : new Blob([data as BlobPart]);
		svgPreviewUrl.value = URL.createObjectURL(blob);
	} catch (error) {
		svgPreviewFailed.value = true;
		console.error('Load SVG preview failed', error);
	} finally {
		svgPreviewLoading.value = false;
	}
};

let sha1RefreshTimer: ReturnType<typeof setTimeout> | undefined;
const clearSha1RefreshTimer = () => {
	if (sha1RefreshTimer) {
		clearTimeout(sha1RefreshTimer);
		sha1RefreshTimer = undefined;
	}
};
const refreshSha1 = async () => {
	clearSha1RefreshTimer();
	if (
		!drawerVisible.value ||
		file.value.type !== 'Driver_File' ||
		file.value.sha1 ||
		!file.value.id
	) {
		return;
	}
	try {
		const { data } = await apiClient.attachment.getAttachmentById({
			id: file.value.id,
		});
		file.value = data;
	} catch (error) {
		console.error('Refresh attachment SHA-1 failed', error);
	}
	if (!file.value.sha1 && drawerVisible.value) {
		sha1RefreshTimer = setTimeout(refreshSha1, 1000);
	}
};
watch(
	[() => props.visible, () => props.defineFile.id],
	() => {
		clearSha1RefreshTimer();
		if (props.visible) {
			sha1RefreshTimer = setTimeout(refreshSha1, 1000);
		}
	},
	{ immediate: true }
);
watch(
	[
		() => props.visible,
		() => props.defineFile.id,
		() => props.defineFile.name,
		mediaFileFormatLookup,
	],
	loadSvgPreview,
	{ immediate: true }
);
onMounted(() => {
	loadMediaFileFormatLookup()
		.then((lookup) => {
			mediaFileFormatLookup.value = lookup;
		})
		.catch(() => {
			mediaFileFormatLookup.value = undefined;
		});
});
onUnmounted(() => {
	clearSha1RefreshTimer();
	revokeSvgPreviewUrl();
});

const handleDelete = async () => {
	if (!file.value.id) return;
	const attachmentId = file.value.id;
	try {
		deleting.value = true;
		await apiClient.attachment
			.deleteAttachment({
				id: attachmentId,
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

const handleEditName = () => {
	editable.value = !editable.value;
	if (editable.value) {
		nextTick(() => {
			// @ts-expect-error
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

const artplayer = ref<InstanceType<typeof Artplayer>>();
const artplayerRef = ref();
const getArtplayerInstance = (art: any) => {
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
					<div v-if="isSvgFile" class="svg-preview-container">
						<div v-if="svgPreviewLoading" class="preview-state">
							{{ t('module.attachment.details.preview.svgLoading') }}
						</div>
						<div v-else-if="svgPreviewFailed" class="preview-state">
							{{ t('module.attachment.details.preview.svgFailed') }}
						</div>
						<iframe
							v-else-if="svgPreviewUrl"
							:src="svgPreviewUrl"
							:title="t('module.attachment.details.preview.svgTitle')"
							class="svg-preview-frame"
							sandbox=""
						></iframe>
					</div>
					<a
						v-else-if="mediaFileCategory === 'IMAGE'"
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
						v-else-if="mediaFileCategory === 'VIDEO'"
						ref="artplayerRef"
						:attachment-id="file.id"
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
						v-else-if="mediaFileCategory === 'AUDIO'"
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
					<el-descriptions-item :label="t('common.label.id')">
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
					<el-descriptions-item label="SHA1">
						{{
							file.sha1 ||
							t('module.attachment.details.descItemValue.sha1Calculating')
						}}
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
			v-if="file.id"
			v-model:visible="attachmentRelationsDialogVisible"
			:attachment-id="file.id"
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

.svg-preview-container {
	width: 100%;
	min-height: 360px;
}

.svg-preview-frame {
	display: block;
	width: 100%;
	height: 60vh;
	min-height: 360px;
	border: 1px solid var(--el-border-color);
	background: var(--el-bg-color);
}

.preview-state {
	display: flex;
	align-items: center;
	justify-content: center;
	min-height: 360px;
	color: var(--el-text-color-secondary);
}
</style>
