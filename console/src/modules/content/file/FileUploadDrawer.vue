<script setup lang="ts">
import { ErrorResponse } from '@uppy/core';
import { UppyFile } from '@uppy/utils';
import { ElMessage } from 'element-plus';
import { computed, ref, watch } from 'vue';

const props = withDefaults(
	defineProps<{
		visible: boolean;
	}>(),
	{
		visible: false,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
}>();

const uploadVisible = ref(false);
const drawerVisible = ref(false);

const handleVisibleChange = (visible: boolean) => {
	emit('update:visible', visible);
	if (!visible) {
		emit('close');
	}
};

const endpoint = computed(() => {
	return '/api/v1alpha1/plugin/install/file';
});

watch(
	() => props.visible,
	(newValue) => {
		if (newValue) {
			uploadVisible.value = true;
			drawerVisible.value = props.visible;
		} else {
			const uploadVisibleTimer = setTimeout(() => {
				uploadVisible.value = false;
				drawerVisible.value = false;
				clearTimeout(uploadVisibleTimer);
			}, 200);
		}
	}
);

const handleClose = (done: () => void) => {
	done();
	handleVisibleChange(false);
};

const onUploaded = async () => {
	handleVisibleChange(false);
};

const onError = (file: UppyFile<unknown>, response: ErrorResponse) => {
	const body = response.body;
	ElMessage.error(body.message);
	console.error(body);
};
</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		title="上传文件"
		direction="ttb"
		:before-close="handleClose"
		size="80%"
	>
		<template #header>
			<div align="center">
				<h4>上传文件</h4>
			</div>
		</template>
		<template #default>
			<div align="center">
				<UppyUpload
					v-if="uploadVisible"
					style="width: 100%"
					:restrictions="{
						maxNumberOfFiles: 6,
					}"
					:endpoint="endpoint"
					auto-proceed
					@uploaded="onUploaded"
					@error="onError"
				/>
			</div>
		</template>
	</el-drawer>
</template>

<style lang="scss" scoped></style>
