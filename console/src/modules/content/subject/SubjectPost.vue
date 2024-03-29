<script setup lang="ts">
import { reactive, ref } from 'vue';
import { Episode, Subject, SubjectTypeEnum } from '@runikaros/api-client';
import EpisodePostDialog from './EpisodePostDialog.vue';
import { Picture } from '@element-plus/icons-vue';
import { formatDate } from '@/utils/date';
import { apiClient } from '@/utils/api-client';
import EpisodeDetailsDialog from './EpisodeDetailsDialog.vue';
import { useRouter } from 'vue-router';
import {
	ElMessage,
	FormInstance,
	FormRules,
	ElRow,
	ElCol,
	ElForm,
	ElFormItem,
	ElSwitch,
	ElDatePicker,
	ElInput,
	ElButton,
	ElRadioGroup,
	ElRadio,
	ElTable,
	ElTableColumn,
	ElImage,
} from 'element-plus';
import {
	episodeGroupLabelMap,
	subjectTypes,
	subjectTypeAliasMap,
} from '@/modules/common/constants';
import AttachmentSelectDialog from '../attachment/AttachmentSelectDialog.vue';
import { base64Encode } from '@/utils/string-util';

const router = useRouter();

const subject = ref<Subject>({
	name: '',
	type: SubjectTypeEnum.Anime,
	nsfw: false,
	name_cn: '',
	episodes: [],
});

const subjectRuleFormRules = reactive<FormRules>({
	name: [
		{ required: true, message: '请输入条目原始名称', trigger: 'blur' },
		{ min: 1, max: 100, message: '长度应该在 1 到 100 之间', trigger: 'blur' },
	],
	summary: [
		{ required: true, message: '请输入条目介绍', trigger: 'blur' },
		{ min: 5, message: '长度应该大于5', trigger: 'blur' },
	],
	type: [
		{
			required: true,
			message: '请选择一个条目类型',
			trigger: 'change',
		},
	],
	airTime: [
		{
			type: 'date',
			required: true,
			message: '请选择一个时间',
			trigger: 'change',
		},
	],
});

const subjectElFormRef = ref<FormInstance>();
const submitForm = async (formEl: FormInstance | undefined) => {
	if (!formEl) return;
	await formEl.validate(async (valid, fields) => {
		if (valid) {
			await apiClient.subject
				.createSubject({
					subject: subject.value,
				})
				.then(() => {
					router.push(
						'/subjects?name=' +
							base64Encode(encodeURI(subject.value.name)) +
							'&nameCn=' +
							base64Encode(encodeURI(subject.value.name_cn as string)) +
							'&nsfw=' +
							subject.value.nsfw +
							'&type=' +
							subject.value.type
					);
				});
		} else {
			console.log('error submit!', fields);
			ElMessage.error('请检查所填内容是否有必要项缺失。');
		}
	});
};

const episodePostDialogVisible = ref(false);
const onEpisodePostDialogCloseWithEpsiode = (ep: Episode) => {
	console.log('receive episode: ', ep);
	subject.value.episodes?.push(ep);
};

const airTimeDateFormatter = (row) => {
	return formatDate(row.air_time, 'yyyy-MM-dd');
};

const removeCurrentRowEpisode = (ep: Episode) => {
	const index: number = subject.value.episodes?.indexOf(ep) as number;
	if (index && index < 0) return;
	subject.value.episodes?.splice(index, 1);
};

const currentEpisode = ref<Episode>();
const showEpisodeDetails = (ep: Episode) => {
	currentEpisode.value = ep;
	episodeDetailsDialogVisible.value = true;
};

const episodeDetailsDialogVisible = ref(false);

const attachmentSelectDialogVisible = ref(false);
const onCloseWithAttachment = (attachment) => {
	subject.value.cover = attachment.url as string;
	attachmentSelectDialogVisible.value = false;
};
</script>

<template>
	<AttachmentSelectDialog
		v-model:visible="attachmentSelectDialogVisible"
		@close-with-attachment="onCloseWithAttachment"
	/>

	<el-row>
		<el-col :xs="24" :sm="24" :md="24" :lg="16" :xl="16">
			<el-form
				ref="subjectElFormRef"
				:rules="subjectRuleFormRules"
				:model="subject"
				label-width="85px"
			>
				<el-form-item label="NSFW">
					<el-row>
						<el-col :span="8">
							<el-form-item>
								<el-switch v-model="subject.nsfw" />
							</el-form-item>
						</el-col>
						<el-col :span="16">
							<el-form-item label="发布时间" prop="airTime">
								<el-date-picker
									v-model="subject.airTime"
									type="date"
									placeholder="请选择一天"
								/>
							</el-form-item>
						</el-col>
					</el-row>
				</el-form-item>

				<el-form-item label="条目封面">
					<el-input v-model="subject.cover" clearable>
						<template #prepend>
							<el-button
								:icon="Picture"
								plain
								@click="attachmentSelectDialogVisible = true"
							/>
						</template>
					</el-input>
				</el-form-item>

				<el-form-item label="条目名称" prop="name">
					<el-input v-model="subject.name" />
				</el-form-item>

				<el-form-item label="条目中文名">
					<el-input v-model="subject.name_cn" />
				</el-form-item>

				<el-form-item label="条目类型" prop="type">
					<el-radio-group v-model="subject.type">
						<el-radio
							v-for="type in subjectTypes"
							:key="type"
							:label="type"
							border
							style="margin-right: 8px"
						>
							{{ subjectTypeAliasMap.get(type) }}
						</el-radio>
					</el-radio-group>
				</el-form-item>

				<el-form-item label="介绍" prop="summary">
					<el-input
						v-model="subject.summary"
						:autosize="{ minRows: 2 }"
						maxlength="10000"
						rows="2"
						show-word-limit
						type="textarea"
					/>
				</el-form-item>

				<el-form-item label="InfoBox">
					<el-input
						v-model="subject.infobox"
						:autosize="{ minRows: 2 }"
						maxlength="10000"
						rows="2"
						show-word-limit
						type="textarea"
						placeholder="一行一个 key:value, 例子 中文名: 天降之物"
					/>
				</el-form-item>

				<EpisodePostDialog
					v-model:visible="episodePostDialogVisible"
					@closeWithEpsiode="onEpisodePostDialogCloseWithEpsiode"
				/>

				<el-form-item label="剧集">
					<el-table :data="subject.episodes" @row-dblclick="showEpisodeDetails">
						<el-table-column
							label="分组"
							prop="group"
							width="110px"
							show-overflow-tooltip
						>
							<template #default="scoped">
								{{ episodeGroupLabelMap.get(scoped.row.group) }}
							</template>
						</el-table-column>
						<el-table-column label="序号" prop="sequence" width="80px" />
						<el-table-column label="原始名称" prop="name" />
						<el-table-column label="中文名称" prop="name_cn" />
						<el-table-column
							label="发布日期"
							prop="air_time"
							:formatter="airTimeDateFormatter"
						/>
						<!-- <el-table-column label="描述" prop="description" /> -->
						<el-table-column align="right">
							<template #header>
								<el-button plain @click="episodePostDialogVisible = true">
									添加剧集
								</el-button>
							</template>
							<template #default="scoped">
								<el-button plain @click="showEpisodeDetails(scoped.row)">
									详情
								</el-button>
								<el-button
									plain
									type="danger"
									@click="removeCurrentRowEpisode(scoped.row)"
								>
									删除
								</el-button>
							</template>
						</el-table-column>
					</el-table>
				</el-form-item>

				<el-form-item>
					<el-button plain @click="submitForm(subjectElFormRef)">
						创建
					</el-button>
				</el-form-item>
			</el-form>
		</el-col>
		<el-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
			<span v-if="subject.cover">
				<el-image
					style="width: 100%"
					:src="subject.cover"
					:zoom-rate="1.2"
					:preview-src-list="new Array(subject.cover)"
					:initial-index="4"
					fit="cover"
				/>
			</span>
		</el-col>
	</el-row>
	<EpisodeDetailsDialog
		v-model:visible="episodeDetailsDialogVisible"
		v-model:subjectId="subject.id"
		v-model:ep="currentEpisode"
	/>
</template>

<style lang="scss" scoped></style>
