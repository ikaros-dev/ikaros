import { definePlugin } from '@runikaros/shared';
import Setting from './Setting.vue';
import { Setting as SettingIcon } from '@element-plus/icons-vue';
import { markRaw } from 'vue';

export default definePlugin({
	name: 'SystemSetting',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/setting',
				name: 'Setting',
				component: Setting,
				meta: {
					title: 'module.setting.title',
					menu: {
						name: 'module.setting.sidebar',
						group: 'system',
						icon: markRaw(SettingIcon),
						priority: 0,
					},
				},
			},
		},
	],
});
