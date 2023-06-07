import { definePlugin } from '@runikaros/shared';
import Login from './Login.vue';
import Profile from './Profile.vue';
import BasicLayout from '@/layouts/BasicLayout.vue';
import { UserFilled } from '@element-plus/icons-vue';

export default definePlugin({
	name: 'User',
	components: {},
	routes: [
		{
			path: '/login',
			component: Login,
			name: 'Login',
			meta: {
				title: 'core.login.title',
				menu: {},
			},
		},
		{
			path: '/profile',
			component: BasicLayout,
			children: [
				{
					path: '',
					name: 'Profile',
					component: Profile,
					meta: {
						title: 'core.profile.title',
						menu: {
							name: 'core.sidebar.menu.items.profile',
							group: 'system',
							icon: markRaw(UserFilled),
							priority: 0,
						},
					},
				},
			],
		},
	],
});
