<script setup lang="ts">
import {
	Attachment,
	AttachmentReferenceTypeEnum,
	Episode,
	EpisodeResource,
} from '@runikaros/api-client';
import { computed, ref, watch } from 'vue';
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
import { base64Encode } from '@/utils/string-util';
// eslint-disable-next-line no-unused-vars
import { apiClient } from '@/utils/api-client';
import { isVideo } from '@/utils/file';
import { Close, Plus } from '@element-plus/icons-vue';
import AttachmentMultiSelectDialog from '@/modules/content/attachment/AttachmentMultiSelectDialog.vue';
import { useI18n } from 'vue-i18n';
import Artplayer from '@/components/video/Artplayer.vue';
import { useUserStore } from '@/stores/user';

const { t } = useI18n();
const userStore = useUserStore();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		subjectId: string | undefined;
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

watch(props, async (newVal) => {
	// console.log(newVal);
	episode.value = newVal.ep as Episode;
	await fetchEpisodeResources();
	if (episodeResources.value) {
		episodeResources.value?.sort(compareFun);
		emit(
			'update:multiResource',
			episodeResources.value && episodeResources.value.length > 1
		);
		loadVideoAttachment();
	}
});

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'update:multiResource', multiResource: boolean): void;
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

// const hasMultiRes = computed({
// 	get() {
// 		return props.multiResource;
// 	},
// 	set(value) {
// 		emit('update:multiResource', value);
// 	}
// })

const removeEpisodeAllAttachmentRefs = async () => {
	// @ts-ignore
	if (!episodeResources.value) {
		ElMessage.warning(
			t(
				'module.subject.dialog.episode.details.message.operate.remove-episode-all-att-refs.waring'
			)
		);
		return;
	}
	await episodeResources.value.forEach(async (resouce) => {
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
	const attIds: string[] = attachments.map((att) => att.id) as string[];
	await delegateBatchMatchingEpisode(currentOperateEpisode.value?.id, attIds);
	await fetchEpisodeResources();
};

const delegateBatchMatchingEpisode = async (
	episodeId: string | undefined,
	attIds: string[]
) => {
	if (!episodeId  || !attIds || attIds.length === 0) {
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

const episodeResources = ref<EpisodeResource[]>([]);
const fetchEpisodeResources = async () => {
	if (!episode.value) return;
	const { data } = await apiClient.episode.getAttachmentRefsById({
		id: episode.value.id as number,
	});
	episodeResources.value = data;
	var multiResource =
		episodeResources.value && episodeResources.value.length > 1;
	emit('update:multiResource', multiResource);
};

const loadVideoAttachment = async () => {
	console.debug('loadVideoAttachment');
	console.debug('episodeResources.value', episodeResources.value);
	if (
		episodeResources.value &&
		episodeResources.value.length == 1 &&
		isVideo(episodeResources.value[0].url as string)
	) {
		console.debug(
			'episodeResources.value[0].attachmentId',
			episodeResources.value[0].attachmentId
		);
		const { data } = await apiClient.attachment.getAttachmentById({
			id: episodeResources.value[0].attachmentId as number,
		});
		currentVideoAttachment.value = data;
	} else {
		console.debug('loadVideoAttachment {}');
		currentVideoAttachment.value = {};
	}
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

const artplayer = ref<Artplayer>();
const getArtplayerInstance = (art: Artplayer) => {
	artplayer.value = art;
};
const currentVideoAttachment = ref<Attachment>({
	id: 0,
});
const onDialogClose = () => {
	emit('close');
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
		@close="onDialogClose"
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
				<div v-if="episodeResources && episodeResources.length > 0">
					<div v-if="!props.multiResource" align="center">
						<router-link
							v-if="userStore.roleHasMaster()" 
							target="_blank"
							:to="
								'/attachments?parentId=' +
								episodeResources[0].parentAttachmentId +
								'&name=' +
								base64Encode(encodeURI(episodeResources[0].name as string))
							"
							>{{ episodeResources[0].name }}</router-link
						>
						<span v-else>{{ episodeResources[0].name }}</span>
						<br />
						<!-- <video
              v-if="isVideo(episode.resources[0].url as string)"
              style="width: 100%"
              :src="episode.resources[0].url"
              controls
              preload="metadata"
            >
              {{
                t('module.subject.dialog.episode.details.hint.video.unsuport')
              }}
            </video> -->
						<artplayer
							v-if="isVideo(episodeResources[0].url as string)"
							v-model:attachmentId="episodeResources[0].attachmentId"
							style="width: 100%"
							@getInstance="getArtplayerInstance"
						/>
						<span v-else>
							{{
								t('module.subject.dialog.episode.details.hint.video.not_video')
							}}
						</span>
					</div>
					<el-row v-else :gutter="12" :span="24">
						<el-col
							v-for="res in episodeResources"
							:key="res.attachmentId"
							:span="8"
						>
							<el-card shadow="hover">
								<router-link
									v-if="userStore.roleHasMaster()" 
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
								<span v-else>
									{{ res.name }}
								</span>
								<span  style="float: right">
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

		<template v-if="userStore.roleHasMaster()" #footer>
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
