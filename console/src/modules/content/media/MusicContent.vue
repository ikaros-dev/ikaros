<script setup lang="ts">
import MediaContentPage from './MediaContentPage.vue';

const musicTypes = ['MUSIC'];
</script>

<template>
	<MediaContentPage
		kind="music"
		:title="$t('module.media.music.title')"
		:types="musicTypes"
		scan-mode="AUDIO"
		detail-route="/music/details"
	/>
</template>
