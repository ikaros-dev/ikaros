import { definePlugin } from '@runikaros/shared';
import Subjects from './Subjects.vue';
import SubjectPut from './SubjectPut.vue';
import SubjectPost from './SubjectPost.vue';
import SubjectDetails from './SubjectDetails.vue';

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
					title: 'module.subject.title',
					hidden: true,
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
					title: 'module.subject.put.title',
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
					title: 'module.subject.details.title',
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
					title: 'module.subject.post.title',
					hidden: true,
				},
			},
		},
	],
});
