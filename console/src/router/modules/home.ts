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
    subsystem("/resource-center", "ResourceCenter", "资源中心", "ep:files", [
      moduleRoute({ path: "/resources", name: "Resources", title: "资源管理", description: "管理资源、标题、标签和生命周期。", endpoint: "/resources", createEndpoint: "/resources", deleteEndpoint: "/resources", actions: [{ name: "archive", label: "归档", path: "/resources/{id}/actions/archive", confirm: "确定归档此资源吗？" }, { name: "restore", label: "恢复", path: "/resources/{id}/actions/restore" }], createFields: [{ name: "type", label: "类型", required: true, defaultValue: "OTHER" }, { name: "title", label: "标题", required: true }, { name: "locale", label: "语言", required: true, defaultValue: "zh-CN" }], columns: ["id", "resourceType", "status", "createdAt"], icon: "ep:files" }),
      moduleRoute({ path: "/documents", name: "Documents", title: "文档管理", description: "管理个人文档和工作副本。", endpoint: "/documents", createEndpoint: "/documents", createFields: [{ name: "title", label: "标题", required: true }, { name: "kind", label: "类型", required: true, defaultValue: "DOCUMENT" }, { name: "locale", label: "语言", defaultValue: "zh-CN" }, { name: "content", label: "内容" }], columns: ["id", "title", "status", "updatedAt"], icon: "ep:document" }),
      moduleRoute({ path: "/collections", name: "Collections", title: "收藏集合", description: "管理资源集合及其成员。", endpoint: "/collections", createEndpoint: "/collections", createFields: [{ name: "name", label: "名称", required: true }, { name: "description", label: "描述" }], columns: ["id", "name", "description", "createdAt"], icon: "ep:collection" }),
      moduleRoute({ path: "/activity", name: "Activity", title: "资源活动", description: "查看资源访问和操作活动。", endpoint: "/activity", columns: ["id", "resourceId", "type", "createdAt"], icon: "ep:histogram" })
    ]),
    subsystem("/content-center", "ContentCenter", "内容与媒体", "ep:video-camera", [
      moduleRoute({ path: "/media", name: "Media", title: "媒体库", description: "查看媒体资源和播放内容。", endpoint: "/media/subjects", columns: ["id", "title", "mediaType", "status"], icon: "ep:video-camera" }),
      moduleRoute({ path: "/reading", name: "Reading", title: "阅读库", description: "管理阅读作品、版本和阅读内容。", endpoint: "/reading/works", columns: ["id", "title", "status", "createdAt"], icon: "ep:reading" }),
      moduleRoute({ path: "/music", name: "Music", title: "音乐库", description: "查看音乐播放列表和播放记录。", endpoint: "/music/playlists", columns: ["id", "name", "status", "createdAt"], icon: "ep:headset" }),
      moduleRoute({ path: "/photos", name: "Photos", title: "照片管理", description: "浏览照片和媒体元数据。", endpoint: "/photos/timeline", columns: ["id", "title", "status", "createdAt"], icon: "ep:picture" }),
      moduleRoute({ path: "/games", name: "Games", title: "游戏档案", description: "管理游戏、版本和数字资产。", endpoint: "/games", columns: ["id", "name", "status", "createdAt"], icon: "ep:monitor" })
    ]),
    subsystem("/storage-center", "StorageCenter", "存储与云盘", "ep:box", [
      moduleRoute({ path: "/drive", name: "Drive", title: "个人云盘", description: "查看云盘空间和文件节点。", endpoint: "/drive/spaces", columns: ["id", "name", "status", "createdAt"], icon: "ep:folder-opened" }),
      moduleRoute({ path: "/storage", name: "Storage", title: "存储管理", description: "管理存储 Provider 和恢复请求。", endpoint: "/admin/storage-providers", columns: ["id", "name", "status", "createdAt"], icon: "ep:box" }),
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
      moduleRoute({ path: "/sessions", name: "Sessions", title: "会话安全", description: "查看当前账号的有效登录会话。", endpoint: "/me/sessions", columns: ["id", "createdAt", "expiresAt", "lastSeenAt"], icon: "ep:monitor" }),
      moduleRoute({ path: "/security", name: "Security", title: "安全中心", description: "查看安全验证挑战和会话状态。", endpoint: "/security/verification-challenges", columns: ["id", "status", "createdAt", "expiresAt"], icon: "ep:warning" }),
      moduleRoute({ path: "/users", name: "Users", title: "用户管理", description: "管理用户状态、角色和账号信息。", endpoint: "/admin/users", columns: ["id", "username", "displayName", "status"], icon: "ep:user" }),
      moduleRoute({ path: "/roles", name: "Roles", title: "角色管理", description: "查看角色及其权限配置。", endpoint: "/admin/roles", columns: ["id", "code", "name", "description"], icon: "ep:key" }),
      moduleRoute({ path: "/permissions", name: "Permissions", title: "权限管理", description: "查看系统权限注册表。", endpoint: "/admin/permissions", columns: ["code", "name", "description"], icon: "ep:lock" })
    ])
  ]
} satisfies RouteConfigsTable;
