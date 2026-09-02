import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import AuthEntry from './AuthEntry.vue'
import Root from './Root.vue'
import './styles.css'
import './error-state.css'
import './auth.css'

const router = createRouter({ history: createWebHistory(), routes: [{ path: '/login', component: App }, { path: '/setup', component: AuthEntry }, { path: '/login/verify', component: AuthEntry }, { path: '/recovery/:pathMatch(.*)*', component: AuthEntry }, { path: '/console/:pathMatch(.*)*', component: App }, { path: '/:pathMatch(.*)*', redirect: '/console/dashboard' }] })
createApp(Root).use(router).mount('#app')
