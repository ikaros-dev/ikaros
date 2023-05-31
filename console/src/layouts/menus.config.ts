import { Component } from 'vue';
import {
	DataAnalysis,
	Edit,
	Files,
	Picture,
	Ship,
	Headset,
	Collection,
	Setting,
	Tools,
	UserFilled,
	Orange,
} from '@element-plus/icons-vue';

export interface MenuItem {
	// menu item index
	id: string;
	// menu item route
	path: string;
	// menu item span
	name: string;
	// @element-plus/icons-vue
	elIcon: Component;
	// sub menu item
	children: Array<MenuItem>;
}

export const menus: Array<MenuItem> = [
	// {
	// 	id: '0',
	// 	path: '/dashboard',
	// 	name: '仪表盘',
	// 	elIcon: Odometer,
	// 	children: [],
	// },
	{
		id: '1',
		path: '#',
		name: '番剧',
		elIcon: DataAnalysis,
		children: [
			{
				id: '11',
				path: '/anime/edit',
				name: '编辑',
				elIcon: Edit,
				children: [],
			},
			{
				id: '12',
				path: '/anime/list',
				name: '列表',
				elIcon: Files,
				children: [],
			},
		],
	},
	{
		id: '2',
		path: '#',
		name: '漫画',
		elIcon: Picture,
		children: [
			{
				id: '21',
				path: '/comic/edit',
				name: '编辑',
				elIcon: Edit,
				children: [],
			},
			{
				id: '22',
				path: '/comic/list',
				name: '列表',
				elIcon: Files,
				children: [],
			},
		],
	},
	{
		id: '3',
		path: '#',
		name: '游戏',
		elIcon: Ship,
		children: [
			{
				id: '31',
				path: '/game/edit',
				name: '编辑',
				elIcon: Edit,
				children: [],
			},
			{
				id: '32',
				path: '/game/list',
				name: '列表',
				elIcon: Files,
				children: [],
			},
		],
	},
	{
		id: '4',
		path: '#',
		name: '音声',
		elIcon: Headset,
		children: [
			{
				id: '41',
				path: '/music/edit',
				name: '编辑',
				elIcon: Edit,
				children: [],
			},
			{
				id: '42',
				path: '/music/list',
				name: '列表',
				elIcon: Files,
				children: [],
			},
		],
	},
	{
		id: '5',
		path: '#',
		name: '小说',
		elIcon: Collection,
		children: [
			{
				id: '51',
				path: '/novel/edit',
				name: '编辑',
				elIcon: Edit,
				children: [],
			},
			{
				id: '52',
				path: '/novel/list',
				name: '列表',
				elIcon: Files,
				children: [],
			},
		],
	},
	{
		id: '9',
		path: '#',
		name: '系统',
		elIcon: Setting,
		children: [
			{
				id: '91',
				path: '/plugin',
				name: '插件',
				elIcon: Tools,
				children: [],
			},
			{
				id: '92',
				path: '/profile',
				name: '个人',
				elIcon: UserFilled,
				children: [],
			},
			{
				id: '93',
				path: '/setting',
				name: '设置',
				elIcon: Orange,
				children: [],
			},
		],
	},
];

export default menus;
