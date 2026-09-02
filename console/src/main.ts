import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import AuthEntry from './AuthEntry.vue'
import Root from './Root.vue'
import AdminCatalog from './AdminCatalog.vue'
import AccountPage from './AccountPage.vue'
import AccountSecurity from './AccountSecurity.vue'
import AccountNotifications from './AccountNotifications.vue'
import OpsCatalog from './OpsCatalog.vue'
import CommunicationCatalog from './CommunicationCatalog.vue'
import AuthenticationPolicy from './AuthenticationPolicy.vue'
import ResourceDetail from './ResourceDetail.vue'
import ResourceLibrary from './ResourceLibrary.vue'
import CollectionWorkspace from './CollectionWorkspace.vue'
import DocumentWorkspace from './DocumentWorkspace.vue'
import DriveWorkspace from './DriveWorkspace.vue'
import DriveHome from './DriveHome.vue'
import DomainWorkspace from './DomainWorkspace.vue'
import PlanningWorkspace from './PlanningWorkspace.vue'
import GoalWorkspace from './GoalWorkspace.vue'
import CalendarWorkspace from './CalendarWorkspace.vue'
import AccessDenied from './AccessDenied.vue'
import { getRouteMeta } from './config/route-meta'
import './styles.css'
import './error-state.css'
import './auth.css'

const routes = [
  { path: '/login', component: App }, { path: '/setup', component: AuthEntry }, { path: '/login/verify', component: AuthEntry }, { path: '/recovery/:pathMatch(.*)*', component: AuthEntry },
  { path: '/console/resources', component: ResourceLibrary }, { path: '/console/resources/:id', component: ResourceDetail }, { path: '/console/collections', component: CollectionWorkspace }, { path: '/console/documents', component: DocumentWorkspace },
  { path: '/console/drive', component: DriveHome }, { path: '/console/drive/spaces', component: DriveWorkspace }, { path: '/console/drive/spaces/:spaceId', component: DriveWorkspace }, { path: '/console/drive/nodes/:nodeId', component: DriveWorkspace }, { path: '/console/drive/transfers', component: DriveWorkspace }, { path: '/console/drive/sync', component: DriveWorkspace }, { path: '/console/drive/conflicts', component: DriveWorkspace }, { path: '/console/drive/revisions', component: DriveWorkspace }, { path: '/console/drive/trash', component: DriveWorkspace }, { path: '/console/drive/quota', component: DriveWorkspace }, { path: '/console/drive/policies', component: DriveWorkspace },
  { path: '/console/platform/parameters', component: AdminCatalog }, { path: '/console/platform/dictionaries', component: AdminCatalog }, { path: '/console/platform/menus', component: AdminCatalog }, { path: '/console/communications/audit', component: AdminCatalog }, { path: '/console/communications/templates', component: CommunicationCatalog }, { path: '/console/communications/providers', component: CommunicationCatalog }, { path: '/console/communications/login-logs', component: CommunicationCatalog }, { path: '/console/communications/security-events', component: CommunicationCatalog },
  { path: '/console/ops/subsystems', component: OpsCatalog }, { path: '/console/ops/storage-health', component: OpsCatalog }, { path: '/console/ops/jobs', component: OpsCatalog }, { path: '/console/ops/plugins', component: OpsCatalog }, { path: '/console/security/authentication', component: AuthenticationPolicy }, { path: '/console/account/profile', component: AccountPage }, { path: '/console/account/preferences', component: AccountPage }, { path: '/console/account/security', component: AccountSecurity }, { path: '/console/account/notifications', component: AccountNotifications }, { path: '/console/403', component: AccessDenied },
  { path: '/console/:pathMatch(.*)*', component: App }, { path: '/:pathMatch(.*)*', redirect: '/console/dashboard' }
]
const router = createRouter({ history: createWebHistory(), routes })
for (const path of ['/console/media', '/console/sharing', '/console/attachments', '/console/storage/tiers', '/console/storage/cache', '/console/storage/archive', '/console/storage/backup', '/console/planning/today', '/console/planning/projects', '/console/planning/calendar', '/console/planning/goals', '/console/planning/focus', '/console/finance', '/console/finance/accounts', '/console/finance/transactions', '/console/finance/budgets', '/console/finance/reconcile', '/console/private-notes', '/console/private-notes/conflicts', '/console/private-notes/recovery', '/console/passwords', '/console/passwords/generator', '/console/passwords/health', '/console/passwords/devices', '/console/ai/assistant', '/console/ai/models', '/console/ai/personas', '/console/ai/privacy', '/console/ai/jobs', '/console/analytics', '/console/analytics/content', '/console/analytics/storage', '/console/analytics/planning', '/console/analytics/system', '/console/analytics/metrics', '/console/analytics/reports', '/console/integration/automation', '/console/integration/executions', '/console/integration/events', '/console/integration/sync', '/console/integration/plugins', '/console/communications/announcements', '/console/communications/notifications', '/console/ops/health', '/console/ops/background']) router.addRoute({ path, component: DomainWorkspace })
router.addRoute({ path: '/console/planning/projects', component: PlanningWorkspace })
router.addRoute({ path: '/console/planning/goals', component: GoalWorkspace })
router.addRoute({ path: '/console/planning/calendar', component: CalendarWorkspace })
router.beforeEach((to) => {
  if (!to.path.startsWith('/console/') || to.path === '/console/403') return true
  let capabilities: string[] = []
  try { const stored = JSON.parse(localStorage.getItem('ikaros-console-capabilities') || '[]'); capabilities = Array.isArray(stored) ? stored : [] } catch { capabilities = [] }
  if (!capabilities.length) return true
  const routeKey = to.path.replace(/^\/console\//, '').replace(/^drive\/spaces\/[^/]+$/, 'drive/spaces').replace(/^drive\/nodes\/[^/]+$/, 'drive/nodes')
  const required = getRouteMeta(routeKey).requiredCapability
  return capabilities.includes('*') || capabilities.includes(required) ? true : { path: '/console/403', query: { capability: required, from: to.fullPath } }
})
createApp(Root).use(router).mount('#app')
