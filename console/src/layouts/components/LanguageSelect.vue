<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { ElSelect, ElOption } from 'element-plus';
import { ref } from 'vue';
import { locales, changeI18nLocal } from '@/locales';

const { locale } = useI18n();
const selectedLanguage = ref(locale);

const changeLanguage = () => {
	changeI18nLocal(selectedLanguage.value);
};
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
