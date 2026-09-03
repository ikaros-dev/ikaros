import { $t } from "@/plugins/i18n";
const { VITE_HIDE_HOME } = import.meta.env;
const Layout = () => import("@/layout/index.vue");

export default {
  path: "/",
  name: "Home",
  component: Layout,
  redirect: "/welcome",
  meta: {
    icon: "ep/home-filled",
    title: $t("menus.pureHome"),
    rank: 0
  },
  children: [
    {
      path: "/welcome",
      name: "Welcome",
      component: () => import("@/views/welcome/index.vue"),
      meta: {
        title: $t("menus.pureHome"),
        showLink: VITE_HIDE_HOME === "true" ? false : true
      }
    },
    { path: "/resources", name: "Resources", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "资源管理", description: "管理资源、标题、标签和生命周期。", endpoint: "/resources", columns: ["id", "resourceType", "status", "createdAt"], icon: "ep:files" } },
    { path: "/documents", name: "Documents", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "文档管理", description: "管理个人文档和工作副本。", endpoint: "/documents", columns: ["id", "title", "status", "updatedAt"], icon: "ep:document" } },
    { path: "/drive", name: "Drive", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "个人云盘", description: "查看云盘空间和文件节点。", endpoint: "/drive/spaces", columns: ["id", "name", "status", "createdAt"], icon: "ep:folder-opened" } },
    { path: "/rooms", name: "Rooms", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "协作房间", description: "查看共享协作房间及其状态。", endpoint: "/rooms", columns: ["id", "name", "status", "createdAt"], icon: "ep:chat-line-round" } },
    { path: "/finance", name: "Finance", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "个人财务", description: "查看账本、账户和财务记录。", endpoint: "/finance/ledgers", columns: ["id", "name", "currency", "createdAt"], icon: "ep:money" } },
    { path: "/media", name: "Media", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "媒体库", description: "查看媒体资源和播放内容。", endpoint: "/media/subjects", columns: ["id", "title", "mediaType", "status"], icon: "ep:video-camera" } }
    ,{ path: "/planning", name: "Planning", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "生产力与计划", description: "管理项目、任务和目标。", endpoint: "/planning/projects", columns: ["id", "name", "status", "createdAt"], icon: "ep:calendar" } }
    ,{ path: "/notes", name: "Notes", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "私密笔记", description: "管理个人私密笔记和知识内容。", endpoint: "/private-notes/vaults", columns: ["id", "name", "status", "updatedAt"], icon: "ep:notebook" } }
    ,{ path: "/passwords", name: "Passwords", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "密码库", description: "管理密码条目和安全凭据。", endpoint: "/password/vaults", columns: ["id", "name", "status", "createdAt"], icon: "ep:lock" } }
    ,{ path: "/photos", name: "Photos", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "照片管理", description: "浏览照片和媒体元数据。", endpoint: "/photos/timeline", columns: ["id", "title", "status", "createdAt"], icon: "ep:picture" } }
    ,{ path: "/games", name: "Games", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "游戏档案", description: "管理游戏、版本和数字资产。", endpoint: "/games", columns: ["id", "name", "status", "createdAt"], icon: "ep:monitor" } }
    ,{ path: "/sharing", name: "Sharing", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "分享协作", description: "管理分享链接和协作空间。", endpoint: "/shares", columns: ["id", "status", "createdAt", "expiresAt"], icon: "ep:share" } }
    ,{ path: "/ingestion", name: "Ingestion", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "内容导入", description: "查看导入来源和扫描任务。", endpoint: "/ingestion/sources", columns: ["id", "name", "status", "createdAt"], icon: "ep:upload" } }
    ,{ path: "/backup", name: "Backup", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "备份恢复", description: "管理恢复点和备份验证。", endpoint: "/admin/backup/restore-points", columns: ["id", "status", "createdAt", "verifiedAt"], icon: "ep:files" } }
    ,{ path: "/security", name: "Security", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "安全中心", description: "查看安全验证挑战和会话状态。", endpoint: "/security/verification-challenges", columns: ["id", "status", "createdAt", "expiresAt"], icon: "ep:warning" } }
    ,{ path: "/users", name: "Users", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "用户管理", description: "管理用户状态、角色和账号信息。", endpoint: "/admin/users", columns: ["id", "username", "displayName", "status"], icon: "ep:user" } }
    ,{ path: "/roles", name: "Roles", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "角色管理", description: "查看角色及其权限配置。", endpoint: "/admin/roles", columns: ["id", "code", "name", "description"], icon: "ep:key" } }
    ,{ path: "/permissions", name: "Permissions", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "权限管理", description: "查看系统权限注册表。", endpoint: "/admin/permissions", columns: ["code", "name", "description"], icon: "ep:lock" } }
    ,{ path: "/tasks", name: "BackgroundTasks", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "后台任务", description: "查看后台任务、进度和执行尝试。", endpoint: "/background-tasks", columns: ["id", "type", "status", "createdAt"], icon: "ep:operation" } }
    ,{ path: "/storage", name: "Storage", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "存储管理", description: "管理存储 Provider 和恢复请求。", endpoint: "/admin/storage-providers", columns: ["id", "name", "status", "createdAt"], icon: "ep:box" } }
  ]
} satisfies RouteConfigsTable;
