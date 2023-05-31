import BasicLayout from '@/layouts/BasicLayout.vue';
import type { RouteRecordRaw } from 'vue-router';

export const routes: Array<RouteRecordRaw> = [
	{
		path: '/login',
		component: () => import('@/views/system/Login.vue'),
	},
	{
		path: '/',
		name: 'basic-layout',
		component: BasicLayout,
		redirect: '/dashboard',
		children: [
			{
				path: '/dashboard',
				component: () => import('@/views/Dashboard.vue'),
			},
			// Anime
			{
				path: '/anime/edit',
				component: () => import('@/views/anime/AnimeEdit.vue'),
			},
			{
				path: '/anime/list',
				component: () => import('@/views/anime/AnimeList.vue'),
			},
			// Comic
			{
				path: '/comic/edit',
				component: () => import('@/views/comic/ComicEdit.vue'),
			},
			{
				path: '/comic/list',
				component: () => import('@/views/comic/ComicList.vue'),
			},
			// Game
			{
				path: '/game/edit',
				component: () => import('@/views/game/GameEdit.vue'),
			},
			{
				path: '/game/list',
				component: () => import('@/views/game/GameList.vue'),
			},
			// Music
			{
				path: '/music/edit',
				component: () => import('@/views/music/MusicEdit.vue'),
			},
			{
				path: '/music/list',
				component: () => import('@/views/music/MusicList.vue'),
			},
			// Novel
			{
				path: '/novel/edit',
				component: () => import('@/views/novel/NovelEdit.vue'),
			},
			{
				path: '/novel/list',
				component: () => import('@/views/novel/NovelList.vue'),
			},
			// System
			{
				path: '/plugin',
				component: () => import('@/views/system/Plugin.vue'),
			},
			{
				path: '/profile',
				component: () => import('@/views/system/Profile.vue'),
			},
			{
				path: '/setting',
				component: () => import('@/views/system/Setting.vue'),
			},
		],
	},
];

export default routes;
