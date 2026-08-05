<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import {
	DirectoryBindingWorkflowEntity,
	LocalScanConfirmRequest,
	LocalScanItem,
	LocalScanPreview,
	LocalScanPreviewRequestModeEnum,
	Subject,
	SubjectTypeEnum,
} from '@runikaros/api-client';
import {
	ElAlert,
	ElButton,
	ElCheckbox,
	ElDescriptions,
	ElDescriptionsItem,
	ElDialog,
	ElDivider,
	ElEmpty,
	ElForm,
	ElFormItem,
	ElInput,
	ElOption,
	ElRadio,
	ElRadioGroup,
	ElSelect,
	ElTable,
	ElTableColumn,
	ElTag,
} from 'element-plus';
import { apiClient } from '@/utils/api-client';
import { base64Encode } from '@/utils/string-util';

type ScanMode = NonNullable<LocalScanPreview['mode']>;
type SubjectSource = 'existing' | 'new';

interface Props {
	visible: boolean;
	directoryId: string;
	workflow?: DirectoryBindingWorkflowEntity;
}

interface RescanDetails {
	report?: string;
	missingPaths: string[];
	probeFailures: string[];
}

const props = defineProps<Props>();
const emit = defineEmits<{
	'update:visible': [value: boolean];
	confirmed: [workflow: DirectoryBindingWorkflowEntity];
	rescanned: [workflow: DirectoryBindingWorkflowEntity];
}>();

const { t } = useI18n();
const unassociatedValue = '__UNASSOCIATED__';
const mode = ref<ScanMode>(LocalScanPreviewRequestModeEnum.Episode);
const preview = ref<LocalScanPreview>();
const previewLoading = ref(false);
const confirmLoading = ref(false);
const rescanLoading = ref(false);
const errorMessage = ref('');
const assignments = reactive<Record<string, string>>({});
const subjectSource = ref<SubjectSource>('existing');
const selectedSubjectId = ref('');
const subjectSearchLoading = ref(false);
const subjectOptions = ref<Subject[]>([]);
const rescanResult = ref<DirectoryBindingWorkflowEntity>();
const rescanDetails = ref<RescanDetails>();
const newSubject = reactive<Subject>({
	name: '',
	type: SubjectTypeEnum.Anime,
	nsfw: false,
	name_cn: '',
});

const dialogVisible = computed({
	get: () => props.visible,
	set: (value: boolean) => emit('update:visible', value),
});

const scanModes: ScanMode[] = [
	LocalScanPreviewRequestModeEnum.Episode,
	LocalScanPreviewRequestModeEnum.Audio,
	LocalScanPreviewRequestModeEnum.Image,
];
const subjectTypes = computed(() =>
	mode.value === LocalScanPreviewRequestModeEnum.Audio
		? [SubjectTypeEnum.Music]
		: Object.values(SubjectTypeEnum)
);
const roles = [
	'PRIMARY',
	'AUTO_ASSOCIATED',
	'PENDING_CONFIRMATION',
	'UNASSOCIATED',
	'UNKNOWN',
] as const;

const items = computed(() => preview.value?.items || []);
const primaryItems = computed(() =>
	items.value.filter((item) => item.role === 'PRIMARY' && item.attachment_id)
);
const pendingItems = computed(() =>
	items.value.filter((item) => item.role === 'PENDING_CONFIRMATION')
);
const groupedItems = computed(() =>
	roles.map((role) => ({
		role,
		items: items.value.filter((item) => item.role === role),
	}))
);
const hasPrimary = computed(() => primaryItems.value.length > 0);
const allPendingHandled = computed(() =>
	pendingItems.value.every(
		(item) =>
			Boolean(item.attachment_id) &&
			Object.prototype.hasOwnProperty.call(
				assignments,
				item.attachment_id as string
			)
	)
);
const hasValidSubject = computed(() => {
	const existingSelected =
		subjectSource.value === 'existing' && Boolean(selectedSubjectId.value);
	const newCompleted =
		subjectSource.value === 'new' &&
		Boolean(newSubject.name.trim() && newSubject.type);
	return Number(existingSelected) + Number(newCompleted) === 1;
});
const canConfirm = computed(
	() =>
		Boolean(preview.value) &&
		hasPrimary.value &&
		allPendingHandled.value &&
		hasValidSubject.value &&
		!confirmLoading.value
);

const clearAssignments = () => {
	Object.keys(assignments).forEach((key) => delete assignments[key]);
};

const defaultSubjectType = (value: ScanMode) => {
	if (value === LocalScanPreviewRequestModeEnum.Audio) {
		return SubjectTypeEnum.Music;
	}
	if (value === LocalScanPreviewRequestModeEnum.Image) {
		return SubjectTypeEnum.Comic;
	}
	return SubjectTypeEnum.Anime;
};

const extractErrorMessage = (error: unknown) => {
	const responseData = (
		error as { response?: { data?: string | { message?: string } } }
	)?.response?.data;
	if (typeof responseData === 'string' && responseData) {
		return responseData;
	}
	if (typeof responseData === 'object' && responseData?.message) {
		return responseData.message;
	}
	return (
		(error as Error)?.message || t('module.attachment.bind.local.error.unknown')
	);
};

const scan = async () => {
	previewLoading.value = true;
	errorMessage.value = '';
	rescanResult.value = undefined;
	rescanDetails.value = undefined;
	try {
		const { data } = await apiClient.binding.previewLocalDirectoryBinding({
			localScanPreviewRequest: {
				directory_id: props.directoryId,
				mode: mode.value,
			},
		});
		preview.value = data;
		clearAssignments();
	} catch (error) {
		errorMessage.value = extractErrorMessage(error);
	} finally {
		previewLoading.value = false;
	}
};

const searchSubjects = async (keyword: string) => {
	subjectSearchLoading.value = true;
	try {
		const { data } = await apiClient.subject.listSubjectsByCondition({
			page: 1,
			size: 20,
			name: base64Encode(keyword),
			type:
				mode.value === LocalScanPreviewRequestModeEnum.Audio
					? SubjectTypeEnum.Music
					: undefined,
		});
		subjectOptions.value = ((data.items || []) as Subject[]).filter((subject) =>
			Boolean(subject.id)
		);
	} catch (error) {
		errorMessage.value = extractErrorMessage(error);
	} finally {
		subjectSearchLoading.value = false;
	}
};

const subjectLabel = (subject: Subject) =>
	subject.name_cn || subject.name || subject.id || '';
const subjectValue = (subject: Subject) => subject.id || '';
const primaryLabel = (item: LocalScanItem) =>
	item.relative_path || item.attachment_id || '';
const primaryValue = (item: LocalScanItem) => item.attachment_id || '';

const confirm = async () => {
	if (!canConfirm.value) {
		return;
	}
	confirmLoading.value = true;
	errorMessage.value = '';
	const request: LocalScanConfirmRequest = {
		directory_id: props.directoryId,
		mode: mode.value,
		assignments: pendingItems.value.map((item) => ({
			attachment_id: item.attachment_id,
			primary_attachment_id:
				assignments[item.attachment_id as string] === unassociatedValue
					? undefined
					: assignments[item.attachment_id as string],
		})),
	};
	if (subjectSource.value === 'existing') {
		request.subject_id = selectedSubjectId.value;
	} else {
		request.subject = {
			name: newSubject.name.trim(),
			name_cn: newSubject.name_cn?.trim() || undefined,
			type: newSubject.type,
			nsfw: newSubject.nsfw,
		};
	}
	try {
		const { data } = await apiClient.binding.confirmLocalDirectoryBinding({
			localScanConfirmRequest: request,
		});
		emit('confirmed', data);
		dialogVisible.value = false;
	} catch (error) {
		errorMessage.value = extractErrorMessage(error);
	} finally {
		confirmLoading.value = false;
	}
};

const parseRescanDetails = (workflow: DirectoryBindingWorkflowEntity) => {
	const details: RescanDetails = { missingPaths: [], probeFailures: [] };
	if (!workflow.localScanState) {
		return details;
	}
	try {
		const state = JSON.parse(workflow.localScanState) as {
			report?: string;
			items?: Array<{
				relative_path?: string;
				missing?: boolean;
				probe_failure_reason?: string;
				tracks?: Array<{ failure_reason?: string }>;
			}>;
		};
		details.report = state.report;
		(state.items || []).forEach((item) => {
			const path =
				item.relative_path || t('module.attachment.bind.local.unknownFile');
			if (item.missing) {
				details.missingPaths.push(path);
			}
			if (item.probe_failure_reason) {
				details.probeFailures.push(`${path}: ${item.probe_failure_reason}`);
			}
			(item.tracks || []).forEach((track) => {
				if (track.failure_reason) {
					details.probeFailures.push(`${path}: ${track.failure_reason}`);
				}
			});
		});
	} catch {
		details.report = t('module.attachment.bind.local.rescan.unreadable');
	}
	return details;
};

const rescan = async () => {
	if (!props.workflow?.id) {
		return;
	}
	rescanLoading.value = true;
	errorMessage.value = '';
	try {
		const { data } = await apiClient.binding.rescanLocalDirectoryBinding({
			id: props.workflow.id,
		});
		rescanResult.value = data;
		rescanDetails.value = parseRescanDetails(data);
		emit('rescanned', data);
	} catch (error) {
		errorMessage.value = extractErrorMessage(error);
	} finally {
		rescanLoading.value = false;
	}
};

const itemFailureMessages = (item: LocalScanItem) => [
	...(item.probe_failure_reason ? [item.probe_failure_reason] : []),
	...(item.tracks || [])
		.map((track) => track.failure_reason)
		.filter((message): message is string => Boolean(message)),
];

watch(mode, (value) => {
	preview.value = undefined;
	selectedSubjectId.value = '';
	errorMessage.value = '';
	clearAssignments();
	newSubject.type = defaultSubjectType(value);
	void searchSubjects('');
});

watch(
	() => props.directoryId,
	() => {
		preview.value = undefined;
		selectedSubjectId.value = '';
		errorMessage.value = '';
		rescanResult.value = undefined;
		rescanDetails.value = undefined;
		clearAssignments();
	}
);

watch(
	() => props.visible,
	(value) => {
		if (value && subjectOptions.value.length === 0) {
			void searchSubjects('');
		}
	}
);
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		:title="t('module.attachment.bind.local.title')"
		width="min(960px, 92vw)"
		:destroy-on-close="false"
	>
		<el-alert
			v-if="errorMessage"
			:title="errorMessage"
			type="error"
			show-icon
			:closable="false"
			class="local-binding-alert"
		/>

		<section v-if="workflow?.id" class="local-binding-section">
			<div class="local-binding-section-header">
				<h3>{{ t('module.attachment.bind.local.rescan.title') }}</h3>
				<el-button :loading="rescanLoading" @click="rescan">
					{{ t('module.attachment.bind.local.rescan.action') }}
				</el-button>
			</div>
			<el-descriptions :column="2" border size="small">
				<el-descriptions-item
					:label="t('module.attachment.bind.local.workflowId')"
				>
					{{ workflow.id }}
				</el-descriptions-item>
				<el-descriptions-item :label="t('module.attachment.bind.local.taskId')">
					{{ workflow.taskId || '-' }}
				</el-descriptions-item>
			</el-descriptions>
			<div v-if="rescanResult" class="local-binding-result">
				<el-alert
					:title="
						t('module.attachment.bind.local.rescan.submitted', {
							taskId: rescanResult.taskId || '-',
						})
					"
					type="success"
					show-icon
					:closable="false"
				/>
				<p v-if="rescanDetails?.report">{{ rescanDetails.report }}</p>
				<div v-if="rescanDetails?.missingPaths.length">
					<strong>{{
						t('module.attachment.bind.local.rescan.missing')
					}}</strong>
					<ul>
						<li v-for="path in rescanDetails.missingPaths" :key="path">
							{{ path }}
						</li>
					</ul>
				</div>
				<div v-if="rescanDetails?.probeFailures.length">
					<strong>{{
						t('module.attachment.bind.local.rescan.probeFailures')
					}}</strong>
					<ul>
						<li v-for="failure in rescanDetails.probeFailures" :key="failure">
							{{ failure }}
						</li>
					</ul>
				</div>
			</div>
		</section>

		<el-divider v-if="workflow?.id" />

		<section class="local-binding-section">
			<div class="local-binding-section-header">
				<h3>{{ t('module.attachment.bind.local.preview.title') }}</h3>
				<el-button type="primary" :loading="previewLoading" @click="scan">
					{{ t('module.attachment.bind.local.preview.action') }}
				</el-button>
			</div>
			<el-form label-position="top">
				<el-form-item :label="t('module.attachment.bind.local.mode.label')">
					<el-radio-group v-model="mode">
						<el-radio
							v-for="value in scanModes"
							:key="value"
							:value="value"
							border
						>
							{{ t(`module.attachment.bind.local.mode.${value}`) }}
						</el-radio>
					</el-radio-group>
				</el-form-item>
			</el-form>
		</section>

		<template v-if="preview">
			<el-alert
				v-if="items.length === 0"
				:title="t('module.attachment.bind.local.preview.empty')"
				type="info"
				show-icon
				:closable="false"
			/>
			<el-alert
				v-else-if="!hasPrimary"
				:title="t('module.attachment.bind.local.preview.noPrimary')"
				type="warning"
				show-icon
				:closable="false"
			/>

			<section
				v-for="group in groupedItems"
				:key="group.role"
				class="local-binding-section local-scan-group"
			>
				<div class="local-binding-section-header">
					<h3>{{ t(`module.attachment.bind.local.role.${group.role}`) }}</h3>
					<el-tag effect="plain">{{ group.items.length }}</el-tag>
				</div>
				<el-empty
					v-if="group.items.length === 0"
					:description="t('module.attachment.bind.local.preview.noItems')"
					:image-size="48"
				/>
				<el-table v-else :data="group.items" size="small" border>
					<el-table-column
						prop="relative_path"
						:label="t('module.attachment.bind.local.file')"
						min-width="220"
						show-overflow-tooltip
					/>
					<el-table-column
						prop="physical_type"
						:label="t('module.attachment.bind.local.physicalType.label')"
						width="120"
					>
						<template #default="scope">
							{{
								t(
									`module.attachment.bind.local.physicalType.${scope.row.physical_type}`
								)
							}}
						</template>
					</el-table-column>
					<el-table-column
						:label="t('module.attachment.bind.local.metadata')"
						min-width="180"
					>
						<template #default="scope">
							<span
								v-for="(value, key) in scope.row.display_metadata"
								:key="key"
								class="local-binding-metadata"
							>
								{{ key }}: {{ value }}
							</span>
							<span v-if="!Object.keys(scope.row.display_metadata || {}).length"
								>-</span
							>
						</template>
					</el-table-column>
					<el-table-column
						:label="t('module.attachment.bind.local.trackStatus')"
						min-width="180"
					>
						<template #default="scope">
							<template v-if="itemFailureMessages(scope.row).length">
								<el-tag
									v-for="failure in itemFailureMessages(scope.row)"
									:key="failure"
									type="danger"
									effect="plain"
									class="local-binding-failure"
								>
									{{ failure }}
								</el-tag>
							</template>
							<span v-else>
								{{
									t('module.attachment.bind.local.probeSuccess', {
										count: scope.row.tracks?.length || 0,
									})
								}}
							</span>
						</template>
					</el-table-column>
					<el-table-column
						v-if="group.role === 'PENDING_CONFIRMATION'"
						:label="t('module.attachment.bind.local.assignment.label')"
						min-width="220"
					>
						<template #default="scope">
							<el-select
								v-if="scope.row.attachment_id"
								v-model="assignments[scope.row.attachment_id]"
								:placeholder="
									t('module.attachment.bind.local.assignment.placeholder')
								"
							>
								<el-option
									:label="
										t('module.attachment.bind.local.assignment.unassociated')
									"
									:value="unassociatedValue"
								/>
								<el-option
									v-for="primary in primaryItems"
									:key="primary.attachment_id"
									:label="primaryLabel(primary)"
									:value="primaryValue(primary)"
								/>
							</el-select>
							<span v-else>{{
								t('module.attachment.bind.local.assignment.invalid')
							}}</span>
						</template>
					</el-table-column>
				</el-table>
			</section>

			<el-divider />
			<section class="local-binding-section">
				<h3>{{ t('module.attachment.bind.local.subject.title') }}</h3>
				<el-radio-group v-model="subjectSource" class="local-subject-source">
					<el-radio value="existing" border>
						{{ t('module.attachment.bind.local.subject.existing') }}
					</el-radio>
					<el-radio value="new" border>
						{{ t('module.attachment.bind.local.subject.new') }}
					</el-radio>
				</el-radio-group>

				<el-form v-if="subjectSource === 'existing'" label-position="top">
					<el-form-item
						:label="t('module.attachment.bind.local.subject.select')"
					>
						<el-select
							v-model="selectedSubjectId"
							filterable
							remote
							reserve-keyword
							:remote-method="searchSubjects"
							:loading="subjectSearchLoading"
							:placeholder="
								t('module.attachment.bind.local.subject.searchPlaceholder')
							"
						>
							<el-option
								v-for="subject in subjectOptions"
								:key="subject.id || subject.name"
								:label="subjectLabel(subject)"
								:value="subjectValue(subject)"
							/>
						</el-select>
					</el-form-item>
				</el-form>

				<el-form v-else label-position="top">
					<el-form-item
						:label="t('module.attachment.bind.local.subject.name')"
						required
					>
						<el-input v-model="newSubject.name" />
					</el-form-item>
					<el-form-item
						:label="t('module.attachment.bind.local.subject.nameCn')"
					>
						<el-input v-model="newSubject.name_cn" />
					</el-form-item>
					<el-form-item
						:label="t('module.attachment.bind.local.subject.type')"
						required
					>
						<el-select v-model="newSubject.type">
							<el-option
								v-for="type in subjectTypes"
								:key="type"
								:label="
									t(`module.attachment.bind.local.subject.typeValue.${type}`)
								"
								:value="type"
							/>
						</el-select>
					</el-form-item>
					<el-form-item>
						<el-checkbox v-model="newSubject.nsfw">
							{{ t('module.attachment.bind.local.subject.nsfw') }}
						</el-checkbox>
					</el-form-item>
				</el-form>
			</section>
		</template>

		<template #footer>
			<el-button @click="dialogVisible = false">
				{{ t('module.attachment.bind.local.cancel') }}
			</el-button>
			<el-button
				type="primary"
				:disabled="!canConfirm"
				:loading="confirmLoading"
				@click="confirm"
			>
				{{ t('module.attachment.bind.local.confirm') }}
			</el-button>
		</template>
	</el-dialog>
</template>

<style scoped>
.local-binding-alert,
.local-binding-result,
.local-binding-section {
	margin-bottom: 16px;
}

.local-binding-section-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	margin-bottom: 12px;
}

.local-binding-section h3 {
	margin: 0 0 12px;
	font-size: 16px;
}

.local-binding-section-header h3 {
	margin: 0;
}

.local-scan-group {
	padding-top: 12px;
	border-top: 1px solid var(--el-border-color-lighter);
}

.local-binding-metadata,
.local-binding-failure {
	display: block;
	margin-bottom: 4px;
	white-space: normal;
}

.local-subject-source {
	margin-bottom: 16px;
}

.local-binding-result ul {
	margin: 8px 0 0;
	padding-left: 20px;
}
</style>
