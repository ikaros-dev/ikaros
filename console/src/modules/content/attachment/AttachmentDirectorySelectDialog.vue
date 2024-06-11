<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElButton, ElDialog } from 'element-plus';
import AttachmentDirectoryTreeSelect from '@/components/modules/content/attachment/AttachmentDirectoryTreeSelect.vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

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
	// eslint-disable-next-line no-unused-vars
	(event: 'closeWithTargetDirId', targetDirid: number): void;
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
	dialogVisible.value = false;
	targetDirectoryId.value = 0;
	emit('close');
};

const targetDirectoryId = ref(0);

const onDirectorySelectDialogButtonClick = async () => {
	if (!targetDirectoryId.value) {
		targetDirectoryId.value = 0;
	}
	emit('closeWithTargetDirId', targetDirectoryId.value);
	dialogVisible.value = false;
};
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		style="width: 50%"
		:title="t('module.attachment.dialog.directory-select.title')"
		@close="onClose"
	>
		<AttachmentDirectoryTreeSelect v-model:target-dirid="targetDirectoryId" />
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="onClose">
					{{
						t('module.attachment.dialog.directory-select.footer.button.cancel')
					}}
				</el-button>
				<el-button type="primary" @click="onDirectorySelectDialogButtonClick">
					{{
						t('module.attachment.dialog.directory-select.footer.button.submit')
					}}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
