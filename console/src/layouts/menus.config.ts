import { Component } from 'vue';

export interface MenuItem {
	// menu item index
	id: string;
	// menu item route
	path: string;
	// menu item span
	name: string;
	// @element-plus/icons-vue
	elIcon: Component;
	// sub menu item
	children: Array<MenuItem>;
}
export const menus: Array<MenuItem> = [];
export default menus;
