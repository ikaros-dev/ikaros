// @ts-nocheck
import { defineStore } from 'pinia';
import { apiClient } from '@/utils/api-client';

interface SettingStoreState {
	siteTitle: string;
	siteSubhead: string;
	logo: string;
	favicon: string;
	allowRegister: boolean;
	defaultRole: string;
	globalHeader: string;
	globalFooter: string;
	remoteEnable: boolean;
}

const settingConfigMapName = 'setting.server.ikaros.run';

export const useSettingStore = defineStore('setting', {
	state: (): SettingStoreState => ({
		siteTitle: '',
		siteSubhead: '',
		logo: '',
		favicon: '',
		allowRegister: false,
		defaultRole: '',
		globalHeader: '',
		globalFooter: '',
		remoteEnable: false,
	}),
	actions: {
		async fetchSystemSetting() {
			try {
				const { data } = await apiClient.configmap.getConfigmapMeta({
					name: settingConfigMapName,
					metaName: 'data',
				});
				// console.log(data)
				this.siteTitle = data.SITE_TITLE;
				this.siteSubhead = data.SITE_SUBHEAD;
				this.logo = data.LOGO;
				this.favicon = data.FAVICON;
				this.allowRegister = data.ALLOW_REGISTER === true;
				this.defaultRole = data.DEFAULT_ROLE;
				this.globalHeader = data.GLOBAL_HEADER;
				this.globalFooter = data.GLOBAL_FOOTER;
				this.remoteEnable = data.REMOTE_ENABLE === true;
			} catch (e) {
				console.error('Failed to fetch system setting', e);
			}
		},
	},
});
