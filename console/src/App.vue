<script setup lang="ts">
import { ElConfigProvider } from 'element-plus';

// import { getBrowserLanguage } from './locales';
import en from 'element-plus/dist/locale/en.mjs';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { useLayoutStore } from './stores/layout';


// const language = getBrowserLanguage();

const layoutStore = useLayoutStore();
const i18nCode = layoutStore.i18nCode;

let locale = zhCn;
console.debug('app i18n code', i18nCode)
if ('en-US' === i18nCode || 'en' == i18nCode) {
	locale = en;
} else if ('zhCn' === i18nCode || 'zh' === i18nCode) {
	locale = zhCn;
} 

</script>

<template>
	<el-config-provider :locale="locale">
		<RouterView />
	</el-config-provider>
</template>

<style scoped lang="scss"></style>
