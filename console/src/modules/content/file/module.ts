import { definePlugin } from '@runikaros/shared';
import BasicLayout from '@/layouts/BasicLayout.vue';
import FileList from './FileList.vue';
import { Document } from '@element-plus/icons-vue';

export default definePlugin({
	name: 'FileList',
	components: {},
	routes: [
		{
			path: '/files',
			component: BasicLayout,
			children: [
				{
					path: '',
					name: 'FileList',
					component: FileList,
					meta: {
						title: 'core.file.title',
						menu: {
							name: 'core.sidebar.menu.items.file',
							group: 'content',
							icon: markRaw(Document),
							priority: 0,
						},
					},
				},
			],
		},
	],
});
