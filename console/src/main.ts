import { createApp } from 'vue'
import App from './App.vue'
import { createPinia } from "pinia";
// setup
import { setupI18n } from "./locales";


const app = createApp(App);
setupI18n(app);

app.use(createPinia());

app.mount('#app')
