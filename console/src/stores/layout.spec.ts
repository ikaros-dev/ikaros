import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { useLayoutStore } from './layout';

describe('布局状态管理', () => {
	beforeEach(() => {
		setActivePinia(createPinia());
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
});
