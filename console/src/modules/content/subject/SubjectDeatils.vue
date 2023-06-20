<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { formatDate } from '@/utils/date';
import {
	Episode,
	Subject,
	SubjectTypeEnum,
	FileEntity,
} from '@runikaros/api-client';
import EpisodeDetailsDialog from './EpisodeDetailsDialog.vue';
import FileSelectDialog from '../file/FileSelectDialog.vue';
import router from '@/router';
import { Check, Close } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import FileMultiSelectDialog from '../file/FileMultiSelectDialog.vue';
import SubjectSyncDialog from './SubjectSyncDialog.vue';

const route = useRoute();

watch(route, () => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	fetchSubjectById();
});

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
		const { data } = await apiClient.subject.searchSubjectById({
			id: subject.value.id as number,
		});
		subject.value = data;
	}
};

// eslint-disable-next-line no-unused-vars
const infoMap = ref<Map<string, string>>();

watch(subject, () => {
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

const fileSelectDialogVisible = ref(false);
const onFileSelectDialogClose = () => {
	fileSelectDialogVisible.value = false;
};
// eslint-disable-next-line no-unused-vars
const onFileSelectDialogCloseWithFile = (file: FileEntity) => {
	// console.log('receive file entity: ', file);
	bindEpisodeAndFile(
		currentOperateEpisode.value?.id as number,
		file.id as number
	);
	fileSelectDialogVisible.value = false;
};
const currentOperateEpisode = ref<Episode>();
// eslint-disable-next-line no-unused-vars
const bingResources = (episode: Episode) => {
	currentOperateEpisode.value = episode;
	fileSelectDialogVisible.value = true;
};

const bindEpisodeAndFile = async (episodeId: number, fileId: number) => {
	await apiClient.episodefile
		.createEpisodeFile({
			episodeId: episodeId,
			fileId: fileId,
		})
		.then(() => {
			fetchSubjectById();
		});
};

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

const batchMatchingButtonLoading = ref(false);
const fileMultiSelectDialogVisible = ref(false);
const onCloseWithFileIdArr = async (fileIds) => {
	console.log('receive fileIdArr', fileIds);
	// eslint-disable-next-line no-unused-vars
	const subjectId = subject.value.id;
	batchMatchingButtonLoading.value = true;
	await apiClient.episodefile
		.batchMatchingEpisodeFile({
			batchMatchingEpisodeFile: {
				subjectId: subjectId as number,
				fileIds: fileIds,
			},
		})
		.then(() => {
			ElMessage.success('批量匹配剧集和资源成功');
			window.location.reload();
		})
		.finally(() => {
			batchMatchingButtonLoading.value = false;
		});
};

const subjectSyncDialogVisible = ref(false);
const onSubjectSyncDialogCloseWithSubjectName = () => {
	ElMessage.success('请求同步条目信息成功');
	fetchSubjectById();
};

onMounted(() => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	fetchSubjectById();
});
</script>

<template>
	<FileMultiSelectDialog
		v-model:visible="fileMultiSelectDialogVisible"
		searchFileType="VIDEO"
		@closeWithFileIdArr="onCloseWithFileIdArr"
	/>
	<SubjectSyncDialog
		v-model:visible="subjectSyncDialogVisible"
		@closeWithSubjectName="onSubjectSyncDialogCloseWithSubjectName"
	/>
	<el-row>
		<el-col :span="24">
			<el-button plain @click="toSubjectPut"> 编辑 </el-button>
			<el-popconfirm title="您确定要删除该条目吗？" @confirm="deleteSubject">
				<template #reference>
					<el-button plain type="danger"> 删除 </el-button>
				</template>
			</el-popconfirm>
			<el-button disabled plain @click="subjectSyncDialogVisible = true">
				信息拉取
			</el-button>
		</el-col>
	</el-row>
	<br />
	<el-row>
		<el-col :xs="24" :sm="24" :md="24" :lg="16" :xl="16">
			<el-row>
				<el-col :xs="24" :sm="24" :md="12" :lg="6" :xl="6">
					<el-image
						style="width: 100%"
						:src="subject.cover"
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
						:column="5"
						size="large"
						border
					>
						<el-descriptions-item label="ID">
							{{ subject.id }}
						</el-descriptions-item>
						<el-descriptions-item label="名称">
							{{ subject.name }}
						</el-descriptions-item>
						<el-descriptions-item label="中文名称">
							{{ subject.name_cn }}
						</el-descriptions-item>
						<el-descriptions-item label="放送时间">
							{{ subject.airTime }}
						</el-descriptions-item>
						<el-descriptions-item label="NSFW">
							{{ subject.nsfw }}
						</el-descriptions-item>
						<el-descriptions-item label="介绍" :span="5">
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
								{{ sync.platform }} : {{ sync.platformId }}
							</span>
						</el-descriptions-item>
					</el-descriptions>
				</el-col>
			</el-row>
			<el-row>
				<el-col :span="24">
					<el-table :data="subject.episodes" @row-dblclick="showEpisodeDetails">
						<el-table-column
							label="序号"
							sortable
							prop="sequence"
							width="80px"
						/>
						<el-table-column label="原始名称" prop="name" />
						<el-table-column label="中文名称" prop="name_cn" />
						<el-table-column
							label="发布日期"
							prop="air_time"
							:formatter="airTimeDateFormatter"
						/>
						<el-table-column label="操作" width="175px">
							<template #header>
								<el-button
									plain
									:loading="batchMatchingButtonLoading"
									@click="fileMultiSelectDialogVisible = true"
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
										scoped.row.resources && scoped.row.resources.length > 0
											? Check
											: Close
									"
									@click="bingResources(scoped.row)"
								>
									绑定
								</el-button>
							</template>
						</el-table-column>
					</el-table>
				</el-col>
			</el-row>
		</el-col>
		<el-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
			<ul>
				<li v-for="(value, key) in infoMap" :key="key" class="infinite-list">
					<span style="font-weight: 800">{{ value[0] }}</span> :
					{{ value[1] }}
				</li>
			</ul>
		</el-col>
	</el-row>
	<EpisodeDetailsDialog
		v-model:visible="episodeDetailsDialogVisible"
		v-model:episode="currentEpisode"
		@removeEpisodeFileBind="fetchSubjectById"
	/>
	<FileSelectDialog
		v-model:visible="fileSelectDialogVisible"
		@close="onFileSelectDialogClose"
		@closeWithFileEntity="onFileSelectDialogCloseWithFile"
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
