import { createRouter, createWebHashHistory } from 'vue-router';
import routesConfig from './routes.config';
import { setupAuthCheckGuard } from './guards/auth-check';

const router = createRouter({
	history: createWebHashHistory(import.meta.env.BASE_URL),
	routes: routesConfig,
	scrollBehavior: () => ({ left: 0, top: 0 }),
});

setupAuthCheckGuard(router);

export default router;
