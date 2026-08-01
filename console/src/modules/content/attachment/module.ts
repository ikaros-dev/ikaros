import { definePlugin } from '@runikaros/shared';
import { markRaw } from 'vue';
import { RouterView } from 'vue-router';
import { Folder as FolderIcon } from '@element-plus/icons-vue';
import Attachments from './Attachments.vue';
import AttachmentDrivers from './AttachmentDrivers.vue';
import AttachmentDriverPost from './AttachmentDriverPost.vue';
import AttachmentDriverPut from './AttachmentDriverPut.vue';

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
				component: RouterView,
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
				children: [
					{
						path: '',
						component: Attachments,
					},
					{
						path: '/attachment/drivers',
						name: 'AttachmentDrivers',
						component: RouterView,
						meta: {
							title: 'module.attachment.driver.title',
							hidden: true,
							menu: {
								name: 'module.attachment.driver.sidebar',
								hidden: true,
								admin: true,
							},
						},
						children: [
							{
								path: '',
								component: AttachmentDrivers,
							},
							{
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
									},
								},
							},
							{
								path: '/attachment/driver/put/:id',
								name: 'AttachmentDriverPut',
								component: AttachmentDriverPut,
								meta: {
									title: 'module.attachment.driver.put.title',
									hidden: true,
									menu: {
										name: 'module.attachment.driver.put.sidebar',
										hidden: true,
										admin: true,
									},
								},
							},
						],
					},
				],
			},
		},
	],
});
