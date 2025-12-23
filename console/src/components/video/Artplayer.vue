<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import Artplayer from 'artplayer';
import SubtitlesOctopus from '@/libs/JavascriptSubtitlesOctopus/subtitles-octopus.js';
import { useFontStore } from '@/stores/font';
import { Attachment } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import type { Setting } from 'artplayer/types/setting';
import { subtitleNameChineseMap } from '@/modules/common/constants';

const beseUrl = import.meta.env.BASE_URL;
const subtitlesOctopusWorkJsPath =
	beseUrl + 'js/JavascriptSubtitlesOctopus/subtitles-octopus-worker.js';

const fontStore = useFontStore();

const props = withDefaults(
	defineProps<{
		attachmentId: number;
	}>(),
	{
		attachmentId: undefined,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'getInstance', instance: Artplayer): void;
}>();

watch(props, (newVal) => {
	if (newVal.attachmentId) {
		fetchAttachment();
	}
});

const attachment = ref<Attachment>();
const fetchAttachment = async () => {
	if (!props.attachmentId) return;
	const { data } = await apiClient.attachment.getAttachmentById({
		id: props.attachmentId,
	});
	const rsp = await apiClient.attachment.getReadUrl({id: data.id as number})
	if (!rsp.data.startsWith('http')) {
		data.url = encodeURI(rsp.data ?? "")
	} else {
		data.url = rsp.data
	}
	attachment.value = data;
};

const fonts = ref<string[]>([]);
const initFonts = async () => {
	const staticFonts: string[] = await fontStore.getStaticFonts();
	fonts.value = staticFonts;
};
interface ArtSubtitle {
	default: boolean;
	html: string;
	url: string;
}

const getSubtitleSimpleNameByAttachmentName = (name: string): string => {
	if (!name) return '';
	var str = name.substring(0, name.lastIndexOf('.'));
	str = str.substring(str.lastIndexOf('.') + 1);
	str = str.toLocaleUpperCase();
	console.log('subtitle simple name', name, str);
	return str;
};

const getSubtitleChineseSimpleNameBySimpleName = (name: string): string => {
	const cnName = subtitleNameChineseMap.get(name) as string;
	return cnName != null && cnName != undefined && cnName != '' ? cnName : name;
};

const artSubtitles = ref<ArtSubtitle[]>([]);
const getVideoSubtitles = async () => {
	artSubtitles.value = [];
	const { data } =
		await apiClient.attachmentRelation.findAttachmentVideoSubtitles({
			attachmentId: props.attachmentId as number,
		});
	// console.log('load video subtitles', data);
	for (let index = 0; index < data!.length; index++) {
		const simpleName = getSubtitleSimpleNameByAttachmentName(
			data![index].name as string
		);
		var artSubtitle: ArtSubtitle = {
			default:
				simpleName === 'SC' || simpleName === 'sc' || simpleName == 'JPSC',
			html: getSubtitleChineseSimpleNameBySimpleName(simpleName),
			url: encodeURI(data![index].url as string),
		};
		artSubtitles.value.push(artSubtitle);
	}
};

var artRef = ref();
var art = ref<Artplayer>();
const subtitleOctopus = ref();

const artplayerPluginAss = (options: any) => {
	return (art: any) => {
		subtitleOctopus.value = new SubtitlesOctopus({
			...options,
			video: art.template.$video,
		});

		subtitleOctopus.value.canvasParent.style.zIndex = 20;
		art.on('destroy', () => subtitleOctopus.value.dispose());

		return {
			name: 'artplayerPluginAss',
			instance: subtitleOctopus.value,
		};
	};
};

const currentSubUrl = ref('');

const artplayerSubtitleEnableSetting: Setting = {
	key: 'artplayerSubtitleEnableSetting',
	html: '开启',
	tooltip: '显示',
	switch: true,
	onSwitch: function (item) {
		item.tooltip = item.switch ? '隐藏' : '显示';
		if (item.switch) {
			subtitleOctopus.value.freeTrack();
		} else {
			subtitleOctopus.value.setTrackByUrl(currentSubUrl.value);
		}
		return !item.switch;
	},
};
const artplayerSubtitleSetting: Setting = {
	key: 'artplayerSubtitleSetting',
	width: 200,
	html: '字幕',
	tooltip: '选择',
	icon: '<img width="22" heigth="22" src="' + beseUrl + 'svg/subtitle.svg">',
	selector: [artplayerSubtitleEnableSetting],
	onSelect: function (item) {
		const newSubtitleUrl = item.url;
		currentSubUrl.value = newSubtitleUrl;
		subtitleOctopus.value.setTrackByUrl(newSubtitleUrl);
		artplayerSubtitleEnableSetting.switch = true;
		return item.html;
	},
};

const initArtplayer = async () => {
	console.debug('start init artplyer....');
	console.debug('att url', attachment.value?.url);
	art.value = new Artplayer({
		container: artRef.value,
		url: attachment.value?.url as string,
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
		contextmenu: [
			{
				html: 'Custom menu',
				click: function (contextmenu) {
					console.info('You clicked on the custom menu');
					contextmenu.show = false;
				},
			},
		],
	});
	// add subtitle list
	console.debug('artSubtitles', artSubtitles);
	if (artSubtitles.value.length > 0) {
		artSubtitles.value.forEach((e) => {
			artplayerSubtitleSetting.selector?.push(e);
			if (e.default) {
				currentSubUrl.value = e.url;
				artplayerSubtitleSetting.tooltip = e.html;
			}
		});
		art.value?.setting.add(artplayerSubtitleSetting);
		if (!currentSubUrl.value) {
			currentSubUrl.value = artSubtitles.value[0].url;
		}
		console.debug('current sub url', currentSubUrl.value);
		art.value.plugins.add(
			artplayerPluginAss({
				// debug: true,
				fonts: fonts.value,
				subUrl: currentSubUrl.value,
				workerUrl: subtitlesOctopusWorkJsPath,
			})
		);
	}
	emit('getInstance', art.value);
};

onMounted(async () => {
	console.debug('attachmentId', props.attachmentId);
	if (props.attachmentId) {
		await fetchAttachment();
		await getVideoSubtitles();
		await initFonts();
		await initArtplayer();
	}
});
onUnmounted(() => {
	if (art.value) {
		art.value.destroy(false);
	}
});
</script>

<template>
	<div class="scale">
		<div class="item">
			<!-- 放在固定 16:9 的父容器里 -->
			<div ref="artRef" style="width: 100%; height: 100%"></div>
		</div>
	</div>
</template>

<style scoped>
.scale {
	width: 100%;
	padding-bottom: 56.25%;
	height: 0;
	position: relative;
}

.item {
	width: 100%;
	height: 100%;
	position: absolute;
}
</style>
