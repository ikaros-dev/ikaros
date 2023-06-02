import { definePlugin } from '@runikaros/shared';
import BasicLayout from '@/layouts/BasicLayout.vue';
import Dashboard from './Dashboard.vue';
import { Odometer } from '@element-plus/icons-vue';

export default definePlugin({
	name: 'Dashboard',
	components: {},
	routes: [
		{
			path: '/',
			component: BasicLayout,
			name: 'Root',
			redirect: '/dashboard',
			children: [
				{
					path: 'dashboard',
					name: 'Dashboard',
					component: Dashboard,
					meta: {
						title: 'core.dashboard.title',
						searchable: true,
						menu: {
							name: 'core.sidebar.menu.items.dashboard',
							group: 'dashboard',
							icon: markRaw(Odometer),
							priority: 0,
							mobile: true,
						},
					},
				},
			],
		},
	],
});
