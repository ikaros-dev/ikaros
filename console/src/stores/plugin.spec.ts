import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { usePluginModuleStore } from './plugin';

describe('插件模块状态管理', () => {
	beforeEach(() => {
		setActivePinia(createPinia());
	});

	it('按注册顺序保存插件模块', () => {
		const store = usePluginModuleStore();
		const firstPlugin = { name: 'first', routes: [] };
		const secondPlugin = { name: 'second', routes: [] };

		store.registerPluginModule(firstPlugin);
		store.registerPluginModule(secondPlugin);

		expect(store.pluginModules).toEqual([firstPlugin, secondPlugin]);
	});
});
