<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { apiClient } from '@/utils/api-client';
import { ElButton, ElCard, ElCol, ElIcon, ElRow } from 'element-plus';
import {
	Files,
	Star,
	View,
	VideoCamera,
	Headset,
	Picture,
} from '@element-plus/icons-vue';

interface ActuatorInfo {
	attachment: { total: number };
	subject: {
		total: number;
		video: number;
		anime: number;
		real: number;
		music: number;
		comic: number;
	};
	subjectCollection: { total: number; doing: number };
}

const { t } = useI18n();
const router = useRouter();
const actuatorInfo = ref<ActuatorInfo>();
const loading = ref(false);
const error = ref(false);

const isCount = (value: unknown): value is number =>
	typeof value === 'number' && Number.isFinite(value) && value >= 0;

const isValidInfo = (value: unknown): value is ActuatorInfo => {
	if (!value || typeof value !== 'object') return false;
	const info = value as Partial<ActuatorInfo>;
	return [
		info.attachment?.total,
		info.subject?.total,
		info.subject?.video,
		info.subject?.anime,
		info.subject?.real,
		info.subject?.music,
		info.subject?.comic,
		info.subjectCollection?.total,
		info.subjectCollection?.doing,
	].every(isCount);
};

const fetchActuatorInfo = async () => {
	loading.value = true;
	error.value = false;
	try {
		const { data } = await apiClient.actuator.info();
		if (!isValidInfo(data)) throw new Error('Invalid actuator info');
		actuatorInfo.value = data;
	} catch {
		actuatorInfo.value = undefined;
		error.value = true;
	} finally {
		loading.value = false;
	}
};

const videoTotal = computed(() => {
	const subject = actuatorInfo.value?.subject;
	return subject ? subject.video + subject.anime + subject.real : undefined;
});
const hasAttachments = computed(
	() => (actuatorInfo.value?.attachment.total ?? 0) > 0
);
const hasSubjects = computed(
	() => (actuatorInfo.value?.subject.total ?? 0) > 0
);

const go = (path: string, importMode = false) => {
	if (importMode) router.push({ path, query: { import: '1' } });
	else router.push(path);
};

onMounted(fetchActuatorInfo);
</script>

<template>
	<div class="dashboard">
		<div v-if="loading" class="dashboard-state">
			{{ t('module.dashboard.loading') }}
		</div>
		<div v-else-if="error" class="dashboard-state dashboard-error">
			<span>{{ t('module.dashboard.error') }}</span>
			<el-button type="primary" @click="fetchActuatorInfo">{{
				t('module.dashboard.retry')
			}}</el-button>
		</div>
		<template v-else-if="actuatorInfo">
			<el-row :gutter="10" class="dashboard-cards">
				<el-col
					v-for="card in [
						{
							label: t('module.dashboard.label.attachment'),
							value: actuatorInfo.attachment.total,
							icon: Files,
							path: '/sources',
						},
						{
							label: t('module.dashboard.label.video'),
							value: videoTotal,
							icon: VideoCamera,
							path: '/videos',
						},
						{
							label: t('module.dashboard.label.music'),
							value: actuatorInfo.subject.music,
							icon: Headset,
							path: '/music',
						},
						{
							label: t('module.dashboard.label.image'),
							value: actuatorInfo.subject.comic,
							icon: Picture,
							path: '/images',
						},
						{
							label: t('module.dashboard.label.collection'),
							value: actuatorInfo.subjectCollection.total,
							icon: Star,
						},
						{
							label: t('module.dashboard.label.doing'),
							value: actuatorInfo.subjectCollection.doing,
							icon: View,
						},
					]"
					:key="card.label"
					:xs="24"
					:sm="12"
					:md="8"
					:lg="4"
					:xl="4"
				>
					<el-card
						shadow="hover"
						class="dashboard-card"
						:class="{ clickable: card.path }"
						@click="card.path && go(card.path)"
					>
						<el-icon size="38"><component :is="card.icon" /></el-icon>
						<div class="dashboard-card-content">
							<span>{{ card.label }}</span>
							<strong>{{ card.value }}</strong>
						</div>
					</el-card>
				</el-col>
			</el-row>

			<div v-if="!hasAttachments && !hasSubjects" class="dashboard-guide">
				<h3>{{ t('module.dashboard.guide.empty.title') }}</h3>
				<p>{{ t('module.dashboard.guide.empty.description') }}</p>
				<el-button type="primary" @click="go('/sources')">{{
					t('module.dashboard.guide.addSource')
				}}</el-button>
			</div>
			<div v-else-if="hasAttachments && !hasSubjects" class="dashboard-guide">
				<h3>{{ t('module.dashboard.guide.import.title') }}</h3>
				<p>{{ t('module.dashboard.guide.import.description') }}</p>
				<el-button @click="go('/videos', true)">{{
					t('module.dashboard.label.video')
				}}</el-button>
				<el-button @click="go('/music', true)">{{
					t('module.dashboard.label.music')
				}}</el-button>
				<el-button @click="go('/images', true)">{{
					t('module.dashboard.label.image')
				}}</el-button>
			</div>
			<div v-else class="dashboard-guide dashboard-actions">
				<span>{{ t('module.dashboard.guide.content.description') }}</span>
				<el-button link type="primary" @click="go('/sources')">{{
					t('module.dashboard.guide.content.sources')
				}}</el-button>
				<el-button link type="primary" @click="go('/videos')">{{
					t('module.dashboard.guide.content.videos')
				}}</el-button>
				<el-button link type="primary" @click="go('/music')">{{
					t('module.dashboard.guide.content.music')
				}}</el-button>
				<el-button link type="primary" @click="go('/images')">{{
					t('module.dashboard.guide.content.images')
				}}</el-button>
			</div>
		</template>
	</div>
</template>

<style lang="scss" scoped>
.dashboard-card {
	display: flex;
	align-items: center;
	gap: 14px;
	margin-bottom: 10px;
}
.dashboard-card.clickable {
	cursor: pointer;
}
.dashboard-card-content {
	display: flex;
	flex-direction: column;
	gap: 5px;
}
.dashboard-card-content span {
	color: var(--el-text-color-secondary);
	font-size: 12px;
}
.dashboard-card-content strong {
	font-size: 28px;
}
.dashboard-state,
.dashboard-guide {
	padding: 28px;
	text-align: center;
}
.dashboard-error {
	color: var(--el-color-danger);
	display: flex;
	justify-content: center;
	align-items: center;
	gap: 12px;
}
.dashboard-guide p {
	color: var(--el-text-color-secondary);
}
.dashboard-actions {
	text-align: left;
}
</style>
