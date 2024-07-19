<script setup lang="ts">

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

const base64 = ref('')



</script>
<template>
    <el-dialog
        v-model="dialogVisible"
        title="Clip image"
        @close="onClose"
    >
        

    </el-dialog>

</template>
<style lang="scss" scoped>
</style>
