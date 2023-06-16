<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { formatDate } from '@/utils/date';
import { Episode, Subject, SubjectTypeEnum } from '@runikaros/api-client';
import EpisodeDetailsDialog from './EpisodeDetailsDialog.vue';
import router from '@/router';

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

onMounted(() => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	fetchSubjectById();
});
</script>

<template>
	<el-row>
		<el-col :span="24">
			<el-button plain @click="toSubjectPut"> 编辑 </el-button>
		</el-col>
	</el-row>
	<br />
	<el-row>
		<el-col :xs="24" :sm="24" :md="24" :lg="16" :xl="16">
			<el-row>
				<el-col :xs="24" :sm="24" :md="12" :lg="6" :xl="6">
					<el-image
						style="width: 100%"
						:src="subject.image?.common"
						:zoom-rate="1.2"
						:preview-src-list="new Array(subject.image?.common)"
						:initial-index="4"
						fit="cover"
					/>
				</el-col>
				<el-col :xs="24" :sm="24" :md="12" :lg="18" :xl="18">
					<el-descriptions
						style="margin: 0 5px"
						direction="vertical"
						:column="4"
						size="large"
						border
					>
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
						<el-descriptions-item label="介绍" :span="4">
							{{ subject.summary }}
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
						<el-table-column label="操作" width="100px">
							<template #default="scoped">
								<el-button plain @click="showEpisodeDetails(scoped.row)"
									>详情</el-button
								>
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
