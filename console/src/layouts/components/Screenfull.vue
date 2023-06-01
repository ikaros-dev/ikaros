<script setup lang="ts">
import screenfull from 'screenfull';

const icon = ref(screenfull.isFullscreen);
const handleFullScreen = () => {
	if (screenfull.isEnabled) {
		screenfull.toggle();
	}
};

const changeIcon = () => {
	icon.value = screenfull.isFullscreen;
};

onMounted(() => {
	screenfull.on('change', changeIcon);
});

onBeforeMount(() => {
	screenfull.off('change', changeIcon);
});
</script>

<template>
	<div id="screenFul" class="screenFulIcon" @click="handleFullScreen">
		<el-icon :size="40">
			<span v-if="!icon"><FullScreen /></span>
			<span v-else><Close /></span>
		</el-icon>
	</div>
</template>

<style lang="scss" scoped>
.screenFulIcon {
	cursor: pointer;
	padding-right: 5px;
}
</style>
