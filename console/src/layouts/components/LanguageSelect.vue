<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { ElSelect, ElOption } from 'element-plus';
import { onMounted, ref } from 'vue';
import { locales, changeI18nLocal } from '@/locales';
import { useLayoutStore } from '@/stores/layout';

const { locale } = useI18n();
const selectedLanguage = ref(locale);
const layoutStore = useLayoutStore();

const changeLanguage = () => {
	changeI18nLocal(selectedLanguage.value);
	// console.debug('selectedLanguage.value', selectedLanguage.value);
	layoutStore.setI18nCode(selectedLanguage.value);
	window.location.reload();
};
onMounted(() => {
	const selectdI18nCode = layoutStore.i18nCode;
	if (selectdI18nCode && selectdI18nCode != '') {
		// console.debug('selectdI18nCode', selectdI18nCode)
		selectedLanguage.value = selectdI18nCode;
		changeI18nLocal(selectedLanguage.value);
	}
});
</script>

<template>
	<el-select
		v-model="selectedLanguage"
		:style="{ width: '100px' }"
		@change="changeLanguage"
	>
		<el-option
			v-for="language in locales.filter((ele) => ele.name)"
			:key="language.code"
			:label="language.name"
			:value="language.code"
		></el-option>
	</el-select>
</template>

<style lang="scss" scoped></style>
