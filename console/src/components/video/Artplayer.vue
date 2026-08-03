<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import Artplayer from 'artplayer';
import { ElButton, ElTag } from 'element-plus';
import type {
	Attachment,
	EpisodeResource,
	MediaTrack,
} from '@runikaros/api-client';
import { useI18n } from 'vue-i18n';
import SubtitlesOctopus from '@/libs/JavascriptSubtitlesOctopus/subtitles-octopus.js';
import { useFontStore } from '@/stores/font';
import { apiClient } from '@/utils/api-client';
// @ts-ignore
import type { Setting } from 'artplayer/types/setting';
import { subtitleNameChineseMap } from '@/modules/common/constants';

const baseUrl = import.meta.env.BASE_URL;
const subtitlesOctopusWorkJsPath =
	baseUrl + 'js/JavascriptSubtitlesOctopus/subtitles-octopus-worker.js';
const audioMimeTypes: Record<string, string> = {
	aac: 'audio/aac',
	flac: 'audio/flac',
	m4a: 'audio/mp4',
	mp3: 'audio/mpeg',
	mp4a: 'audio/mp4',
	oga: 'audio/ogg',
	ogg: 'audio/ogg',
	opus: 'audio/ogg; codecs=opus',
	wav: 'audio/wav',
};

const props = defineProps<{
	attachmentId?: string;
	resource?: EpisodeResource;
}>();

const emit = defineEmits<{
	(event: 'getInstance', instance: Artplayer): void;
}>();

const { t } = useI18n();
const fontStore = useFontStore();
const effectiveAttachmentId = computed(
	() => props.resource?.attachmentId ?? props.attachmentId
);
const resourceTracks = computed(() => props.resource?.tracks ?? []);

const attachment = ref<Attachment>();
const fetchAttachment = async () => {
	if (props.resource?.url) {
		attachment.value = {
			id: props.resource.attachmentId,
			name: props.resource.name,
			url: props.resource.url,
		};
		return;
	}
	if (!effectiveAttachmentId.value) return;
	const { data } = await apiClient.attachment.getAttachmentById({
		id: effectiveAttachmentId.value,
	});
	if (!data.id) return;
	const response = await apiClient.attachment.getReadUrl({ id: data.id });
	data.url = response.data.startsWith('http')
		? response.data
		: encodeURI(response.data ?? '');
	attachment.value = data;
};

const fonts = ref<string[]>([]);
const initFonts = async () => {
	fonts.value = await fontStore.getStaticFonts();
};

interface ArtSubtitle {
	default: boolean;
	html: string;
	url: string;
}

const getSubtitleSimpleNameByAttachmentName = (name: string): string => {
	if (!name) return '';
	let simpleName = name.substring(0, name.lastIndexOf('.'));
	simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1);
	return simpleName.toLocaleUpperCase();
};

const getSubtitleChineseSimpleNameBySimpleName = (name: string): string => {
	const chineseName = subtitleNameChineseMap.get(name) as string;
	return chineseName ? chineseName : name;
};

const artSubtitles = ref<ArtSubtitle[]>([]);
const getVideoSubtitles = async () => {
	artSubtitles.value = [];
	if (!effectiveAttachmentId.value) return;
	const { data } =
		await apiClient.attachmentRelation.findAttachmentVideoSubtitles({
			attachmentId: effectiveAttachmentId.value,
		});
	for (const subtitle of data ?? []) {
		const simpleName = getSubtitleSimpleNameByAttachmentName(
			subtitle.name as string
		);
		artSubtitles.value.push({
			default: simpleName === 'SC' || simpleName === 'JPSC',
			html: getSubtitleChineseSimpleNameBySimpleName(simpleName),
			url: encodeURI(subtitle.url as string),
		});
	}
	for (const track of resourceTracks.value) {
		if (track.kind !== 'subtitle' || !isTrackSwitchable(track)) continue;
		const url = encodeURI(track.url as string);
		if (artSubtitles.value.some((subtitle) => subtitle.url === url)) continue;
		artSubtitles.value.push({
			default: Boolean(track.default_track),
			html:
				track.title ||
				track.language ||
				track.codec ||
				t('module.subject.dialog.episode.details.media.track.unknown'),
			url,
		});
	}
};

const artRef = ref<HTMLDivElement>();
const art = ref<Artplayer>();
const subtitleOctopus = ref<any>();
const currentSubUrl = ref('');
const currentAudioTrack = ref<MediaTrack>();
const selectedTrackKey = ref('');

const artplayerPluginAss = (options: Record<string, unknown>) => {
	return (player: Artplayer) => {
		subtitleOctopus.value = new SubtitlesOctopus({
			...options,
			video: player.template.$video,
		});
		subtitleOctopus.value.canvasParent.style.zIndex = 20;
		player.on('destroy', () => subtitleOctopus.value?.dispose());
		return {
			name: 'artplayerPluginAss',
			instance: subtitleOctopus.value,
		};
	};
};

const trackKey = (track: MediaTrack) =>
	[
		track.kind,
		track.attachment_id ?? 'embedded',
		track.index ?? 'external',
		track.url,
	].join(':');

const isExternalTrack = (track: MediaTrack) =>
	Boolean(track.attachment_id && track.url);

const audioTrackIsSupported = (track: MediaTrack) => {
	const codec = (track.codec ?? '').replace(/^\./, '').toLowerCase();
	const extension = track.url
		?.split(/[?#]/, 1)[0]
		.split('.')
		.pop()
		?.toLowerCase();
	const mimeType = audioMimeTypes[codec] ?? audioMimeTypes[extension ?? ''];
	return Boolean(
		mimeType && document.createElement('audio').canPlayType(mimeType)
	);
};

const isTrackSwitchable = (track: MediaTrack) => {
	if (!track.playable || !isExternalTrack(track)) return false;
	if (track.kind === 'subtitle') return true;
	return track.kind === 'audio' && audioTrackIsSupported(track);
};

const trackKind = (track: MediaTrack) =>
	t(
		track.kind === 'audio'
			? 'module.subject.dialog.episode.details.media.track.audio'
			: 'module.subject.dialog.episode.details.media.track.subtitle'
	);

const trackTitle = (track: MediaTrack) =>
	track.title ||
	track.language ||
	(track.index == null
		? t('module.subject.dialog.episode.details.media.track.unknown')
		: t('module.subject.dialog.episode.details.media.track.index', {
				index: track.index + 1,
			}));

const trackAvailability = (track: MediaTrack) => {
	if (track.failure_reason) return track.failure_reason;
	if (!isExternalTrack(track)) {
		return t(
			'module.subject.dialog.episode.details.media.track.embeddedUnavailable'
		);
	}
	if (track.kind === 'audio' && !audioTrackIsSupported(track)) {
		return t(
			'module.subject.dialog.episode.details.media.track.browserUnsupported'
		);
	}
	return isTrackSwitchable(track)
		? t('module.subject.dialog.episode.details.media.track.playable')
		: t('module.subject.dialog.episode.details.media.track.unavailable');
};

const selectTrack = (track: MediaTrack) => {
	if (!isTrackSwitchable(track)) return;
	selectedTrackKey.value = trackKey(track);
	if (track.kind === 'audio') {
		currentAudioTrack.value = track;
		return;
	}
	currentSubUrl.value = encodeURI(track.url as string);
	subtitleOctopus.value?.setTrackByUrl(currentSubUrl.value);
};

const createSubtitleSettings = (): Setting[] => {
	const enableSetting: Setting = {
		key: 'artplayerSubtitleEnableSetting',
		html: t('module.subject.dialog.episode.details.media.subtitle.enable'),
		tooltip: t('module.subject.dialog.episode.details.media.subtitle.show'),
		switch: true,
		onSwitch(item) {
			item.tooltip = item.switch
				? t('module.subject.dialog.episode.details.media.subtitle.hide')
				: t('module.subject.dialog.episode.details.media.subtitle.show');
			if (item.switch) {
				subtitleOctopus.value?.freeTrack();
			} else if (currentSubUrl.value) {
				subtitleOctopus.value?.setTrackByUrl(currentSubUrl.value);
			}
			return !item.switch;
		},
	};
	const subtitleSetting: Setting = {
		key: 'artplayerSubtitleSetting',
		width: 200,
		html: t('module.subject.dialog.episode.details.media.track.subtitle'),
		tooltip: t('module.subject.dialog.episode.details.media.subtitle.select'),
		icon: `<img width="22" height="22" src="${baseUrl}svg/subtitle.svg">`,
		selector: [enableSetting, ...artSubtitles.value],
		onSelect(item) {
			currentSubUrl.value = item.url;
			subtitleOctopus.value?.setTrackByUrl(item.url);
			enableSetting.switch = true;
			return item.html;
		},
	};
	return [subtitleSetting];
};

const initArtplayer = () => {
	if (!artRef.value || !attachment.value?.url) return;
	art.value = new Artplayer({
		container: artRef.value,
		url: attachment.value.url,
		volume: 0.5,
		isLive: false,
		muted: false,
		autoplay: false,
		pip: true,
		autoSize: true,
		autoMini: true,
		screenshot: true,
		setting: true,
		loop: true,
		flip: true,
		playbackRate: true,
		aspectRatio: true,
		fullscreen: true,
		fullscreenWeb: true,
		subtitleOffset: false,
		miniProgressBar: true,
		mutex: true,
		backdrop: true,
		playsInline: true,
		autoPlayback: true,
		airplay: true,
		theme: 'skyblue',
		lang: navigator.language.toLowerCase(),
		moreVideoAttr: {
			crossOrigin: 'anonymous',
		},
		plugins: [],
		settings: [],
	});
	if (artSubtitles.value.length > 0) {
		const defaultSubtitle =
			artSubtitles.value.find((subtitle) => subtitle.default) ??
			artSubtitles.value[0];
		currentSubUrl.value = defaultSubtitle.url;
		for (const setting of createSubtitleSettings()) {
			art.value.setting.add(setting);
		}
		art.value.plugins.add(
			artplayerPluginAss({
				fonts: fonts.value,
				subUrl: currentSubUrl.value,
				workerUrl: subtitlesOctopusWorkJsPath,
			})
		);
	}
	emit('getInstance', art.value);
};

const initialize = async () => {
	art.value?.destroy(false);
	art.value = undefined;
	currentAudioTrack.value = undefined;
	selectedTrackKey.value = '';
	currentSubUrl.value = '';
	attachment.value = undefined;
	if (!effectiveAttachmentId.value) return;
	await fetchAttachment();
	await getVideoSubtitles();
	await initFonts();
	initArtplayer();
};

let mounted = false;
onMounted(async () => {
	mounted = true;
	await initialize();
});
watch(effectiveAttachmentId, async (newId, oldId) => {
	if (mounted && newId !== oldId) await initialize();
});
onUnmounted(() => {
	mounted = false;
	art.value?.destroy(false);
});
</script>

<template>
	<div>
		<div class="scale">
			<div class="item">
				<div ref="artRef" class="artplayer-container"></div>
			</div>
		</div>
		<div v-if="resourceTracks.length > 0" class="track-list">
			<div
				v-for="track in resourceTracks"
				:key="trackKey(track)"
				class="track-item"
			>
				<div class="track-description">
					<div class="track-title">
						<span>{{ trackKind(track) }} · {{ trackTitle(track) }}</span>
						<el-tag v-if="track.default_track" size="small" type="info">
							{{
								t('module.subject.dialog.episode.details.media.track.default')
							}}
						</el-tag>
						<el-tag size="small" type="info">
							{{
								t(
									isExternalTrack(track)
										? 'module.subject.dialog.episode.details.media.track.external'
										: 'module.subject.dialog.episode.details.media.track.embedded'
								)
							}}
						</el-tag>
					</div>
					<div class="track-meta">
						{{
							track.language ||
							t(
								'module.subject.dialog.episode.details.media.track.unknownLanguage'
							)
						}}
						<span v-if="track.codec"> · {{ track.codec }}</span>
						· {{ trackAvailability(track) }}
					</div>
				</div>
				<el-button
					v-if="isTrackSwitchable(track)"
					size="small"
					:type="selectedTrackKey === trackKey(track) ? 'primary' : 'default'"
					@click="selectTrack(track)"
				>
					{{
						t(
							selectedTrackKey === trackKey(track)
								? 'module.subject.dialog.episode.details.media.track.selected'
								: 'module.subject.dialog.episode.details.media.track.switch'
						)
					}}
				</el-button>
			</div>
			<audio
				v-if="currentAudioTrack?.url"
				:key="currentAudioTrack.url"
				class="external-audio"
				:src="encodeURI(currentAudioTrack.url)"
				controls
				autoplay
			>
				{{ t('module.attachment.details.message.hint.audioFormat') }}
			</audio>
		</div>
	</div>
</template>

<style scoped>
.scale {
	position: relative;
	width: 100%;
	height: 0;
	padding-bottom: 56.25%;
}

.item {
	position: absolute;
	width: 100%;
	height: 100%;
}

.artplayer-container {
	width: 100%;
	height: 100%;
}

.track-list {
	margin-top: 12px;
	border-top: 1px solid var(--el-border-color-lighter);
}

.track-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
	min-height: 56px;
	padding: 8px 0;
	border-bottom: 1px solid var(--el-border-color-lighter);
}

.track-description {
	min-width: 0;
}

.track-title {
	display: flex;
	align-items: center;
	gap: 8px;
	flex-wrap: wrap;
	color: var(--el-text-color-primary);
}

.track-meta {
	margin-top: 4px;
	font-size: 13px;
	color: var(--el-text-color-secondary);
}

.external-audio {
	width: 100%;
	margin-top: 12px;
}
</style>
