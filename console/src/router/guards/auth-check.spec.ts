import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { NavigationGuardNext, Router } from 'vue-router';

const guardMocks = vi.hoisted(() => ({
	setApiClientJwtToken: vi.fn(),
	useUserStore: vi.fn(),
	resolve: vi.fn(),
}));

vi.mock('@/stores/user', () => ({ useUserStore: guardMocks.useUserStore }));
vi.mock('@/utils/api-client', () => ({
	setApiClientJwtToken: guardMocks.setApiClientJwtToken,
}));

import { resolvePostLoginRoute, setupAuthCheckGuard } from './auth-check';

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
		resolve: guardMocks.resolve,
	};
	setupAuthCheckGuard(router as unknown as Router);
	return guard as Exclude<Guard, undefined>;
};

describe('身份认证路由守卫', () => {
	beforeEach(() => {
		window.history.replaceState({}, '', '/console/#/subjects');
		guardMocks.resolve.mockImplementation((target: string) => ({
			name: target.startsWith('/attachments')
				? 'router.title.notfound'
				: 'ResolvedRoute',
		}));
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

	it('匿名用户访问不存在的路由时仍跳转登录页', () => {
		guardMocks.useUserStore.mockReturnValue({ isAnonymous: true });
		const guard = installGuard();
		const next = vi.fn();

		guard({ name: 'router.title.notfound' } as never, {} as never, next);

		expect(next).toHaveBeenCalledWith({
			name: 'Login',
			query: { redirect_uri: window.location.href },
		});
	});

	it('已登录用户访问登录页时恢复合法的控制台目标', () => {
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

		expect(guardMocks.resolve).toHaveBeenCalledWith('/subjects');
		expect(next).toHaveBeenCalledWith('/subjects');
	});

	it('已失效的登录恢复地址回退到仪表板', () => {
		const router = { resolve: guardMocks.resolve } as unknown as Router;
		const redirectUri = `${window.location.origin}/console/#/attachments?name=&parentId=019b715b-08c7-7509-ab14-2abe47f440f3`;

		expect(resolvePostLoginRoute(router, redirectUri)).toEqual({
			name: 'Dashboard',
		});
		expect(guardMocks.resolve).toHaveBeenCalledWith(
			'/attachments?name=&parentId=019b715b-08c7-7509-ab14-2abe47f440f3'
		);
	});

	it('非同源或非控制台 Hash 地址回退到仪表板', () => {
		const router = { resolve: guardMocks.resolve } as unknown as Router;

		expect(
			resolvePostLoginRoute(router, 'https://example.com/#/subjects')
		).toEqual({
			name: 'Dashboard',
		});
		expect(resolvePostLoginRoute(router, '/console/subjects')).toEqual({
			name: 'Dashboard',
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
