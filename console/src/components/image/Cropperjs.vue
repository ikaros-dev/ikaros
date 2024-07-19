<script setup lang="ts">
import Cropper from 'cropperjs';
import { onMounted, ref, watch } from 'vue';

const props = withDefaults(
	defineProps<{
		url: string;
    aspectRatio: number;
	}>(),
	{
		url: '',
    aspectRatio: 24/39,
	}
);


const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'clip-img', base64Encode: string | undefined): void;
}>();

watch(props, (val)=>{
  cropper.value?.setAspectRatio(val.aspectRatio);
  cropper.value?.replace(val.url);
})


const croimg = ref({});
const cropper = ref<Cropper>();

const clipImgEmitBase64 = (cvs) => {
  const base64 = cvs.toDataURL('image/webp', 1);
  emit('clip-img', base64);
}

onMounted(()=>{
  cropper.value = new Cropper(croimg.value as HTMLImageElement, {
    aspectRatio: props.aspectRatio,
    viewMode: 1,
    dragMode: 'move',
    ready(){
      clipImgEmitBase64(cropper.value?.getCroppedCanvas);
    },
    cropend(){
      clipImgEmitBase64(cropper.value?.getCroppedCanvas);
    },
    zoom(){
      clipImgEmitBase64(cropper.value?.getCroppedCanvas);
    },
  })
})
</script>
<template>
  <div>
    <img ref="croimg" class="fit img" :src="props.url" />
  </div>
</template>
<style lang="scss" scoped>
.img {
  object-fit: contain;
  max-width: 100%;
  width: 100%;
  height: 100%;
}
</style>
