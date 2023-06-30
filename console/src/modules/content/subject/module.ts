import { definePlugin } from '@runikaros/shared';
import { Tickets } from '@element-plus/icons-vue';
import Subjects from './Subjects.vue';
import SubjectPut from './SubjectPut.vue';
import SubjectPost from './SubjectPost.vue';
import SubjectDetails from './SubjectDetails.vue';
import { markRaw } from 'vue';

export default definePlugin({
	name: 'Subjects',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/subjects',
				name: 'Subjects',
				component: Subjects,
				meta: {
					title: 'core.subject.title',
					menu: {
						name: 'core.sidebar.menu.items.subjects',
						group: 'content',
						icon: markRaw(Tickets),
						priority: 1,
					},
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/subjects/subject/put/:id',
				name: 'SubjectPut',
				component: SubjectPut,
				meta: {
					title: 'core.subject.put.title',
					hidden: true,
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/subjects/subject/details/:id',
				name: 'SubjectDetails',
				component: SubjectDetails,
				meta: {
					title: 'core.subject.details.title',
					hidden: true,
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/subjects/subject/post',
				name: 'SubjectPost',
				component: SubjectPost,
				meta: {
					title: 'core.subject.post.title',
					hidden: true,
				},
			},
		},
	],
});
