import type { App } from 'vue';
import { createPinia } from 'pinia';
import piniaPersist from 'pinia-plugin-persist';

const pinia = createPinia();
pinia.use(piniaPersist);

export function setupPinia(app: App) {
	app.use(pinia);
}

export default pinia;
