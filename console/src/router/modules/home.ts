const Layout = () => import("@/layout/index.vue");
const WorkspaceView = () => import("@/layout/components/WorkspaceView.vue");

type WorkspaceOptions = {
  title: string;
  icon: string;
  capability?: string;
};

type PageOptions = {
  icon?: string;
  showLink?: boolean;
  showParent?: boolean;
  activePath?: string;
};

const workspaceRanks: Record<string, number> = {
  Dashboard: 10,
  Resources: 20,
  Storage: 30,
  Apps: 40,
  System: 50
};

function workspace(
  path: string,
  name: string,
  options: WorkspaceOptions,
  children: any[],
  redirect?: string
) {
  return {
    path,
    name,
    component: WorkspaceView,
    ...(redirect ? { redirect } : {}),
    meta: {
      title: options.title,
      icon: options.icon,
      ...(options.capability ? { capability: options.capability } : {}),
      workspace: name,
      showParent: true,
      rank: workspaceRanks[name]
    },
    children
  };
}

function directory(
  path: string,
  name: string,
  title: string,
  icon: string,
  redirect: string,
  children: any[]
) {
  return {
    path,
    name,
    component: WorkspaceView,
    redirect,
    meta: { title, icon, showParent: true },
    children
  };
}

function page(
  path: string,
  name: string,
  title: string,
  description: string,
  component: any,
  capability?: string,
  options: PageOptions = {}
) {
  return {
    path,
    name,
    component,
    meta: {
      title,
      description,
      icon: options.icon ?? "ep:document",
      ...(capability ? { capability } : {}),
      ...(options.showLink === false ? { showLink: false } : {}),
      ...(options.showParent ? { showParent: true } : {}),
      ...(options.activePath ? { activePath: options.activePath } : {})
    }
  };
}

const hidden = (activePath: string): PageOptions => ({
  showLink: false,
  activePath
});

export default {
  path: "/",
  name: "IkarosConsole",
  component: Layout,
  redirect: "/dashboard",
  meta: { title: "Ikaros Console", icon: "ep:menu" },
  children: [
    workspace(
      "/dashboard",
      "Dashboard",
      { title: "仪表盘", icon: "ep:data-analysis", capability: "dashboard.read" },
      [
        page(
          "",
          "DashboardHome",
          "仪表盘",
          "汇总需要关注的事项、正在执行的工作、资源与存储状态，作为进入各工作域的起点。",
          () => import("@/views/console/Dashboard.vue"),
          "dashboard.read",
          { icon: "ep:data-analysis" }
        )
      ]
    ),

    workspace(
      "/resources",
      "Resources",
      { title: "资源", icon: "ep:files" },
      [
        page(
          "library",
          "ResourceLibrary",
          "资源库",
          "统一浏览、检索和管理 Ikaros 中的资源，并进入资源详情。",
          () => import("@/views/console/resources/Library.vue"),
          "resource.read",
          { icon: "ep:collection" }
        ),
        page(
          "library/:resourceId",
          "ResourceDetail",
          "资源详情",
          "查看单个资源的基础信息、文件、元数据、关系、状态与相关活动。",
          () => import("@/views/console/resources/ResourceDetail.vue"),
          "resource.read",
          hidden("/resources/library")
        ),
        page(
          "library/collections",
          "ResourceCollections",
          "资源集合",
          "管理资源集合以及资源与集合之间的组织关系。",
          () => import("@/views/console/resources/Collections.vue"),
          "collection.read",
          hidden("/resources/library")
        ),
        page(
          "library/search",
          "ResourceSearch",
          "资源搜索",
          "跨资源执行搜索，并通过筛选快速定位目标资源。",
          () => import("@/views/console/resources/Search.vue"),
          "search.use",
          hidden("/resources/library")
        ),
        page(
          "add",
          "AddResource",
          "添加资源",
          "从本地、NAS、对象存储、远程地址或插件来源添加资源，并发起导入流程。",
          () => import("@/views/console/resources/Add.vue"),
          "ingestion.read",
          { icon: "ep:upload" }
        ),
        page(
          "activity",
          "ActivityCenter",
          "活动中心",
          "统一查看导入、存储、元数据、备份、下载、AI 与自动化等后台工作。",
          () => import("@/views/console/resources/Activity.vue"),
          "activity.read",
          { icon: "ep:histogram" }
        ),
        page(
          "activity/:activityId",
          "ActivityDetail",
          "活动详情",
          "查看单个后台工作的状态、进度、结果、错误与执行信息。",
          () => import("@/views/console/resources/ActivityDetail.vue"),
          "activity.read",
          hidden("/resources/activity")
        )
      ],
      "/resources/library"
    ),

    workspace(
      "/storage",
      "Storage",
      { title: "存储", icon: "ep:box" },
      [
        page(
          "overview",
          "StorageOverview",
          "存储概览",
          "查看存储容量、层级分布、Provider 状态、恢复状态与需要关注的问题。",
          () => import("@/views/console/storage/Overview.vue"),
          "storage.read",
          { icon: "ep:data-analysis" }
        ),
        page(
          "providers",
          "StorageProviders",
          "存储提供方",
          "配置和管理存储提供方、凭据、容量、层级与健康状态。",
          () => import("@/views/console/storage/Providers.vue"),
          "storage.provider.read",
          { icon: "ep:setting" }
        ),
        page(
          "providers/:providerId",
          "StorageProviderDetail",
          "存储提供方详情",
          "查看和维护单个存储提供方的配置、容量和运行状态。",
          () => import("@/views/console/storage/ProviderDetail.vue"),
          "storage.provider.read",
          hidden("/storage/providers")
        ),
        page(
          "policy",
          "StoragePolicy",
          "存储策略",
          "配置资源在不同存储层级之间的生命周期、保留、副本与迁移策略。",
          () => import("@/views/console/storage/Policy.vue"),
          "storage.policy.read"
        ),
        page(
          "archive",
          "StorageArchive",
          "归档管理",
          "管理归档资源、恢复请求、恢复预算以及待清理对象。",
          () => import("@/views/console/storage/Archive.vue"),
          "storage.archive.read"
        ),
        page(
          "backup",
          "StorageBackup",
          "备份管理",
          "管理备份与恢复点，并查看备份验证和发布状态。",
          () => import("@/views/console/storage/Backup.vue"),
          "backup.read"
        ),
        page(
          "maintenance",
          "StorageMaintenance",
          "存储维护",
          "处理完整性校验、Blob、Placement、GC、Delivery 与高级存储诊断。",
          () => import("@/views/console/storage/Maintenance.vue"),
          "storage.maintenance.read",
          { icon: "ep:tools" }
        )
      ],
      "/storage/overview"
    ),

    workspace(
      "/apps",
      "Apps",
      { title: "应用", icon: "ep:grid" },
      [
        page(
          "overview",
          "AppsOverview",
          "应用中心",
          "查看已启用的核心应用和插件应用，并进入对应产品。",
          () => import("@/views/console/apps/Overview.vue"),
          undefined,
          { icon: "ep:grid" }
        ),
        page(
          "drive",
          "Drive",
          "云盘",
          "浏览和管理个人文件、文件夹、空间以及云盘相关操作。",
          () => import("@/views/console/apps/Drive.vue"),
          "drive.space.read",
          { icon: "ep:folder" }
        ),
        page(
          "drive/nodes/:nodeId",
          "DriveNodeDetail",
          "云盘文件详情",
          "查看单个文件或文件夹的版本、存储、共享、同步与活动信息。",
          () => import("@/views/console/apps/DriveNodeDetail.vue"),
          "drive.file.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/transfers",
          "DriveTransfers",
          "云盘传输",
          "查看上传、下载和其他云盘传输任务的状态。",
          () => import("@/views/console/apps/DriveTransfers.vue"),
          "drive.transfer.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/sync",
          "DriveSync",
          "云盘同步",
          "管理设备与云盘之间的同步关系和同步状态。",
          () => import("@/views/console/apps/DriveSync.vue"),
          "drive.sync.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/conflicts",
          "DriveConflicts",
          "云盘冲突",
          "查看并处理同步过程中产生的文件冲突。",
          () => import("@/views/console/apps/DriveConflicts.vue"),
          "drive.conflict.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/trash",
          "DriveTrash",
          "云盘回收站",
          "查看已删除的云盘对象，并执行恢复或永久删除。",
          () => import("@/views/console/apps/DriveTrash.vue"),
          "drive.trash.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/settings",
          "DriveSettings",
          "云盘设置",
          "管理云盘配额、策略、备份绑定和产品级设置。",
          () => import("@/views/console/apps/DriveSettings.vue"),
          "drive.settings.read",
          hidden("/apps/drive")
        ),
        page(
          "documents",
          "Documents",
          "文档",
          "创建、组织和管理文档资源。",
          () => import("@/views/console/apps/Documents.vue"),
          "document.read",
          { icon: "ep:document" }
        ),
        page(
          "documents/:id/edit",
          "DocumentEditor",
          "文档编辑器",
          "编辑单个文档的正文、版本和发布状态。",
          () => import("@/views/console/apps/DocumentEditor.vue"),
          "document.read",
          hidden("/apps/documents")
        ),
        page("media", "Media", "媒体", "浏览和播放视频、音频等媒体资源，并管理媒体相关体验。", () => import("@/views/console/apps/Media.vue"), "media.read"),
        page("planning", "Planning", "计划", "管理项目、任务、计划与时间安排。", () => import("@/views/console/apps/Planning.vue"), "planning.read"),
        page("finance", "Finance", "财务", "管理账户、交易、预算、对账与个人财务数据。", () => import("@/views/console/apps/Finance.vue"), "finance.read"),
        page("private-notes", "PrivateNotes", "私密笔记", "管理需要更高隐私保护的私密笔记与同步状态。", () => import("@/views/console/apps/PrivateNotes.vue"), "private_note.read"),
        page("passwords", "Passwords", "密码库", "管理密码条目、安全状态、设备访问与密码健康检查。", () => import("@/views/console/apps/Passwords.vue"), "password.read"),
        page("ai", "AI", "AI", "提供 AI 对话、内容辅助、模型与智能能力入口。", () => import("@/views/console/apps/Ai.vue"), "ai.read"),
        page("sharing", "Sharing", "分享", "管理资源分享、访问范围、链接和共享状态。", () => import("@/views/console/apps/Sharing.vue"), "share.read"),
        page("analytics", "Analytics", "数据分析", "查看资源、存储、应用与使用情况的数据分析和报表。", () => import("@/views/console/apps/Analytics.vue"), "analytics.read"),
        page("automation", "Automation", "自动化", "配置自动化规则、触发条件、动作与执行结果。", () => import("@/views/console/apps/Automation.vue"), "automation.read"),
        page("plugins", "PluginApps", "插件应用", "查看由插件提供的应用入口，并进入已启用的插件应用。", () => import("@/views/console/apps/Plugins.vue"), "app.read"),
        page("plugins/:appId", "PluginApp", "插件应用页面", "承载插件提供的独立应用页面；实际业务内容由对应插件实现。", () => import("@/views/console/apps/PluginApp.vue"), undefined, hidden("/apps/plugins"))
      ],
      "/apps/overview"
    ),

    workspace(
      "/system",
      "System",
      { title: "系统", icon: "ep:setting" },
      [
        directory("access", "SystemAccess", "访问控制", "ep:lock", "/system/access/users", [
          page("users", "SystemUsers", "用户管理", "管理平台用户、账号状态、角色分配和会话治理。", () => import("@/views/console/system/access/Users.vue"), "user.read", { icon: "ep:user" }),
          page("roles-permissions", "SystemRolesPermissions", "角色与权限", "统一管理角色、权限目录以及角色与权限之间的映射。", () => import("@/views/console/system/access/RolesPermissions.vue"), "role.read", { icon: "ep:key" }),
          page("authentication", "SystemAuthentication", "身份认证", "配置平台登录、Step-up、恢复与身份认证策略。", () => import("@/views/console/system/access/Authentication.vue"), "security.authentication.read", { icon: "ep:key" })
        ]),
        directory("integrations", "SystemIntegrations", "集成", "ep:connection", "/system/integrations/plugins", [
          page("plugins", "SystemPlugins", "插件管理", "管理插件安装、启停、升级、权限和生命周期。", () => import("@/views/console/system/integrations/Plugins.vue"), "integration.read"),
          page("external", "SystemExternalIntegrations", "外部集成", "管理 Connector、Metadata Source、Webhook 等平台外部连接。", () => import("@/views/console/system/integrations/External.vue"), "integration.read"),
          page("events", "SystemEventDelivery", "事件投递", "查看事件投递状态、失败重试和平台事件链路。", () => import("@/views/console/system/integrations/Events.vue"), "integration.read")
        ]),
        directory("communications", "SystemCommunications", "通知与审计", "ep:bell", "/system/communications/notifications", [
          page("notifications", "SystemNotifications", "通知中心", "管理系统通知规则、渠道、模板与投递策略。", () => import("@/views/console/system/communications/Notifications.vue"), "notification.read", { icon: "ep:bell" }),
          page("audit", "SystemAudit", "审计日志", "查询平台审计事件，追踪用户、系统和高风险操作。", () => import("@/views/console/system/communications/Audit.vue"), "audit.read")
        ]),
        directory("settings", "SystemSettings", "平台配置", "ep:setting", "/system/settings/parameters", [
          page("parameters", "SystemParameters", "系统参数", "管理平台级系统参数及其生效范围。", () => import("@/views/console/system/settings/Parameters.vue"), "platform.read", { showParent: true, icon: "ep:setting" })
        ]),
        directory("operations", "SystemOperations", "运维", "ep:monitor", "/system/operations/health", [
          page("health", "SystemHealth", "系统健康", "查看应用、数据库、任务、事件和存储等核心组件健康状态。", () => import("@/views/console/system/operations/Health.vue"), "system.health.read", { icon: "ep:monitor" }),
          page("diagnostics", "SystemDiagnostics", "系统诊断", "提供面向运维人员的低层诊断信息和故障排查入口。", () => import("@/views/console/system/operations/Diagnostics.vue"), "system.diagnostics.read", { icon: "ep:warning" })
        ])
      ],
      "/system/access/users"
    ),

    {
      path: "/account",
      name: "Account",
      component: WorkspaceView,
      redirect: "/account/profile",
      meta: { title: "个人账户", showLink: false },
      children: [
        page("profile", "AccountProfile", "个人资料", "管理当前账号的头像、昵称、语言和基础资料。", () => import("@/views/account/Profile.vue"), "account.self.read"),
        page("preferences", "AccountPreferences", "偏好设置", "管理当前账号的界面、语言、时区和使用偏好。", () => import("@/views/account/Preferences.vue"), "account.preference.read"),
        page("notifications", "AccountNotifications", "通知设置", "管理当前账号接收哪些通知以及通知偏好。", () => import("@/views/account/Notifications.vue"), "account.notification.read"),
        page("security", "AccountSecurity", "账户安全", "管理当前账号的密码、恢复方式和安全状态。", () => import("@/views/account/Security.vue"), "account.security.read"),
        page("sessions", "AccountSessions", "登录会话", "查看当前账号的登录会话，并管理需要退出或撤销的会话。", () => import("@/views/account/Sessions.vue"), "account.security.read"),
        page("api-tokens", "AccountApiTokens", "API 令牌", "管理当前账号创建的 API 令牌及其访问范围和生命周期。", () => import("@/views/account/ApiTokens.vue"))
      ]
    }
  ]
} satisfies RouteConfigsTable;
