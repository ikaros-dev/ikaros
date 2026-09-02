import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import AuthEntry from './AuthEntry.vue'
import Root from './Root.vue'
import AdminCatalog from './AdminCatalog.vue'
import AccountPage from './AccountPage.vue'
import OpsCatalog from './OpsCatalog.vue'
import CommunicationCatalog from './CommunicationCatalog.vue'
import AuthenticationPolicy from './AuthenticationPolicy.vue'
import './styles.css'
import './error-state.css'
import './auth.css'

const router = createRouter({ history: createWebHistory(), routes: [{ path: '/login', component: App }, { path: '/setup', component: AuthEntry }, { path: '/login/verify', component: AuthEntry }, { path: '/recovery/:pathMatch(.*)*', component: AuthEntry }, { path: '/console/platform/parameters', component: AdminCatalog }, { path: '/console/platform/dictionaries', component: AdminCatalog }, { path: '/console/platform/menus', component: AdminCatalog }, { path: '/console/communications/audit', component: AdminCatalog }, { path: '/console/communications/templates', component: CommunicationCatalog }, { path: '/console/communications/providers', component: CommunicationCatalog }, { path: '/console/communications/login-logs', component: CommunicationCatalog }, { path: '/console/communications/security-events', component: CommunicationCatalog }, { path: '/console/ops/subsystems', component: OpsCatalog }, { path: '/console/ops/storage-health', component: OpsCatalog }, { path: '/console/ops/jobs', component: OpsCatalog }, { path: '/console/ops/plugins', component: OpsCatalog }, { path: '/console/security/authentication', component: AuthenticationPolicy }, { path: '/console/account/profile', component: AccountPage }, { path: '/console/account/preferences', component: AccountPage }, { path: '/console/:pathMatch(.*)*', component: App }, { path: '/:pathMatch(.*)*', redirect: '/console/dashboard' }] })
createApp(Root).use(router).mount('#app')
