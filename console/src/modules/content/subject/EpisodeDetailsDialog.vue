<script setup lang="ts">
import {Attachment, AttachmentReferenceTypeEnum, Episode, EpisodeResource,} from '@runikaros/api-client';
import {computed, ref, watch} from 'vue';
import {
  ElButton,
  ElCard,
  ElCol,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElMessage,
  ElPopconfirm,
  ElRow,
} from 'element-plus';
import {base64Encode} from '@/utils/string-util';
// eslint-disable-next-line no-unused-vars
import {apiClient} from '@/utils/api-client';
import {isVideo} from '@/utils/file';
import {Close, Plus} from '@element-plus/icons-vue';
import AttachmentMultiSelectDialog from '@/modules/content/attachment/AttachmentMultiSelectDialog.vue';
import {useI18n} from 'vue-i18n';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
    subjectId: number | string | undefined;
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
	if (episode.value?.resources) {
		episode.value.resources?.sort(compareFun);
	}
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

const removeEpisodeAllAttachmentRefs = async () => {
	// @ts-ignore
	if (
		!episode.value ||
		!episode.value.resources ||
		episode.value.resources.length === 0
	) {
		ElMessage.warning(
			t(
				'module.subject.dialog.episode.details.message.operate.remove-episode-all-att-refs.waring'
			)
		);
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
	ElMessage.success(
		t(
			'module.subject.dialog.episode.details.message.operate.remove-episode-all-att-refs.success'
		)
	);
	dialogVisible.value = false;
	emit('removeEpisodeFilesBind');
};

const removeEpisodeAttachmentRef = async (attachmentId) => {
	if (!attachmentId || !episode.value.id) return;

	await apiClient.attachmentRef.removeByTypeAndAttachmentIdAndReferenceId({
		attachmentReference: {
			type: 'EPISODE' as AttachmentReferenceTypeEnum,
			attachmentId: attachmentId,
			referenceId: episode.value.id,
		},
	});
	ElMessage.success(
		t(
			'module.subject.dialog.episode.details.message.operate.remove-episode-att-ref.success'
		)
	);
	await fetchEpisodeResources();
};

// eslint-disable-next-line no-unused-vars
const urlIsArachivePackage = (url: string | undefined): boolean => {
	return !url || url.endsWith('zip') || url.endsWith('7z');
};

const batchMatchingEpisodeButtonLoading = ref(false);
const currentOperateEpisode = ref<Episode>();
const attachmentMultiSelectDialogVisible = ref(false);
const bingResources = (episode: Episode) => {
	// console.log('episode', episode);
	currentOperateEpisode.value = episode;
	attachmentMultiSelectDialogVisible.value = true;
};

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
	ElMessage.success(
		t(
			'module.subject.dialog.episode.details.message.operate.match-single-episode-attachment.success'
		)
	);
	await fetchEpisodeResources();
};
const onCloseWithAttachments = async (attachments: Attachment[]) => {
	// console.log('attachments', attachments);
	const attIds: number[] = attachments.map((att) => att.id) as number[];
	await delegateBatchMatchingEpisode(currentOperateEpisode.value?.id, attIds);
	await fetchEpisodeResources();
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
			ElMessage.success(
				t(
					'module.subject.dialog.episode.details.message.operate.batch-match-episode-atts.success'
				)
			);
		})
		.finally(() => {
			batchMatchingEpisodeButtonLoading.value = false;
		});
};

const fetchEpisodeResources = async () => {
	if (!episode.value.id) return;
	const { data } = await apiClient.episode.findEpisodeAttachmentRefsById({
		id: episode.value.id as number,
	});
	episode.value.resources = data;
};

const compareFun = (r1: EpisodeResource, r2: EpisodeResource): number => {
	const name1 = r1.name;
	const name2 = r2.name;
	if (!name1 || !name2) return 0;
	if (name1 < name2) {
		return -1;
	}
	if (name1 > name2) {
		return 1;
	}
	return 0;
};
</script>

<template>
	<AttachmentMultiSelectDialog
		v-model:visible="attachmentMultiSelectDialogVisible"
		@close-with-attachments="onCloseWithAttachments"
	/>
	<el-dialog
		v-model="dialogVisible"
		:title="t('module.subject.dialog.episode.details.title')"
		width="70%"
	>
		<el-descriptions border :column="1">
			<el-descriptions-item
				:label="t('module.subject.dialog.episode.details.label.name')"
			>
				{{ episode?.name }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.dialog.episode.details.label.name_cn')"
			>
				{{ episode?.name_cn }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.dialog.episode.details.label.air_time')"
			>
				{{ episode?.air_time }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.dialog.episode.details.label.sequence')"
			>
				{{ episode?.sequence }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.dialog.episode.details.label.description')"
			>
				{{ episode?.description }}
			</el-descriptions-item>
			<el-descriptions-item
				:label="t('module.subject.dialog.episode.details.label.resources')"
			>
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
							{{
								t('module.subject.dialog.episode.details.hint.video.unsuport')
							}}
						</video>
						<span v-else>
							{{
								t('module.subject.dialog.episode.details.hint.video.not_video')
							}}
						</span>
					</div>
					<el-row v-else :gutter="12" :span="24">
						<el-col
							v-for="res in episode?.resources"
							:key="res.attachmentId"
							:span="8"
						>
							<el-card shadow="hover">
								<router-link
									target="_blank"
									:to="
									'/attachments?parentId=' +
									res.parentAttachmentId +
									'&name=' +
									base64Encode(encodeURI(res.name as string))
								"
								>
									<span>
										{{ res.name }}
									</span>
								</router-link>
								<span style="float: right">
									<el-popconfirm
										:title="
											t(
												'module.subject.dialog.episode.details.popconfirm.title'
											)
										"
										width="250"
										@confirm="removeEpisodeAttachmentRef(res.attachmentId)"
									>
										<template #reference>
											<el-button plain type="danger" :icon="Close" />
										</template>
									</el-popconfirm>
								</span>
							</el-card>
						</el-col>
					</el-row>
				</div>
				<span v-else>
					{{ t('module.subject.dialog.episode.details.hint.no_bind') }}
				</span>
			</el-descriptions-item>
		</el-descriptions>

		<template #footer>
			<el-button
				plain
				:icon="Plus"
				:loading="batchMatchingEpisodeButtonLoading"
				@click="bingResources(episode)"
			>
				{{ t('module.subject.dialog.episode.details.footer.button.add-bind') }}
			</el-button>
			<el-popconfirm
				:title="
					t('module.subject.dialog.episode.details.footer.popconfirm.title')
				"
				width="280"
				@confirm="removeEpisodeAllAttachmentRefs"
			>
				<template #reference>
					<el-button plain type="danger" :icon="Close">
						{{
							t(
								'module.subject.dialog.episode.details.footer.popconfirm.button.remove-all-binds'
							)
						}}
					</el-button>
				</template>
			</el-popconfirm>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
