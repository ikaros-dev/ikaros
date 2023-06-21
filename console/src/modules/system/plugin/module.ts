import { definePlugin } from '@runikaros/shared';
import Plugin from './Plugin.vue';
import { Connection } from '@element-plus/icons-vue';
import { markRaw } from 'vue';
import PluginDetail from '@/modules/system/plugin/PluginDetail.vue';

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
						icon: markRaw(Connection),
						priority: 0,
					},
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/plugin/:name/details',
				name: 'PluginDetails',
				component: PluginDetail,
				meta: {
					title: 'core.plugin.details.title',
					hidden: true,
				},
			},
		},
	],
});
