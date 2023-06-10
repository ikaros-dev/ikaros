import { definePlugin } from '@runikaros/shared';
import Plugin from './Plugin.vue';
// @ts-ignore
import IconParkOutlinePlug from '~icons/icon-park-outline/plug';

export default definePlugin({
	name: 'User',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/plugins',
				name: 'Plugins',
				component: Plugin,
				meta: {
					title: 'core.plugin.title',
					menu: {
						name: 'core.sidebar.menu.items.plugins',
						group: 'system',
						icon: markRaw(IconParkOutlinePlug),
						priority: 0,
					},
				},
			},
		},
	],
});
