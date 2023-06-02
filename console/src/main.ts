import { createApp } from 'vue';
import { createPinia } from 'pinia';
import piniaPersist from 'pinia-plugin-persist';
import App from './App.vue';
import router from './router';
import i18n from './locales';
import '@/styles/reset.scss';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';

import type { PluginModule, RouteRecordAppend } from '@runikaros/shared';
import type { RouteRecordRaw } from 'vue-router';

import { coreModules } from './modules';
import { usePluginModuleStore } from '@/stores/plugin';

const pinia = createPinia();
pinia.use(piniaPersist);

const app = createApp(App);
app.use(pinia);
app.use(i18n);

function loadElementPlusIconsVue() {
	for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
		app.component(key, component);
	}
}

function registerModule(pluginModule: PluginModule, core: boolean) {
	// Register module all components.
	if (pluginModule.components) {
		Object.keys(pluginModule.components).forEach((key) => {
			const component = pluginModule.components?.[key];
			if (component) {
				app.component(key, component);
			}
		});
	}

	// Register module all routes
	if (pluginModule.routes) {
		if (!Array.isArray(pluginModule.routes)) {
			return;
		}

		resetRouteMeta(pluginModule.routes);

		for (const route of pluginModule.routes) {
			if ('parentName' in route) {
				router.addRoute(route.parentName, route.route);
			} else {
				router.addRoute(route);
			}
		}
	}

	function resetRouteMeta(routes: RouteRecordRaw[] | RouteRecordAppend[]) {
		for (const route of routes) {
			if ('parentName' in route) {
				if (route.route.meta?.menu) {
					route.route.meta = {
						...route.route.meta,
						core,
					};
				}
				if (route.route.children) {
					resetRouteMeta(route.route.children);
				}
			} else {
				if (route.meta?.menu) {
					route.meta = {
						...route.meta,
						core,
					};
				}
				if (route.children) {
					resetRouteMeta(route.children);
				}
			}
		}
	}
}

function loadCoreModules() {
	coreModules.forEach((module) => {
		registerModule(module, true);
	});
}

// eslint-disable-next-line no-unused-vars
const pluginModuleStore = usePluginModuleStore();

// eslint-disable-next-line no-unused-vars
function loadStyle(href: string) {
	return new Promise(function (resolve, reject) {
		let shouldAppend = false;
		let el: HTMLLinkElement | null = document.querySelector(
			'script[src="' + href + '"]'
		);
		if (!el) {
			el = document.createElement('link');
			el.rel = 'stylesheet';
			el.type = 'text/css';
			el.href = href;
			shouldAppend = true;
		} else if (el.hasAttribute('data-loaded')) {
			resolve(el);
			return;
		}

		el.addEventListener('error', reject);
		el.addEventListener('abort', reject);
		el.addEventListener('load', function loadStyleHandler() {
			el?.setAttribute('data-loaded', 'true');
			resolve(el);
		});

		if (shouldAppend) document.head.prepend(el);
	});
}

// const pluginErrorMessages: Array<string> = [];

// Start init app.
(async function () {
	await initApp();
})();

async function initApp() {
	try {
		loadElementPlusIconsVue();
		loadCoreModules();
	} catch (e) {
		console.log('Init app fail: ', e);
	} finally {
		app.use(router);
		app.mount('#app');
	}
}
