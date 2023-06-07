import { definePlugin } from '@runikaros/shared';
import BasicLayout from '@/layouts/BasicLayout.vue';
import Files from './Files.vue';
import { Files as FilesIcon } from '@element-plus/icons-vue';

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
					name: 'Files',
					component: Files,
					meta: {
						title: 'core.file.title',
						menu: {
							name: 'core.sidebar.menu.items.files',
							group: 'content',
							icon: markRaw(FilesIcon),
							priority: 0,
						},
					},
				},
			],
		},
	],
});
