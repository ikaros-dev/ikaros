<script setup lang="ts">
import { FileEntity, SubjectImage } from '@runikaros/api-client';
import FileSelectDialog from '../file/FileSelectDialog.vue';
import { Picture } from '@element-plus/icons-vue';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		subjectImage: SubjectImage;
	}>(),
	{
		visible: false,
		subjectImage: undefined,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'update:subjectImage', subjectImage: SubjectImage): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close', subjectImage: SubjectImage): void;
}>();

const drawerVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const image = ref<SubjectImage>({});

watch(
	() => props.subjectImage,
	(newValue) => {
		if (newValue) {
			image.value = newValue;
		}
	}
);

const handleClose = (done: () => void) => {
	done();
	drawerVisible.value = false;
};

const fileSelectDialogVisible = ref(false);
const onFileSelectDialogClose = () => {
	fileSelectDialogVisible.value = false;
};
const onFileSelectDialogCloseWithUrl = (file: FileEntity) => {
	// console.log('receive file entity: ', file);
	switch (currentSelectFileField.value) {
		case 'common':
			image.value.common = file.url;
			break;
		case 'large':
			image.value.large = file.url;
			break;
		case 'medium':
			image.value.medium = file.url;
			break;
		case 'small':
			image.value.small = file.url;
			break;
		case 'grid':
			image.value.grid = file.url;
			break;
	}
	fileSelectDialogVisible.value = false;
};
const currentSelectFileField = ref('');
const selectFile = (field: string) => {
	currentSelectFileField.value = field;
	fileSelectDialogVisible.value = true;
};
const onConfirm = () => {
	emit('close', image as SubjectImage);
	drawerVisible.value = false;
};
</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		title="条目图片设置"
		direction="rtl"
		:before-close="handleClose"
		size="40%"
	>
		<FileSelectDialog
			v-model:visible="fileSelectDialogVisible"
			@close="onFileSelectDialogClose"
			@closeWithFileEntity="onFileSelectDialogCloseWithUrl"
		/>

		<el-row>
			<el-col :span="24">
				<el-form :model="image" label-width="85px">
					<el-form-item label="common">
						<el-input v-model="image.common" clearable>
							<template #prepend>
								<el-button
									:icon="Picture"
									plain
									@click="selectFile('common')"
								/>
							</template>
						</el-input>
					</el-form-item>
					<el-form-item label="large">
						<el-input v-model="image.large" clearable>
							<template #prepend>
								<el-button :icon="Picture" plain @click="selectFile('large')" />
							</template>
						</el-input>
					</el-form-item>
					<el-form-item label="medium">
						<el-input v-model="image.medium" clearable>
							<template #prepend>
								<el-button
									:icon="Picture"
									plain
									@click="selectFile('medium')"
								/>
							</template>
						</el-input>
					</el-form-item>
					<el-form-item label="small">
						<el-input v-model="image.small" clearable>
							<template #prepend>
								<el-button :icon="Picture" plain @click="selectFile('small')" />
							</template>
						</el-input>
					</el-form-item>
					<el-form-item label="grid">
						<el-input v-model="image.grid" clearable>
							<template #prepend>
								<el-button :icon="Picture" plain @click="selectFile('grid')" />
							</template>
						</el-input>
					</el-form-item>
					<el-form-item>
						<el-button plain @click="onConfirm">完成</el-button>
					</el-form-item>
				</el-form>
			</el-col>
		</el-row>
	</el-drawer>
</template>

<style lang="scss" scoped></style>
