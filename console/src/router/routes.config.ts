import BasicLayout from '@/layouts/BasicLayout.vue';
import type { RouteRecordRaw } from 'vue-router';

export const routes: Array<RouteRecordRaw> = [
	{
		path: '/hello',
		component: () => import('@/components/HelloWorld.vue'),
	},
	{
		path: '/',
		component: BasicLayout,
	},
];

export default routes;
