import {definePlugin} from '@runikaros/shared';
import {markRaw} from 'vue';
import {Ship} from '@element-plus/icons-vue';
import About from './About.vue';

export default definePlugin({
	name: 'SystemAbout',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/about',
				name: 'About',
				component: About,
				meta: {
					title: 'module.about.title',
					menu: {
						name: 'module.about.sidebar',
						group: 'system',
						icon: markRaw(Ship),
						priority: 99,
					},
				},
			},
		},
	],
});
