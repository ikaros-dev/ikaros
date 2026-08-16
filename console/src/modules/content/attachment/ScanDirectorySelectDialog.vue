<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { Attachment, AttachmentDriver } from '@runikaros/api-client';
import {
	ElAlert,
	ElBreadcrumb,
	ElBreadcrumbItem,
	ElButton,
	ElDialog,
	ElEmpty,
	ElIcon,
	ElScrollbar,
} from 'element-plus';
import { Folder, Setting } from '@element-plus/icons-vue';
import RefreshButton from '@/components/common/RefreshButton.vue';
import { apiClient } from '@/utils/api-client';
import { attachmentRootId } from '@/modules/common/constants';
import { useI18n } from 'vue-i18n';

interface DirectoryPath {
	id: string;
	name: string;
	driverId: string;
	isSourceRoot: boolean;
}

const props = defineProps<{ visible: boolean }>();
const { t } = useI18n();
const emit = defineEmits<{
	'update:visible': [value: boolean];
	selected: [directoryId: string, path: string];
	manageSources: [];
}>();

const loading = ref(false);
const refreshing = ref(false);
const errorMessage = ref('');
const directories = ref<Attachment[]>([]);
const paths = ref<DirectoryPath[]>([]);
const sourceRoots = ref<DirectoryPath[]>([]);
const showingSystemRoot = computed(() => paths.value.length === 0);
const currentDirectory = computed(() => paths.value.at(-1));
const canSelectCurrent = computed(
	() => Boolean(currentDirectory.value) && !currentDirectory.value?.isSourceRoot
);
const currentPath = computed(() =>
	paths.value.map((path) => path.name).join(' / ')
);

const isDirectory = (attachment: Attachment) =>
	attachment.type === 'Directory' || attachment.type === 'Driver_Directory';

const listDirectories = async (parentId: string, refresh = false) => {
	const request = {
		page: 1,
		size: 999999,
		parentId,
	};
	const { data } = refresh
		? await apiClient.attachmentDriver.listAttachmentsByCondition({
				...request,
				refresh: true,
			})
		: await apiClient.attachment.listAttachmentsByCondition1(request);
	return ((data.items || []) as Attachment[]).filter(isDirectory);
};

const loadCurrentDirectory = async (refresh = false) => {
	const current = currentDirectory.value;
	if (!current) {
		directories.value = sourceRoots.value.map((source) => ({
			id: source.id,
			name: source.name,
			type: 'Driver_Directory',
			driverId: source.driverId,
		}));
		return;
	}
	directories.value = await listDirectories(current.id, refresh);
};

const loadSources = async () => {
	loading.value = true;
	errorMessage.value = '';
	paths.value = [];
	try {
		const [{ data: driverPage }, rootDirectories] = await Promise.all([
			apiClient.attachmentDriver.listDriversByCondition({
				page: 1,
				size: 1000,
			}),
			listDirectories(attachmentRootId),
		]);
		const drivers = ((driverPage.items || []) as AttachmentDriver[]).filter(
			(driver) => driver.enable && driver.type === 'LOCAL' && driver.id
		);
		const driverMap = new Map(drivers.map((driver) => [driver.id, driver]));
		sourceRoots.value = rootDirectories
			.filter(
				(directory) =>
					directory.type === 'Driver_Directory' &&
					directory.id &&
					directory.driverId &&
					driverMap.has(directory.driverId)
			)
			.map((directory) => ({
				id: directory.id as string,
				name: t('module.attachment.dialog.scan-directory.source-root', {
					id: (directory.driverId as string).slice(0, 8),
				}),
				driverId: directory.driverId as string,
				isSourceRoot: true,
			}));
		if (sourceRoots.value.length === 1) {
			paths.value = [sourceRoots.value[0]];
		}
		await loadCurrentDirectory();
	} catch (error) {
		errorMessage.value =
			(error as Error)?.message ||
			t('module.attachment.dialog.scan-directory.load-error');
		directories.value = [];
	} finally {
		loading.value = false;
	}
};

const enterDirectory = async (directory: Attachment) => {
	if (!directory.id) return;
	const sourceRoot = sourceRoots.value.find(
		(source) => source.id === directory.id
	);
	paths.value.push(
		sourceRoot || {
			id: directory.id,
			name: directory.name || '',
			driverId:
				currentDirectory.value?.driverId || (directory.driverId as string),
			isSourceRoot: false,
		}
	);
	loading.value = true;
	try {
		await loadCurrentDirectory();
	} finally {
		loading.value = false;
	}
};

const navigateTo = async (index: number) => {
	paths.value = paths.value.slice(0, index + 1);
	loading.value = true;
	try {
		await loadCurrentDirectory();
	} finally {
		loading.value = false;
	}
};

const navigateToSystemRoot = async () => {
	paths.value = [];
	await loadCurrentDirectory();
};

const refreshCurrentDirectory = async () => {
	refreshing.value = true;
	errorMessage.value = '';
	try {
		await loadCurrentDirectory(true);
	} catch (error) {
		errorMessage.value =
			(error as Error)?.message ||
			t('module.attachment.dialog.scan-directory.refresh-error');
	} finally {
		refreshing.value = false;
	}
};

const selectCurrentDirectory = () => {
	if (!canSelectCurrent.value || !currentDirectory.value) return;
	emit('selected', currentDirectory.value.id, currentPath.value);
	emit('update:visible', false);
};

watch(
	() => props.visible,
	(visible) => {
		if (visible) void loadSources();
	}
);
</script>

<template>
	<el-dialog
		:model-value="visible"
		:title="t('module.attachment.dialog.scan-directory.title')"
		width="min(680px, 92vw)"
		@update:model-value="emit('update:visible', $event)"
	>
		<el-alert
			v-if="errorMessage"
			:title="errorMessage"
			type="error"
			show-icon
			:closable="false"
		/>
		<div class="directory-toolbar">
			<div class="directory-path">
				<span class="directory-path-label">{{
					t('module.attachment.breadcrumb.label')
				}}</span>
				<el-breadcrumb separator=">" class="directory-breadcrumb">
					<el-breadcrumb-item v-if="sourceRoots.length !== 1">
						<el-button
							link
							class="breadcrumb-path"
							@click="navigateToSystemRoot"
						>
							{{ t('module.attachment.dialog.scan-directory.system-root') }}
						</el-button>
					</el-breadcrumb-item>
					<el-breadcrumb-item v-for="(path, index) in paths" :key="path.id">
						<el-button
							link
							class="breadcrumb-path"
							:class="{ 'source-root-path': path.isSourceRoot }"
							@click="navigateTo(index)"
						>
							{{ path.name }}
						</el-button>
					</el-breadcrumb-item>
				</el-breadcrumb>
			</div>
			<RefreshButton
				v-if="!showingSystemRoot"
				:loading="refreshing"
				@click="refreshCurrentDirectory"
			>
				{{ t('module.attachment.btn.refresh') }}
			</RefreshButton>
		</div>
		<el-alert
			v-if="!showingSystemRoot"
			:title="t('module.attachment.dialog.scan-directory.refresh-hint')"
			type="info"
			show-icon
			:closable="false"
			class="refresh-hint"
		/>
		<el-scrollbar v-loading="loading" height="360px" class="directory-list">
			<!-- eslint-disable-next-line vue/no-restricted-html-elements -- 原生按钮承载整行目录选择语义和布局 -->
			<button
				v-for="directory in directories"
				:key="directory.id"
				type="button"
				class="directory-row"
				@click="enterDirectory(directory)"
			>
				<el-icon><Folder /></el-icon>
				<span class="directory-name">{{ directory.name }}</span>
			</button>
			<el-empty
				v-if="!loading && directories.length === 0"
				:description="
					sourceRoots.length === 0
						? t('module.attachment.dialog.scan-directory.no-sources')
						: t('module.attachment.dialog.scan-directory.empty')
				"
			>
				<el-button
					v-if="sourceRoots.length === 0"
					:icon="Setting"
					@click="emit('manageSources')"
				>
					{{ t('module.attachment.dialog.scan-directory.manage-sources') }}
				</el-button>
			</el-empty>
		</el-scrollbar>
		<template #footer>
			<el-button @click="emit('update:visible', false)">{{
				t('common.button.cancel')
			}}</el-button>
			<el-button
				type="primary"
				:disabled="!canSelectCurrent"
				@click="selectCurrentDirectory"
			>
				{{ t('module.attachment.dialog.scan-directory.select-current') }}
			</el-button>
		</template>
	</el-dialog>
</template>

<style scoped>
.directory-toolbar {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16px;
}

.directory-path {
	display: flex;
	flex: 1;
	align-items: baseline;
	gap: 12px;
	min-width: 0;
}

.directory-path-label {
	flex: none;
	line-height: 24px;
}

.directory-breadcrumb {
	display: flex;
	flex-wrap: wrap;
	align-items: baseline;
	min-width: 0;
}

:deep(.directory-breadcrumb .el-breadcrumb__item) {
	display: flex;
	align-items: baseline;
	min-width: 0;
	max-width: 100%;
}

:deep(.directory-breadcrumb .el-breadcrumb__inner) {
	display: block;
	min-width: 0;
	max-width: 100%;
	line-height: 24px;
}

:deep(.directory-breadcrumb .el-breadcrumb__separator) {
	display: inline-block;
	line-height: 24px;
}

.breadcrumb-path {
	display: block;
	max-width: 100%;
	height: auto;
	min-height: 24px;
	padding: 0;
	border: 0;
	font: inherit;
	line-height: 24px;
	text-align: left;
}

.source-root-path {
	font-weight: var(--el-font-weight-primary);
}

:deep(.breadcrumb-path > span),
.directory-name {
	min-width: 0;
	line-height: 24px;
	white-space: normal;
	overflow-wrap: anywhere;
	word-break: break-word;
}

:deep(.breadcrumb-path > span) {
	display: block;
}

:deep(.refresh-button) {
	flex: none;
}

.refresh-hint {
	margin-top: 12px;
}

.directory-list {
	margin-top: 12px;
	border: 1px solid var(--el-border-color-light);
	border-radius: 4px;
}

.directory-row {
	display: flex;
	align-items: center;
	gap: 10px;
	width: 100%;
	min-height: 44px;
	padding: 8px 12px;
	border: 0;
	border-bottom: 1px solid var(--el-border-color-lighter);
	background: transparent;
	color: var(--el-text-color-primary);
	cursor: pointer;
	text-align: left;
}

.directory-row .el-icon {
	flex: none;
	margin-top: 2px;
}

.directory-name {
	flex: 1;
}

.directory-row:hover {
	background: var(--el-fill-color-light);
}

@media (max-width: 767px) {
	.directory-toolbar {
		flex-wrap: wrap;
	}

	.directory-path {
		flex-basis: 100%;
	}

	:deep(.refresh-button) {
		margin-left: auto;
	}
}
</style>
