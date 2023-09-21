<script setup lang="ts">
import { ElConfigProvider } from 'element-plus';

import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import { getBrowserLanguage } from './locales';
import en from 'element-plus/dist/locale/en.mjs';
import ja from 'element-plus/dist/locale/ja.mjs';

const language = getBrowserLanguage();

let locale = zhCn;
if ('en' === language) {
	locale = en;
} else if ('jp' === language) {
	locale = ja;
}
</script>

<template>
	<el-config-provider :locale="locale">
		<RouterView />
	</el-config-provider>
</template>

<style scoped lang="scss"></style>
