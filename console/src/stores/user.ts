import {defineStore} from 'pinia';

import type {User} from '@runikaros/api-client';
import {JwtApplyParamAuthTypeEnum} from '@runikaros/api-client';
import {apiClient, setApiClientJwtToken} from '@/utils/api-client';

interface UserStoreState {
    authType: JwtApplyParamAuthTypeEnum;
	currentUser?: User;
	isAnonymous: boolean;
    jwtToken?: string;
}

export const useUserStore = defineStore('user', {
	state: (): UserStoreState => ({
		authType: JwtApplyParamAuthTypeEnum.UsernamePassword,
		currentUser: undefined,
		isAnonymous: true,
		jwtToken: undefined,
	}),
	actions: {
		async fetchCurrentUser() {
			if (this.jwtToken) setApiClientJwtToken(this.jwtToken);
			try {
				const { data, status } = await apiClient.user.getCurrentUserDetail();
				// console.log('rsp status', status);
				// console.log('rsp data: ', data);
				if (status === 200) {
					this.currentUser = data;
					this.isAnonymous = false;
					// console.log('current user: ', this.currentUser)
				} else {
					this.jwtToken = undefined;
					this.isAnonymous = true;
                }
			} catch (e) {
				console.error('Failed to fetch current user', e);
				this.isAnonymous = true;
				this.jwtToken = undefined;
			}
		},
        async applyJwtToken(username: string, password: string) {
			try {
                const {data, status} = await apiClient.security.applyJwtToken({
					jwtApplyParam: {
						authType: 'USERNAME_PASSWORD',
						username: username,
                        password: password,
                    },
                });
				if (status === 200) {
					this.jwtToken = data;
					this.isAnonymous = false;
				} else {
					this.jwtToken = undefined;
					this.isAnonymous = true;
                }
			} catch (e) {
				console.error('Failed to apply jwt token', e);
				this.isAnonymous = true;
			}
		},
		jwtTokenLogout() {
			this.jwtToken = undefined;
			this.isAnonymous = true;
			this.currentUser = undefined;
        },
	},
	persist: {
		enabled: true,
		strategies: [
			{
				key: 'ikaros-store-user',
				storage: localStorage,
			},
		],
	},
});
