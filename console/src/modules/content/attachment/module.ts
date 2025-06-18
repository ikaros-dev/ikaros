import { definePlugin } from '@runikaros/shared';
import { markRaw } from 'vue';
import { Folder as FolderIcon } from '@element-plus/icons-vue';
import Attachments from './Attachments.vue';

// <el-icon><MessageBox /></el-icon>
export default definePlugin({
	name: 'User',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/attachments',
				name: 'Attachments',
				component: Attachments,
				meta: {
					title: 'module.attachment.title',
					menu: {
						name: 'module.attachment.sidebar',
						group: 'content',
						icon: markRaw(FolderIcon),
						priority: 0,
						admin: true,
					},
				},
			},
		},
	],
});
