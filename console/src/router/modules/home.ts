const Layout = () => import("@/layout/index.vue");
const ModulePage = () => import("@/views/modules/ModulePage.vue");
const DashboardPage = () => import("@/views/dashboard/index.vue");

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
    { path: "/console/attachments", name: "AttachmentsSpecRoute", component: () => import("@/views/attachments/index.vue"), meta: { title: "附件与 Blob", showLink: false } },
    { path: "/console/storage/tiers", name: "StorageTiersSpecRoute", component: () => import("@/views/storage/Tiers.vue"), meta: { title: "持久化存储层", showLink: false } },
    { path: "/console/storage/archive", name: "StorageArchiveSpecRoute", component: () => import("@/views/storage/Archive.vue"), meta: { title: "归档与恢复", showLink: false } },
    { path: "/console/storage/cache", name: "StorageCacheSpecRoute", component: () => import("@/views/storage/Cache.vue"), meta: { title: "缓存与我的下载", showLink: false } },
    { path: "/console/storage/backup", name: "StorageBackupSpecRoute", component: ModulePage, meta: { title: "备份与恢复", showLink: false, description: "管理备份集与恢复向导。", endpoint: "/admin/backup/restore-points", columns: ["id", "status", "createdAt", "verifiedAt"], icon: "ep:files" } },
    { path: "/console/planning/today", name: "PlanningTodaySpecRoute", component: ModulePage, meta: { title: "今日计划", showLink: false, description: "查看今日任务、优先级和完成进度。", endpoint: "/planning/tasks", columns: ["id", "title", "status", "dueAt"], icon: "ep:calendar" } },
    { path: "/console/planning/projects", name: "PlanningProjectsSpecRoute", component: ModulePage, meta: { title: "项目管理", showLink: false, description: "管理项目、任务和目标。", endpoint: "/planning/projects", columns: ["id", "name", "status", "createdAt"], icon: "ep:calendar" } },
    { path: "/console/planning/calendar", name: "PlanningCalendarSpecRoute", component: ModulePage, meta: { title: "日历", showLink: false, description: "按时间查看计划事项。", endpoint: "/planning/calendar", columns: ["id", "title", "startAt", "endAt"], icon: "ep:calendar" } },
    { path: "/console/planning/goals", name: "PlanningGoalsSpecRoute", component: ModulePage, meta: { title: "目标", showLink: false, description: "跟踪目标、关键结果与进度。", endpoint: "/planning/goals", columns: ["id", "title", "status", "progress"], icon: "ep:aim" } },
    { path: "/console/planning/focus", name: "PlanningFocusSpecRoute", component: ModulePage, meta: { title: "专注", showLink: false, description: "管理专注会话和今日投入。", endpoint: "/planning/focus-sessions", columns: ["id", "status", "startedAt", "durationSeconds"], icon: "ep:timer" } },
    { path: "/console/finance", name: "FinanceSpecRoute", component: ModulePage, meta: { title: "个人记账", showLink: false, description: "查看账本、账户和财务摘要。", endpoint: "/finance/ledgers", columns: ["id", "name", "currency", "createdAt"], icon: "ep:money" } },
    { path: "/console/finance/accounts", name: "FinanceAccountsSpecRoute", component: ModulePage, meta: { title: "账户", showLink: false, description: "管理财务账户和余额。", endpoint: "/finance/accounts", columns: ["id", "name", "type", "balance"], icon: "ep:wallet" } },
    { path: "/console/finance/transactions", name: "FinanceTransactionsSpecRoute", component: ModulePage, meta: { title: "交易记录", showLink: false, description: "查看和管理收支交易。", endpoint: "/finance/transactions", columns: ["id", "accountId", "amount", "occurredAt"], icon: "ep:money" } },
    { path: "/console/finance/budgets", name: "FinanceBudgetsSpecRoute", component: ModulePage, meta: { title: "预算", showLink: false, description: "管理预算和执行情况。", endpoint: "/finance/budgets", columns: ["id", "name", "amount", "spent"], icon: "ep:pie-chart" } },
    { path: "/console/finance/reconcile", name: "FinanceReconcileSpecRoute", component: ModulePage, meta: { title: "对账", showLink: false, description: "检查账户与交易记录的一致性。", endpoint: "/finance/reconcile", columns: ["id", "status", "difference", "updatedAt"], icon: "ep:finished" } },
    { path: "/console/private-notes", name: "PrivateNotesSpecRoute", component: ModulePage, meta: { title: "私密笔记", showLink: false, description: "在加密边界内管理个人笔记。", endpoint: "/private-notes/notes", columns: ["id", "title", "updatedAt", "status"], icon: "ep:lock" } },
    { path: "/console/private-notes/conflicts", name: "PrivateNotesConflictsSpecRoute", component: ModulePage, meta: { title: "笔记冲突", showLink: false, description: "处理同步冲突并保留版本历史。", endpoint: "/private-notes/conflicts", columns: ["id", "noteId", "status", "createdAt"], icon: "ep:warning" } },
    { path: "/console/private-notes/recovery", name: "PrivateNotesRecoverySpecRoute", component: ModulePage, meta: { title: "笔记恢复", showLink: false, description: "查看恢复状态和安全恢复操作。", endpoint: "/private-notes/recovery", columns: ["id", "status", "createdAt", "updatedAt"], icon: "ep:refresh" } },
    { path: "/console/account/profile", name: "AccountProfileSpecRoute", component: ModulePage, meta: { title: "个人资料", showLink: false, description: "查看和维护个人资料。", endpoint: "/me", columns: ["id", "username", "displayName", "email", "status"], icon: "ep:user" } },
    { path: "/console/account/preferences", name: "AccountPreferencesSpecRoute", component: ModulePage, meta: { title: "偏好设置", showLink: false, description: "管理 Console 显示和交互偏好。", endpoint: "/me/preferences", columns: ["key", "value", "updatedAt"], icon: "ep:setting" } },
    { path: "/console/account/notifications", name: "AccountNotificationsSpecRoute", component: ModulePage, meta: { title: "通知设置", showLink: false, description: "管理通知渠道和订阅偏好。", endpoint: "/me/notifications", columns: ["channel", "enabled", "updatedAt"], icon: "ep:bell" } },
    { path: "/console/account/security", name: "AccountSecuritySpecRoute", component: ModulePage, meta: { title: "账户安全", showLink: false, description: "管理认证方式和安全状态。", endpoint: "/security/verification-challenges", columns: ["id", "status", "createdAt", "expiresAt"], icon: "ep:lock" } },
    subsystem("/resource-center", "ResourceCenter", "资源中心", "ep:files", [
      moduleRoute({ path: "/resources", name: "Resources", title: "资源管理", description: "管理资源、标题、标签和生命周期。", endpoint: "/resources", createEndpoint: "/resources", deleteEndpoint: "/resources", actions: [{ name: "archive", label: "归档", path: "/resources/{id}/actions/archive", confirm: "确定归档此资源吗？" }, { name: "restore", label: "恢复", path: "/resources/{id}/actions/restore" }], createFields: [{ name: "type", label: "类型", required: true, defaultValue: "OTHER" }, { name: "title", label: "标题", required: true }, { name: "locale", label: "语言", required: true, defaultValue: "zh-CN" }], columns: ["id", "resourceType", "status", "createdAt"], icon: "ep:files" }),
      moduleRoute({ path: "/documents", name: "Documents", title: "文档管理", description: "管理个人文档和工作副本。", endpoint: "/documents", createEndpoint: "/documents", createFields: [{ name: "title", label: "标题", required: true }, { name: "kind", label: "类型", required: true, defaultValue: "DOCUMENT" }, { name: "locale", label: "语言", defaultValue: "zh-CN" }, { name: "content", label: "内容" }], columns: ["id", "title", "status", "updatedAt"], icon: "ep:document" }),
      moduleRoute({ path: "/collections", name: "Collections", title: "收藏集合", description: "管理资源集合及其成员。", endpoint: "/collections", createEndpoint: "/collections", createFields: [{ name: "name", label: "名称", required: true }, { name: "description", label: "描述" }], columns: ["id", "name", "description", "createdAt"], icon: "ep:collection" }),
      { path: "/activity", name: "Activity", component: () => import("@/views/workbench/Activity.vue"), meta: { title: "资源活动", icon: "ep:histogram" } },
      { path: "/console/activity", name: "WorkbenchActivity", component: () => import("@/views/workbench/Activity.vue"), meta: { title: "我的活动与收藏", showLink: false } }
    ]),
    subsystem("/content-center", "ContentCenter", "内容与媒体", "ep:video-camera", [
      moduleRoute({ path: "/media", name: "Media", title: "媒体库", description: "查看媒体资源和播放内容。", endpoint: "/media/subjects", columns: ["id", "title", "mediaType", "status"], icon: "ep:video-camera" }),
      moduleRoute({ path: "/reading", name: "Reading", title: "阅读库", description: "管理阅读作品、版本和阅读内容。", endpoint: "/reading/works", columns: ["id", "title", "status", "createdAt"], icon: "ep:reading" }),
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
      moduleRoute({ path: "/backup", name: "Backup", title: "备份恢复", description: "管理恢复点和备份验证。", endpoint: "/admin/backup/restore-points", columns: ["id", "status", "createdAt", "verifiedAt"], icon: "ep:files" })
    ]),
    subsystem("/ingestion-center", "IngestionCenter", "导入与处理", "ep:upload", [
      moduleRoute({ path: "/ingestion", name: "Ingestion", title: "内容导入", description: "查看导入来源和扫描任务。", endpoint: "/ingestion/sources", columns: ["id", "name", "status", "createdAt"], icon: "ep:upload" }),
      moduleRoute({ path: "/tasks", name: "BackgroundTasks", title: "后台任务", description: "查看后台任务、进度和执行尝试。", endpoint: "/background-tasks", columns: ["id", "type", "status", "createdAt"], icon: "ep:operation" })
    ]),
    subsystem("/collaboration-center", "CollaborationCenter", "协作与分享", "ep:chat-line-round", [
      moduleRoute({ path: "/rooms", name: "Rooms", title: "协作房间", description: "查看共享协作房间及其状态。", endpoint: "/rooms", columns: ["id", "name", "status", "createdAt"], icon: "ep:chat-line-round" }),
      moduleRoute({ path: "/sharing", name: "Sharing", title: "分享协作", description: "管理分享链接和协作空间。", endpoint: "/shares", columns: ["id", "status", "createdAt", "expiresAt"], icon: "ep:share" })
    ]),
    subsystem("/planning-center", "PlanningCenter", "计划与财务", "ep:calendar", [
      moduleRoute({ path: "/planning", name: "Planning", title: "生产力与计划", description: "管理项目、任务和目标。", endpoint: "/planning/projects", createEndpoint: "/planning/projects", createFields: [{ name: "name", label: "项目名称", required: true }, { name: "description", label: "描述" }], columns: ["id", "name", "status", "createdAt"], icon: "ep:calendar" }),
      moduleRoute({ path: "/finance", name: "Finance", title: "个人财务", description: "查看账本、账户和财务记录。", endpoint: "/finance/ledgers", columns: ["id", "name", "currency", "createdAt"], icon: "ep:money" })
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
