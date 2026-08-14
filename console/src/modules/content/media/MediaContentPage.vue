<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue';
import type { Subject } from '@runikaros/api-client';
import { ElButton, ElCol, ElRow } from 'element-plus';
import ContentBrowser from '@/components/modules/content/ContentBrowser.vue';
import SubjectCardLink from '@/components/modules/content/subject/SubjectCardLink.vue';
import { apiClient } from '@/utils/api-client';
import { base64Encode } from '@/utils/string-util';
import LocalDirectoryBindingDialog from '../attachment/LocalDirectoryBindingDialog.vue';
import ScanDirectorySelectDialog from '../attachment/ScanDirectorySelectDialog.vue';
import FileSourceManagerDialog from '../attachment/FileSourceManagerDialog.vue';
import { FolderOpened, Search } from '@element-plus/icons-vue';

type ScanMode = 'EPISODE' | 'AUDIO' | 'IMAGE';

interface Props {
	kind: string;
	title: string;
	types: string[];
	scanMode: ScanMode;
	detailRoute: string;
}

const props = defineProps<Props>();
const searchInput = ref('');
const keyword = ref('');
const subjects = ref<Subject[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const page = ref(1);
const size = ref(12);
const total = ref(0);
const importVisible = ref(false);
const directorySelectVisible = ref(false);
const fileSourceManagerVisible = ref(false);
const selectedDirectoryId = ref('');
const selectedDirectoryPath = ref('');
const bindingDialogRef =
	ref<InstanceType<typeof LocalDirectoryBindingDialog>>();
const pageSizes = [12, 24, 48, 96];

const extractErrorMessage = (requestError: unknown) => {
	const responseData = (
		requestError as { response?: { data?: string | { message?: string } } }
	)?.response?.data;
	if (typeof responseData === 'string' && responseData) return responseData;
	if (typeof responseData === 'object' && responseData?.message) {
		return responseData.message;
	}
	return (requestError as Error)?.message || '加载内容失败';
};

const fetchSubjects = async () => {
	loading.value = true;
	error.value = null;
	try {
		const { data } = await apiClient.get('/api/v1/subjects/condition', {
			params: {
				page: page.value,
				size: size.value,
				keyword: base64Encode(keyword.value),
				types: props.types.join(','),
			},
		});
		subjects.value = (data.items || []) as Subject[];
		page.value = data.page ?? page.value;
		size.value = data.size ?? size.value;
		total.value = data.total ?? 0;
	} catch (requestError) {
		subjects.value = [];
		total.value = 0;
		error.value = extractErrorMessage(requestError);
	} finally {
		loading.value = false;
	}
};

const search = async () => {
	keyword.value = searchInput.value.trim();
	page.value = 1;
	await fetchSubjects();
};

const changePage = async (value: number) => {
	page.value = value;
	await fetchSubjects();
};

const changeSize = async (value: number) => {
	size.value = value;
	page.value = 1;
	await fetchSubjects();
};

const openImport = () => {
	importVisible.value = true;
	void nextTick(() => bindingDialogRef.value?.scan());
};

const selectDirectory = (directoryId: string, path: string) => {
	selectedDirectoryId.value = directoryId;
	selectedDirectoryPath.value = path;
};

const manageSources = () => {
	directorySelectVisible.value = false;
	fileSourceManagerVisible.value = true;
};

const onFileSourcesChanged = () => {
	fileSourceManagerVisible.value = false;
	directorySelectVisible.value = true;
};

const detailPath = (subjectId: string) => `${props.detailRoute}/${subjectId}`;

onMounted(fetchSubjects);
</script>

<template>
	<ContentBrowser
		:title="title"
		:search-model-value="searchInput"
		:search-placeholder="$t(`module.media.${kind}.searchPlaceholder`)"
		:loading="loading"
		:error="error"
		:empty-title="$t(`module.media.${kind}.emptyTitle`)"
		:empty-description="$t(`module.media.${kind}.emptyDescription`)"
		:page="page"
		:size="size"
		:total="total"
		:page-sizes="pageSizes"
		@update:search-model-value="searchInput = $event"
		@search="search"
		@retry="fetchSubjects"
		@update:page="changePage"
		@update:size="changeSize"
	>
		<template #actions>
			<el-button :icon="FolderOpened" @click="directorySelectVisible = true">
				{{
					selectedDirectoryPath ||
					$t('module.attachment.bind.local.directory.select')
				}}
			</el-button>
			<el-button
				type="primary"
				:icon="Search"
				:disabled="!selectedDirectoryId"
				@click="openImport"
			>
				{{ $t('module.attachment.bind.local.preview.action') }}
			</el-button>
		</template>
		<template #empty-actions>
			<span />
		</template>

		<el-row :gutter="20" class="media-content-grid">
			<el-col
				v-for="subject in subjects"
				:key="subject.id"
				:xs="12"
				:sm="8"
				:md="6"
				:lg="4"
				:xl="4"
			>
				<SubjectCardLink
					:id="subject.id ?? ''"
					:name="subject.name"
					:name-cn="subject.name_cn"
					:cover="subject.cover ?? ''"
					:percentage="0"
					:to="detailPath(subject.id ?? '')"
				/>
			</el-col>
		</el-row>
	</ContentBrowser>

	<LocalDirectoryBindingDialog
		ref="bindingDialogRef"
		v-model:visible="importVisible"
		:directory-id="selectedDirectoryId"
		:mode="scanMode"
		hide-setup-controls
		@confirmed="fetchSubjects"
	/>
	<ScanDirectorySelectDialog
		v-model:visible="directorySelectVisible"
		@selected="selectDirectory"
		@manage-sources="manageSources"
	/>
	<FileSourceManagerDialog
		v-model:visible="fileSourceManagerVisible"
		@changed="onFileSourcesChanged"
	/>
</template>

<style scoped>
.media-content-grid {
	row-gap: 20px;
}
</style>
