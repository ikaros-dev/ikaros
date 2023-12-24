<script setup lang="ts">
import { Attachment, Episode } from '@runikaros/api-client';
import { computed, ref, watch, onMounted } from 'vue';
import {
	ElButton,
	ElDescriptions,
	ElDescriptionsItem,
	ElDialog,
	ElMessage,
	ElPopconfirm,
	ElRow,
	ElCol,
	ElCard,
} from 'element-plus';
import { base64Encode } from '@/utils/string-util';
// eslint-disable-next-line no-unused-vars
import { apiClient } from '@/utils/api-client';
import { AttachmentReferenceTypeEnum } from '@runikaros/api-client';
import { isVideo } from '@/utils/file';
import { Plus } from '@element-plus/icons-vue';
import AttachmentMultiSelectDialog from '@/modules/content/attachment/AttachmentMultiSelectDialog.vue';
import AttachmentSelectDialog from '@/modules/content/attachment/AttachmentSelectDialog.vue';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		subjectId: number | undefined;
		// episode
		ep: Episode | undefined;
		multiResource?: boolean;
	}>(),
	{
		visible: false,
		multiResource: false,
	}
);

const episode = ref<Episode>({});

watch(props, (newVal) => {
	// console.log(newVal);
	episode.value = newVal.ep as Episode;
});

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'removeEpisodeFilesBind'): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const removeEpisodeAttachmentRef = async () => {
	// @ts-ignore
	if (
		!episode.value ||
		!episode.value.resources ||
		episode.value.resources.length === 0
	) {
		ElMessage.warning('操作无效，您当前剧集并未绑定资源文件');
		return;
	}
	await episode.value.resources.forEach(async (resouce) => {
		await apiClient.attachmentRef.removeByTypeAndAttachmentIdAndReferenceId({
			attachmentReference: {
				type: 'EPISODE' as AttachmentReferenceTypeEnum,
				attachmentId: resouce.attachmentId,
				referenceId: resouce.episodeId,
			},
		});
	});
	ElMessage.success('移除剧集所有附件绑定成功');
	dialogVisible.value = false;
	emit('removeEpisodeFilesBind');
};

// eslint-disable-next-line no-unused-vars
const urlIsArachivePackage = (url: string | undefined): boolean => {
	return !url || url.endsWith('zip') || url.endsWith('7z');
};

const episodeHasMultiResource = ref(false);
const initEpisodeHasMultiResource = () => {
	if (episode.value?.resources?.length && episode.value?.resources?.length > 1)
		episodeHasMultiResource.value = true;
};

const batchMatchingEpisodeButtonLoading = ref(false);
const batchMatchingSubjectButtonLoading = ref(false);
const currentOperateEpisode = ref<Episode>();
const attachmentMultiSelectDialogVisible = ref(false);
const bindMasterIsEpisodeFlag = ref(false);
const bingResources = (episode: Episode) => {
	// console.log('episode', episode);
	currentOperateEpisode.value = episode;
	if (episodeHasMultiResource.value) {
		attachmentMultiSelectDialogVisible.value = true;
		bindMasterIsEpisodeFlag.value = true;
	} else {
		attachmentSelectDialog.value = true;
	}
};

const attachmentSelectDialog = ref(false);
// eslint-disable-next-line no-unused-vars
const onCloseWithAttachmentForAttachmentSelectDialog = async (
	attachment: Attachment
) => {
	console.log('attachment', attachment);
	console.log('currentOperateEpisode', currentOperateEpisode.value);
	await apiClient.attachmentRef.saveAttachmentReference({
		attachmentReference: {
			type: 'EPISODE' as AttachmentReferenceTypeEnum,
			attachmentId: attachment.id as number,
			referenceId: currentOperateEpisode.value?.id as number,
		},
	});
	ElMessage.success('单个剧集和附件匹配成功');
	// await fetchDatas();
};
const onCloseWithAttachments = async (attachments: Attachment[]) => {
	// console.log('attachments', attachments);
	const attIds: number[] = attachments.map((att) => att.id) as number[];
	if (bindMasterIsEpisodeFlag.value) {
		await delegateBatchMatchingEpisode(currentOperateEpisode.value?.id, attIds);
	} else {
		if (!props.subjectId) {
			ElMessage.error('操作失败，当前剧集所属条目ID未指定');
			return;
		}
		await delegateBatchMatchingSubject(props.subjectId, attIds);
	}
};

const delegateBatchMatchingSubject = async (
	subjectId: number | undefined,
	attIds: number[]
) => {
	if (!subjectId || subjectId <= 0 || !attIds || attIds.length === 0) {
		return;
	}
	batchMatchingSubjectButtonLoading.value = true;
	await apiClient.attachmentRef
		.matchingAttachmentsAndSubjectEpisodes({
			batchMatchingSubjectEpisodesAttachment: {
				subjectId: subjectId,
				attachmentIds: attIds,
			},
		})
		.then(() => {
			ElMessage.success('批量匹配条目所有剧集和多资源成功');
			window.location.reload();
		})
		.finally(() => {
			batchMatchingSubjectButtonLoading.value = false;
		});
};

const delegateBatchMatchingEpisode = async (
	episodeId: number | undefined,
	attIds: number[]
) => {
	if (!episodeId || episodeId <= 0 || !attIds || attIds.length === 0) {
		return;
	}
	batchMatchingEpisodeButtonLoading.value = true;
	await apiClient.attachmentRef
		.matchingAttachmentsForEpisode({
			batchMatchingEpisodeAttachment: {
				episodeId: episodeId,
				attachmentIds: attIds,
			},
		})
		.then(() => {
			ElMessage.success('批量匹单个剧集和多资源成功');
			window.location.reload();
		})
		.finally(() => {
			batchMatchingEpisodeButtonLoading.value = false;
		});
};

onMounted(initEpisodeHasMultiResource);
</script>

<template>
	<AttachmentSelectDialog
		v-model:visible="attachmentSelectDialog"
		@close-with-attachment="onCloseWithAttachmentForAttachmentSelectDialog"
	/>

	<AttachmentMultiSelectDialog
		v-model:visible="attachmentMultiSelectDialogVisible"
		@close-with-attachments="onCloseWithAttachments"
	/>
	<el-dialog v-model="dialogVisible" title="剧集详情" width="70%">
		<el-descriptions border :column="1">
			<el-descriptions-item label="原始名称">
				{{ episode?.name }}
			</el-descriptions-item>
			<el-descriptions-item label="中文名称">
				{{ episode?.name_cn }}
			</el-descriptions-item>
			<el-descriptions-item label="放送时间">
				{{ episode?.air_time }}
			</el-descriptions-item>
			<el-descriptions-item label="序列号">
				{{ episode?.sequence }}
			</el-descriptions-item>
			<el-descriptions-item label="描述">
				{{ episode?.description }}
			</el-descriptions-item>
			<el-descriptions-item label="资源">
				<div v-if="episode?.resources && episode?.resources.length > 0">
					<div v-if="!props.multiResource" align="center">
						<router-link
							target="_blank"
							:to="
								'/attachments?parentId=' +
								episode?.resources[0].parentAttachmentId +
								'&name=' +
								base64Encode(encodeURI(episode?.resources[0].name as string))
							"
							>{{ episode?.resources[0].name }}</router-link
						>
						<br />
						<video
							v-if="isVideo(episode.resources[0].url as string)"
							style="width: 100%"
							:src="episode.resources[0].url"
							controls
							preload="metadata"
						>
							您的浏览器不支持这个格式的视频
						</video>
						<span v-else>
							当前资源文件非视频文件、或者不可读取，如是视频文件且需读取，请先从远端拉取。
						</span>
					</div>
					<el-row v-else :gutter="12" :span="24">
						<el-col
							v-for="res in episode?.resources"
							:key="res.attachmentId"
							:span="8"
						>
							<router-link
								target="_blank"
								:to="
									'/attachments?parentId=' +
									res.parentAttachmentId +
									'&name=' +
									base64Encode(encodeURI(res.name as string))
								"
							>
								<el-card shadow="hover">
									{{ res.name }}
								</el-card>
							</router-link>
						</el-col>
					</el-row>
				</div>
				<span v-else> 当前剧集暂未绑定资源文件 </span>
			</el-descriptions-item>
		</el-descriptions>

		<template #footer>
			<el-button
				plain
				:icon="Plus"
				:loading="batchMatchingEpisodeButtonLoading"
				@click="bingResources(episode)"
			>
				添加绑定
			</el-button>
			<el-popconfirm
				title="此操作会移除当前剧集所有资源绑定，确定移除绑定吗？"
				width="280"
				@confirm="removeEpisodeAttachmentRef"
			>
				<template #reference>
					<el-button plain type="danger">移除所有绑定</el-button>
				</template>
			</el-popconfirm>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
