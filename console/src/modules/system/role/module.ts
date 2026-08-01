import { definePlugin } from '@runikaros/shared';
import { Ticket } from '@element-plus/icons-vue';
import { markRaw } from 'vue';
import { RouterView } from 'vue-router';
import Roles from './Roles.vue';
import RoleAuthorities from './RoleAuthorities.vue';

export default definePlugin({
	name: 'SystemSetting',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/roles',
				name: 'Roles',
				component: RouterView,
				meta: {
					title: 'module.roles.title',
					menu: {
						name: 'module.roles.sidebar',
						group: 'system',
						icon: markRaw(Ticket),
						priority: 1,
						admin: true,
					},
				},
				children: [
					{
						path: '',
						component: Roles,
					},
					{
						path: 'authorities/:roleId',
						alias: '/role/authorities/roleId/:roleId',
						name: 'RoleAuthorities',
						component: RoleAuthorities,
						meta: {
							title: 'module.roles.authorities.sidebar',
							hidden: true,
						},
					},
				],
			},
		},
	],
});
