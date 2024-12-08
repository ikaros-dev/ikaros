<script setup lang="ts">
import { computed, ref } from 'vue';
import Cropperjs from './Cropperjs.vue';
import { ElButton, ElDialog } from 'element-plus';
import { apiClient } from '@/utils/api-client';
import { randomUUID } from '@/utils/id';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		url: string;
	}>(),
	{
		visible: false,
		url: '',
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'updateUrl', newUrl: string): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const onClose = () => {
	emit('close');
};

const base64 = ref('');
const onClipImg = (base64Encode) => {
	base64.value = base64Encode;
};

const onConfirm = async () => {
	if (!base64.value) return;
	const file = base64ToFile(base64.value as string, randomUUID() + '.webp');
	const { data } = await apiClient.attachment.uploadAttachment({
		file: file,
	});
	emit('updateUrl', data.url as string);
	dialogVisible.value = false;
};

const base64ToFile = (base64, filename): File => {
	const arr = base64.split(',');
	const mime = arr[0].match(/:(.*?);/)[1];
	const bstr = atob(arr[1]);
	let n = bstr.length;
	const u8arr = new Uint8Array(n);

	while (n--) {
		u8arr[n] = bstr.charCodeAt(n);
	}

	return new File([u8arr], filename, { type: mime });
};
</script>
<template>
	<el-dialog v-model="dialogVisible" title="Clip image" @close="onClose">
		<Cropperjs :url="props.url" @clip-img="onClipImg" />
		<template #footer>
			<span>
				<el-button plain @click="onConfirm"> Confirm </el-button>
			</span>
		</template>
	</el-dialog>
</template>
<style lang="scss" scoped></style>
