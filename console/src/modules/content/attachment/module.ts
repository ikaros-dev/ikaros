import { definePlugin } from '@runikaros/shared';
import { markRaw } from 'vue';
import { Folder as FolderIcon } from '@element-plus/icons-vue';
import Attachment from './Attachment.vue';

// <el-icon><MessageBox /></el-icon>
export default definePlugin({
	name: 'User',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/attachment',
				name: 'Attachment',
				component: Attachment,
				meta: {
					title: 'core.attachment.title',
					menu: {
						name: 'core.sidebar.menu.items.attachment',
						group: 'content',
						icon: markRaw(FolderIcon),
						priority: 0,
					},
				},
			},
		},
	],
});
