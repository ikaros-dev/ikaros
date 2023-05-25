import { createRouter, createWebHashHistory } from 'vue-router';
// eslint-disable-next-line import/no-unresolved
import routesConfig from './routes.config';

const router = createRouter({
	history: createWebHashHistory(import.meta.env.BASE_URL),
	routes: routesConfig,
	scrollBehavior: () => ({ left: 0, top: 0 }),
});

export default router;
