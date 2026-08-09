import { definePlugin } from '@runikaros/shared';
import BasicLayout from '@/layouts/BasicLayout.vue';
import Dashboard from './Dashboard.vue';

export default definePlugin({
	name: 'Dashboard',
	components: {},
	routes: [
		{
			path: '/',
			name: 'Root',
			component: BasicLayout,
			redirect: '/dashboard',
			children: [
				{
					path: 'dashboard',
					name: 'Dashboard',
					component: Dashboard,
					meta: {
						title: 'module.dashboard.title',
					},
				},
			],
		},
	],
});
