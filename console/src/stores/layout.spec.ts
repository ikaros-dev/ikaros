import { createApp, nextTick } from 'vue';
import { createPinia, setActivePinia } from 'pinia';
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';
import { beforeEach, describe, expect, it } from 'vitest';
import { useLayoutStore } from './layout';

describe('布局状态管理', () => {
	beforeEach(() => {
		localStorage.clear();
		const app = createApp({});
		const pinia = createPinia().use(piniaPluginPersistedstate);
		app.use(pinia);
		setActivePinia(pinia);
	});

	it('更新侧栏、当前路由和语言', () => {
		const store = useLayoutStore();

		store.switchLayoutAsideExtendState();
		store.updatecurrentActivePathByRoutePath('/subjects');
		store.setI18nCode('en');

		expect(store.asideIsExtend).toBe(false);
		expect(store.currentActivePath).toBe('/subjects');
		expect(store.i18nCode).toBe('en');
	});

	it('从原有本地存储 key 恢复布局状态', () => {
		localStorage.setItem(
			'ikaros-store-layout',
			JSON.stringify({
				asideIsExtend: false,
				currentActivePath: '/subjects',
				i18nCode: 'en',
			})
		);

		const store = useLayoutStore();

		expect(store.asideIsExtend).toBe(false);
		expect(store.currentActivePath).toBe('/subjects');
		expect(store.i18nCode).toBe('en');
	});

	it('将布局状态写入原有本地存储 key', async () => {
		const store = useLayoutStore();

		store.switchLayoutAsideExtendState();
		store.updatecurrentActivePathByRoutePath('/subjects');
		store.setI18nCode('en');
		await nextTick();

		expect(
			JSON.parse(localStorage.getItem('ikaros-store-layout') ?? '{}')
		).toEqual({
			asideIsExtend: false,
			currentActivePath: '/subjects',
			i18nCode: 'en',
		});
	});
});
