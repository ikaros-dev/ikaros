import { useUserStore } from '@/stores/user';
import { setApiClientJwtToken } from '@/utils/api-client';
import type { RouteLocationRaw, Router } from 'vue-router';

const whiteList = ['Setup', 'Login', 'Binding'];
const notFoundRouteName = 'router.title.notfound';

const dashboardRoute: RouteLocationRaw = { name: 'Dashboard' };

export function resolvePostLoginRoute(
	router: Router,
	redirectUri: unknown
): RouteLocationRaw {
	if (typeof redirectUri !== 'string') return dashboardRoute;

	try {
		const url = new URL(redirectUri, window.location.origin);
		if (url.origin !== window.location.origin || !url.hash.startsWith('#/')) {
			return dashboardRoute;
		}

		const target = url.hash.slice(1);
		const resolved = router.resolve(target);
		return resolved.name === notFoundRouteName ? dashboardRoute : target;
	} catch {
		return dashboardRoute;
	}
}

export function setupAuthCheckGuard(router: Router) {
	router.beforeEach((to, from, next) => {
		const userStore = useUserStore();

		// 获取jwt token，配置到请求头
		if (userStore.authType == 'EMAIL_PASSWORD' && userStore.jwtToken) {
			setApiClientJwtToken(userStore.jwtToken);
		}

		if (userStore.isAnonymous) {
			// console.log('Anonymous redirect to: ', to)
			if (whiteList.includes(to.name as string)) {
				next();
				return;
			}

			next({
				name: 'Login',
				query: {
					redirect_uri: window.location.href,
				},
			});
			return;
		} else {
			// console.log('Not anonymous redirect to: ', to)
			if (to.name === 'Login') {
				if (to.query.redirect_uri) {
					next(resolvePostLoginRoute(router, to.query.redirect_uri));
					return;
				} else {
					next({
						name: 'Dashboard',
					});
					return;
				}
			}
		}

		next();
	});
}
