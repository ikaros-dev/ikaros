import { defineStore } from 'pinia';
import { menus, MenuItem } from '@/layouts/menus.config';

interface LayoutStoreState {
	asideIsExtend: boolean;
	currentActiveId: string;
}

const getIdByPath = (path) => {
	const defaultId = '0';
	if ('/' === path || '/dashboard' === path) {
		return defaultId;
	}

	const items: MenuItem[] = [];
	menus.forEach((menu) => {
		if (menu.children && menu.children.length && menu.children.length > 0) {
			const item: MenuItem[] = menu.children.filter((it) => {
				return it.path === path;
			});
			if (item.length) {
				items.push(item[0]);
			}
		}
	});

	return items.length ? items[0] && items[0].id : defaultId;
};

export const useLayoutStore = defineStore('layout', {
	state: (): LayoutStoreState => ({
		asideIsExtend: true,
		currentActiveId: '0',
	}),
	actions: {
		switchLayoutAsideExtendState() {
			this.asideIsExtend = !this.asideIsExtend;
		},
		updateCurrentActionIdByRoutePath(path) {
			this.currentActiveId = getIdByPath(path);
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
