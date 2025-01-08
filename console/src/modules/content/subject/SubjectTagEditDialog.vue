<script setup lang="ts">
import { Tag } from '@runikaros/api-client';
import { computed } from 'vue';
import { ElDialog } from 'element-plus';


const props = withDefaults(
	defineProps<{
		visible: boolean;
		tag?: Tag;
	}>(),
	{
		visible: false,
		tag: undefined,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'closeWithTag', tag: Tag): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});



</script>
<template>
  <el-dialog
		v-model="dialogVisible"
		title="编辑标签"
	>
    <h2>subject tag edit dialog</h2>

    <hr />

    <p>{{ tag }}</p>

</el-dialog>
</template>
<style lang="scss" scoped>
</style>
