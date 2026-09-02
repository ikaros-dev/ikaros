import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import './styles.css'
import './error-state.css'
import './auth.css'

const router = createRouter({ history: createWebHistory(), routes: [{ path: '/login', component: App }, { path: '/console/:pathMatch(.*)*', component: App }, { path: '/:pathMatch(.*)*', redirect: '/console/dashboard' }] })
createApp(App).use(router).mount('#app')
