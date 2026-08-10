<script setup lang="ts">
import MediaContentPage from './MediaContentPage.vue';

const videoTypes = ['VIDEO', 'ANIME', 'REAL'];
</script>

<template>
	<MediaContentPage
		kind="video"
		:title="$t('module.media.video.title')"
		:types="videoTypes"
		scan-mode="EPISODE"
		detail-route="/videos/details"
	/>
</template>
