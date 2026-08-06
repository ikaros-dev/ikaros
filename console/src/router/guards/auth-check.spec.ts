import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { NavigationGuardNext, Router } from 'vue-router';

const guardMocks = vi.hoisted(() => ({
	setApiClientJwtToken: vi.fn(),
	useUserStore: vi.fn(),
}));

vi.mock('@/stores/user', () => ({ useUserStore: guardMocks.useUserStore }));
vi.mock('@/utils/api-client', () => ({
	setApiClientJwtToken: guardMocks.setApiClientJwtToken,
}));

import { setupAuthCheckGuard } from './auth-check';

type Guard = (
	to: Parameters<NavigationGuardNext>[0],
	from: Parameters<NavigationGuardNext>[0],
	next: NavigationGuardNext
) => unknown;

const installGuard = () => {
	let guard: Guard | undefined;
	const router = {
		beforeEach: vi.fn((registeredGuard: Guard) => {
			guard = registeredGuard;
		}),
	};
	setupAuthCheckGuard(router as unknown as Router);
	return guard as Exclude<Guard, undefined>;
};

describe('身份认证路由守卫', () => {
	beforeEach(() => {
		window.history.replaceState({}, '', '/console/#/subjects');
	});

	it('匿名用户可以访问白名单路由', () => {
		guardMocks.useUserStore.mockReturnValue({ isAnonymous: true });
		const guard = installGuard();
		const next = vi.fn();

		guard({ name: 'Login' } as never, {} as never, next);

		expect(next).toHaveBeenCalledWith();
	});

	it('匿名用户访问受限路由时跳转登录页', () => {
		guardMocks.useUserStore.mockReturnValue({ isAnonymous: true });
		const guard = installGuard();
		const next = vi.fn();

		guard({ name: 'Dashboard' } as never, {} as never, next);

		expect(next).toHaveBeenCalledWith({
			name: 'Login',
			query: { redirect_uri: window.location.href },
		});
	});

	it('已登录用户访问登录页时跳转原目标', () => {
		guardMocks.useUserStore.mockReturnValue({ isAnonymous: false });
		const guard = installGuard();
		const next = vi.fn();

		guard(
			{
				name: 'Login',
				query: { redirect_uri: '/console/#/subjects' },
			} as never,
			{} as never,
			next
		);

		expect(next).toHaveBeenCalledWith({
			name: 'Redirect',
			query: { redirect_uri: '/console/#/subjects' },
		});
	});

	it('已登录用户直接访问登录页时跳转仪表盘', () => {
		guardMocks.useUserStore.mockReturnValue({ isAnonymous: false });
		const guard = installGuard();
		const next = vi.fn();

		guard({ name: 'Login', query: {} } as never, {} as never, next);

		expect(next).toHaveBeenCalledWith({ name: 'Dashboard' });
	});

	it('邮箱密码认证用户进入普通路由时注入 JWT', () => {
		guardMocks.useUserStore.mockReturnValue({
			authType: 'EMAIL_PASSWORD',
			isAnonymous: false,
			jwtToken: 'jwt-token',
		});
		const guard = installGuard();
		const next = vi.fn();

		guard({ name: 'Dashboard' } as never, {} as never, next);

		expect(guardMocks.setApiClientJwtToken).toHaveBeenCalledWith('jwt-token');
		expect(next).toHaveBeenCalledWith();
	});
});
