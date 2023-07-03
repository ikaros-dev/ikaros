import { definePlugin } from '@runikaros/shared';
// eslint-disable-next-line no-unused-vars
import Files from './Files.vue';
import Folders from './Folders.vue';
import {
	Files as FilesIcon,
	Folder as FolderIcon,
} from '@element-plus/icons-vue';
import { markRaw } from 'vue';

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
		{
			parentName: 'Root',
			route: {
				path: '/folders',
				name: 'Folders',
				component: Folders,
				meta: {
					title: 'core.folder.title',
					menu: {
						name: 'core.sidebar.menu.items.folders',
						group: 'content',
						icon: markRaw(FolderIcon),
						priority: 0,
					},
				},
			},
		},
	],
});
