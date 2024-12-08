<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import {
	Attachment,
	Episode,
	Subject,
	SubjectTypeEnum,
} from '@runikaros/api-client';
import EpisodePostDialog from './EpisodePostDialog.vue';
import EpisodePutDialog from './EpisodePutDialog.vue';
import { Picture } from '@element-plus/icons-vue';
import { formatDate } from '@/utils/date';
import { apiClient } from '@/utils/api-client';
import EpisodeDetailsDialog from './EpisodeDetailsDialog.vue';
import { useRoute, useRouter } from 'vue-router';
import {
	ElButton,
	ElCol,
	ElDatePicker,
	ElForm,
	ElFormItem,
	ElImage,
	ElInput,
	ElMessage,
	ElRadio,
	ElRadioGroup,
	ElRow,
	ElSwitch,
	ElTable,
	ElTableColumn,
	FormInstance,
	FormRules,
} from 'element-plus';
import {
	episodeGroupLabelMap,
	subjectTypeAliasMap,
	subjectTypes,
} from '@/modules/common/constants';
import AttachmentSelectDialog from '../attachment/AttachmentSelectDialog.vue';
import { base64Encode } from '@/utils/string-util';
import { useSubjectStore } from '@/stores/subject';
import { useI18n } from 'vue-i18n';
import CropperjsDialog from '@/components/image/CropperjsDialog.vue';

const router = useRouter();
const route = useRoute();
const subjectStore = useSubjectStore();
const { t } = useI18n();

watch(route, () => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	fetchSubjectById();
});

const subject = ref<Subject>({
	name: '',
	type: SubjectTypeEnum.Anime,
	nsfw: false,
	name_cn: '',
});
const episodes = ref<Episode[]>([]);
const removeEpisodes = ref<Episode[]>([]);

// eslint-disable-next-line no-unused-vars
const fetchSubjectById = async () => {
	if (subject.value.id) {
		const { data } = await apiClient.subject.searchSubjectById({
			id: subject.value.id as number,
		});
		subject.value = data;
	}
	await fetchEpisodes();
};
const fetchEpisodes = async () => {
	if (!subject.value || !subject.value.id) return;
	const { data } = await apiClient.episode.getAllBySubjectId({
		id: subject.value.id as number,
	});
	episodes.value = data;
};

const subjectRuleFormRules = reactive<FormRules>({
	name: [
		{
			required: true,
			message: t('module.subject.put.message.form-rule.name.required'),
			trigger: 'blur',
		},
		{
			min: 1,
			max: 100,
			message: t('module.subject.put.message.form-rule.name.length'),
			trigger: 'blur',
		},
	],
	summary: [
		{
			required: true,
			message: t('module.subject.put.message.form-rule.summary.required'),
			trigger: 'blur',
		},
		{
			min: 5,
			message: t('module.subject.put.message.form-rule.summary.length'),
			trigger: 'blur',
		},
	],
	type: [
		{
			required: true,
			message: t('module.subject.put.message.form-rule.type.required'),
			trigger: 'change',
		},
	],
	airTime: [
		{
			type: 'date',
			required: true,
			message: t('module.subject.put.message.form-rule.air_time.type'),
			trigger: 'change',
		},
	],
});

const submitBtnLoading = ref(false);
const subjectElFormRef = ref<FormInstance>();
const submitForm = async (formEl: FormInstance | undefined) => {
	if (!formEl) return;
	await formEl.validate(async (valid, fields) => {
		if (valid) {
			submitBtnLoading.value = true;
			await apiClient.subject.updateSubject({
				subject: subject.value,
			});
			// console.debug('subject', subject.value);
			// console.debug('episodes', episodes.value);
			for (var episode of episodes.value) {
				// console.debug('episode', episode);
				episode.subject_id = subject.value.id as number;
				await apiClient.episode.putEpisode({ episode: episode });
			}
			for (var remEp of removeEpisodes.value) {
				await apiClient.episode.deleteById({ id: remEp.id as number });
			}
			removeEpisodes.value = [];
			submitBtnLoading.value = false;
			ElMessage.success(
				t('module.subject.put.message.form-rule.update-success', {
					name: subject.value.name,
				})
			);
			subjectStore.clearSubjectCacheById(subject.value.id as number);
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
		} else {
			console.log('error submit!', fields);
			ElMessage.error(t('module.subject.put.message.form-rule.validate-fail'));
		}
	});
};

const episodePostDialogVisible = ref(false);
const onEpisodePostDialogCloseWithEpsiode = (ep: Episode) => {
	// console.log('receive episode: ', ep);
	episodes.value?.push(ep);
};

const airTimeDateFormatter = (row) => {
	return formatDate(row.air_time, 'yyyy-MM-dd');
};

const removeCurrentRowEpisode = (ep: Episode) => {
	const index: number = episodes.value?.indexOf(ep) as number;
	if (index && index < 0) return;
	episodes.value?.splice(index, 1);
	removeEpisodes.value.push(ep);
};

const currentEpisode = ref<Episode>();
const showEpisodeDetails = (ep: Episode) => {
	currentEpisode.value = ep;
	episodeDetailsDialogVisible.value = true;
};

const episodeDetailsDialogVisible = ref(false);

const attachmentSelectDialogVisible = ref(false);
const onAttachmentSelectDialogColseWithAttachment = (
	attachment: Attachment
) => {
	// console.log('receive file entity: ', file);
	subject.value.cover = attachment.url as string;
	attachmentSelectDialogVisible.value = false;
};

const crrentPutEpisode = ref<Episode>();
const episodePutDialogVisible = ref(false);
const toEPisodeEdit = (episode) => {
	crrentPutEpisode.value = episode;
	episodePutDialogVisible.value = true;
};
const onEpisodePutDialogCloseWithEpsiode = (episode) => {
	currentEpisode.value = episode;
	episodePostDialogVisible.value = false;
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

const cropperjsDialogVisible = ref(false);
const cropperjsOldUrl = ref('');

const onCroperjsUpdateUrl = (newUrl) => {
	console.debug('Croperjs newUrl', newUrl);
	subject.value.cover = newUrl;
};

const oepnCropperjsDialog = () => {
	if (!subject.value.cover) return;
	cropperjsOldUrl.value = subject.value.cover;
	cropperjsDialogVisible.value = true;
};

onMounted(() => {
	//@ts-ignore
	subject.value.id = route.params.id as number;
	fetchSubjectById();
	initEpisodeHasMultiResource();
});
</script>

<template>
	<AttachmentSelectDialog
		v-model:visible="attachmentSelectDialogVisible"
		@close-with-attachment="onAttachmentSelectDialogColseWithAttachment"
	/>
	<el-row :gutter="10">
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
							<el-form-item
								:label="t('module.subject.put.label.air_time')"
								prop="airTime"
							>
								<el-date-picker
									v-model="subject.airTime"
									type="date"
									:placeholder="t('module.subject.put.date-picker.placeholder')"
								/>
							</el-form-item>
						</el-col>
					</el-row>
				</el-form-item>

				<el-form-item :label="t('module.subject.put.label.cover')">
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

				<el-form-item :label="t('module.subject.put.label.name')" prop="name">
					<el-input v-model="subject.name" />
				</el-form-item>

				<el-form-item :label="t('module.subject.put.label.name_cn')">
					<el-input v-model="subject.name_cn" />
				</el-form-item>

				<el-form-item :label="t('module.subject.put.label.type')" prop="type">
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

				<el-form-item
					:label="t('module.subject.put.label.summary')"
					prop="summary"
				>
					<el-input
						v-model="subject.summary"
						maxlength="10000"
						rows="5"
						show-word-limit
						type="textarea"
					/>
				</el-form-item>

				<el-form-item :label="t('module.subject.put.label.infobox')">
					<el-input
						v-model="subject.infobox"
						maxlength="10000"
						rows="15"
						show-word-limit
						type="textarea"
						:placeholder="t('module.subject.put.infobox-input.placeholder')"
					/>
				</el-form-item>

				<EpisodePostDialog
					v-model:visible="episodePostDialogVisible"
					@closeWithEpsiode="onEpisodePostDialogCloseWithEpsiode"
				/>
				<EpisodePutDialog
					v-model:visible="episodePutDialogVisible"
					v-model:ep="crrentPutEpisode"
					@close-with-epsiode="onEpisodePutDialogCloseWithEpsiode"
				/>

				<el-form-item :label="t('module.subject.put.label.episodes')">
					<el-table :data="episodes" @row-dblclick="showEpisodeDetails">
						<el-table-column
							:label="t('module.subject.put.label.episode-table.group')"
							prop="group"
							width="110px"
							show-overflow-tooltip
						>
							<template #default="scoped">
								{{ episodeGroupLabelMap.get(scoped.row.group) }}
							</template>
						</el-table-column>
						<el-table-column
							:label="t('module.subject.put.label.episode-table.sequence')"
							prop="sequence"
							width="90px"
						/>
						<el-table-column
							:label="t('module.subject.put.label.episode-table.name')"
							prop="name"
						/>
						<el-table-column
							:label="t('module.subject.put.label.episode-table.name_cn')"
							prop="name_cn"
						/>
						<el-table-column
							:label="t('module.subject.put.label.episode-table.air_time')"
							prop="air_time"
							:formatter="airTimeDateFormatter"
						/>
						<!-- <el-table-column :label="t('module.subject.put.label.episode-table.description')" prop="description" /> -->
						<el-table-column align="right" width="350">
							<template #header>
								<el-button plain @click="episodePostDialogVisible = true">
									{{ t('module.subject.put.text.button.episode.add') }}
								</el-button>
							</template>
							<template #default="scoped">
								<el-button plain @click="showEpisodeDetails(scoped.row)">
									{{ t('module.subject.put.text.button.episode.details') }}
								</el-button>
								<el-button plain @click="toEPisodeEdit(scoped.row)">
									{{ t('module.subject.put.text.button.episode.edit') }}
								</el-button>
								<el-button
									plain
									type="danger"
									@click="removeCurrentRowEpisode(scoped.row)"
								>
									{{ t('module.subject.put.text.button.episode.remove') }}
								</el-button>
							</template>
						</el-table-column>
					</el-table>
				</el-form-item>

				<el-form-item>
					<el-button
						plain
						:loading="submitBtnLoading"
						@click="submitForm(subjectElFormRef)"
					>
						{{ t('module.subject.put.text.button.subject.create') }}
					</el-button>
				</el-form-item>
			</el-form>
		</el-col>
		<el-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
			<el-row>
				<el-col :span="24">
					<el-button @click="oepnCropperjsDialog">裁剪</el-button>
				</el-col>
			</el-row>
			<br />
			<el-row>
				<el-col :span="24">
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
		</el-col>
	</el-row>
	<EpisodeDetailsDialog
		v-model:visible="episodeDetailsDialogVisible"
		v-model:ep="currentEpisode"
		v-model:subjectId="subject.id"
		v-model:multi-resource="episodeHasMultiResource"
		@remove-episode-files-bind="fetchSubjectById"
	/>
	<CropperjsDialog
		v-model:visible="cropperjsDialogVisible"
		v-model:url="cropperjsOldUrl"
		@update-url="onCroperjsUpdateUrl"
	/>
</template>

<style lang="scss" scoped>
.ik-form-item {
	max-width: 800px;
}
</style>
