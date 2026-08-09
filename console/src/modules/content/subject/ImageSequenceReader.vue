<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElButton, ElEmpty, ElPagination } from 'element-plus';
import { RefreshRight } from '@element-plus/icons-vue';
import type { EpisodeResource } from '@runikaros/api-client';
import { isAxiosError } from 'axios';
import { useI18n } from 'vue-i18n';
import { apiClient } from '@/utils/api-client';

const props = defineProps<{
	episodeId: string;
	resources: EpisodeResource[];
}>();

const { t } = useI18n();
const currentPage = ref(1);
const loadingProgress = ref(false);
const savingProgress = ref(false);
const progressError = ref(false);
const imageLoadFailed = ref(false);
const imageReloadKey = ref(0);
let initializationId = 0;

const pageCount = computed(() => props.resources.length);
const currentResource = computed(() => props.resources[currentPage.value - 1]);

const clampPage = (page: number) => {
	if (pageCount.value === 0) return 1;
	return Math.min(Math.max(Math.trunc(page) || 1, 1), pageCount.value);
};

const persistProgress = async () => {
	if (!props.episodeId || pageCount.value === 0) return;
	savingProgress.value = true;
	progressError.value = false;
	try {
		await apiClient.collectionEpisode.updateCollectionEpisode({
			episodeId: props.episodeId,
			progress: currentPage.value,
			duration: pageCount.value,
		});
	} catch (error) {
		progressError.value = true;
		console.error('Save image sequence progress failed', error);
	} finally {
		savingProgress.value = false;
	}
};

const initializeProgress = async () => {
	const currentInitializationId = ++initializationId;
	currentPage.value = 1;
	imageLoadFailed.value = false;
	progressError.value = false;
	if (!props.episodeId || pageCount.value === 0) return;

	loadingProgress.value = true;
	try {
		const response = await apiClient.collectionEpisode.findCollectionEpisode({
			episodeId: props.episodeId,
		});
		if (currentInitializationId !== initializationId) return;
		const collection = response?.data;
		currentPage.value = clampPage(collection?.progress ?? 1);
		if (
			!collection ||
			collection.progress !== currentPage.value ||
			collection.duration !== pageCount.value
		) {
			await persistProgress();
		}
	} catch (error) {
		if (currentInitializationId !== initializationId) return;
		if (isAxiosError(error) && error.response?.status === 404) {
			await persistProgress();
			return;
		}
		progressError.value = true;
		console.error('Load image sequence progress failed', error);
	} finally {
		if (currentInitializationId === initializationId) {
			loadingProgress.value = false;
		}
	}
};

const onPageChange = async (page: number) => {
	currentPage.value = clampPage(page);
	imageLoadFailed.value = false;
	imageReloadKey.value += 1;
	await persistProgress();
};

const retryImage = () => {
	imageLoadFailed.value = false;
	imageReloadKey.value += 1;
};

watch(
	[
		() => props.episodeId,
		() => props.resources.map((resource) => resource.attachmentId).join(','),
	],
	initializeProgress,
	{ immediate: true }
);
</script>

<template>
	<div class="image-sequence-reader">
		<el-empty
			v-if="pageCount === 0"
			:description="
				t('module.subject.dialog.episode.details.media.image.empty')
			"
		/>
		<template v-else>
			<div class="image-stage" :aria-busy="loadingProgress">
				<div v-if="loadingProgress" class="reader-state">
					{{ t('module.subject.dialog.episode.details.media.image.loading') }}
				</div>
				<div v-else-if="imageLoadFailed" class="reader-state">
					<p>
						{{
							t('module.subject.dialog.episode.details.media.image.loadFailed')
						}}
					</p>
					<el-button :icon="RefreshRight" @click="retryImage">
						{{ t('module.subject.dialog.episode.details.media.image.retry') }}
					</el-button>
				</div>
				<img
					v-else
					:key="`${currentResource?.attachmentId}-${imageReloadKey}`"
					:src="encodeURI(currentResource?.url ?? '')"
					:alt="
						currentResource?.name ??
						t('module.subject.dialog.episode.details.media.image.page', {
							page: currentPage,
						})
					"
					class="sequence-image"
					@error="imageLoadFailed = true"
				/>
			</div>
			<div class="reader-footer">
				<el-pagination
					:current-page="currentPage"
					:page-count="pageCount"
					layout="prev, pager, next"
					@current-change="onPageChange"
				/>
				<span class="progress-state">
					{{
						t('module.subject.dialog.episode.details.media.image.progress', {
							page: currentPage,
							total: pageCount,
						})
					}}
					<span v-if="savingProgress">
						{{ t('module.subject.dialog.episode.details.media.image.saving') }}
					</span>
					<span v-else-if="progressError" class="progress-error">
						{{
							t('module.subject.dialog.episode.details.media.image.saveFailed')
						}}
					</span>
				</span>
			</div>
		</template>
	</div>
</template>

<style scoped>
.image-sequence-reader {
	width: 100%;
}

.image-stage {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 100%;
	min-height: 320px;
	max-height: 70vh;
	background: var(--el-fill-color-light);
	overflow: auto;
}

.sequence-image {
	display: block;
	max-width: 100%;
	max-height: 70vh;
	object-fit: contain;
}

.reader-state {
	padding: 32px;
	text-align: center;
	color: var(--el-text-color-secondary);
}

.reader-footer {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
	padding-top: 12px;
	flex-wrap: wrap;
}

.progress-state {
	color: var(--el-text-color-secondary);
}

.progress-error {
	color: var(--el-color-danger);
}
</style>
