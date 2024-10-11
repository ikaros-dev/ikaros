<script setup lang="ts">
import Aside from './Aside.vue';
import Header from './Header.vue';
import variables from '@/styles/variables.module.scss';
import {useLayoutStore} from '@/stores/layout';
import GlobalSearchDialog from '@/components/global-search/GlobalSearchDialog.vue';
import {computed, onMounted, onUnmounted, ref} from 'vue';
import {ElAside, ElCard, ElContainer, ElHeader, ElMain} from 'element-plus';
import {RouterView} from 'vue-router';
import {useSettingStore} from '@/stores/setting';

const layoutStore = useLayoutStore();
const settingStore = useSettingStore();

const asideWidth = computed(() => {
	return layoutStore.asideIsExtend
		? variables.sideBarWidth
		: variables.hideSideBarWidth;
});

const globalSearchDialogVisible = ref(false);

const isMac = /macintosh|mac os x/i.test(navigator.userAgent);

const handleGlobalSearchKeybinding = (e: KeyboardEvent) => {
	const { key, ctrlKey, metaKey } = e;
	if (key === '/' && ((ctrlKey && !isMac) || metaKey)) {
		globalSearchDialogVisible.value = true;
		e.preventDefault();
	}
};

const headerDom = ref();
const footerDom = ref();

const loadGlobalHeader = ()=>{
	if (settingStore.globalHeader) {
		var headerDiv = document.createElement("div");
		headerDiv.innerHTML = settingStore.globalHeader;
		headerDom.value.appendChild(headerDiv);
		// 获取所有的 <script> 标签
		const scriptElements = headerDiv.querySelectorAll<HTMLScriptElement>('script');
		
		// 提取并加载每个 script 的 src
		scriptElements.forEach(script => {
			const src = script.getAttribute('src');
			
			// 如果存在 src 属性
			if (src) {
				// 创建一个新的 script 标签
				const newScript = document.createElement('script');
				newScript.src = src;
				newScript.async = true; // 异步加载

				// 动态设置属性
				if (script instanceof HTMLScriptElement) {
					const attributes = script.attributes;
					for (let i = 0; i < attributes.length; i++) {
						const attr = attributes[i];
						console.debug(`Attribute Name: ${attr.name}, Attribute Value: ${attr.value}`);
						newScript.setAttribute(attr.name, attr.value);
					}
				}

				// 将新的 script 标签添加到 <head> 中，开始加载
				document.head.appendChild(newScript);
			}
		});
	}
}

const loadGolbalFooter = ()=>{
	if (settingStore.globalFooter) {
		var fotterDiv = document.createElement("div");
		fotterDiv.innerHTML = settingStore.globalFooter;
		footerDom.value.appendChild(fotterDiv);
		
		// 获取所有的 <script> 标签
		const scriptElements = fotterDiv.querySelectorAll<HTMLScriptElement>('script');
  
		// 提取并加载每个 script 的 src
		scriptElements.forEach(script => {
			console.debug('script', script);
			const src = script.getAttribute('src');
			
			// 如果存在 src 属性
			if (src) {
				// 创建一个新的 script 标签
				const newScript = document.createElement('script');
				newScript.src = src;
				newScript.async = true; // 异步加载

				// 动态设置属性
				if (script instanceof HTMLScriptElement) {
					const attributes = script.attributes;
					for (let i = 0; i < attributes.length; i++) {
						const attr = attributes[i];
						console.debug(`Attribute Name: ${attr.name}, Attribute Value: ${attr.value}`);
						newScript.setAttribute(attr.name, attr.value);
					}
				}
				
				// 将新的 script 标签添加到 <head> 中，开始加载
				footerDom.value.appendChild(newScript);
			}
		});
	}
}

onMounted(() => {
	document.addEventListener('keydown', handleGlobalSearchKeybinding);
	loadGlobalHeader();
	loadGolbalFooter();
});

onUnmounted(() => {
	document.removeEventListener('keydown', handleGlobalSearchKeybinding);
});
</script>

<template>
	<div v-if="settingStore.globalHeader">
		<div ref="headerDom"></div>
	</div>
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
	<div v-if="settingStore.globalFooter">
		<div ref="footerDom"></div>
	</div>
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
