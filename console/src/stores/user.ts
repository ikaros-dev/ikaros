import { defineStore } from 'pinia';

import type { User } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';

interface UserStoreState {
	currentUser?: User;
	isAnonymous: boolean;
}

export const useUserStore = defineStore('user', {
	state: (): UserStoreState => ({
		currentUser: undefined,
		isAnonymous: true,
	}),
	actions: {
		// async login(username:string, password:string) {
		//     try {

		//       } catch (e) {
		//         console.error("Failed to login", e);
		//     }
		// },
		async fetchCurrentUser() {
			try {
				const { data, status } = await apiClient.user.getCurrentUserDetail();
				// console.log('rsp status', status);
				// console.log('rsp data: ', data);
				if (status === 200) {
					this.currentUser = data;
					this.isAnonymous = false;
					// console.log('current user: ', this.currentUser)
				}
			} catch (e) {
				console.error('Failed to fetch current user', e);
				this.isAnonymous = true;
			}
		},
	},
	// In order to config pinia-plugin-persist, please see https://github.com/Seb-L/pinia-plugin-persist
	// persist: {
	// 	enabled: true,
	// 	strategies: [
	// 		{
	// 			key: 'ikaros-store-user',
	// 			storage: localStorage,
	// 		},
	// 	],
	// },
});
