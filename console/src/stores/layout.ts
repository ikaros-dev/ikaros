import { defineStore } from 'pinia';

interface LayoutStoreState {
	asideIsExtend: boolean;
	currentActivePath: string;
}

export const useLayoutStore = defineStore('layout', {
	state: (): LayoutStoreState => ({
		asideIsExtend: true,
		currentActivePath: '0',
	}),
	actions: {
		switchLayoutAsideExtendState() {
			this.asideIsExtend = !this.asideIsExtend;
		},
	},
	// In order to config pinia-plugin-persist, please see https://github.com/Seb-L/pinia-plugin-persist
	persist: {
		enabled: true,
		strategies: [
			{
				key: 'ikaros-store-layout',
				storage: localStorage,
			},
		],
	},
});
