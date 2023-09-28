import type { App } from 'vue';
import { createI18n } from 'vue-i18n';
// @ts-ignore
import en from './en.yaml';
// @ts-ignore
import zhCN from './zh-CN.yaml';

export const locales = [
	{
		code: 'en',
		package: en,
		hidden: true,
	},
	{
		name: 'English',
		code: 'en-US',
		package: en,
	},
	{
		name: '简体中文',
		code: 'zh-CN',
		package: zhCN,
	},
	{
		code: 'zh',
		package: zhCN,
	},
];

const messages = locales.reduce((acc, cur) => {
	acc[cur.code] = cur.package;
	return acc;
}, {});

const i18n = createI18n({
	legacy: false,
	locale: getBrowserLanguage(),
	fallbackLocale: getBrowserLanguage(),
	messages,
});

export function getBrowserLanguage(): string {
	const browserLanguage = navigator.language;
	// eslint-disable-next-line no-unused-vars
	const language = messages[browserLanguage]
		? browserLanguage
		: browserLanguage.split('-')[0];
	// return language in messages ? language : 'zh-CN';
	return 'en';
}

export function setupI18n(app: App) {
	app.use(i18n);
}

export { i18n };
