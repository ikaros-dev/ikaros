<script setup lang="ts">
import {
	Attachment,
	AttachmentReferenceTypeEnum,
	EpisodeCollection,
	SubjectCollection,
} from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { formatDate } from '@/utils/date';
import {
	Episode,
	Subject,
	SubjectTypeEnum,
	SubjectTag,
} from '@runikaros/api-client';
import EpisodeDetailsDialog from './EpisodeDetailsDialog.vue';
import router from '@/router';
import { Check, Close } from '@element-plus/icons-vue';
import SubjectSyncDialog from './SubjectSyncDialog.vue';
import { useRoute } from 'vue-router';
import { onMounted, ref, watch } from 'vue';
import {
	ElButton,
	ElCol,
	ElDescriptions,
	ElDescriptionsItem,
	ElImage,
	ElMessage,
	ElPopconfirm,
	ElRow,
	ElTable,
	ElTableColumn,
	ElSelect,
	ElOption,
	ElInput,
	ElTag,
} from 'element-plus';
import SubjectRemoteActionDialog from './SubjectRemoteActionDialog.vue';
import { useSettingStore } from '@/stores/setting';
import { episodeGroupLabelMap } from '@/modules/common/constants';
import { useUserStore } from '@/stores/user';
import SubjectRelationDialog from './SubjectRelationDialog.vue';
import { useSubjectStore } from '@/stores/subject';
import { nextTick } from 'vue';
import AttachmentMultiSelectDialog from '@/modules/content/attachment/AttachmentMultiSelectDialog.vue';
import AttachmentSelectDialog from '@/modules/content/attachment/AttachmentSelectDialog.vue';
import SubjectCollectDialog from '@/components/modules/content/subject/SubjectCollectDialog.vue';

const route = useRoute();
const settingStore = useSettingStore();
const userStore = useUserStore();
const subjectStore = useSubjectStore();

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

// eslint-disable-next-line no-unused-vars
const fetchSubjectById = async () => {
	if (subject.value.id) {
		subject.value = await subjectStore.fetchSubjectById(
			subject.value.id as number
		);
	}
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
			ElMessage.success('删除条目' + subject.value.name + '成功');
			router.push('/subjects');
		});
};

const subjectSyncDialogVisible = ref(false);
const openSubjectSyncDialog = () => {
	subjectSyncDialogVisible.value = true;
};
const onSubjectSyncDialogCloseWithSubjectName = () => {
	ElMessage.success('请求更新条目信息成功');
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

const notCollectText = '未收藏';
const clickCollectText = '点击收藏';
const removeCollectText = '取消收藏';
const collectText = '已收藏';
const collectButtonText = ref(notCollectText);
const updateSubjectCollection = async () => {
	var isUnCollect = subjectCollection.value && subjectCollection.value.type;
	if (!isUnCollect) {
		return;
	}
	await apiClient.subjectCollection.collectSubject({
		userId: userStore.currentUser?.entity?.id as number,
		subjectId: subject.value.id as number,
		type: subjectCollection.value.type as
			| 'WISH'
			| 'DOING'
			| 'DONE'
			| 'SHELVE'
			| 'DISCARD',
	});
	ElMessage.success('更新成功');
};
const updateSubjectCollectionProgress = async () => {
	await apiClient.subjectCollection.updateSubjectCollectionMainEpProgress({
		userId: userStore.currentUser?.entity?.id as number,
		subjectId: subject.value.id as number,
		progress: subjectCollection.value.main_ep_progress as number,
	});
	ElMessage.success('更新条目正片观看进度成功');
	await fetchDatas();
};

const subjectCollectDialogVisible = ref(false);
const changeSubjectCollectState = async () => {
	var isUnCollect = subjectCollection.value && subjectCollection.value.type;
	console.log('isUnCollect', isUnCollect);
	if (isUnCollect) {
		// un collect
		await apiClient.subjectCollection.removeSubjectCollect({
			userId: userStore.currentUser?.entity?.id as number,
			subjectId: subject.value.id as number,
		});
		ElMessage.success('取消收藏成功');
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
	const rsp = await apiClient.subjectCollection.findSubjectCollection({
		userId: userStore.currentUser?.entity?.id as number,
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
		await apiClient.episodeCollection.findEpisodeCollectionsByUserIdAndSubjectId(
			{
				userId: userStore.currentUser?.entity?.id as number,
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
	await apiClient.episodeCollection.updateEpisodeCollectionFinish({
		userId: userStore.currentUser?.entity?.id as number,
		episodeId: episode.id as number,
		finish: isFinish,
	});
	ElMessage.success('标记是否观看完成成功');
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
	ElMessage.success('单个剧集和附件匹配成功');
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
	ElMessage.success('移除标签【' + tag.name + '】成功');
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
	var tagName = newTag.value.name;
	if (
		!tagName ||
		tagName === '' ||
		tags.value.filter((t) => tagName === t.name).length > 0
	) {
		ElMessage.warning('标签名为空或者重复，跳过创建标签操作。');
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
	ElMessage.success('新建标签【' + newTag.value.name + '】成功');
	await fetchTags();
	newTagInputVisible.value = false;
	newTagInputRef.value!.input!.value = '';
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
			<el-button plain @click="toSubjectPut"> 编辑</el-button>
			<el-button plain @click="openSubjectSyncDialog"> 更新</el-button>
			<el-popconfirm title="您确定要删除该条目吗？" @confirm="deleteSubject">
				<template #reference>
					<el-button plain type="danger"> 删除</el-button>
				</template>
			</el-popconfirm>

			<el-button plain @click="openSubjectRelationDialog"> 关系</el-button>
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
						:preview-src-list="new Array(subject.cover)  as string[]"
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
						<el-descriptions-item label="名称" :span="1">
							{{ subject.name }}
						</el-descriptions-item>
						<el-descriptions-item label="中文名称" :span="1">
							{{ subject.name_cn }}
						</el-descriptions-item>
						<el-descriptions-item label="放送时间" :span="1">
							{{ subject.airTime }}
						</el-descriptions-item>
						<el-descriptions-item label="类型" :span="1">
							{{ subject.type }}
						</el-descriptions-item>
						<el-descriptions-item label="NSFW" :span="1">
							{{ subject.nsfw }}
						</el-descriptions-item>
						<el-descriptions-item label="介绍" :span="6">
							{{ subject.summary }}
						</el-descriptions-item>
					</el-descriptions>
					<el-descriptions
						v-if="subject.syncs && subject.syncs.length > 0"
						size="large"
						border
					>
						<el-descriptions-item label="同步平台">
							<span v-for="(sync, index) in subject.syncs" :key="index">
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
						<el-descriptions-item label="标签">
							<el-tag
								v-for="tag in tags"
								:key="tag.id"
								closable
								style="margin-right: 5px"
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
							/>
							<el-button v-else size="small" @click="showNewTagInput">
								新增标签
							</el-button>
						</el-descriptions-item>
					</el-descriptions>
					<el-descriptions size="large" border>
						<el-descriptions-item label="收藏状态">
							<el-popconfirm
								:title="
									'您确定要' +
									(subjectCollection && subjectCollection.type
										? '取消收藏'
										: '收藏') +
									'该条目吗？'
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
								<el-option label="想看" value="WISH" />
								<el-option label="在看" value="DOING" />
								<el-option label="看完" value="DONE" />
								<el-option label="搁置" value="SHELVE" />
								<el-option label="抛弃" value="DISCARD" />
							</el-select>
							&nbsp;&nbsp; 观看进度：
							<el-input
								v-model="subjectCollection.main_ep_progress"
								placeholder="输入观看进度，回车更新"
								style="width: 200px"
								@change="updateSubjectCollectionProgress"
							/>
						</el-descriptions-item>
					</el-descriptions>
				</el-col>
			</el-row>
			<el-row>
				<el-col :span="24">
					<el-table :data="subject.episodes" @row-dblclick="showEpisodeDetails">
						<el-table-column
							label="分组"
							prop="group"
							width="110px"
							show-overflow-tooltip
							sortable
						>
							<template #default="scoped">
								{{ episodeGroupLabelMap.get(scoped.row.group) }}
							</template>
						</el-table-column>
						<el-table-column
							label="序号"
							prop="sequence"
							width="80px"
							sortable
						/>
						<el-table-column label="原始名称" prop="name" />
						<el-table-column label="中文名称" prop="name_cn" />
						<el-table-column
							label="发布日期"
							prop="air_time"
							sortable
							:formatter="airTimeDateFormatter"
						/>
						<el-table-column label="操作" width="320">
							<template #header>
								<el-button
									plain
									:loading="batchMatchingSubjectButtonLoading"
									@click="
										() => {
											attachmentMultiSelectDialogVisible = true;
											bindMasterIsEpisodeFlag = false;
										}
									"
								>
									批量绑定资源
								</el-button>
							</template>
							<template #default="scoped">
								<el-button plain @click="showEpisodeDetails(scoped.row)">
									详情
								</el-button>

								<el-button
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
											? '重置'
											: '看完'
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
									<span v-if="scoped.row.resources[0].canRead"> 推送 </span>
									<span v-else> 拉取 </span>
								</el-button>
							</template>
						</el-table-column>
					</el-table>
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
