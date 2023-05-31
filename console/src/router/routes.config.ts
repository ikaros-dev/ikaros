import BasicLayout from '@/layouts/BasicLayout.vue';
import type { RouteRecordRaw } from 'vue-router';

export const routes: Array<RouteRecordRaw> = [
	{
		path: '/login',
		component: () => import('@/views/system/Login.vue'),
	},
	{
		path: '/',
		name: '首页',
		component: BasicLayout,
		redirect: '/dashboard',
		children: [
			{
				path: '/dashboard',
				name: '仪表盘',
				component: () => import('@/views/Dashboard.vue'),
			},
			// Anime
			{
				path: '/anime/edit',
				name: '动漫编辑',
				component: () => import('@/views/anime/AnimeEdit.vue'),
			},
			{
				path: '/anime/list',
				name: '动漫列表',
				component: () => import('@/views/anime/AnimeList.vue'),
			},
			// Comic
			{
				path: '/comic/edit',
				name: '漫画编辑',
				component: () => import('@/views/comic/ComicEdit.vue'),
			},
			{
				path: '/comic/list',
				name: '漫画列表',
				component: () => import('@/views/comic/ComicList.vue'),
			},
			// Game
			{
				path: '/game/edit',
				name: '游戏编辑',
				component: () => import('@/views/game/GameEdit.vue'),
			},
			{
				path: '/game/list',
				name: '游戏列表',
				component: () => import('@/views/game/GameList.vue'),
			},
			// Music
			{
				path: '/music/edit',
				name: '音声编辑',
				component: () => import('@/views/music/MusicEdit.vue'),
			},
			{
				path: '/music/list',
				name: '音声列表',
				component: () => import('@/views/music/MusicList.vue'),
			},
			// Novel
			{
				path: '/novel/edit',
				name: '小说编辑',
				component: () => import('@/views/novel/NovelEdit.vue'),
			},
			{
				path: '/novel/list',
				name: '小说列表',
				component: () => import('@/views/novel/NovelList.vue'),
			},
			// System
			{
				path: '/plugin',
				name: '系统插件',
				component: () => import('@/views/system/Plugin.vue'),
			},
			{
				path: '/profile',
				name: '个人中心',
				component: () => import('@/views/system/Profile.vue'),
			},
			{
				path: '/setting',
				name: '系统配置',
				component: () => import('@/views/system/Setting.vue'),
			},
		],
	},
];

export default routes;
