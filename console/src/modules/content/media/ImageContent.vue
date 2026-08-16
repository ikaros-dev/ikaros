<script setup lang="ts">
import MediaContentPage from './MediaContentPage.vue';

const imageTypes = ['COMIC'];
</script>

<template>
	<MediaContentPage
		kind="image"
		:title="$t('module.media.image.title')"
		:types="imageTypes"
		scan-mode="IMAGE"
		detail-route="/images/details"
	/>
</template>
