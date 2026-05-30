import { definePlugin } from '@runikaros/shared';
import { markRaw } from 'vue';
import { Coin } from '@element-plus/icons-vue';
import RegularRules from './RegularRules.vue';

export default definePlugin({
	name: 'RegularRules',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/regulars',
				name: 'Regulars',
				component: RegularRules,
				meta: {
					title: 'module.regular.title',
					menu: {
						name: 'module.regular.sidebar',
						group: 'content',
						icon: markRaw(Coin),
						priority: 2,
					},
				},
			},
		},
	],
});
