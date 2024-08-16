<script setup lang="ts">
import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.css';
import {onMounted, ref, watch} from 'vue';

const props = withDefaults(
	defineProps<{
		url: string;
		aspectRatio?: number;
	}>(),
	{
		url: '',
		aspectRatio: 430 / 600,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'clip-img', base64Encode: string | undefined): void;
}>();

watch(props, (val) => {
	cropper.value?.setAspectRatio(val.aspectRatio);
	cropper.value?.replace(val.url);
});

const croimg = ref({});
const cropper = ref<Cropper>();

const clipImgEmitBase64 = (cvs: HTMLCanvasElement) => {
	if (!cvs) return;
	const base64 = cvs.toDataURL('image/webp', 1);
	emit('clip-img', base64);
};

onMounted(() => {
	cropper.value = new Cropper(croimg.value as HTMLImageElement, {
		aspectRatio: props.aspectRatio,
		viewMode: 1,
		dragMode: 'move',
		autoCropArea: 1,
		ready() {
			clipImgEmitBase64(
				cropper.value?.getCroppedCanvas({
					imageSmoothingQuality: 'high',
				}) as HTMLCanvasElement
			);
		},
		cropend() {
			clipImgEmitBase64(
				cropper.value?.getCroppedCanvas({
					imageSmoothingQuality: 'high',
				}) as HTMLCanvasElement
			);
		},
		zoom() {
			clipImgEmitBase64(
				cropper.value?.getCroppedCanvas({
					imageSmoothingQuality: 'high',
				}) as HTMLCanvasElement
			);
		},
	});
});
</script>
<template>
	<div>
		<img ref="croimg" class="fit img" :src="props.url" />
	</div>
</template>
<style lang="scss" scoped>
.img {
	display: block;
	// object-fit: contain;
	max-width: 100%;
	// width: 100%;
	// height: 100%;
}
</style>
