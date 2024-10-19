<script setup lang="ts">
import {
  Attachment,
  AttachmentReferenceTypeEnum,
  Episode,
  EpisodeCollection,
  EpisodeGroupEnum,
  EpisodeResource,
  Subject,
  SubjectCollection,
  SubjectSync,
  SubjectTag,
  SubjectTypeEnum,
} from '@runikaros/api-client';
import {apiClient} from '@/utils/api-client';
import {formatDate} from '@/utils/date';
import EpisodeDetailsDialog from './EpisodeDetailsDialog.vue';
import router from '@/router';
import {Check, Close} from '@element-plus/icons-vue';
import SubjectSyncDialog from './SubjectSyncDialog.vue';
import {useRoute} from 'vue-router';
import {nextTick, onMounted, ref, watch} from 'vue';
import {
  ElButton,
  ElCol,
  ElDescriptions,
  ElDescriptionsItem,
  ElImage,
  ElInput,
  ElMessage,
  ElOption,
  ElPopconfirm,
  ElRow,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag,
} from 'element-plus';
import SubjectRemoteActionDialog from './SubjectRemoteActionDialog.vue';
import {useSettingStore} from '@/stores/setting';
import SubjectRelationDialog from './SubjectRelationDialog.vue';
import {useSubjectStore} from '@/stores/subject';
import AttachmentMultiSelectDialog from '@/modules/content/attachment/AttachmentMultiSelectDialog.vue';
import AttachmentSelectDialog from '@/modules/content/attachment/AttachmentSelectDialog.vue';
import SubjectCollectDialog from '@/components/modules/content/subject/SubjectCollectDialog.vue';
import {useI18n} from 'vue-i18n';

const route = useRoute();
const settingStore = useSettingStore();
const subjectStore = useSubjectStore();
const { t } = useI18n();

const refreshSubjectRelactionDialog = ref(true);
watch(route, async () => {
	if (!route.params?.id && route.params?.id === undefined) {
		return;
	}
	// console.log(route.params.id);
	await fetchDatas();
	doRefreshSubjectRelactionDialog();
});

const doRefreshSubjectRelactionDialog = () => {
	subjectRelationDialogVisible.value = false;
	refreshSubjectRelactionDialog.value = false;
	nextTick(() => {
		refreshSubjectRelactionDialog.value = true;
	});
};

const subject = ref<Subject>({
	id: -1,
	name: '',
	type: SubjectTypeEnum.Other,
	nsfw: true,
	name_cn: '',
});

const episodes = ref<Episode[]>([]);
const episodeResources = ref<EpisodeResource[]>([]);

// eslint-disable-next-line no-unused-vars
const fetchSubjectById = async () => {
	if (subject.value.id) {
		subject.value = await subjectStore.fetchSubjectById(
			subject.value.id as number
		);
		await fetchEpisodes();
		await fetchEpisodeResources();
		await fetchSubjectSyncs();
	}
};

const fetchEpisodes = async () => {
	const { data } = await apiClient.episode.getAllBySubjectId({
		id: subject.value.id as number,
	});
	episodes.value = data;
	if (!episodes.value || episodes.value.length === 0) {
		batchMatchingSubjectButtonDisable.value = true;
		deleteMatchingSubjectButtonDisable.value = true;
	} else {
		batchMatchingSubjectButtonDisable.value = false;
		deleteMatchingSubjectButtonDisable.value = false;
	}
	if (episodes.value) {
		episodes.value = episodes.value.sort(
			(a, b) => (a.sequence ?? 0) - (b.sequence ?? 0)
		);
		loadEpisodeGroupLabels();
	}
	// console.debug('episodes', episodes.value);
};

const fetchEpisodeResources = async () => {
	// console.debug('episodes', episodes.value);
	episodeResources.value = [];
	episodes.value.forEach(async (episode) => {
		var ep = episode as Episode;
		// console.debug('ep', ep);
		if (ep.id) {
			const { data } = await apiClient.episode.getAttachmentRefsById({
				id: ep.id as number,
			});
			data.forEach((res) => {
				if (res) episodeResources.value.push(res);
			});
		}
	});
};

// eslint-disable-next-line no-unused-vars
const infoMap = ref<Map<string, string>>();

watch(subject, () => {
	if (!subject.value.infobox) {
		return;
	}
	// console.log('subject.value.infobox', subject.value.infobox);
	const infobox: string = subject.value.infobox as string;
	// console.log("infobox.indexOf('\n') < 0", infobox.indexOf('\n') < 0);
	// console.log("infobox.indexOf(':') >= 0", infobox.indexOf(':') >= 0);
	let map = new Map<string, string>();
	if (infobox.indexOf('\n') < 0) {
		if (infobox.indexOf(':') >= 0) {
			const strArr = infobox.split(':');
			if (strArr.length == 2) {
				map.set(strArr[0], strArr[1]);
			}
			if (strArr.length > 2) {
				const firstStr = strArr[0];
				let index = infobox.indexOf(firstStr);
				const value = infobox.substring(
					index + firstStr.length + 1,
					infobox.length
				);
				// console.log('str', str);
				// console.log('firstStr', firstStr);
				// console.log('index + firstStr.length + 1', index + firstStr.length + 1);
				// console.log('value', value);
				map.set(firstStr, value);
			}
		} else {
			return;
		}
	}

	for (const str of infobox.split('\n')) {
		const strArr = str.split(':');
		if (strArr.length == 2) {
			map.set(strArr[0], strArr[1]);
		}
		if (strArr.length > 2) {
			const firstStr = strArr[0];
			let index = str.indexOf(firstStr);
			const value = str.substring(index + firstStr.length + 1, str.length);
			// console.log('str', str);
			// console.log('firstStr', firstStr);
			// console.log('index + firstStr.length + 1', index + firstStr.length + 1);
			// console.log('value', value);
			map.set(firstStr, value);
		}
	}
	infoMap.value = map;
	// console.log('infoMap', infoMap.value);
});

// eslint-disable-next-line no-unused-vars
const airTimeDateFormatter = (row) => {
	// console.log('row', row);
	return formatDate(new Date(row.air_time), 'yyyy-MM-dd');
};

const currentEpisode = ref<Episode>();
const showEpisodeDetails = (ep: Episode) => {
	currentEpisode.value = ep;
	episodeDetailsDialogVisible.value = true;
	const resources: EpisodeResource[] = episodeResources.value.filter(
		(e) => e.episodeId === ep.id
	);
	episodeHasMultiResource.value = (resources &&
		resources.length > 1) as boolean;
};

const episodeDetailsDialogVisible = ref(false);

const toSubjectPut = () => {
	if (subject.value.id) {
		router.push('/subjects/subject/put/' + subject.value.id);
	} else {
		console.error('subject id no value', subject.value.id);
	}
};

const episodeHasMultiResource = ref(false);
const initEpisodeHasMultiResource = () => {
	if (
		subject.value.type === 'ANIME' ||
		subject.value.type === 'GAME' ||
		subject.value.type === 'NOVEL'
	) {
		return;
	}
	episodeHasMultiResource.value = true;
};

const currentOperateEpisode = ref<Episode>();
// const bingResources = (episode: Episode) => {
// 	// console.log('episode', episode);
// 	currentOperateEpisode.value = episode;
// 	if (episodeHasMultiResource.value) {
// 		attachmentMultiSelectDialogVisible.value = true;
// 		bindMasterIsEpisodeFlag.value = true;
// 	} else {
// 		attachmentSelectDialog.value = true;
// 	}
// };

const deleteSubject = async () => {
	if (!subject.value.id) {
		return;
	}
	await apiClient.subject
		.deleteSubjectById({
			id: subject.value.id,
		})
		.then(() => {
			ElMessage.success(
				t('module.subject.details.message.operate.delete.success', {
					name: subject.value.name,
				})
			);
			router.push('/subjects');
		});
};

const subjectSyncDialogVisible = ref(false);
const openSubjectSyncDialog = () => {
	subjectSyncDialogVisible.value = true;
};
const onSubjectSyncDialogCloseWithSubjectName = () => {
	ElMessage.success(t('module.subject.details.message.operate.update.success'));
	fetchSubjectById();
};

const subjectRelationDialogVisible = ref(false);
const openSubjectRelationDialog = () => {
	subjectRelationDialogVisible.value = true;
};

const fileRemoteActionDialogVisible = ref(false);
const fileRemoteFileId = ref(-1);
const fileRemoteIsPush = ref(true);
const openFileRemoteActionDialog = (fileId, fileCanRead) => {
	fileRemoteFileId.value = fileId as number;
	fileRemoteIsPush.value = fileCanRead as boolean;
	fileRemoteActionDialogVisible.value = true;
};

const subjectRemoteActionDialogVisible = ref(false);
const subjectRemoteIsPush = ref(true);
const subjectRemoteFileId = ref(subject.value.id);
const onSubjectRemoteActionDialogClose = () => {
	subjectRemoteActionDialogVisible.value = false;
	router.push('/tasks');
};
// const onSubjectRemoteButtonClick = (isPush: boolean) => {
// 	subjectRemoteIsPush.value = isPush;
// 	subjectRemoteFileId.value = subject.value.id;
// 	subjectRemoteActionDialogVisible.value = true;
// };

const notCollectText = t('module.subject.details.text.collect.not');
const clickCollectText = t('module.subject.details.text.collect.click');
const removeCollectText = t('module.subject.details.text.collect.cancel');
const collectText = t('module.subject.details.text.collect.done');
const collectButtonText = ref(notCollectText);
const updateSubjectCollection = async () => {
	var isUnCollect = subjectCollection.value && subjectCollection.value.type;
	if (!isUnCollect) {
		return;
	}
	await apiClient.collectionSubject.collectSubject({
		subjectId: subject.value.id as number,
		type: subjectCollection.value.type as
			| 'WISH'
			| 'DOING'
			| 'DONE'
			| 'SHELVE'
			| 'DISCARD',
	});
	ElMessage.success(t('module.subject.collect.message.operate.update.success'));
};

const subjectCollectDialogVisible = ref(false);
const changeSubjectCollectState = async () => {
	var isUnCollect = subjectCollection.value && subjectCollection.value.type;
	console.log('isUnCollect', isUnCollect);
	if (isUnCollect) {
		// un collect
		await apiClient.collectionSubject.removeSubjectCollect({
			subjectId: subject.value.id as number,
		});
		ElMessage.success(
			t('module.subject.details.message.operate.cancel.success')
		);
	} else {
		// collect
		subjectCollectDialogVisible.value = true;
	}
	fetchSubjectCollection();
};
// eslint-disable-next-line no-unused-vars
const subjectCollection = ref<SubjectCollection>({});
// eslint-disable-next-line no-unused-vars
const fetchSubjectCollection = async () => {
	// eslint-disable-next-line no-unused-vars
	const rsp = await apiClient.collectionSubject.findCollectionSubject({
		subjectId: subject.value.id as number,
	});

	// if (rsp.status === 404) {
	// 	subjectCollection.value = {};
	// 	return;
	// }

	if (rsp && rsp.status === 200) {
		subjectCollection.value = rsp.data;
		collectButtonText.value = collectText;
	} else {
		subjectCollection.value = {};
		collectButtonText.value = notCollectText;
	}
};

const episodeCollections = ref<EpisodeCollection[]>([]);
const fetchEpisodeCollections = async () => {
	const { data } =
		await apiClient.collectionEpisode.findCollectionEpisodesByUserIdAndSubjectId(
			{
				subjectId: subject.value.id as number,
			}
		);
	episodeCollections.value = data;
};
const getEpisodeCollectionByEpisodeId = (episode: Episode) => {
	if (!episodeCollections.value || !episode) {
		return undefined;
	}
	var result = episodeCollections.value.find(
		(ele) => ele?.episode_id === episode.id
	);
	// console.log('result', result);
	return result;
};
const udpateEpisodeCollectionProgress = async (
	isFinish: boolean,
	episode: Episode
) => {
	if (!subjectCollection.value.id) {
		ElMessage.warning(
			t('module.subject.episode.collect.message.operate.mark-finish-notcollect')
		);
		return;
	}
	await apiClient.collectionEpisode.updateCollectionEpisodeFinish({
		episodeId: episode.id as number,
		finish: isFinish,
	});
	ElMessage.success(
		t('module.subject.episode.collect.message.operate.mark-finish')
	);
	await fetchDatas();
};

const fetchDatas = async () => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	await fetchSubjectById();
	await fetchTags();
	await initEpisodeHasMultiResource();
	await fetchSubjectCollection();
	await fetchEpisodeCollections();
};

const bindMasterIsEpisodeFlag = ref(false);
const batchMatchingSubjectButtonLoading = ref(false);
const batchMatchingSubjectButtonDisable = ref(false);
const deleteMatchingSubjectButtonDisable = ref(false);
const batchMatchingEpisodeButtonLoading = ref(false);
const attachmentMultiSelectDialogVisible = ref(false);
const onCloseWithAttachments = async (attachments: Attachment[]) => {
	// console.log('attachments', attachments);
	const attIds: number[] = attachments.map((att) => att.id) as number[];
	if (bindMasterIsEpisodeFlag.value) {
		await delegateBatchMatchingEpisode(currentOperateEpisode.value?.id, attIds);
	} else {
		await delegateBatchMatchingSubject(subject.value.id, attIds);
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
			ElMessage.success(
				t(
					'module.attachment.reference.message.operate.batch-match-subject-episodes-atts'
				)
			);
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
			ElMessage.success(
				t(
					'module.attachment.reference.message.operate.batch-match-episode-atts'
				)
			);
			window.location.reload();
		})
		.finally(() => {
			batchMatchingEpisodeButtonLoading.value = false;
		});
};

const attachmentSelectDialog = ref(false);
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
		t('module.attachment.reference.message.operate.batch-match-episode-atts')
	);
	await fetchDatas();
};

const tags = ref<SubjectTag[]>([]);
const fetchTags = async () => {
	var subjectId = subject.value.id;
	if (!subjectId) return;
	const { data } = await apiClient.tag.listSubjectTagsBySubjectId({
		subjectId: subjectId,
	});
	tags.value = data as SubjectTag[];
};
const onTagRemove = async (tag: SubjectTag) => {
	await apiClient.tag.removeTagByCondition({
		type: 'SUBJECT',
		masterId: tag.subjectId,
		name: tag.name,
	});
	ElMessage.success(
		t('module.subject.tag.message.operate.remove', { name: tag.name })
	);
	await fetchTags();
};
const newTagInputVisible = ref(false);
const newTagInputRef = ref();
const showNewTagInput = () => {
	newTagInputVisible.value = true;
	nextTick(() => {
		newTagInputRef.value!.input!.focus();
	});
};
const newTag = ref<SubjectTag>({});
const onNewTagNameChange = async () => {
	if (!newTagInputVisible.value) return;
	var tagName = newTag.value.name;
	if (
		!tagName ||
		tagName === '' ||
		tags.value.filter((t) => tagName === t.name).length > 0
	) {
		ElMessage.warning(
			t('module.subject.tag.message.hint.rename-when-repetition')
		);
		newTagInputVisible.value = false;
		return;
	}
	await apiClient.tag.createTag({
		tag: {
			type: 'SUBJECT',
			masterId: subject.value.id,
			name: newTag.value.name,
		},
	});
	ElMessage.success(
		t('module.subject.tag.message.operate.create', { name: newTag.value.name })
	);
	await fetchTags();
	newTagInputVisible.value = false;
	newTagInputRef.value!.input!.value = '';
	newTag.value.name = '';
};

const batchCancenMatchingSubjectButtonLoading = ref(false);
const deleteBatchingAttachments = async () => {
	// console.debug('deleteBatchingAttachments subject episodes', subject.value.episodes)
	batchCancenMatchingSubjectButtonLoading.value = true;
	await episodes.value?.forEach(async (ep) => {
		await apiClient.attachmentRef.removeAllByTypeAndReferenceId({
			attachmentReference: {
				type: 'EPISODE',
				referenceId: ep.id,
			},
		});
	});
	batchCancenMatchingSubjectButtonLoading.value = false;
	ElMessage.success(
		t('module.subject.details.message.operate.delete-batch-attachments.success')
	);
	fetchDatas();
};

const isEpisodeBindResource = (episode: Episode): boolean | undefined => {
	const resources: EpisodeResource[] = episodeResources.value.filter(
		(e) => e.episodeId === episode.id
	);
	// console.debug('episodeResources.value', episodeResources.value);
	// console.debug('episode', episode);
	// console.debug('resources', resources);
	return resources && resources.length > 0;
};

interface EpisdoeGroupItem {
	group: EpisodeGroupEnum;
	label: string;
	count: number;
}

const episodeGroupItems = ref<EpisdoeGroupItem[]>([]);
const doPushEpGroupItems = (
	group: EpisodeGroupEnum,
	groupCountMap: Map<EpisodeGroupEnum, number>,
	epGroupItems: EpisdoeGroupItem[]
) => {
	if (!groupCountMap.has(group)) return;
	const count = groupCountMap.get(group) as number;
	epGroupItems.push({
		group: group,
		count: count,
		label:
			t('module.subject.episode.group.' + group.toString()) + '(' + count + ')',
	});
	groupCountMap.delete(group);
};
const loadEpisodeGroupLabels = () => {
	const groupCountMap = new Map<EpisodeGroupEnum, number>();
	console.debug('subject', subject.value);
	episodes.value?.forEach((ep) => {
		const epGroup = ep.group as EpisodeGroupEnum;
		if (groupCountMap.get(epGroup)) {
			let count = groupCountMap.get(epGroup) as number;
			count++;
			groupCountMap.set(epGroup, count);
		} else {
			groupCountMap.set(epGroup, 1);
		}
	});
	console.debug('groupCountMap', groupCountMap);
	const epGroupItems: EpisdoeGroupItem[] = [];
	// MAIN
	doPushEpGroupItems(EpisodeGroupEnum.Main, groupCountMap, epGroupItems);

	// OP
	doPushEpGroupItems(EpisodeGroupEnum.OpeningSong, groupCountMap, epGroupItems);

	// ED
	doPushEpGroupItems(EpisodeGroupEnum.EndingSong, groupCountMap, epGroupItems);

	// SP
	doPushEpGroupItems(
		EpisodeGroupEnum.SpecialPromotion,
		groupCountMap,
		epGroupItems
	);

	// remaining
	groupCountMap.forEach((val, key) => {
		epGroupItems.push({
			group: key,
			label: t('module.subject.episode.group.' + key) + '(' + val + ')',
			count: val,
		});
	});
	console.debug('epGroupLabelSet', epGroupItems);
	episodeGroupItems.value = epGroupItems;
};

const subjectSyncs = ref<SubjectSync[]>([]);
const fetchSubjectSyncs = async () => {
	const { data } =
		await apiClient.subjectSync.getSubjectSyncsBySubjectId({
			id: subject.value.id as number,
		});
	subjectSyncs.value = data;
};

onMounted(fetchDatas);
</script>

<template>
	<AttachmentMultiSelectDialog
		v-model:visible="attachmentMultiSelectDialogVisible"
		@close-with-attachments="onCloseWithAttachments"
	/>
	<SubjectSyncDialog
		v-model:visible="subjectSyncDialogVisible"
		:define-subject-id="subject.id"
		:is-merge="true"
		@closeWithSubjectName="onSubjectSyncDialogCloseWithSubjectName"
	/>

	<SubjectRemoteActionDialog
		v-model:visible="subjectRemoteActionDialogVisible"
		v-model:is-push="subjectRemoteIsPush"
		v-model:subjectId="subjectRemoteFileId"
		@close="onSubjectRemoteActionDialogClose"
	/>

	<SubjectRelationDialog
		v-if="refreshSubjectRelactionDialog"
		v-model:visible="subjectRelationDialogVisible"
	/>

	<SubjectCollectDialog
		v-model:visible="subjectCollectDialogVisible"
		v-model:subjectId="subject.id"
		@close="fetchSubjectCollection"
	/>

	<el-row>
		<el-col :span="24">
			<el-button plain @click="toSubjectPut">
				{{ t('module.subject.details.text.button.edit') }}
			</el-button>
			<el-button plain @click="openSubjectSyncDialog">
				{{ t('module.subject.details.text.button.update') }}
			</el-button>
			<el-popconfirm
				:title="t('module.subject.details.dele-popconfirm.title')"
				@confirm="deleteSubject"
			>
				<template #reference>
					<el-button plain type="danger">
						{{ t('module.subject.details.text.button.delete') }}
					</el-button>
				</template>
			</el-popconfirm>

			<el-button plain @click="openSubjectRelationDialog">
				{{ t('module.subject.details.text.button.relaction') }}
			</el-button>
		</el-col>
	</el-row>
	<br />
	<el-row>
		<el-col :xs="24" :sm="24" :md="24" :lg="20" :xl="20">
			<el-row>
				<el-col :xs="24" :sm="24" :md="12" :lg="6" :xl="6">
					<el-image
						style="width: 100%"
						:src="subject.cover as string"
						:zoom-rate="1.2"
						:preview-src-list="new Array(subject.cover) as string[]"
						:initial-index="4"
						fit="cover"
					/>
				</el-col>
				<el-col :xs="24" :sm="24" :md="12" :lg="18" :xl="18">
					<el-descriptions
						style="margin: 0 5px"
						direction="vertical"
						:column="6"
						size="large"
						border
					>
						<el-descriptions-item label="ID" :span="1">
							{{ subject.id }}
						</el-descriptions-item>
						<el-descriptions-item
							:label="t('module.subject.details.label.name')"
							:span="1"
						>
							{{ subject.name }}
						</el-descriptions-item>
						<el-descriptions-item
							:label="t('module.subject.details.label.name_cn')"
							:span="1"
						>
							{{ subject.name_cn }}
						</el-descriptions-item>
						<el-descriptions-item
							:label="t('module.subject.details.label.air_time')"
							:span="1"
						>
							{{ subject.airTime }}
						</el-descriptions-item>
						<el-descriptions-item
							:label="t('module.subject.details.label.type')"
							:span="1"
						>
							{{ subject.type }}
						</el-descriptions-item>
						<el-descriptions-item label="NSFW" :span="1">
							{{ subject.nsfw }}
						</el-descriptions-item>
						<el-descriptions-item
							:label="t('module.subject.details.label.summary')"
							:span="6"
						>
							{{ subject.summary }}
						</el-descriptions-item>
					</el-descriptions>
					<el-descriptions
						v-if="subjectSyncs && subjectSyncs.length > 0"
						size="large"
						border
					>
						<el-descriptions-item
							:label="t('module.subject.details.label.sync-platform')"
						>
							<span v-for="(sync, index) in subjectSyncs" :key="index">
								{{ sync.platform }} :
								<span v-if="sync.platform === 'BGM_TV'">
									<a
										:href="'https://bgm.tv/subject/' + sync.platformId"
										target="_blank"
									>
										{{ sync.platformId }}
									</a>
								</span>
								<span v-else>
									{{ sync.platformId }}
								</span>
							</span>
						</el-descriptions-item>
					</el-descriptions>
					<el-descriptions size="large" border>
						<el-descriptions-item
							:label="t('module.subject.details.label.tag')"
						>
							<el-tag
								v-for="tag in tags"
								:key="tag.id"
								closable
								style="margin-right: 5px; margin-top: 5px"
								:disable-transitions="false"
								@close="onTagRemove(tag)"
							>
								{{ tag.name }}
							</el-tag>
							<el-input
								v-if="newTagInputVisible"
								ref="newTagInputRef"
								v-model="newTag.name"
								size="small"
								style="max-width: 80px"
								@blur="onNewTagNameChange"
								@keydown.enter="onNewTagNameChange"
							/>
							<el-button
								v-else
								size="small"
								style="margin-top: 5px"
								@click="showNewTagInput"
							>
								{{ t('module.subject.details.text.button.add-tag') }}
							</el-button>
						</el-descriptions-item>
					</el-descriptions>
					<el-descriptions size="large" border>
						<el-descriptions-item
							:label="t('module.subject.details.label.collect-status')"
						>
							<el-popconfirm
								:title="
									t(
										'module.subject.details.cancel-collect-popconfirm.title-prefix'
									) +
									(subjectCollection && subjectCollection.type
										? t(
												'module.subject.details.cancel-collect-popconfirm.cancel-collect'
										  )
										: t(
												'module.subject.details.cancel-collect-popconfirm.collect'
										  )) +
									t(
										'module.subject.details.cancel-collect-popconfirm.title-postfix'
									)
								"
								@confirm="changeSubjectCollectState"
							>
								<template #reference>
									<el-button
										style="width: 100px"
										plain
										@mouseleave="
											collectButtonText =
												subjectCollection && subjectCollection.type
													? collectText
													: notCollectText
										"
										@mouseover="
											collectButtonText =
												subjectCollection && subjectCollection.type
													? removeCollectText
													: clickCollectText
										"
									>
										{{ collectButtonText }}
									</el-button>
								</template>
							</el-popconfirm>
							&nbsp;&nbsp;
							<el-select
								v-if="subjectCollection && subjectCollection.type"
								v-model="subjectCollection.type"
								placeholder="Select"
								@change="updateSubjectCollection"
							>
								<el-option
									:label="t('module.subject.collect.type.wish')"
									value="WISH"
								/>
								<el-option
									:label="t('module.subject.collect.type.doing')"
									value="DOING"
								/>
								<el-option
									:label="t('module.subject.collect.type.done')"
									value="DONE"
								/>
								<el-option
									:label="t('module.subject.collect.type.shelve')"
									value="SHELVE"
								/>
								<el-option
									:label="t('module.subject.collect.type.discard')"
									value="DISCARD"
								/>
							</el-select>
						</el-descriptions-item>
					</el-descriptions>
				</el-col>
			</el-row>
			<el-row>
				<el-col :span="24">
					<el-tabs type="border-card">
						<el-tab-pane
							v-for="item in episodeGroupItems"
							:key="item.group"
							:label="item.label"
						>
							<el-table
								:data="episodes?.filter((ep) => ep.group === item.group)"
								@row-dblclick="showEpisodeDetails"
							>
								<el-table-column
									:label="t('module.subject.details.episode.label.sequence')"
									prop="sequence"
									width="80px"
									sortable
								/>
								<el-table-column
									:label="t('module.subject.details.episode.label.name')"
									prop="name"
								/>
								<el-table-column
									:label="t('module.subject.details.episode.label.name_cn')"
									prop="name_cn"
								/>
								<el-table-column
									:label="t('module.subject.details.episode.label.air_time')"
									prop="air_time"
									sortable
									:formatter="airTimeDateFormatter"
								/>
								<el-table-column
									:label="t('module.subject.details.episode.label.operate')"
									width="320"
								>
									<template
										v-if="item.group === EpisodeGroupEnum.Main.toString()"
										#header
									>
										<el-button
											plain
											:loading="batchMatchingSubjectButtonLoading"
											:disabled="batchMatchingSubjectButtonDisable"
											@click="
												() => {
													attachmentMultiSelectDialogVisible = true;
													bindMasterIsEpisodeFlag = false;
												}
											"
										>
											{{
												t(
													'module.subject.details.episode.label.button.batch-resources'
												)
											}}
										</el-button>
										<el-popconfirm
											:title="
												t(
													'module.subject.details.cancel-batch-popconfirm.title'
												)
											"
											@confirm="deleteBatchingAttachments"
										>
											<template #reference>
												<el-button
													plain
													type="danger"
													:disabled="deleteMatchingSubjectButtonDisable"
													:loading="batchCancenMatchingSubjectButtonLoading"
												>
													{{
														t(
															'module.subject.details.episode.label.button.cancel-batch-resources'
														)
													}}
												</el-button>
											</template>
										</el-popconfirm>
									</template>
									<template #default="scoped">
										<el-button
											plain
											:icon="isEpisodeBindResource(scoped.row) ? Check : Close"
											:color="
												isEpisodeBindResource(scoped.row)
													? '#00CCFF'
													: '#FF0000'
											"
											@click="showEpisodeDetails(scoped.row)"
										>
											{{
												t('module.subject.details.episode.label.button.details')
											}}
										</el-button>

										<el-button
											v-if="subjectCollection && subjectCollection.type"
											plain
											:icon="
												getEpisodeCollectionByEpisodeId(scoped.row)?.finish
													? Check
													: Close
											"
											@click="
												udpateEpisodeCollectionProgress(
													!getEpisodeCollectionByEpisodeId(scoped.row)?.finish,
													scoped.row
												)
											"
										>
											{{
												getEpisodeCollectionByEpisodeId(scoped.row)?.finish
													? t(
															'module.subject.details.episode.label.button.reset'
													  )
													: t(
															'module.subject.details.episode.label.button.done'
													  )
											}}
										</el-button>
										<!-- <el-button
                      plain
                      @click="showEpisodeCollectionDetails(scoped.row)"
                    >
                      进度
                    </el-button> -->
										<el-button
											v-if="
												settingStore.remoteEnable &&
												scoped.row.resources &&
												scoped.row.resources.length > 0
											"
											plain
											@click="
												openFileRemoteActionDialog(
													scoped.row.resources[0].file_id,
													scoped.row.resources[0].canRead
												)
											"
										>
											<span v-if="scoped.row.resources[0].canRead">
												{{
													t('module.subject.details.episode.label.button.push')
												}}
											</span>
											<span v-else>
												{{
													t('module.subject.details.episode.label.button.pull')
												}}
											</span>
										</el-button>
									</template>
								</el-table-column>
							</el-table>
						</el-tab-pane>
					</el-tabs>
				</el-col>
			</el-row>
		</el-col>
		<el-col :xs="24" :sm="24" :md="24" :lg="4" :xl="4">
			<ul>
				<li v-for="(value, key) in infoMap" :key="key" class="infinite-list">
					<span style="font-weight: 800">{{ value[0] }}</span> :
					{{ value[1] }}
				</li>
			</ul>
		</el-col>
	</el-row>
	<EpisodeDetailsDialog
		v-model:subjectId="subject.id"
		v-model:visible="episodeDetailsDialogVisible"
		v-model:ep="currentEpisode"
		v-model:multiResource="episodeHasMultiResource"
		@close="fetchEpisodeResources"
		@removeEpisodeFilesBind="fetchSubjectById"
	/>

	<AttachmentSelectDialog
		v-model:visible="attachmentSelectDialog"
		@close-with-attachment="onCloseWithAttachmentForAttachmentSelectDialog"
	/>
</template>

<style lang="scss" scoped>
.infoMapLabel {
	min-width: 100px;
}

.infinite-list {
	// overflow: auto;
	padding: 0;
	margin: 5px;
	list-style: none;
}
</style>
