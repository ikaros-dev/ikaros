import { createApp, nextTick } from 'vue';
import { createPinia, setActivePinia } from 'pinia';
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const apiMocks = vi.hoisted(() => ({
	applyJwtToken: vi.fn(),
	getRolesForUser: vi.fn(),
	getUserMe: vi.fn(),
	setApiClientJwtToken: vi.fn(),
	validateTotp: vi.fn(),
}));

vi.mock('@/utils/api-client', () => ({
	apiClient: {
		security: { applyJwtToken: apiMocks.applyJwtToken },
		userMe: { getUserMe: apiMocks.getUserMe },
		userRole: { getRolesForUser: apiMocks.getRolesForUser },
	},
	setApiClientJwtToken: apiMocks.setApiClientJwtToken,
}));

vi.mock('axios', () => ({
	default: { post: apiMocks.validateTotp },
}));

import { useUserStore } from './user';

describe('用户状态管理', () => {
	beforeEach(() => {
		localStorage.clear();
		const app = createApp({});
		const pinia = createPinia().use(piniaPluginPersistedstate);
		app.use(pinia);
		setActivePinia(pinia);
	});

	it('从原有本地存储 key 恢复用户状态', () => {
		localStorage.setItem(
			'ikaros-store-user',
			JSON.stringify({
				isAnonymous: false,
				jwtToken: 'persisted-token',
				refreshToken: 'persisted-refresh-token',
			})
		);

		const store = useUserStore();

		expect(store.isAnonymous).toBe(false);
		expect(store.jwtToken).toBe('persisted-token');
		expect(store.refreshToken).toBe('persisted-refresh-token');
	});

	it('将用户状态写入原有本地存储 key', async () => {
		const store = useUserStore();

		store.isAnonymous = false;
		store.jwtToken = 'jwt-token';
		store.refreshToken = 'refresh-token';
		await nextTick();

		expect(
			JSON.parse(localStorage.getItem('ikaros-store-user') ?? '{}')
		).toMatchObject({
			isAnonymous: false,
			jwtToken: 'jwt-token',
			refreshToken: 'refresh-token',
		});
	});

	it('获取当前用户及角色并恢复登录状态', async () => {
		apiMocks.getUserMe.mockResolvedValue({
			data: { entity: { id: 'user-id', username: 'ikaros' } },
			status: 200,
		});
		apiMocks.getRolesForUser.mockResolvedValue({
			data: [{ id: 'role-id', name: 'MASTER' }],
		});
		const store = useUserStore();
		store.jwtToken = 'jwt-token';

		await store.fetchCurrentUser();

		expect(apiMocks.setApiClientJwtToken).toHaveBeenCalledWith('jwt-token');
		expect(apiMocks.getRolesForUser).toHaveBeenCalledWith({
			userId: 'user-id',
		});
		expect(store.currentUser?.entity?.username).toBe('ikaros');
		expect(store.isAnonymous).toBe(false);
		expect(store.roleHasMaster()).toBe(true);
	});

	it('在接口拒绝访问时清除认证并记录一次性状态', async () => {
		apiMocks.getUserMe.mockResolvedValue({ data: undefined, status: 403 });
		const store = useUserStore();
		store.jwtToken = 'expired-token';
		store.isAnonymous = false;

		await store.fetchCurrentUser();

		expect(store.jwtToken).toBeUndefined();
		expect(store.isAnonymous).toBe(true);
		expect(apiMocks.setApiClientJwtToken).toHaveBeenLastCalledWith();
		expect(store.consumeConsoleAccessDenied()).toBe(true);
		expect(store.consumeConsoleAccessDenied()).toBe(false);
	});

	it('未登录响应清除认证但不记录拒绝访问', async () => {
		apiMocks.getUserMe.mockResolvedValue({ data: undefined, status: 401 });
		const store = useUserStore();

		await store.fetchCurrentUser();

		expect(store.isAnonymous).toBe(true);
		expect(store.consoleAccessDenied).toBe(false);
	});

	it('在获取用户异常时清除认证状态', async () => {
		apiMocks.getUserMe.mockRejectedValue(new Error('network error'));
		const store = useUserStore();
		store.jwtToken = 'expired-token';
		const error = vi
			.spyOn(console, 'error')
			.mockImplementation(() => undefined);

		await store.fetchCurrentUser();

		expect(error).toHaveBeenCalled();
		expect(store.jwtToken).toBeUndefined();
		expect(store.consoleAccessDenied).toBe(false);
	});

	it('保存普通登录返回的访问令牌', async () => {
		apiMocks.applyJwtToken.mockResolvedValue({
			data: {
				accessToken: 'access-token',
				refreshToken: 'refresh-token',
				totpRequired: false,
			},
			status: 200,
		});
		const store = useUserStore();

		await store.applyJwtToken('user', 'password');

		expect(apiMocks.applyJwtToken).toHaveBeenCalledWith({
			jwtApplyParam: {
				authType: 'USERNAME_PASSWORD',
				username: 'user',
				password: 'password',
			},
		});
		expect(store.jwtToken).toBe('access-token');
		expect(store.refreshToken).toBe('refresh-token');
		expect(store.isAnonymous).toBe(false);
	});

	it('保存二步验证临时令牌但保持匿名状态', async () => {
		apiMocks.applyJwtToken.mockResolvedValue({
			data: { tempToken: 'temp-token', totpRequired: true },
			status: 200,
		});
		const store = useUserStore();

		await store.applyJwtToken('user', 'password');

		expect(store.totpRequired).toBe(true);
		expect(store.totpTempToken).toBe('temp-token');
		expect(store.jwtToken).toBeUndefined();
		expect(store.isAnonymous).toBe(true);
	});

	it('登录失败时保持匿名并清除令牌', async () => {
		apiMocks.applyJwtToken.mockResolvedValue({ data: {}, status: 401 });
		const store = useUserStore();
		store.jwtToken = 'old-token';

		await store.applyJwtToken('user', 'wrong-password');

		expect(store.jwtToken).toBeUndefined();
		expect(store.refreshToken).toBeUndefined();
		expect(store.totpRequired).toBe(false);
		expect(store.isAnonymous).toBe(true);
	});

	it('登录接口异常时保持匿名状态', async () => {
		apiMocks.applyJwtToken.mockRejectedValue(new Error('network error'));
		const error = vi
			.spyOn(console, 'error')
			.mockImplementation(() => undefined);
		const store = useUserStore();

		await store.applyJwtToken('user', 'password');

		expect(error).toHaveBeenCalled();
		expect(store.isAnonymous).toBe(true);
		expect(store.totpRequired).toBe(false);
	});

	it('验证 TOTP 后完成登录', async () => {
		apiMocks.validateTotp.mockResolvedValue({
			data: { accessToken: 'access-token', refreshToken: 'refresh-token' },
			status: 200,
		});
		const store = useUserStore();
		store.totpRequired = true;
		store.totpTempToken = 'temp-token';

		await expect(store.validateTotp('123456')).resolves.toBe(true);

		expect(apiMocks.validateTotp).toHaveBeenCalledWith(
			'/api/v1/security/auth/totp/validate',
			{ tempToken: 'temp-token', code: '123456' }
		);
		expect(store.jwtToken).toBe('access-token');
		expect(store.totpRequired).toBe(false);
		expect(store.totpTempToken).toBeUndefined();
	});

	it('TOTP 响应无令牌或接口异常时返回失败', async () => {
		apiMocks.validateTotp.mockResolvedValueOnce({ data: {}, status: 200 });
		const error = vi
			.spyOn(console, 'error')
			.mockImplementation(() => undefined);
		const store = useUserStore();

		await expect(store.validateTotp('000000')).resolves.toBe(false);

		apiMocks.validateTotp.mockRejectedValueOnce(new Error('network error'));
		await expect(store.validateTotp('000000')).resolves.toBe(false);
		expect(error).toHaveBeenCalled();
	});

	it('退出时清除全部认证信息', () => {
		const store = useUserStore();
		store.jwtToken = 'jwt-token';
		store.refreshToken = 'refresh-token';
		store.currentRoles = [{ name: 'MASTER' }];
		store.isAnonymous = false;

		store.jwtTokenLogout();

		expect(store.jwtToken).toBeUndefined();
		expect(store.refreshToken).toBeUndefined();
		expect(store.currentRoles).toBeUndefined();
		expect(store.isAnonymous).toBe(true);
		expect(store.roleHasMaster()).toBeUndefined();
	});

	it('没有 MASTER 角色时返回 false', () => {
		const store = useUserStore();
		store.currentRoles = [{ name: 'USER' }];

		expect(store.roleHasMaster()).toBe(false);
	});
});
