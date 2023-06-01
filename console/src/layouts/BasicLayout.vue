<script setup lang="ts">
import Aside from './Aside.vue';
import Header from './Header.vue';
import variables from '@/styles/variables.module.scss';
import { useLayoutStore } from '@/stores/layout';
const layoutStore = useLayoutStore();

const asideWidth = computed(() => {
	return layoutStore.asideIsExtend
		? variables.sideBarWidth
		: variables.hideSideBarWidth;
});
</script>

<template>
	<el-container class="app-wrapper">
		<el-aside :width="asideWidth" class="sidebar-container">
			<Aside />
		</el-aside>
		<el-container
			class="container"
			:class="{ hidderContainer: !layoutStore.asideIsExtend }"
		>
			<el-header><Header /></el-header>
			<el-main class="ik-blc-main">
				<RouterView />
			</el-main>
			<!-- <el-footer height="30px"><Footer /></el-footer> -->
		</el-container>
	</el-container>
</template>

<style lang="scss" scoped>
@import '@/styles/variables.module.scss';
.ik-blc-main {
	background-color: rgb(245, 245, 245);
}
.el-header {
	padding: 0;
	margin: 0;
}

.app-container {
	position: relative;
	width: 100%;
	height: 100%;
}
.container {
	width: calc(100% - $sideBarWidth);
	height: 100%;

	position: fixed;
	top: 0;
	right: 0;
	z-index: 9;
	transition: all 0.28s;
	&.hidderContainer {
		width: calc(100% - $hideSideBarWidth);
	}
}
</style>
