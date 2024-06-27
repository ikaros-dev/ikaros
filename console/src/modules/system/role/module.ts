import {definePlugin} from '@runikaros/shared';
import {Ticket} from '@element-plus/icons-vue';
import {markRaw} from 'vue';
import Roles from './Roles.vue';

export default definePlugin({
	name: 'SystemSetting',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/roles',
				name: 'Roles',
				component: Roles,
				meta: {
					title: 'module.roles.title',
					menu: {
						name: 'module.roles.sidebar',
						group: 'system',
						icon: markRaw(Ticket),
						priority: 1,
					},
				},
			},
		},
	],
});
