import BasicLayout from '@/layouts/BasicLayout.vue';
import type { RouteRecordRaw } from 'vue-router';
import type { MenuGroupType } from '@runikaros/shared';
import NotFound from '@/views/exception/NotFound.vue';
import Forbidden from '@/views/exception/Forbidden.vue';
import Redirect from '@/views/system/Redirect.vue';

export const routes: Array<RouteRecordRaw> = [
	{
		path: '/:pathMatch(.*)*',
		component: BasicLayout,
		children: [{ path: '', name: 'NotFound', component: NotFound }],
	},
	{
		path: '/403',
		component: BasicLayout,
		children: [
			{
				path: '',
				name: 'Forbidden',
				component: Forbidden,
			},
		],
	},
	{
		path: '/login',
		name: 'Login',
		component: () => import('@/modules/system/user/Login.vue'),
	},
	{
		path: '/redirect',
		name: 'Redirect',
		component: Redirect,
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
		name: 'core.sidebar.menu.groups.content',
		priority: 1,
	},
	{
		id: 'interface',
		name: 'core.sidebar.menu.groups.interface',
		priority: 2,
	},
	{
		id: 'system',
		name: 'core.sidebar.menu.groups.system',
		priority: 3,
	},
	{
		id: 'tool',
		name: 'core.sidebar.menu.groups.tool',
		priority: 4,
	},
];

export default routes;
