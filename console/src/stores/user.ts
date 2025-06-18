import { defineStore } from 'pinia';

import type { Role, User } from '@runikaros/api-client';
import { JwtApplyParamAuthTypeEnum } from '@runikaros/api-client';
import { apiClient, setApiClientJwtToken } from '@/utils/api-client';

interface UserStoreState {
	authType: JwtApplyParamAuthTypeEnum;
	currentUser?: User;
	currentRoles?: Role[];
	isAnonymous: boolean;
	jwtToken?: string;
	refreshToken?: string;
}

export const useUserStore = defineStore('user', {
	state: (): UserStoreState => ({
		authType: JwtApplyParamAuthTypeEnum.UsernamePassword,
		currentUser: undefined,
		isAnonymous: true,
		jwtToken: undefined,
		refreshToken: undefined,
	}),
	actions: {
		async fetchCurrentUser() {
			if (this.jwtToken) setApiClientJwtToken(this.jwtToken);
			try {
				const { data, status } = await apiClient.userMe.getUserMe();
				// console.log('rsp status', status);
				// console.log('rsp data: ', data);
				if (status === 200) {
					this.currentUser = data;
					this.isAnonymous = false;
					await this.fetchCurrentRole();
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
				const { data, status } = await apiClient.security.applyJwtToken({
					jwtApplyParam: {
						authType: 'USERNAME_PASSWORD',
						username: username,
						password: password,
					},
				});
				if (status === 200) {
					this.jwtToken = data.accessToken;
					this.refreshToken = data.refreshToken;
					this.isAnonymous = false;
				} else {
					this.jwtToken = undefined;
					this.refreshToken = undefined;
					this.isAnonymous = true;
				}
			} catch (e) {
				console.error('Failed to apply jwt token', e);
				this.isAnonymous = true;
			}
		},
		jwtTokenLogout() {
			this.jwtToken = undefined;
			this.refreshToken = undefined;
			this.isAnonymous = true;
			this.currentUser = undefined;
		},
		async fetchCurrentRole() {
			const {data} = await apiClient.userRole.getRolesForUser({userId: String(this.currentUser?.entity?.id ?? -1)});
			this.currentRoles = data
		},
		roleHasMaster() {
			return this.currentRoles?.some((item) => item.name === "MASTER");
		}
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
