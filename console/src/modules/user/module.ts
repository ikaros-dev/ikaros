import { definePlugin } from '@runikaros/shared';
import Login from './Login.vue';
import Profile from './Profile.vue';
import { UserFilled } from '@element-plus/icons-vue';
import { markRaw } from 'vue';
import Collection from './Collection.vue';
import { MessageBox } from '@element-plus/icons-vue';

// <el-icon><MessageBox /></el-icon>
export default definePlugin({
	name: 'User',
	components: {},
	routes: [
		{
			parentName: '',
			route: {
				path: '/login',
				component: Login,
				name: 'Login',
				meta: {
					title: 'core.login.title',
					menu: {},
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/profile',
				name: 'Profile',
				component: Profile,
				meta: {
					title: 'core.profile.title',
					menu: {
						name: 'core.sidebar.menu.items.profile',
						group: 'user',
						icon: markRaw(UserFilled),
						priority: 1,
					},
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/collection',
				name: 'Collection',
				component: Collection,
				meta: {
					title: 'core.collection.title',
					menu: {
						name: 'core.sidebar.menu.items.collection',
						group: 'user',
						icon: markRaw(MessageBox),
						priority: 1,
					},
				},
			},
		},
	],
});
