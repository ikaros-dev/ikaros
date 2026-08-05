import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

import type { Role, User } from '@runikaros/api-client';
import { JwtApplyParamAuthTypeEnum } from '@runikaros/api-client';
import { apiClient, setApiClientJwtToken } from '@/utils/api-client';
import axios from 'axios';

export const useUserStore = defineStore('user', () => {
	const authType = ref<JwtApplyParamAuthTypeEnum>(JwtApplyParamAuthTypeEnum.UsernamePassword);
	const currentUser = ref<User | undefined>(undefined);
	const currentRoles = ref<Role[] | undefined>(undefined);
	const isAnonymous = ref(true);
	const jwtToken = ref<string | undefined>(undefined);
	const refreshToken = ref<string | undefined>(undefined);
	const totpTempToken = ref<string | undefined>(undefined);
	const totpRequired = ref(false);
	const consoleAccessDenied = ref(false);

	function clearAuthentication() {
		currentUser.value = undefined;
		currentRoles.value = undefined;
		isAnonymous.value = true;
		jwtToken.value = undefined;
		refreshToken.value = undefined;
		totpTempToken.value = undefined;
		totpRequired.value = false;
		setApiClientJwtToken();
	}

	async function fetchCurrentUser() {
		if (jwtToken.value) setApiClientJwtToken(jwtToken.value);
		try {
			const { data, status } = await apiClient.userMe.getUserMe({
				validateStatus: (status) =>
					status === 401 ||
					status === 403 ||
					(status >= 200 && status < 300),
			});
			if (status === 200) {
				consoleAccessDenied.value = false;
				currentUser.value = data;
				isAnonymous.value = false;
				await fetchCurrentRole();
			} else {
				clearAuthentication();
				consoleAccessDenied.value = status === 403;
			}
		} catch (e) {
			console.error('Failed to fetch current user', e);
			clearAuthentication();
			consoleAccessDenied.value = false;
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
			if (status === 200 && !data.totpRequired) {
				// 正常登录 - 无二步验证
				consoleAccessDenied.value = false;
				jwtToken.value = data.accessToken;
				refreshToken.value = data.refreshToken;
				isAnonymous.value = false;
				totpRequired.value = false;
				totpTempToken.value = undefined;
			} else if (status === 200 && data.totpRequired) {
				// 需要二步验证
				consoleAccessDenied.value = false;
				totpRequired.value = true;
				totpTempToken.value = data.tempToken;
				jwtToken.value = undefined;
				refreshToken.value = undefined;
				isAnonymous.value = true;
			} else {
				jwtToken.value = undefined;
				refreshToken.value = undefined;
				isAnonymous.value = true;
				totpRequired.value = false;
			}
		} catch (e) {
			console.error('Failed to apply jwt token', e);
			isAnonymous.value = true;
			totpRequired.value = false;
		}
	}

	async function validateTotp(code: string): Promise<boolean> {
		try {
			const baseURL = import.meta.env.VITE_API_URL || '/api';
			const { data, status } = await axios.post(
				`${baseURL}/v1/security/auth/totp/validate`,
				{
					tempToken: totpTempToken.value,
					code: code,
				}
			);
			if (status === 200 && data.accessToken) {
				consoleAccessDenied.value = false;
				jwtToken.value = data.accessToken;
				refreshToken.value = data.refreshToken;
				isAnonymous.value = false;
				totpRequired.value = false;
				totpTempToken.value = undefined;
				return true;
			}
		} catch (e) {
			console.error('Failed to validate TOTP', e);
		}
		return false;
	}

	function jwtTokenLogout() {
		clearAuthentication();
		consoleAccessDenied.value = false;
	}

	function consumeConsoleAccessDenied() {
		const accessDenied = consoleAccessDenied.value;
		consoleAccessDenied.value = false;
		return accessDenied;
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
		totpTempToken,
		totpRequired,
		consoleAccessDenied,
		fetchCurrentUser,
		applyJwtToken,
		validateTotp,
		jwtTokenLogout,
		consumeConsoleAccessDenied,
		fetchCurrentRole,
		roleHasMaster,
	};
}, {
	persist: {
		key: 'ikaros-store-user',
		storage: localStorage,
	},
});
