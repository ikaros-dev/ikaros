import { definePlugin } from '@runikaros/shared';
import Files from './Files.vue';
import { Files as FilesIcon } from '@element-plus/icons-vue';

export default definePlugin({
	name: 'FileList',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/files',
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
		},
	],
});
