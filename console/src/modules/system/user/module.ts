import { definePlugin } from '@runikaros/shared';
import { UserFilled } from '@element-plus/icons-vue';
import { markRaw } from 'vue';
import Users from './Users.vue';

export default definePlugin({
	name: 'SystemSetting',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/users',
				name: 'Users',
				component: Users,
				meta: {
					title: 'module.users.title',
					menu: {
						name: 'module.users.sidebar',
						group: 'system',
						icon: markRaw(UserFilled),
						priority: 2,
					},
				},
			},
		},
	],
});
