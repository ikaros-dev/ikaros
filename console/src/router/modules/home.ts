const Layout = () => import("@/layout/index.vue");
const WorkspaceView = () => import("@/layout/components/WorkspaceView.vue");
const ModulePage = () => import("@/views/modules/ModulePage.vue");
const OverviewPage = () => import("@/views/dashboard/index.vue");
const LibraryPage = () => import("@/views/resources/index.vue");
const AddContentPage = () => import("@/views/ingestion/index.vue");
const ActivityPage = () => import("@/views/workbench/Activity.vue");

type WorkspaceOptions = { title: string; icon: string; capability: string };

const workspaceRanks: Record<string, number> = {
  Overview: 10,
  Library: 20,
  AddContent: 30,
  Activity: 40,
  Storage: 50,
  Apps: 60,
  System: 70
};

function workspace(path: string, name: string, options: WorkspaceOptions, children: any[]) {
  return {
    path,
    name,
    component: WorkspaceView,
    meta: { ...options, workspace: name, showParent: true, rank: workspaceRanks[name] },
    children
  };
}

const page = (path: string, name: string, title: string, component: any, capability: string, icon = "ep:document") => ({
  path,
  name,
  component,
  meta: { title, icon, capability }
});

const app = (path: string, name: string, title: string, component: any, capability: string) =>
  page(path, name, title, component, capability, "ep:grid");

const applicationPages = [
  app("drive", "Drive", "Drive", () => import("@/views/drive/index.vue"), "drive.space.read"),
  page("drive/nodes/:nodeId", "DriveNodeDetail", "Drive 文件详情", () => import("@/views/drive/NodeDetail.vue"), "drive.file.read"),
  page("drive/transfers", "DriveTransfers", "Drive 传输", () => import("@/views/drive/Operations.vue"), "drive.transfer.read"),
  page("drive/sync", "DriveSync", "Drive 同步", () => import("@/views/drive/Operations.vue"), "drive.sync.read"),
  page("drive/conflicts", "DriveConflicts", "Drive 冲突", () => import("@/views/drive/Conflicts.vue"), "drive.conflict.read"),
  page("drive/trash", "DriveTrash", "Drive 回收站", () => import("@/views/drive/Operations.vue"), "drive.trash.read"),
  page("drive/settings", "DriveSettings", "Drive 设置", () => import("@/views/drive/Operations.vue"), "drive.settings.read"),
  app("documents", "Documents", "Documents", () => import("@/views/documents/index.vue"), "document.read"),
  page("documents/:id/edit", "DocumentEditor", "文档编辑器", () => import("@/views/documents/Editor.vue"), "document.read"),
  app("media", "Media", "Media", () => import("@/views/media/index.vue"), "media.read"),
  app("planning", "Planning", "Planning", () => import("@/views/planning/Projects.vue"), "planning.read"),
  app("finance", "Finance", "Finance", () => import("@/views/finance/index.vue"), "finance.read"),
  app("private-notes", "PrivateNotes", "Private Notes", () => import("@/views/notes/index.vue"), "private_note.read"),
  app("passwords", "Passwords", "Passwords", () => import("@/views/password/index.vue"), "password.read"),
  app("ai", "Ai", "AI", () => import("@/views/ai/Assistant.vue"), "ai.read"),
  app("sharing", "Sharing", "Sharing", () => import("@/views/sharing/index.vue"), "share.read"),
  app("analytics", "Analytics", "Analytics", () => import("@/views/analytics/index.vue"), "analytics.read"),
  app("automation", "Automation", "Automation", () => import("@/views/integration/index.vue"), "automation.read")
];

export default {
  path: "/",
  name: "IkarosConsole",
  component: Layout,
  redirect: "/overview",
  meta: { title: "Ikaros Console", icon: "ep:menu" },
  children: [
    workspace("/overview", "Overview", { title: "Overview", icon: "ep:data-analysis", capability: "dashboard.read" }, [
      page("", "OverviewHome", "Overview", OverviewPage, "dashboard.read", "ep:data-analysis")
    ]),
    workspace("/library", "Library", { title: "Library", icon: "ep:files", capability: "resource.read" }, [
      page("", "LibraryHome", "Library", LibraryPage, "resource.read", "ep:files"),
      page(":resourceId", "ResourceDetail", "Resource Detail", () => import("@/views/resources/Detail.vue"), "resource.read"),
      page("collections", "Collections", "Collections", () => import("@/views/collections/index.vue"), "collection.read", "ep:collection"),
      page("search", "LibrarySearch", "Library Search", () => import("@/views/workbench/Search.vue"), "search.use", "ep:search")
    ]),
    workspace("/add", "AddContent", { title: "Add Content", icon: "ep:upload", capability: "ingestion.read" }, [
      page("", "AddContentHome", "Add Content", AddContentPage, "ingestion.read", "ep:upload")
    ]),
    workspace("/activity", "Activity", { title: "Activity", icon: "ep:histogram", capability: "activity.read" }, [
      page("", "ActivityHome", "Activity", ActivityPage, "activity.read", "ep:histogram"),
      page(":activityId", "ActivityDetail", "Activity Detail", ActivityPage, "activity.read")
    ]),
    workspace("/storage", "Storage", { title: "Storage", icon: "ep:box", capability: "storage.read" }, [
      page("", "StorageHome", "Storage", () => import("@/views/storage/Tiers.vue"), "storage.read", "ep:box"),
      page("providers", "StorageProviders", "Storage Providers", () => import("@/views/storage/Tiers.vue"), "storage.provider.read", "ep:setting"),
      page("providers/:providerId", "StorageProviderDetail", "Storage Provider Detail", () => import("@/views/storage/Tiers.vue"), "storage.provider.read"),
      page("policy", "StoragePolicy", "Storage Policy", () => import("@/views/storage/Tiers.vue"), "storage.policy.read"),
      page("archive", "StorageArchive", "Archive & Restore", () => import("@/views/storage/Archive.vue"), "storage.archive.read", "ep:refresh-left"),
      page("maintenance", "StorageMaintenance", "Storage Maintenance", () => import("@/views/storage/Cache.vue"), "storage.maintenance.read", "ep:tools"),
      page("backup", "StorageBackup", "Backup", () => import("@/views/storage/Backup.vue"), "backup.read", "ep:files")
    ]),
    workspace("/apps", "Apps", { title: "Apps", icon: "ep:grid", capability: "app.read" }, [
      page("", "AppsHome", "Apps", ModulePage, "app.read", "ep:grid"),
      ...applicationPages
    ]),
    workspace("/system", "System", { title: "System", icon: "ep:setting", capability: "system.read" }, [
      page("", "SystemHome", "System", ModulePage, "system.read", "ep:setting"),
      page("access", "SystemAccess", "Access", () => import("@/views/security/Users.vue"), "identity.read", "ep:lock"),
      page("access/users", "SystemUsers", "Users", () => import("@/views/security/Users.vue"), "user.read", "ep:user"),
      page("access/roles", "SystemRoles", "Roles", () => import("@/views/security/Permissions.vue"), "role.read", "ep:key"),
      page("access/permissions", "SystemPermissions", "Permissions", () => import("@/views/security/Permissions.vue"), "permission.read", "ep:lock"),
      page("access/authentication", "SystemAuthentication", "Authentication", () => import("@/views/security/Authentication.vue"), "security.authentication.read", "ep:key"),
      page("audit", "SystemAudit", "Audit", () => import("@/views/communications/Audit.vue"), "audit.read"),
      page("integrations", "SystemIntegrations", "Integrations", () => import("@/views/integration/index.vue"), "integration.read", "ep:connection"),
      page("notifications", "SystemNotifications", "Notifications", () => import("@/views/communications/Notifications.vue"), "notification.read", "ep:bell"),
      page("settings", "SystemSettings", "Settings", () => import("@/views/platform/Parameters.vue"), "platform.read", "ep:setting"),
      page("health", "SystemHealth", "Health", () => import("@/views/operations/Health.vue"), "system.health.read", "ep:monitor"),
      page("diagnostics", "SystemDiagnostics", "Diagnostics", () => import("@/views/operations/Background.vue"), "system.diagnostics.read", "ep:warning")
    ])
  ]
} satisfies RouteConfigsTable;
