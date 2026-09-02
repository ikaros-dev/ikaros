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
import ResourceDetail from './ResourceDetail.vue'
import ResourceLibrary from './ResourceLibrary.vue'
import CollectionWorkspace from './CollectionWorkspace.vue'
import DocumentWorkspace from './DocumentWorkspace.vue'
import DriveWorkspace from './DriveWorkspace.vue'
import DomainWorkspace from './DomainWorkspace.vue'
import './styles.css'
import './error-state.css'
import './auth.css'

const router = createRouter({ history: createWebHistory(), routes: [{ path: '/login', component: App }, { path: '/setup', component: AuthEntry }, { path: '/login/verify', component: AuthEntry }, { path: '/recovery/:pathMatch(.*)*', component: AuthEntry }, { path: '/console/resources', component: ResourceLibrary }, { path: '/console/resources/:id', component: ResourceDetail }, { path: '/console/collections', component: CollectionWorkspace }, { path: '/console/documents', component: DocumentWorkspace }, { path: '/console/drive', component: DriveWorkspace }, { path: '/console/drive/spaces', component: DriveWorkspace }, { path: '/console/drive/transfers', component: DriveWorkspace }, { path: '/console/drive/sync', component: DriveWorkspace }, { path: '/console/drive/conflicts', component: DriveWorkspace }, { path: '/console/drive/revisions', component: DriveWorkspace }, { path: '/console/drive/trash', component: DriveWorkspace }, { path: '/console/drive/quota', component: DriveWorkspace }, { path: '/console/drive/policies', component: DriveWorkspace }, { path: '/console/platform/parameters', component: AdminCatalog }, { path: '/console/platform/dictionaries', component: AdminCatalog }, { path: '/console/platform/menus', component: AdminCatalog }, { path: '/console/communications/audit', component: AdminCatalog }, { path: '/console/communications/templates', component: CommunicationCatalog }, { path: '/console/communications/providers', component: CommunicationCatalog }, { path: '/console/communications/login-logs', component: CommunicationCatalog }, { path: '/console/communications/security-events', component: CommunicationCatalog }, { path: '/console/ops/subsystems', component: OpsCatalog }, { path: '/console/ops/storage-health', component: OpsCatalog }, { path: '/console/ops/jobs', component: OpsCatalog }, { path: '/console/ops/plugins', component: OpsCatalog }, { path: '/console/security/authentication', component: AuthenticationPolicy }, { path: '/console/account/profile', component: AccountPage }, { path: '/console/account/preferences', component: AccountPage }, { path: '/console/:pathMatch(.*)*', component: App }, { path: '/:pathMatch(.*)*', redirect: '/console/dashboard' }] })
for (const path of ['/console/media', '/console/sharing', '/console/attachments', '/console/storage/tiers', '/console/storage/cache', '/console/storage/archive', '/console/storage/backup', '/console/planning/today', '/console/planning/projects', '/console/planning/calendar', '/console/planning/goals', '/console/planning/focus', '/console/finance', '/console/finance/accounts', '/console/finance/transactions', '/console/finance/budgets', '/console/finance/reconcile', '/console/private-notes', '/console/private-notes/conflicts', '/console/private-notes/recovery', '/console/passwords', '/console/passwords/generator', '/console/passwords/health', '/console/passwords/devices', '/console/ai/assistant', '/console/ai/models', '/console/ai/personas', '/console/ai/privacy', '/console/ai/jobs', '/console/analytics', '/console/analytics/content', '/console/analytics/storage', '/console/analytics/planning', '/console/analytics/system', '/console/analytics/metrics', '/console/analytics/reports', '/console/integration/automation', '/console/integration/runs', '/console/integration/import', '/console/integration/plugins']) router.addRoute({ path, component: DomainWorkspace })
createApp(Root).use(router).mount('#app')
