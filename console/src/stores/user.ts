import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

import type { Role, User } from '@runikaros/api-client';
import { JwtApplyParamAuthTypeEnum } from '@runikaros/api-client';
import { apiClient, setApiClientJwtToken } from '@/utils/api-client';

export const useUserStore = defineStore('user', () => {
	const authType = ref<JwtApplyParamAuthTypeEnum>(JwtApplyParamAuthTypeEnum.UsernamePassword);
	const currentUser = ref<User | undefined>(undefined);
	const currentRoles = ref<Role[] | undefined>(undefined);
	const isAnonymous = ref(true);
	const jwtToken = ref<string | undefined>(undefined);
	const refreshToken = ref<string | undefined>(undefined);

	async function fetchCurrentUser() {
		if (jwtToken.value) setApiClientJwtToken(jwtToken.value);
		try {
			const { data, status } = await apiClient.userMe.getUserMe();
			if (status === 200) {
				currentUser.value = data;
				isAnonymous.value = false;
				await fetchCurrentRole();
			} else {
				jwtToken.value = undefined;
				isAnonymous.value = true;
			}
		} catch (e) {
			console.error('Failed to fetch current user', e);
			isAnonymous.value = true;
			jwtToken.value = undefined;
		}
	}

	async function applyJwtToken(username: string, password: string) {
		try {
			const { data, status } = await apiClient.security.applyJwtToken({
				jwtApplyParam: {
					authType: 'USERNAME_PASSWORD',
					username: username,
					password: password,
				},
			});
			if (status === 200) {
				jwtToken.value = data.accessToken;
				refreshToken.value = data.refreshToken;
				isAnonymous.value = false;
			} else {
				jwtToken.value = undefined;
				refreshToken.value = undefined;
				isAnonymous.value = true;
			}
		} catch (e) {
			console.error('Failed to apply jwt token', e);
			isAnonymous.value = true;
		}
	}

	function jwtTokenLogout() {
		jwtToken.value = undefined;
		refreshToken.value = undefined;
		isAnonymous.value = true;
		currentUser.value = undefined;
	}

	async function fetchCurrentRole() {
		const { data } = await apiClient.userRole.getRolesForUser({
			userId: String(currentUser.value?.entity?.id ?? -1),
		});
		currentRoles.value = data;
	}

	function roleHasMaster() {
		return currentRoles.value?.some((item) => item.name === 'MASTER');
	}

	return {
		authType,
		currentUser,
		currentRoles,
		isAnonymous,
		jwtToken,
		refreshToken,
		fetchCurrentUser,
		applyJwtToken,
		jwtTokenLogout,
		fetchCurrentRole,
		roleHasMaster,
	};
}, {
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
