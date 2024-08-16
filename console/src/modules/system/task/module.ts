import {definePlugin} from '@runikaros/shared';
import Tasks from './Tasks.vue';
import {MostlyCloudy} from '@element-plus/icons-vue';
import {markRaw} from 'vue';
import TaskDetails from './TaskDetails.vue';

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
					title: 'module.tasks.title',
					menu: {
						name: 'module.tasks.sidebar',
						group: 'system',
						icon: markRaw(MostlyCloudy),
						priority: 0,
					},
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/tasks/task/details/:id',
				name: 'TaskDetails',
				component: TaskDetails,
				meta: {
					title: 'module.tasks.details.title',
					hidden: true,
				},
			},
		},
	],
});
