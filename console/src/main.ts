import { createApp } from 'vue';
import { createPinia } from 'pinia';
import piniaPersist from 'pinia-plugin-persist';
import App from './App.vue';
import router from './router';
import '@/styles/reset.scss';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';

const pinia = createPinia();
pinia.use(piniaPersist);

const app = createApp(App);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
	app.component(key, component);
}
app.use(pinia);
app.use(router);
app.mount('#app');
