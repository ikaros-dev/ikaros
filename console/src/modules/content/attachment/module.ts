import { definePlugin } from '@runikaros/shared';
import { markRaw } from 'vue';
import { Folder as FolderIcon } from '@element-plus/icons-vue';
import Attachments from './Attachments.vue';
import AttachmentDrivers from './AttachmentDrivers.vue'
import AttachmentDriverPost from './AttachmentDriverPost.vue'

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
		{
			parentName: 'Root',
			route: {
				path: '/attachment/drivers',
				name: 'AttachmentDrivers',
				component: AttachmentDrivers,
				meta: {
					title: 'module.attachment.driver.title',
					hidden: true,
					menu: {
						name: 'module.attachment.driver.sidebar',
						hidden: true,
						admin: true,
					}
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/attachment/driver/post',
				name: 'AttachmentDriverPost',
				component: AttachmentDriverPost,
				meta: {
					title: 'module.attachment.driver.post.title',
					hidden: true,
					menu: {
						name: 'module.attachment.driver.post.sidebar',
						hidden: true,
						admin: true,
					}
				},
			},
		},
	],
});
