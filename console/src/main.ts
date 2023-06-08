import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { setupI18n, i18n } from './locales';
import { setupPinia } from './stores';
import './styles/index.scss';

import type { PluginModule, RouteRecordAppend } from '@runikaros/shared';
import type { RouteRecordRaw } from 'vue-router';

import { coreModules } from './modules';
import { usePluginModuleStore } from '@/stores/plugin';
import { apiClient } from '@/utils/api-client';
import { useUserStore } from '@/stores/user';
import { ElMessage } from 'element-plus';

const app = createApp(App);
setupI18n(app);
setupPinia(app);

function registerModule(module: PluginModule, core: boolean) {
	// Register module all components.
	if (module.components) {
		Object.keys(module.components).forEach((key) => {
			const component = module.components?.[key];
			if (component) {
				app.component(key, component);
			}
		});
	}

	// Register module all routes
	if (module.routes) {
		if (!Array.isArray(module.routes)) {
			return;
		}

		resetRouteMeta(module.routes);

		for (const route of module.routes) {
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

const pluginErrorMessages: Array<string> = [];
const pluginModuleStore = usePluginModuleStore();
async function loadPluginModules() {
	const { data } = await apiClient.plugin.getpluginsbyPaging({
		page: '1',
		size: '99999',
	});
	// console.log('Load all-plugins: ', data);

	// Get all started plugins
	const plugins = data.items.filter((plugin) => {
		// @ts-ignore
		const { entry, stylesheet } = plugin || {};
		// @ts-ignore
		return plugin.state === 'STARTED' && (!!entry || !!stylesheet);
	});

	for (const plugin of plugins) {
		// @ts-ignore
		const { entry, stylesheet } = plugin || {
			entry: '',
			stylesheet: '',
		};

		if (entry) {
			try {
				const { load } = useScriptTag(
					// @ts-ignore
					`${import.meta.env.VITE_API_URL}${plugin?.entry}`
				);

				await load();

				// @ts-ignore
				const pluginModule = window[plugin.name];

				if (pluginModule) {
					registerModule(pluginModule, false);
					pluginModuleStore.registerPluginModule({
						...pluginModule,
					});
				}
			} catch (e) {
				const message = i18n.global.t(
					'core.plugin.loader.message.entry_load_failed',
					// @ts-ignore
					{ name: plugin.name }
				);
				console.error(message, e);
				pluginErrorMessages.push(message);
			}
		}

		if (stylesheet) {
			try {
				await loadStyle(`${import.meta.env.VITE_API_URL}${stylesheet}`);
			} catch (e) {
				const message = i18n.global.t(
					'core.plugin.loader.message.style_load_failed',
					// @ts-ignore
					{ name: plugin.name }
				);
				console.error(message, e);
				pluginErrorMessages.push(message);
			}
		}
	}

	if (pluginErrorMessages.length > 0) {
		pluginErrorMessages.forEach((message) => {
			ElMessage.error(message);
		});
	}
}

// Start init app.
(async function () {
	await initApp();
})();

async function initApp() {
	try {
		loadCoreModules();
		const userStore = useUserStore();
		await userStore.fetchCurrentUser();

		if (userStore.isAnonymous) {
			return;
		}

		try {
			await loadPluginModules();
		} catch (e) {
			console.error('Failed to load plugins', e);
		}
	} catch (e) {
		console.log('Init app fail: ', e);
	} finally {
		app.use(router);
		app.mount('#app');
	}
}
