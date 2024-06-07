import BasicLayout from '@/layouts/BasicLayout.vue';
import type { RouteRecordRaw } from 'vue-router';
import type { MenuGroupType } from '@runikaros/shared';

export const routes: Array<RouteRecordRaw> = [
	{
		path: '/:pathMatch(.*)*',
		component: BasicLayout,
		children: [
			{
				path: '',
				name: 'router.title.notfound',
				component: () => import('@/views/exception/NotFound.vue'),
			},
		],
	},
	{
		path: '/forbidden',
		component: BasicLayout,
		redirect: '/forbidden',
		children: [
			{
				path: '',
				name: 'router.title.forbidden',
				component: () => import('@/views/exception/Forbidden.vue'),
			},
		],
	},
	{
		path: '/login',
		name: 'Login',
		component: () => import('@/modules/user/Login.vue'),
	},
	{
		path: '/redirect',
		name: 'Redirect',
		component: () => import('@/views/system/Redirect.vue'),
	},
];

export const coreMenuGroups: MenuGroupType[] = [
	{
		id: 'dashboard',
		name: undefined,
		priority: 0,
	},
	{
		id: 'content',
		name: 'router.sidebar.group.content',
		priority: 1,
	},
	{
		id: 'user',
		name: 'router.sidebar.group.user',
		priority: 2,
	},
	{
		id: 'interface',
		name: 'router.sidebar.group.interface',
		priority: 3,
	},
	{
		id: 'system',
		name: 'router.sidebar.group.system',
		priority: 4,
	},
	{
		id: 'tool',
		name: 'router.sidebar.group.tool',
		priority: 5,
	},
];

export default routes;
