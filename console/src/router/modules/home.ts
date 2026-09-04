const Layout = () => import("@/layout/index.vue");
const ModulePage = () => import("@/views/modules/ModulePage.vue");
const DashboardPage = () => import("@/views/dashboard/index.vue");
const ResourcesPage = () => import("@/views/resources/index.vue");
const CollectionsPage = () => import("@/views/collections/index.vue");
const DocumentsPage = () => import("@/views/documents/index.vue");
const PlanningTodayPage = () => import("@/views/planning/Today.vue");
const PlanningProjectsPage = () => import("@/views/planning/Projects.vue");
const PlanningCalendarPage = () => import("@/views/planning/Calendar.vue");
const PlanningGoalsPage = () => import("@/views/planning/Goals.vue");
const PlanningFocusPage = () => import("@/views/planning/Focus.vue");
const FinancePage = () => import("@/views/finance/index.vue");
const FinanceAccountsPage = () => import("@/views/finance/Accounts.vue");
const FinanceTransactionsPage = () => import("@/views/finance/Transactions.vue");
const FinanceBudgetsPage = () => import("@/views/finance/Budgets.vue");
const FinanceReconcilePage = () => import("@/views/finance/Reconcile.vue");
const PrivateNotesPage = () => import("@/views/notes/index.vue");
const AccountProfilePage = () => import("@/views/account/Profile.vue");
const AccountPreferencesPage = () => import("@/views/account/Preferences.vue");
const AiAssistantPage = () => import("@/views/ai/Assistant.vue");
const AiModelsPage = () => import("@/views/ai/Models.vue");
const AiPersonasPage = () => import("@/views/ai/Personas.vue");
const AiPrivacyPage = () => import("@/views/ai/Privacy.vue");
const AiJobsPage = () => import("@/views/ai/Jobs.vue");
const AnnouncementsPage = () => import("@/views/communications/Announcements.vue");
const NotificationsPage = () => import("@/views/communications/Notifications.vue");
const SecurityUsersPage = () => import("@/views/security/Users.vue");
const SecurityPermissionsPage = () => import("@/views/security/Permissions.vue");
const SecuritySessionsPage = () => import("@/views/security/Sessions.vue");
const SecurityAuthenticationPage = () => import("@/views/security/Authentication.vue");
const OpsHealthPage = () => import("@/views/operations/Health.vue");
const OpsJobsPage = () => import("@/views/operations/Jobs.vue");
const OpsBackgroundPage = () => import("@/views/operations/Background.vue");
const PlatformParametersPage = () => import("@/views/platform/Parameters.vue");
const PlatformDictionariesPage = () => import("@/views/platform/Dictionaries.vue");
const PlatformMenusPage = () => import("@/views/platform/Menus.vue");

type ModuleOptions = {
  path: string;
  name: string;
  title: string;
  description: string;
  endpoint: string;
  icon: string;
  columns: string[];
  createEndpoint?: string;
  createFields?: Array<{ name: string; label: string; required?: boolean; defaultValue?: string }>;
  deleteEndpoint?: string;
  actions?: Array<{ name: string; label: string; path: string; method?: string; confirm?: string }>;
};

function moduleRoute(options: ModuleOptions) {
  const { path, name, title, description, endpoint, icon, columns, ...operations } = options;
  return {
    path,
    name,
    component: ModulePage,
    meta: { title, description, endpoint, columns, icon, ...operations }
  };
}

function subsystem(path: string, name: string, title: string, icon: string, children: any[]) {
  return { path, name, meta: { title, icon, showParent: true }, children };
}

export default {
  path: "/",
  name: "IkarosConsole",
  component: Layout,
  redirect: "/dashboard",
  meta: { title: "Ikaros 管理控制台", icon: "ep:menu" },
  children: [
    { path: "/dashboard", name: "Dashboard", component: DashboardPage, meta: { title: "仪表盘", icon: "ep:data-analysis" } },
    { path: "/console/search", name: "WorkbenchSearch", component: () => import("@/views/workbench/Search.vue"), meta: { title: "全局搜索", showLink: false } },
    { path: "/console/dashboard", name: "DashboardSpecRoute", component: DashboardPage, meta: { title: "仪表盘", showLink: false } },
    { path: "/console/resources", name: "ResourcesSpecRoute", component: ResourcesPage, meta: { title: "资源管理", showLink: false } },
    { path: "/console/resources/:resourceId", name: "ResourceDetailSpecRoute", component: () => import("@/views/resources/Detail.vue"), meta: { title: "资源详情", showLink: false } },
    { path: "/console/collections", name: "CollectionsSpecRoute", component: CollectionsPage, meta: { title: "收藏集合", showLink: false } },
    { path: "/console/documents", name: "DocumentsSpecRoute", component: DocumentsPage, meta: { title: "文档管理", showLink: false } },
    { path: "/console/media", name: "MediaSpecRoute", component: () => import("@/views/media/index.vue"), meta: { title: "媒体库", showLink: false } },
    { path: "/console/sharing", name: "SharingSpecRoute", component: () => import("@/views/sharing/index.vue"), meta: { title: "分享协作", showLink: false } },
    { path: "/console/drive", name: "DriveSpecRoute", component: () => import("@/views/drive/index.vue"), meta: { title: "个人云盘", showLink: false } },
    { path: "/console/drive/trash", name: "DriveTrashSpecRoute", component: () => import("@/views/drive/Operations.vue"), meta: { title: "云盘回收站", showLink: false } },
    { path: "/console/drive/transfers", name: "DriveTransfersSpecRoute", component: () => import("@/views/drive/Operations.vue"), meta: { title: "传输任务", showLink: false } },
    { path: "/console/drive/sync", name: "DriveSyncSpecRoute", component: () => import("@/views/drive/Operations.vue"), meta: { title: "云盘同步", showLink: false } },
    { path: "/console/drive/conflicts", name: "DriveConflictsSpecRoute", component: () => import("@/views/drive/Operations.vue"), meta: { title: "同步冲突", showLink: false } },
    { path: "/console/drive/quota", name: "DriveQuotaSpecRoute", component: () => import("@/views/drive/Operations.vue"), meta: { title: "云盘配额", showLink: false } },
    { path: "/console/drive/policies", name: "DrivePoliciesSpecRoute", component: () => import("@/views/drive/Operations.vue"), meta: { title: "云盘策略", showLink: false } },
    { path: "/console/drive/spaces", name: "DriveSpacesSpecRoute", component: () => import("@/views/drive/index.vue"), meta: { title: "云盘空间", showLink: false } },
    { path: "/console/drive/revisions", name: "DriveRevisionsSpecRoute", component: () => import("@/views/drive/Operations.vue"), meta: { title: "文件版本", showLink: false } },
    subsystem("/ai-center", "AiCenter", "AI 智能", "ep:magic-stick", [
      { path: "/assistant", name: "AiAssistant", component: AiAssistantPage, meta: { title: "AI 助手", description: "与 AI 助手协作处理内容和任务。", icon: "ep:chat-dot-round" } },
      { path: "/models", name: "AiModels", component: AiModelsPage, meta: { title: "AI 模型", description: "查看可用模型和能力配置。", icon: "ep:cpu" } },
      { path: "/personas", name: "AiPersonas", component: AiPersonasPage, meta: { title: "AI 人格", description: "管理助手人格和提示配置。", icon: "ep:user" } },
      { path: "/privacy", name: "AiPrivacy", component: AiPrivacyPage, meta: { title: "AI 隐私", description: "管理 AI 数据使用和隐私边界。", icon: "ep:lock" } },
      { path: "/jobs", name: "AiJobs", component: AiJobsPage, meta: { title: "AI 任务", description: "查看 AI 后台任务及执行状态。", icon: "ep:operation" } }
    ]),
    subsystem("/operations-center", "OperationsCenter", "系统运维", "ep:monitor", [
      { path: "/health", name: "OpsHealth", component: OpsHealthPage, meta: { title: "系统健康", description: "查看服务、依赖和存储健康状态。", icon: "ep:monitor" } },
      { path: "/jobs", name: "OpsJobs", component: OpsJobsPage, meta: { title: "运维任务", description: "查看运维任务及执行结果。", icon: "ep:operation" } },
      { path: "/background", name: "OpsBackground", component: OpsBackgroundPage, meta: { title: "后台任务", description: "查看后台操作进度。", icon: "ep:loading" } }
    ]),
    subsystem("/security-center", "SecurityCenter", "身份安全", "ep:lock", [
      { path: "/users", name: "SecurityUsers", component: SecurityUsersPage, meta: { title: "安全用户", description: "管理用户、账号状态和安全策略。", icon: "ep:user" } },
      { path: "/permissions", name: "SecurityPermissions", component: SecurityPermissionsPage, meta: { title: "权限", description: "管理权限注册表和授权能力。", icon: "ep:lock" } },
      { path: "/sessions", name: "SecuritySessions", component: SecuritySessionsPage, meta: { title: "会话", description: "查看活动会话和登录设备。", icon: "ep:connection" } },
      { path: "/authentication", name: "SecurityAuthentication", component: SecurityAuthenticationPage, meta: { title: "认证设置", description: "管理认证方式和验证挑战。", icon: "ep:key" } }
    ]),
    subsystem("/integration-center", "IntegrationCenter", "集成自动化", "ep:connection", [
      moduleRoute({ path: "/automation", name: "IntegrationAutomation", title: "自动化", description: "管理自动化规则和触发器。", endpoint: "/integration/automation", columns: ["id", "name", "status", "updatedAt"], icon: "ep:connection" }),
      moduleRoute({ path: "/executions", name: "IntegrationExecutions", title: "自动化执行", description: "查看自动化执行记录。", endpoint: "/integration/executions", columns: ["id", "automationId", "status", "createdAt"], icon: "ep:operation" }),
      moduleRoute({ path: "/events", name: "IntegrationEvents", title: "集成事件", description: "查看外部集成事件和投递状态。", endpoint: "/integration/events", columns: ["id", "type", "status", "createdAt"], icon: "ep:bell" }),
      moduleRoute({ path: "/sync", name: "IntegrationSync", title: "同步", description: "管理外部数据同步任务。", endpoint: "/integration/sync", columns: ["id", "source", "status", "updatedAt"], icon: "ep:refresh" }),
      moduleRoute({ path: "/plugins", name: "IntegrationPlugins", title: "插件", description: "管理集成插件及其权限。", endpoint: "/integration/plugins", columns: ["id", "name", "status", "version"], icon: "ep:cpu" })
    ]),
    subsystem("/communications-center", "CommunicationsCenter", "沟通与审计", "ep:notification", [
      { path: "/announcements", name: "Announcements", component: AnnouncementsPage, meta: { title: "公告", description: "发布和管理系统公告。", icon: "ep:notification" } },
      { path: "/notifications", name: "Notifications", component: NotificationsPage, meta: { title: "通知", description: "查看通知投递和用户触达状态。", icon: "ep:bell" } },
      moduleRoute({ path: "/audit", name: "CommunicationsAudit", title: "沟通审计", description: "审计公告和通知操作记录。", endpoint: "/communications/audit", columns: ["id", "action", "actorId", "createdAt"], icon: "ep:document" })
    ]),
    subsystem("/analytics-center", "AnalyticsCenter", "数据分析", "ep:data-analysis", [
      moduleRoute({ path: "/overview", name: "AnalyticsOverview", title: "分析总览", description: "查看跨模块运营指标。", endpoint: "/analytics/overview", columns: ["metric", "value", "period", "updatedAt"], icon: "ep:data-analysis" }),
      moduleRoute({ path: "/content", name: "AnalyticsContent", title: "内容分析", description: "分析内容规模和使用情况。", endpoint: "/analytics/content", columns: ["metric", "value", "period"], icon: "ep:document" }),
      moduleRoute({ path: "/storage", name: "AnalyticsStorage", title: "存储分析", description: "分析 Blob、附件和存储层使用情况。", endpoint: "/analytics/storage", columns: ["metric", "value", "tier", "period"], icon: "ep:box" }),
      moduleRoute({ path: "/planning", name: "AnalyticsPlanning", title: "计划分析", description: "分析任务、项目和目标进度。", endpoint: "/analytics/planning", columns: ["metric", "value", "period"], icon: "ep:calendar" }),
      moduleRoute({ path: "/system", name: "AnalyticsSystem", title: "系统分析", description: "分析系统健康和运行指标。", endpoint: "/analytics/system", columns: ["metric", "value", "status", "updatedAt"], icon: "ep:monitor" }),
      moduleRoute({ path: "/metrics", name: "AnalyticsMetrics", title: "指标", description: "浏览系统注册指标。", endpoint: "/analytics/metrics", columns: ["name", "value", "unit", "updatedAt"], icon: "ep:trend-charts" }),
      moduleRoute({ path: "/reports", name: "AnalyticsReports", title: "分析报告", description: "管理分析报告和导出任务。", endpoint: "/analytics/reports", columns: ["id", "name", "status", "createdAt"], icon: "ep:document" })
    ]),
    subsystem("/configuration-center", "ConfigurationCenter", "平台配置", "ep:setting", [
      { path: "/parameters", name: "PlatformParameters", component: PlatformParametersPage, meta: { title: "平台参数", description: "管理平台运行参数。", icon: "ep:setting" } },
      { path: "/dictionaries", name: "PlatformDictionaries", component: PlatformDictionariesPage, meta: { title: "数据字典", description: "管理平台字典项。", icon: "ep:list" } },
      { path: "/menus", name: "PlatformMenus", component: PlatformMenusPage, meta: { title: "平台菜单", description: "管理菜单和导航结构。", icon: "ep:menu" } }
    ]),
    subsystem("/password-center", "PasswordCenter", "密码管理", "ep:key", [
      moduleRoute({ path: "/vault", name: "Passwords", title: "密码库", description: "管理密码条目和安全状态。", endpoint: "/passwords", columns: ["id", "title", "username", "updatedAt"], icon: "ep:lock" }),
      moduleRoute({ path: "/generator", name: "PasswordGenerator", title: "密码生成器", description: "生成符合策略的随机密码。", endpoint: "/passwords/generator", columns: ["id", "length", "strength", "createdAt"], icon: "ep:magic-stick" }),
      moduleRoute({ path: "/health", name: "PasswordHealth", title: "密码健康", description: "检查重复、弱密码和泄露风险。", endpoint: "/passwords/health", columns: ["id", "title", "risk", "updatedAt"], icon: "ep:warning" }),
      moduleRoute({ path: "/devices", name: "PasswordDevices", title: "密码设备", description: "管理密码库授权设备。", endpoint: "/passwords/devices", columns: ["id", "name", "status", "lastSeenAt"], icon: "ep:mobile-phone" })
    ]),
    subsystem("/notes-center", "NotesCenter", "私密笔记", "ep:lock", [
      { path: "/notes", name: "PrivateNotes", component: PrivateNotesPage, meta: { title: "私密笔记", description: "在加密边界内管理个人笔记。", icon: "ep:document" } },
      moduleRoute({ path: "/conflicts", name: "PrivateNotesConflicts", title: "笔记冲突", description: "处理同步冲突并保留版本历史。", endpoint: "/private-notes/conflicts", columns: ["id", "noteId", "status", "createdAt"], icon: "ep:warning" }),
      moduleRoute({ path: "/recovery", name: "PrivateNotesRecovery", title: "笔记恢复", description: "查看恢复状态和安全恢复操作。", endpoint: "/private-notes/recovery", columns: ["id", "status", "createdAt", "updatedAt"], icon: "ep:refresh" })
    ]),
    subsystem("/account-center", "AccountCenter", "个人中心", "ep:user", [
      { path: "/profile", name: "AccountProfile", component: AccountProfilePage, meta: { title: "个人资料", description: "查看和维护个人资料。", icon: "ep:user" } },
      { path: "/preferences", name: "AccountPreferences", component: AccountPreferencesPage, meta: { title: "偏好设置", description: "管理 Console 显示和交互偏好。", icon: "ep:setting" } },
      moduleRoute({ path: "/notifications", name: "AccountNotifications", title: "通知设置", description: "管理通知渠道和订阅偏好。", endpoint: "/me/notifications", columns: ["channel", "enabled", "updatedAt"], icon: "ep:bell" }),
      moduleRoute({ path: "/security", name: "AccountSecurity", title: "账户安全", description: "管理认证方式和安全状态。", endpoint: "/security/verification-challenges", columns: ["id", "status", "createdAt", "expiresAt"], icon: "ep:lock" })
    ]),
    { path: "/console/attachments", name: "AttachmentsSpecRoute", component: () => import("@/views/attachments/index.vue"), meta: { title: "附件与 Blob", showLink: false } },
    { path: "/console/storage/tiers", name: "StorageTiersSpecRoute", component: () => import("@/views/storage/Tiers.vue"), meta: { title: "持久化存储层", showLink: false } },
    { path: "/console/storage/archive", name: "StorageArchiveSpecRoute", component: () => import("@/views/storage/Archive.vue"), meta: { title: "归档与恢复", showLink: false } },
    { path: "/console/storage/cache", name: "StorageCacheSpecRoute", component: () => import("@/views/storage/Cache.vue"), meta: { title: "缓存与我的下载", showLink: false } },
    { path: "/console/storage/backup", name: "StorageBackupSpecRoute", component: () => import("@/views/storage/Backup.vue"), meta: { title: "备份与恢复", showLink: false } },
    { path: "/console/planning/today", name: "PlanningTodaySpecRoute", component: PlanningTodayPage, meta: { title: "今日计划", showLink: false } },
    { path: "/console/planning/projects", name: "PlanningProjectsSpecRoute", component: PlanningProjectsPage, meta: { title: "项目管理", showLink: false } },
    { path: "/console/planning/calendar", name: "PlanningCalendarSpecRoute", component: PlanningCalendarPage, meta: { title: "日历", showLink: false } },
    { path: "/console/planning/goals", name: "PlanningGoalsSpecRoute", component: PlanningGoalsPage, meta: { title: "目标", showLink: false } },
    { path: "/console/planning/focus", name: "PlanningFocusSpecRoute", component: PlanningFocusPage, meta: { title: "专注", showLink: false } },
    { path: "/console/finance", name: "FinanceSpecRoute", component: FinancePage, meta: { title: "个人记账", showLink: false } },
    { path: "/console/finance/accounts", name: "FinanceAccountsSpecRoute", component: FinanceAccountsPage, meta: { title: "账户", showLink: false } },
    { path: "/console/finance/transactions", name: "FinanceTransactionsSpecRoute", component: FinanceTransactionsPage, meta: { title: "交易记录", showLink: false } },
    { path: "/console/finance/budgets", name: "FinanceBudgetsSpecRoute", component: FinanceBudgetsPage, meta: { title: "预算", showLink: false } },
    { path: "/console/finance/reconcile", name: "FinanceReconcileSpecRoute", component: FinanceReconcilePage, meta: { title: "对账", showLink: false } },
    { path: "/console/private-notes", name: "PrivateNotesSpecRoute", component: PrivateNotesPage, meta: { title: "私密笔记", showLink: false } },
    { path: "/console/private-notes/conflicts", name: "PrivateNotesConflictsSpecRoute", component: () => import("@/views/notes/Security.vue"), meta: { title: "笔记冲突", showLink: false } },
    { path: "/console/private-notes/recovery", name: "PrivateNotesRecoverySpecRoute", component: () => import("@/views/notes/Security.vue"), meta: { title: "笔记恢复", showLink: false } },
    { path: "/console/account/profile", name: "AccountProfileSpecRoute", component: AccountProfilePage, meta: { title: "个人资料", showLink: false } },
    { path: "/console/account/preferences", name: "AccountPreferencesSpecRoute", component: AccountPreferencesPage, meta: { title: "偏好设置", showLink: false } },
    { path: "/console/account/notifications", name: "AccountNotificationsSpecRoute", component: () => import("@/views/communications/Notifications.vue"), meta: { title: "通知设置", showLink: false } },
    { path: "/console/account/security", name: "AccountSecuritySpecRoute", component: () => import("@/views/security/Authentication.vue"), meta: { title: "账户安全", showLink: false } },
    { path: "/console/ai/assistant", name: "AiAssistantSpecRoute", component: AiAssistantPage, meta: { title: "AI 助手", showLink: false } },
    { path: "/console/ai/models", name: "AiModelsSpecRoute", component: AiModelsPage, meta: { title: "AI 模型", showLink: false } },
    { path: "/console/ai/personas", name: "AiPersonasSpecRoute", component: AiPersonasPage, meta: { title: "AI 人格", showLink: false } },
    { path: "/console/ai/privacy", name: "AiPrivacySpecRoute", component: AiPrivacyPage, meta: { title: "AI 隐私", showLink: false } },
    { path: "/console/ai/jobs", name: "AiJobsSpecRoute", component: AiJobsPage, meta: { title: "AI 任务", showLink: false } },
    { path: "/console/communications/announcements", name: "AnnouncementsSpecRoute", component: AnnouncementsPage, meta: { title: "公告", showLink: false } },
    { path: "/console/communications/notifications", name: "NotificationsSpecRoute", component: NotificationsPage, meta: { title: "通知", showLink: false } },
    { path: "/console/communications/audit", name: "CommunicationsAuditSpecRoute", component: ModulePage, meta: { title: "沟通审计", showLink: false, description: "审计公告和通知操作记录。", endpoint: "/communications/audit", columns: ["id", "action", "actorId", "createdAt"], icon: "ep:document" } },
    { path: "/console/analytics", name: "AnalyticsSpecRoute", component: () => import("@/views/analytics/index.vue"), meta: { title: "数据分析", showLink: false } },
    { path: "/console/analytics/content", name: "AnalyticsContentSpecRoute", component: () => import("@/views/analytics/index.vue"), meta: { title: "内容分析", showLink: false } },
    { path: "/console/analytics/storage", name: "AnalyticsStorageSpecRoute", component: () => import("@/views/analytics/index.vue"), meta: { title: "存储分析", showLink: false } },
    { path: "/console/analytics/planning", name: "AnalyticsPlanningSpecRoute", component: () => import("@/views/analytics/index.vue"), meta: { title: "计划分析", showLink: false } },
    { path: "/console/analytics/system", name: "AnalyticsSystemSpecRoute", component: () => import("@/views/analytics/index.vue"), meta: { title: "系统分析", showLink: false } },
    { path: "/console/analytics/metrics", name: "AnalyticsMetricsSpecRoute", component: () => import("@/views/analytics/index.vue"), meta: { title: "指标", showLink: false } },
    { path: "/console/analytics/reports", name: "AnalyticsReportsSpecRoute", component: () => import("@/views/analytics/index.vue"), meta: { title: "分析报告", showLink: false } },
    { path: "/console/platform/parameters", name: "PlatformParametersSpecRoute", component: PlatformParametersPage, meta: { title: "平台参数", showLink: false } },
    { path: "/console/platform/dictionaries", name: "PlatformDictionariesSpecRoute", component: PlatformDictionariesPage, meta: { title: "数据字典", showLink: false } },
    { path: "/console/platform/menus", name: "PlatformMenusSpecRoute", component: PlatformMenusPage, meta: { title: "平台菜单", showLink: false } },
    { path: "/console/ops/health", name: "OpsHealthSpecRoute", component: OpsHealthPage, meta: { title: "系统健康", showLink: false } },
    { path: "/console/ops/jobs", name: "OpsJobsSpecRoute", component: OpsJobsPage, meta: { title: "运维任务", showLink: false } },
    { path: "/console/ops/background", name: "OpsBackgroundSpecRoute", component: OpsBackgroundPage, meta: { title: "后台任务", showLink: false } },
    { path: "/console/security/users", name: "SecurityUsersSpecRoute", component: SecurityUsersPage, meta: { title: "安全用户", showLink: false } },
    { path: "/console/security/permissions", name: "SecurityPermissionsSpecRoute", component: SecurityPermissionsPage, meta: { title: "权限", showLink: false } },
    { path: "/console/security/sessions", name: "SecuritySessionsSpecRoute", component: SecuritySessionsPage, meta: { title: "会话", showLink: false } },
    { path: "/console/security/authentication", name: "SecurityAuthenticationSpecRoute", component: SecurityAuthenticationPage, meta: { title: "认证设置", showLink: false } },
    { path: "/console/passwords", name: "PasswordsSpecRoute", component: ModulePage, meta: { title: "密码管理", showLink: false, description: "管理密码条目和安全状态。", endpoint: "/passwords", columns: ["id", "title", "username", "updatedAt"], icon: "ep:lock" } },
    { path: "/console/passwords/generator", name: "PasswordGeneratorSpecRoute", component: ModulePage, meta: { title: "密码生成器", showLink: false, description: "生成符合策略的随机密码。", endpoint: "/passwords/generator", columns: ["id", "length", "strength", "createdAt"], icon: "ep:magic-stick" } },
    { path: "/console/passwords/health", name: "PasswordHealthSpecRoute", component: ModulePage, meta: { title: "密码健康", showLink: false, description: "检查重复、弱密码和泄露风险。", endpoint: "/passwords/health", columns: ["id", "title", "risk", "updatedAt"], icon: "ep:warning" } },
    { path: "/console/passwords/devices", name: "PasswordDevicesSpecRoute", component: ModulePage, meta: { title: "密码设备", showLink: false, description: "管理密码库授权设备。", endpoint: "/passwords/devices", columns: ["id", "name", "status", "lastSeenAt"], icon: "ep:mobile-phone" } },
    { path: "/console/integration/automation", name: "IntegrationAutomationSpecRoute", component: ModulePage, meta: { title: "自动化", showLink: false, description: "管理自动化规则和触发器。", endpoint: "/integration/automation", columns: ["id", "name", "status", "updatedAt"], icon: "ep:connection" } },
    { path: "/console/integration/executions", name: "IntegrationExecutionsSpecRoute", component: ModulePage, meta: { title: "自动化执行", showLink: false, description: "查看自动化执行记录。", endpoint: "/integration/executions", columns: ["id", "automationId", "status", "createdAt"], icon: "ep:operation" } },
    { path: "/console/integration/events", name: "IntegrationEventsSpecRoute", component: ModulePage, meta: { title: "集成事件", showLink: false, description: "查看外部集成事件和投递状态。", endpoint: "/integration/events", columns: ["id", "type", "status", "createdAt"], icon: "ep:bell" } },
    { path: "/console/integration/sync", name: "IntegrationSyncSpecRoute", component: ModulePage, meta: { title: "同步", showLink: false, description: "管理外部数据同步任务。", endpoint: "/integration/sync", columns: ["id", "source", "status", "updatedAt"], icon: "ep:refresh" } },
    { path: "/console/integration/plugins", name: "IntegrationPluginsSpecRoute", component: ModulePage, meta: { title: "插件", showLink: false, description: "管理集成插件及其权限。", endpoint: "/integration/plugins", columns: ["id", "name", "status", "version"], icon: "ep:cpu" } },
    subsystem("/resource-center", "ResourceCenter", "资源中心", "ep:files", [
      { path: "/resources", name: "Resources", component: ResourcesPage, meta: { title: "资源管理", description: "管理资源、标题、标签和生命周期。", icon: "ep:files" } },
      { path: "/documents", name: "Documents", component: DocumentsPage, meta: { title: "文档管理", description: "管理个人文档和工作副本。", icon: "ep:document" } },
      { path: "/collections", name: "Collections", component: CollectionsPage, meta: { title: "收藏集合", description: "管理资源集合及其成员。", icon: "ep:collection" } },
      { path: "/activity", name: "Activity", component: () => import("@/views/workbench/Activity.vue"), meta: { title: "资源活动", icon: "ep:histogram" } },
      { path: "/console/activity", name: "WorkbenchActivity", component: () => import("@/views/workbench/Activity.vue"), meta: { title: "我的活动与收藏", showLink: false } }
    ]),
    subsystem("/content-center", "ContentCenter", "内容与媒体", "ep:video-camera", [
      { path: "/media", name: "Media", component: () => import("@/views/media/index.vue"), meta: { title: "媒体库", icon: "ep:video-camera" } },
      { path: "/reading", name: "Reading", component: () => import("@/views/reading/index.vue"), meta: { title: "阅读库", icon: "ep:reading" } },
      moduleRoute({ path: "/music", name: "Music", title: "音乐库", description: "查看音乐播放列表和播放记录。", endpoint: "/music/playlists", columns: ["id", "name", "status", "createdAt"], icon: "ep:headset" }),
      moduleRoute({ path: "/photos", name: "Photos", title: "照片管理", description: "浏览照片和媒体元数据。", endpoint: "/photos/timeline", columns: ["id", "title", "status", "createdAt"], icon: "ep:picture" }),
      moduleRoute({ path: "/games", name: "Games", title: "游戏档案", description: "管理游戏、版本和数字资产。", endpoint: "/games", columns: ["id", "name", "status", "createdAt"], icon: "ep:monitor" })
    ]),
    subsystem("/storage-center", "StorageCenter", "存储与云盘", "ep:box", [
      { path: "/attachments", name: "Attachments", component: () => import("@/views/attachments/index.vue"), meta: { title: "附件与 Blob", icon: "ep:paperclip" } },
      { path: "/console/attachments/:attachmentId", name: "AttachmentDetail", component: () => import("@/views/attachments/Detail.vue"), meta: { title: "附件详情", showLink: false } },
      { path: "/drive", name: "Drive", component: () => import("@/views/drive/index.vue"), meta: { title: "个人云盘", icon: "ep:folder-opened" } },
      { path: "/console/drive/nodes/:nodeId", name: "DriveNodeDetail", component: () => import("@/views/drive/NodeDetail.vue"), meta: { title: "文件详情", showLink: false } },
      { path: "/storage/tiers", name: "StorageTiers", component: () => import("@/views/storage/Tiers.vue"), meta: { title: "持久化存储层", icon: "ep:box" } },
      { path: "/storage/archive", name: "StorageArchive", component: () => import("@/views/storage/Archive.vue"), meta: { title: "归档与恢复", icon: "ep:refresh-left" } },
      { path: "/storage/cache", name: "StorageCache", component: () => import("@/views/storage/Cache.vue"), meta: { title: "缓存与我的下载", icon: "ep:coffee-cup" } },
      { path: "/backup", name: "Backup", component: () => import("@/views/storage/Backup.vue"), meta: { title: "备份恢复", icon: "ep:files" } }
    ]),
    subsystem("/ingestion-center", "IngestionCenter", "导入与处理", "ep:upload", [
      moduleRoute({ path: "/ingestion", name: "Ingestion", title: "内容导入", description: "查看导入来源和扫描任务。", endpoint: "/ingestion/sources", columns: ["id", "name", "status", "createdAt"], icon: "ep:upload" }),
      moduleRoute({ path: "/tasks", name: "BackgroundTasks", title: "后台任务", description: "查看后台任务、进度和执行尝试。", endpoint: "/background-tasks", columns: ["id", "type", "status", "createdAt"], icon: "ep:operation" })
    ]),
    subsystem("/collaboration-center", "CollaborationCenter", "协作与分享", "ep:chat-line-round", [
      moduleRoute({ path: "/rooms", name: "Rooms", title: "协作房间", description: "查看共享协作房间及其状态。", endpoint: "/rooms", columns: ["id", "name", "status", "createdAt"], icon: "ep:chat-line-round" }),
      { path: "/sharing", name: "Sharing", component: () => import("@/views/sharing/index.vue"), meta: { title: "分享协作", icon: "ep:share" } }
    ]),
    subsystem("/planning-center", "PlanningCenter", "计划与财务", "ep:calendar", [
      moduleRoute({ path: "/planning", name: "Planning", title: "生产力与计划", description: "管理项目、任务和目标。", endpoint: "/planning/projects", createEndpoint: "/planning/projects", createFields: [{ name: "name", label: "项目名称", required: true }, { name: "description", label: "描述" }], columns: ["id", "name", "status", "createdAt"], icon: "ep:calendar" }),
      { path: "/planning/today", name: "PlanningToday", component: PlanningTodayPage, meta: { title: "今日计划", description: "查看今日任务、优先级和完成进度。", icon: "ep:calendar" } },
      { path: "/planning/projects", name: "PlanningProjects", component: PlanningProjectsPage, meta: { title: "项目管理", description: "管理项目、任务和目标。", icon: "ep:calendar" } },
      { path: "/planning/calendar", name: "PlanningCalendar", component: PlanningCalendarPage, meta: { title: "日历", description: "按时间查看计划事项。", icon: "ep:calendar" } },
      { path: "/planning/goals", name: "PlanningGoals", component: PlanningGoalsPage, meta: { title: "目标", description: "跟踪目标、关键结果与进度。", icon: "ep:aim" } },
      { path: "/planning/focus", name: "PlanningFocus", component: PlanningFocusPage, meta: { title: "专注", description: "管理专注会话和今日投入。", icon: "ep:timer" } },
      { path: "/finance", name: "Finance", component: FinancePage, meta: { title: "个人财务", description: "查看账本、账户和财务记录。", icon: "ep:money" } },
      { path: "/finance/accounts", name: "FinanceAccounts", component: FinanceAccountsPage, meta: { title: "账户", description: "管理财务账户和余额。", icon: "ep:wallet" } },
      { path: "/finance/transactions", name: "FinanceTransactions", component: FinanceTransactionsPage, meta: { title: "交易记录", description: "查看和管理收支交易。", icon: "ep:money" } },
      { path: "/finance/budgets", name: "FinanceBudgets", component: FinanceBudgetsPage, meta: { title: "预算", description: "管理预算和执行情况。", icon: "ep:pie-chart" } },
      { path: "/finance/reconcile", name: "FinanceReconcile", component: FinanceReconcilePage, meta: { title: "对账", description: "检查账户与交易记录的一致性。", icon: "ep:finished" } }
    ]),
    subsystem("/identity-center", "IdentityCenter", "身份与安全", "ep:lock", [
      moduleRoute({ path: "/account", name: "Account", title: "我的账户", description: "查看当前登录用户资料。", endpoint: "/me", columns: ["id", "username", "displayName", "email", "status"], icon: "ep:user-filled" }),
      moduleRoute({ path: "/security", name: "Security", title: "安全中心", description: "查看安全验证挑战和会话状态。", endpoint: "/security/verification-challenges", columns: ["id", "status", "createdAt", "expiresAt"], icon: "ep:warning" }),
      moduleRoute({ path: "/users", name: "Users", title: "用户管理", description: "管理用户状态、角色和账号信息。", endpoint: "/admin/users", columns: ["id", "username", "displayName", "status"], icon: "ep:user" }),
      moduleRoute({ path: "/roles", name: "Roles", title: "角色管理", description: "查看角色及其权限配置。", endpoint: "/admin/roles", columns: ["id", "code", "name", "description"], icon: "ep:key" }),
      moduleRoute({ path: "/permissions", name: "Permissions", title: "权限管理", description: "查看系统权限注册表。", endpoint: "/admin/permissions", columns: ["code", "name", "description"], icon: "ep:lock" })
    ])
  ]
} satisfies RouteConfigsTable;
