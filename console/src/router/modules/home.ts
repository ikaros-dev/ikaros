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
    subsystem("/ai-center", "AiCenter", "AI 智能", "ep:magic-stick", [
      { path: "assistant", name: "AiAssistant", component: AiAssistantPage, meta: { title: "AI 助手", description: "与 AI 助手协作处理内容和任务。", icon: "ep:chat-dot-round" } },
      { path: "models", name: "AiModels", component: AiModelsPage, meta: { title: "AI 模型", description: "查看可用模型和能力配置。", icon: "ep:cpu" } },
      { path: "personas", name: "AiPersonas", component: AiPersonasPage, meta: { title: "AI 人格", description: "管理助手人格和提示配置。", icon: "ep:user" } },
      { path: "privacy", name: "AiPrivacy", component: AiPrivacyPage, meta: { title: "AI 隐私", description: "管理 AI 数据使用和隐私边界。", icon: "ep:lock" } },
      { path: "jobs", name: "AiJobs", component: AiJobsPage, meta: { title: "AI 任务", description: "查看 AI 后台任务及执行状态。", icon: "ep:operation" } }
    ]),
    subsystem("/operations-center", "OperationsCenter", "系统运维", "ep:monitor", [
      { path: "health", name: "OpsHealth", component: OpsHealthPage, meta: { title: "系统健康", description: "查看服务、依赖和存储健康状态。", icon: "ep:monitor" } },
      { path: "jobs", name: "OpsJobs", component: OpsJobsPage, meta: { title: "运维任务", description: "查看运维任务及执行结果。", icon: "ep:operation" } },
      { path: "background", name: "OpsBackground", component: OpsBackgroundPage, meta: { title: "后台任务", description: "查看后台操作进度。", icon: "ep:loading" } }
    ]),
    subsystem("/integration-center", "IntegrationCenter", "集成自动化", "ep:connection", [
      { path: "automation", name: "IntegrationAutomation", component: () => import("@/views/integration/index.vue"), meta: { title: "自动化", icon: "ep:connection" } },
      { path: "executions", name: "IntegrationExecutions", component: () => import("@/views/integration/index.vue"), meta: { title: "自动化执行", icon: "ep:operation" } },
      { path: "events", name: "IntegrationEvents", component: () => import("@/views/integration/index.vue"), meta: { title: "集成事件", icon: "ep:bell" } },
      { path: "sync", name: "IntegrationSync", component: () => import("@/views/integration/index.vue"), meta: { title: "同步", icon: "ep:refresh" } },
      { path: "plugins", name: "IntegrationPlugins", component: () => import("@/views/integration/index.vue"), meta: { title: "插件", icon: "ep:cpu" } }
    ]),
    subsystem("/communications-center", "CommunicationsCenter", "沟通与审计", "ep:notification", [
      { path: "announcements", name: "Announcements", component: AnnouncementsPage, meta: { title: "公告", description: "发布和管理系统公告。", icon: "ep:notification" } },
      { path: "notifications", name: "Notifications", component: NotificationsPage, meta: { title: "通知", description: "查看通知投递和用户触达状态。", icon: "ep:bell" } },
      { path: "audit", name: "CommunicationsAudit", component: () => import("@/views/communications/Audit.vue"), meta: { title: "沟通审计", icon: "ep:document" } }
    ]),
    subsystem("/analytics-center", "AnalyticsCenter", "数据分析", "ep:data-analysis", [
      { path: "overview", name: "AnalyticsOverview", component: () => import("@/views/analytics/index.vue"), meta: { title: "分析总览", icon: "ep:data-analysis" } },
      { path: "content", name: "AnalyticsContent", component: () => import("@/views/analytics/index.vue"), meta: { title: "内容分析", icon: "ep:document" } },
      { path: "storage", name: "AnalyticsStorage", component: () => import("@/views/analytics/index.vue"), meta: { title: "存储分析", icon: "ep:box" } },
      { path: "planning", name: "AnalyticsPlanning", component: () => import("@/views/analytics/index.vue"), meta: { title: "计划分析", icon: "ep:calendar" } },
      { path: "system", name: "AnalyticsSystem", component: () => import("@/views/analytics/index.vue"), meta: { title: "系统分析", icon: "ep:monitor" } },
      { path: "metrics", name: "AnalyticsMetrics", component: () => import("@/views/analytics/index.vue"), meta: { title: "指标", icon: "ep:trend-charts" } },
      { path: "reports", name: "AnalyticsReports", component: () => import("@/views/analytics/index.vue"), meta: { title: "分析报告", icon: "ep:document" } }
    ]),
    subsystem("/platform-config", "ConfigurationCenter", "平台配置", "ep:setting", [
      { path: "parameters", name: "PlatformParameters", component: PlatformParametersPage, meta: { title: "平台参数", description: "管理平台运行参数。", icon: "ep:setting" } },
      { path: "dictionaries", name: "PlatformDictionaries", component: PlatformDictionariesPage, meta: { title: "数据字典", description: "管理平台字典项。", icon: "ep:list" } },
      { path: "menus", name: "PlatformMenus", component: PlatformMenusPage, meta: { title: "平台菜单", description: "管理菜单和导航结构。", icon: "ep:menu" } }
    ]),
    subsystem("/password-center", "PasswordCenter", "密码管理", "ep:key", [
      { path: "vault", name: "Passwords", component: () => import("@/views/password/index.vue"), meta: { title: "密码库", icon: "ep:lock" } },
      { path: "generator", name: "PasswordGenerator", component: () => import("@/views/password/index.vue"), meta: { title: "密码生成器", icon: "ep:magic-stick" } },
      { path: "health", name: "PasswordHealth", component: () => import("@/views/password/index.vue"), meta: { title: "密码健康", icon: "ep:warning" } },
      { path: "devices", name: "PasswordDevices", component: () => import("@/views/password/index.vue"), meta: { title: "密码设备", icon: "ep:mobile-phone" } }
    ]),
    subsystem("/private-notes", "NotesCenter", "私密笔记", "ep:lock", [
      { path: "overview", name: "PrivateNotes", component: PrivateNotesPage, meta: { title: "私密笔记", description: "在加密边界内管理个人笔记。", icon: "ep:document" } },
      { path: "conflicts", name: "PrivateNotesConflicts", component: () => import("@/views/notes/Security.vue"), meta: { title: "笔记冲突", icon: "ep:warning" } },
      { path: "recovery", name: "PrivateNotesRecovery", component: () => import("@/views/notes/Security.vue"), meta: { title: "笔记恢复", icon: "ep:refresh" } }
    ]),
    subsystem("/account-center", "AccountCenter", "个人中心", "ep:user", [
      { path: "profile", name: "AccountProfile", component: AccountProfilePage, meta: { title: "个人资料", description: "查看和维护个人资料。", icon: "ep:user" } },
      { path: "preferences", name: "AccountPreferences", component: AccountPreferencesPage, meta: { title: "偏好设置", description: "管理 Console 显示和交互偏好。", icon: "ep:setting" } },
      { path: "notifications", name: "AccountNotifications", component: () => import("@/views/communications/Notifications.vue"), meta: { title: "通知设置", icon: "ep:bell" } },
      { path: "security", name: "AccountSecurity", component: () => import("@/views/security/Authentication.vue"), meta: { title: "账户安全", icon: "ep:lock" } }
    ]),
    subsystem("/resource-center", "ResourceCenter", "资源中心", "ep:files", [
      { path: "global-search", name: "WorkbenchSearch", component: () => import("@/views/workbench/Search.vue"), meta: { title: "全局搜索", showLink: false } },
      { path: "library", name: "Resources", component: ResourcesPage, meta: { title: "资源管理", description: "管理资源、标题、标签和生命周期。", icon: "ep:files" } },
      { path: "library/:resourceId", name: "ResourceDetail", component: () => import("@/views/resources/Detail.vue"), meta: { title: "资源详情", showLink: false } },
      { path: "documents", name: "Documents", component: DocumentsPage, meta: { title: "文档管理", description: "管理个人文档和工作副本。", icon: "ep:document" } },
      { path: "collections", name: "Collections", component: CollectionsPage, meta: { title: "收藏集合", description: "管理资源集合及其成员。", icon: "ep:collection" } },
      { path: "activity", name: "Activity", component: () => import("@/views/workbench/Activity.vue"), meta: { title: "资源活动", icon: "ep:histogram" } },
      { path: "activity/overview", name: "WorkbenchActivity", component: () => import("@/views/workbench/Activity.vue"), meta: { title: "我的活动与收藏", showLink: false } }
    ]),
    subsystem("/content-center", "ContentCenter", "内容与媒体", "ep:video-camera", [
      { path: "media", name: "Media", component: () => import("@/views/media/index.vue"), meta: { title: "媒体库", icon: "ep:video-camera" } },
      { path: "reading", name: "Reading", component: () => import("@/views/reading/index.vue"), meta: { title: "阅读库", icon: "ep:reading" } },
      { path: "music", name: "Music", component: () => import("@/views/media/Catalog.vue"), meta: { title: "音乐库", icon: "ep:headset" } },
      { path: "photos", name: "Photos", component: () => import("@/views/media/Catalog.vue"), meta: { title: "照片管理", icon: "ep:picture" } },
      { path: "games", name: "Games", component: () => import("@/views/media/Catalog.vue"), meta: { title: "游戏档案", icon: "ep:monitor" } }
    ]),
    subsystem("/storage-center", "StorageCenter", "存储与云盘", "ep:box", [
      { path: "attachments", name: "Attachments", component: () => import("@/views/attachments/index.vue"), meta: { title: "附件与 Blob", icon: "ep:paperclip" } },
      { path: "attachments/:attachmentId", name: "AttachmentDetail", component: () => import("@/views/attachments/Detail.vue"), meta: { title: "附件详情", showLink: false } },
      { path: "drive", name: "Drive", component: () => import("@/views/drive/index.vue"), meta: { title: "个人云盘", icon: "ep:folder-opened" } },
      { path: "drive/nodes/:nodeId", name: "DriveNodeDetail", component: () => import("@/views/drive/NodeDetail.vue"), meta: { title: "文件详情", showLink: false } },
      { path: "drive/trash", name: "DriveTrash", component: () => import("@/views/drive/Operations.vue"), meta: { title: "云盘回收站", showLink: false } },
      { path: "drive/transfers", name: "DriveTransfers", component: () => import("@/views/drive/Operations.vue"), meta: { title: "传输任务", showLink: false } },
      { path: "drive/sync", name: "DriveSync", component: () => import("@/views/drive/Operations.vue"), meta: { title: "云盘同步", showLink: false } },
      { path: "drive/conflicts", name: "DriveConflicts", component: () => import("@/views/drive/Operations.vue"), meta: { title: "同步冲突", showLink: false } },
      { path: "drive/quota", name: "DriveQuota", component: () => import("@/views/drive/Operations.vue"), meta: { title: "云盘配额", showLink: false } },
      { path: "drive/policies", name: "DrivePolicies", component: () => import("@/views/drive/Operations.vue"), meta: { title: "云盘策略", showLink: false } },
      { path: "drive/spaces", name: "DriveSpaces", component: () => import("@/views/drive/index.vue"), meta: { title: "云盘空间", showLink: false } },
      { path: "drive/spaces/:spaceId", name: "DriveSpaceDetail", component: () => import("@/views/drive/Operations.vue"), meta: { title: "云盘空间详情", showLink: false } },
      { path: "drive/sync/:bindingId", name: "DriveBindingDetail", component: () => import("@/views/drive/Operations.vue"), meta: { title: "同步绑定详情", showLink: false } },
      { path: "drive/revisions", name: "DriveRevisions", component: () => import("@/views/drive/Operations.vue"), meta: { title: "文件版本", showLink: false } },
      { path: "tiers", name: "StorageTiers", component: () => import("@/views/storage/Tiers.vue"), meta: { title: "持久化存储层", icon: "ep:box" } },
      { path: "providers", name: "StorageProviders", component: () => import("@/views/storage/Tiers.vue"), meta: { title: "存储 Provider", icon: "ep:setting" } },
      { path: "archive", name: "StorageArchive", component: () => import("@/views/storage/Archive.vue"), meta: { title: "归档与恢复", icon: "ep:refresh-left" } },
      { path: "cache", name: "StorageCache", component: () => import("@/views/storage/Cache.vue"), meta: { title: "缓存与我的下载", icon: "ep:coffee-cup" } },
      { path: "backup", name: "Backup", component: () => import("@/views/storage/Backup.vue"), meta: { title: "备份恢复", icon: "ep:files" } },
    ]),
    subsystem("/edge-acceleration", "EdgeAccelerationCenter", "边缘加速", "ep:connection", [
      { path: "reliability", name: "StorageDeliveryReliability", component: () => import("@/views/storage/Delivery.vue"), meta: { title: "投递可靠性", icon: "ep:monitor" } },
      { path: "failover", name: "StorageDeliveryFailover", component: () => import("@/views/storage/Delivery.vue"), meta: { title: "故障切换", icon: "ep:warning" } },
      { path: "restore", name: "StorageDeliveryRestore", component: () => import("@/views/storage/Delivery.vue"), meta: { title: "恢复协调", icon: "ep:refresh" } },
      { path: "budget", name: "StorageDeliveryBudget", component: () => import("@/views/storage/Delivery.vue"), meta: { title: "流量预算", icon: "ep:data-line" } },
      { path: "purge", name: "StorageDeliveryPurge", component: () => import("@/views/storage/Delivery.vue"), meta: { title: "CDN 清理", icon: "ep:delete" } }
    ]),
    subsystem("/ingestion-center", "IngestionCenter", "导入与处理", "ep:upload", [
      { path: "import", name: "Ingestion", component: () => import("@/views/ingestion/index.vue"), meta: { title: "内容导入", icon: "ep:upload" } },
      { path: "tasks", name: "BackgroundTasks", component: OpsBackgroundPage, meta: { title: "后台任务", icon: "ep:operation" } }
    ]),
    subsystem("/collaboration-center", "CollaborationCenter", "协作与分享", "ep:chat-line-round", [
      { path: "rooms", name: "Rooms", component: () => import("@/views/sharing/index.vue"), meta: { title: "协作房间", icon: "ep:chat-line-round" } },
      { path: "sharing", name: "Sharing", component: () => import("@/views/sharing/index.vue"), meta: { title: "分享协作", icon: "ep:share" } }
    ]),
    subsystem("/planning-center", "PlanningCenter", "项目与计划", "ep:calendar", [
      { path: "overview", name: "Planning", component: () => import("@/views/planning/Projects.vue"), meta: { title: "生产力与计划", icon: "ep:calendar" } },
      { path: "today", name: "PlanningToday", component: PlanningTodayPage, meta: { title: "今日计划", description: "查看今日任务、优先级和完成进度。", icon: "ep:calendar" } },
      { path: "projects", name: "PlanningProjects", component: PlanningProjectsPage, meta: { title: "项目管理", description: "管理项目、任务和目标。", icon: "ep:calendar" } },
      { path: "calendar", name: "PlanningCalendar", component: PlanningCalendarPage, meta: { title: "日历", description: "按时间查看计划事项。", icon: "ep:calendar" } },
      { path: "goals", name: "PlanningGoals", component: PlanningGoalsPage, meta: { title: "目标", description: "跟踪目标、关键结果与进度。", icon: "ep:aim" } },
      { path: "focus", name: "PlanningFocus", component: PlanningFocusPage, meta: { title: "专注", description: "管理专注会话和今日投入。", icon: "ep:timer" } },
    ]),
    subsystem("/finance-center", "FinanceCenter", "记账与财务", "ep:money", [
      { path: "finance", name: "Finance", component: FinancePage, meta: { title: "个人财务", description: "查看账本、账户和财务记录。", icon: "ep:money" } },
      { path: "accounts", name: "FinanceAccounts", component: FinanceAccountsPage, meta: { title: "账户", description: "管理财务账户和余额。", icon: "ep:wallet" } },
      { path: "transactions", name: "FinanceTransactions", component: FinanceTransactionsPage, meta: { title: "交易记录", description: "查看和管理收支交易。", icon: "ep:money" } },
      { path: "budgets", name: "FinanceBudgets", component: FinanceBudgetsPage, meta: { title: "预算", description: "管理预算和执行情况。", icon: "ep:pie-chart" } },
      { path: "reconcile", name: "FinanceReconcile", component: FinanceReconcilePage, meta: { title: "对账", description: "检查账户与交易记录的一致性。", icon: "ep:finished" } }
    ]),
    subsystem("/identity-center", "IdentityCenter", "身份与安全", "ep:lock", [
      { path: "security", name: "Security", component: SecurityAuthenticationPage, meta: { title: "安全中心", icon: "ep:warning" } },
      { path: "users", name: "Users", component: SecurityUsersPage, meta: { title: "用户管理", icon: "ep:user" } },
      { path: "roles", name: "Roles", component: SecurityPermissionsPage, meta: { title: "角色管理", icon: "ep:key" } },
      { path: "permissions", name: "Permissions", component: SecurityPermissionsPage, meta: { title: "权限管理", icon: "ep:lock" } },
      { path: "sessions", name: "SecuritySessions", component: SecuritySessionsPage, meta: { title: "JWT 状态", description: "查看本地 Access / Refresh Token 状态。", icon: "ep:key" } },
      { path: "authentication", name: "SecurityAuthentication", component: SecurityAuthenticationPage, meta: { title: "认证设置", description: "管理认证方式和验证挑战。", icon: "ep:key" } }
    ])
  ]
} satisfies RouteConfigsTable;
