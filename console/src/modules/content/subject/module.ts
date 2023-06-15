import { definePlugin } from '@runikaros/shared';
import Subjects from './Subjects.vue';
import SubjectEdit from './SubjectEdit.vue';
import { Tickets } from '@element-plus/icons-vue';

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
				path: '/subjects/subject/edit/:id',
				name: 'SubjectEdit',
				component: SubjectEdit,
				meta: {
					title: 'core.subject.edit.title',
					hidden: true,
				},
			},
		},
	],
});
