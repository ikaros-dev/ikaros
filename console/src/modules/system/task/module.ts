import { definePlugin } from '@runikaros/shared';
import Tasks from './Tasks.vue';
import { Ship } from '@element-plus/icons-vue';
import { markRaw } from 'vue';

export default definePlugin({
	name: 'SystemSetting',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/tasks',
				name: 'Tasks',
				component: Tasks,
				meta: {
					title: 'core.tasks.title',
					menu: {
						name: 'core.sidebar.menu.items.tasks',
						group: 'system',
						icon: markRaw(Ship),
						priority: 0,
					},
				},
			},
		},
	],
});
