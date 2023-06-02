import { defineStore } from 'pinia';
import type { PluginModule as PluginModuleRaw } from '@runikaros/shared';

export interface PluginModule extends PluginModuleRaw {}

interface PluginStoreState {
	pluginModules: PluginModule[];
}

export const usePluginModuleStore = defineStore('plugin', {
	state: (): PluginStoreState => ({
		pluginModules: [],
	}),
	actions: {
		registerPluginModule(pluginModule: PluginModule) {
			this.pluginModules.push(pluginModule);
		},
	},
});
