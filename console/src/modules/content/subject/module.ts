import { definePlugin } from '@runikaros/shared';
import BasicLayout from '@/layouts/BasicLayout.vue';
import Subjects from './Subjects.vue';
import { Collection } from '@element-plus/icons-vue';

export default definePlugin({
	name: 'Subjects',
	components: {},
	routes: [
		{
			path: '/subjects',
			component: BasicLayout,
			children: [
				{
					path: '',
					name: 'Subjects',
					component: Subjects,
					meta: {
						title: 'core.subject.title',
						menu: {
							name: 'core.sidebar.menu.items.subjects',
							group: 'content',
							icon: markRaw(Collection),
							priority: 1,
						},
					},
				},
			],
		},
	],
});
