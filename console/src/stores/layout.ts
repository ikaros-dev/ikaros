import { defineStore } from 'pinia';

interface LayoutStoreState {
	asideIsExtend: boolean;
	currentActivePath: string;
	i18nCode: string;
}

export const useLayoutStore = defineStore('layout', {
	state: (): LayoutStoreState => ({
		asideIsExtend: true,
		currentActivePath: '/dashboard',
		i18nCode: 'zh-CN',
	}),
	actions: {
		switchLayoutAsideExtendState() {
			this.asideIsExtend = !this.asideIsExtend;
		},
		updatecurrentActivePathByRoutePath(path) {
			this.currentActivePath = path;
		},
		setI18nCode(code: string) {
			this.i18nCode = code;
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
