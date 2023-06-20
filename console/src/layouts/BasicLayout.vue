<script setup lang="ts">
import Aside from './Aside.vue';
import Header from './Header.vue';
import variables from '@/styles/variables.module.scss';
import { useLayoutStore } from '@/stores/layout';
import GlobalSearchDialog from '@/components/global-search/GlobalSearchDialog.vue';
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { ElContainer, ElAside, ElHeader, ElMain, ElCard } from 'element-plus';
import { RouterView } from 'vue-router';

const layoutStore = useLayoutStore();

const asideWidth = computed(() => {
	return layoutStore.asideIsExtend
		? variables.sideBarWidth
		: variables.hideSideBarWidth;
});

const globalSearchDialogVisible = ref(false);

const isMac = /macintosh|mac os x/i.test(navigator.userAgent);

const handleGlobalSearchKeybinding = (e: KeyboardEvent) => {
	const { key, ctrlKey, metaKey } = e;
	if (key === 'k' && ((ctrlKey && !isMac) || metaKey)) {
		globalSearchDialogVisible.value = true;
		e.preventDefault();
	}
};

onMounted(() => {
	document.addEventListener('keydown', handleGlobalSearchKeybinding);
});

onUnmounted(() => {
	document.removeEventListener('keydown', handleGlobalSearchKeybinding);
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
				<el-card shadow="never">
					<RouterView />
				</el-card>
			</el-main>
		</el-container>
	</el-container>
	<GlobalSearchDialog v-model:visible="globalSearchDialogVisible" />
</template>

<style lang="scss" scoped>
@import '@/styles/variables.module.scss';
.ik-blc-main {
	// background-color: rgb(245, 245, 245);
	background-color: rgba(212, 226, 248, 0.244);
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
