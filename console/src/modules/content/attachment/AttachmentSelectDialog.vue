<script setup lang="ts">
import { Attachment } from '@runikaros/api-client';
import { computed } from 'vue';

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
	(event: 'closeWithAttachment', attachment: Attachment): void;
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
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		title="附件选择器"
		width="85%"
		@close="onClose"
	>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
