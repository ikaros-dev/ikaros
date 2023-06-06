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
	// {
	// 	path: '/',
	// 	name: '首页',
	// 	component: BasicLayout,
	// 	redirect: '/dashboard',
	// 	children: [
	// 		// {
	// 		// 	path: '/dashboard',
	// 		// 	name: '仪表盘',
	// 		// 	component: () => import('@/views/Dashboard.vue'),
	// 		// },
	// 		// Anime
	// 		{
	// 			path: '/anime/edit',
	// 			name: '动漫编辑',
	// 			component: () => import('@/views/anime/AnimeEdit.vue'),
	// 		},
	// 		{
	// 			path: '/anime/list',
	// 			name: '动漫列表',
	// 			component: () => import('@/views/anime/AnimeList.vue'),
	// 		},
	// 		// Comic
	// 		{
	// 			path: '/comic/edit',
	// 			name: '漫画编辑',
	// 			component: () => import('@/views/comic/ComicEdit.vue'),
	// 		},
	// 		{
	// 			path: '/comic/list',
	// 			name: '漫画列表',
	// 			component: () => import('@/views/comic/ComicList.vue'),
	// 		},
	// 		// Game
	// 		{
	// 			path: '/game/edit',
	// 			name: '游戏编辑',
	// 			component: () => import('@/views/game/GameEdit.vue'),
	// 		},
	// 		{
	// 			path: '/game/list',
	// 			name: '游戏列表',
	// 			component: () => import('@/views/game/GameList.vue'),
	// 		},
	// 		// Music
	// 		{
	// 			path: '/music/edit',
	// 			name: '音声编辑',
	// 			component: () => import('@/views/music/MusicEdit.vue'),
	// 		},
	// 		{
	// 			path: '/music/list',
	// 			name: '音声列表',
	// 			component: () => import('@/views/music/MusicList.vue'),
	// 		},
	// 		// Novel
	// 		{
	// 			path: '/novel/edit',
	// 			name: '小说编辑',
	// 			component: () => import('@/views/novel/NovelEdit.vue'),
	// 		},
	// 		{
	// 			path: '/novel/list',
	// 			name: '小说列表',
	// 			component: () => import('@/views/novel/NovelList.vue'),
	// 		},
	// 		// System
	// 		{
	// 			path: '/plugin',
	// 			name: '系统插件',
	// 			component: () => import('@/views/system/Plugin.vue'),
	// 		},
	// 		{
	// 			path: '/profile',
	// 			name: '个人中心',
	// 			component: () => import('@/views/system/Profile.vue'),
	// 		},
	// 		{
	// 			path: '/setting',
	// 			name: '系统配置',
	// 			component: () => import('@/views/system/Setting.vue'),
	// 		},
	// 	],
	// },
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
