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
    meta: { title, icon, showParent: true, menuGroup: true },
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
      {
        title: "menus.dashboard",
        icon: "ep:data-analysis",
        capability: "dashboard.read"
      },
      [
        page(
          "",
          "DashboardHome",
          "menus.dashboard",
          "menuDescriptions.dashboard",
          () => import("@/views/console/Dashboard.vue"),
          "dashboard.read",
          { icon: "ep:data-analysis" }
        )
      ]
    ),

    workspace(
      "/resources",
      "Resources",
      { title: "menus.resources", icon: "ep:files" },
      [
        page(
          "library",
          "ResourceLibrary",
          "menus.resourceLibrary",
          "menuDescriptions.resourceLibrary",
          () => import("@/views/console/resources/Library.vue"),
          "resource.read",
          { icon: "ep:collection" }
        ),
        page(
          "library/:resourceId",
          "ResourceDetail",
          "menus.resourceDetail",
          "menuDescriptions.resourceDetail",
          () => import("@/views/console/resources/ResourceDetail.vue"),
          "resource.read",
          hidden("/resources/library")
        ),
        page(
          "library/collections",
          "ResourceCollections",
          "menus.resourceCollections",
          "menuDescriptions.resourceCollections",
          () => import("@/views/console/resources/Collections.vue"),
          "collection.read",
          hidden("/resources/library")
        ),
        page(
          "library/search",
          "ResourceSearch",
          "menus.resourceSearch",
          "menuDescriptions.resourceSearch",
          () => import("@/views/console/resources/Search.vue"),
          "search.use",
          hidden("/resources/library")
        ),
        page(
          "add",
          "AddResource",
          "menus.addResource",
          "menuDescriptions.addResource",
          () => import("@/views/console/resources/Add.vue"),
          "ingestion.read",
          { icon: "ep:upload" }
        ),
        page(
          "activity",
          "ActivityCenter",
          "menus.activityCenter",
          "menuDescriptions.activityCenter",
          () => import("@/views/console/resources/Activity.vue"),
          "activity.read",
          { icon: "ep:histogram" }
        ),
        page(
          "activity/:activityId",
          "ActivityDetail",
          "menus.activityDetail",
          "menuDescriptions.activityDetail",
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
      { title: "menus.storage", icon: "ep:box" },
      [
        page(
          "overview",
          "StorageOverview",
          "menus.storageOverview",
          "menuDescriptions.storageOverview",
          () => import("@/views/console/storage/Overview.vue"),
          "storage.read",
          { icon: "ep:data-analysis" }
        ),
        page(
          "providers",
          "StorageProviders",
          "menus.storageProviders",
          "menuDescriptions.storageProviders",
          () => import("@/views/console/storage/Providers.vue"),
          "storage.provider.read",
          { icon: "ep:setting" }
        ),
        page(
          "providers/:providerId",
          "StorageProviderDetail",
          "menus.storageProviderDetail",
          "menuDescriptions.storageProviderDetail",
          () => import("@/views/console/storage/ProviderDetail.vue"),
          "storage.provider.read",
          hidden("/storage/providers")
        ),
        page(
          "policy",
          "StoragePolicy",
          "menus.storagePolicy",
          "menuDescriptions.storagePolicy",
          () => import("@/views/console/storage/Policy.vue"),
          "storage.policy.read"
        ),
        page(
          "archive",
          "StorageArchive",
          "menus.archiveManagement",
          "menuDescriptions.archiveManagement",
          () => import("@/views/console/storage/Archive.vue"),
          "storage.archive.read"
        ),
        page(
          "backup",
          "StorageBackup",
          "menus.backupManagement",
          "menuDescriptions.backupManagement",
          () => import("@/views/console/storage/Backup.vue"),
          "backup.read"
        ),
        page(
          "maintenance",
          "StorageMaintenance",
          "menus.storageMaintenance",
          "menuDescriptions.storageMaintenance",
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
      { title: "menus.apps", icon: "ep:grid" },
      [
        page(
          "drive",
          "Drive",
          "menus.drive",
          "menuDescriptions.drive",
          () => import("@/views/console/apps/Drive.vue"),
          "drive.space.read",
          { icon: "ep:folder" }
        ),
        page(
          "drive/nodes/:nodeId",
          "DriveNodeDetail",
          "menus.driveNodeDetail",
          "menuDescriptions.driveNodeDetail",
          () => import("@/views/console/apps/DriveNodeDetail.vue"),
          "drive.file.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/transfers",
          "DriveTransfers",
          "menus.driveTransfers",
          "menuDescriptions.driveTransfers",
          () => import("@/views/console/apps/DriveTransfers.vue"),
          "drive.transfer.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/sync",
          "DriveSync",
          "menus.driveSync",
          "menuDescriptions.driveSync",
          () => import("@/views/console/apps/DriveSync.vue"),
          "drive.sync.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/conflicts",
          "DriveConflicts",
          "menus.driveConflicts",
          "menuDescriptions.driveConflicts",
          () => import("@/views/console/apps/DriveConflicts.vue"),
          "drive.conflict.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/trash",
          "DriveTrash",
          "menus.driveTrash",
          "menuDescriptions.driveTrash",
          () => import("@/views/console/apps/DriveTrash.vue"),
          "drive.trash.read",
          hidden("/apps/drive")
        ),
        page(
          "drive/settings",
          "DriveSettings",
          "menus.driveSettings",
          "menuDescriptions.driveSettings",
          () => import("@/views/console/apps/DriveSettings.vue"),
          "drive.settings.read",
          hidden("/apps/drive")
        ),
        page(
          "documents",
          "Documents",
          "menus.documents",
          "menuDescriptions.documents",
          () => import("@/views/console/apps/Documents.vue"),
          "document.read",
          { icon: "ep:document" }
        ),
        page(
          "documents/:id/edit",
          "DocumentEditor",
          "menus.documentEditor",
          "menuDescriptions.documentEditor",
          () => import("@/views/console/apps/DocumentEditor.vue"),
          "document.read",
          hidden("/apps/documents")
        ),
        page(
          "media",
          "Media",
          "menus.media",
          "menuDescriptions.media",
          () => import("@/views/console/apps/Media.vue"),
          "media.read"
        ),
        page(
          "planning",
          "Planning",
          "menus.planning",
          "menuDescriptions.planning",
          () => import("@/views/console/apps/Planning.vue"),
          "planning.read"
        ),
        page(
          "finance",
          "Finance",
          "menus.finance",
          "menuDescriptions.finance",
          () => import("@/views/console/apps/Finance.vue"),
          "finance.read"
        ),
        page(
          "private-notes",
          "PrivateNotes",
          "menus.privateNotes",
          "menuDescriptions.privateNotes",
          () => import("@/views/console/apps/PrivateNotes.vue"),
          "private_note.read"
        ),
        page(
          "passwords",
          "Passwords",
          "menus.passwords",
          "menuDescriptions.passwords",
          () => import("@/views/console/apps/Passwords.vue"),
          "password.read"
        ),
        page(
          "ai",
          "AI",
          "menus.ai",
          "menuDescriptions.ai",
          () => import("@/views/console/apps/Ai.vue"),
          "ai.read"
        ),
        page(
          "sharing",
          "Sharing",
          "menus.sharing",
          "menuDescriptions.sharing",
          () => import("@/views/console/apps/Sharing.vue"),
          "share.read"
        ),
        page(
          "analytics",
          "Analytics",
          "menus.analytics",
          "menuDescriptions.analytics",
          () => import("@/views/console/apps/Analytics.vue"),
          "analytics.read"
        ),
        page(
          "automation",
          "Automation",
          "menus.automation",
          "menuDescriptions.automation",
          () => import("@/views/console/apps/Automation.vue"),
          "automation.read"
        ),
        page(
          "plugins/:appId",
          "PluginApp",
          "menus.pluginApp",
          "menuDescriptions.pluginApp",
          () => import("@/views/console/apps/PluginApp.vue"),
          undefined,
          { showLink: false }
        )
      ],
      "/apps/drive"
    ),

    workspace(
      "/system",
      "System",
      { title: "menus.system", icon: "ep:setting" },
      [
        directory(
          "access",
          "SystemAccess",
          "menus.accessControl",
          "ep:lock",
          "/system/access/users",
          [
            page(
              "users",
              "SystemUsers",
              "menus.userManagement",
              "menuDescriptions.userManagement",
              () => import("@/views/console/system/access/Users.vue"),
              "system.user.read",
              { icon: "ep:user" }
            ),
            page(
              "roles-permissions",
              "SystemRolesPermissions",
              "menus.rolesPermissions",
              "menuDescriptions.rolesPermissions",
              () =>
                import("@/views/console/system/access/RolesPermissions.vue"),
              "system.role.read",
              { icon: "ep:key" }
            ),
            page(
              "authentication",
              "SystemAuthentication",
              "menus.authentication",
              "menuDescriptions.authentication",
              () => import("@/views/console/system/access/Authentication.vue"),
              "security.authentication.read",
              { icon: "ep:key" }
            )
          ]
        ),
        directory(
          "integrations",
          "SystemIntegrations",
          "menus.integrations",
          "ep:connection",
          "/system/integrations/apps",
          [
            page(
              "apps",
              "SystemApps",
              "menus.appManagement",
              "menuDescriptions.appManagement",
              () => import("@/views/console/system/integrations/Apps.vue"),
              "integration.read"
            ),
            page(
              "external",
              "SystemExternalIntegrations",
              "menus.externalIntegrations",
              "menuDescriptions.externalIntegrations",
              () => import("@/views/console/system/integrations/External.vue"),
              "integration.read"
            ),
            page(
              "events",
              "SystemEventDelivery",
              "menus.eventDelivery",
              "menuDescriptions.eventDelivery",
              () => import("@/views/console/system/integrations/Events.vue"),
              "integration.read"
            )
          ]
        ),
        directory(
          "communications",
          "SystemCommunications",
          "menus.communications",
          "ep:bell",
          "/system/communications/notifications",
          [
            page(
              "notifications",
              "SystemNotifications",
              "menus.notifications",
              "menuDescriptions.notifications",
              () =>
                import(
                  "@/views/console/system/communications/Notifications.vue"
                ),
              "notification.read",
              { icon: "ep:bell" }
            ),
            page(
              "audit",
              "SystemAudit",
              "menus.auditLog",
              "menuDescriptions.auditLog",
              () => import("@/views/console/system/communications/Audit.vue"),
              "audit.read"
            )
          ]
        ),
        directory(
          "settings",
          "SystemSettings",
          "menus.platformSettings",
          "ep:setting",
          "/system/settings/parameters",
          [
            page(
              "parameters",
              "SystemParameters",
              "menus.systemParameters",
              "menuDescriptions.systemParameters",
              () => import("@/views/console/system/settings/Parameters.vue"),
              "platform.read",
              { showParent: true, icon: "ep:setting" }
            )
          ]
        ),
        directory(
          "operations",
          "SystemOperations",
          "menus.operations",
          "ep:monitor",
          "/system/operations/health",
          [
            page(
              "health",
              "SystemHealth",
              "menus.systemHealth",
              "menuDescriptions.systemHealth",
              () => import("@/views/console/system/operations/Health.vue"),
              "system.health.read",
              { icon: "ep:monitor" }
            ),
            page(
              "diagnostics",
              "SystemDiagnostics",
              "menus.systemDiagnostics",
              "menuDescriptions.systemDiagnostics",
              () => import("@/views/console/system/operations/Diagnostics.vue"),
              "system.diagnostics.read",
              { icon: "ep:warning" }
            )
          ]
        )
      ],
      "/system/access/users"
    ),

    {
      path: "/account",
      name: "Account",
      component: WorkspaceView,
      redirect: "/account/profile",
      meta: { title: "menus.account", showLink: false },
      children: [
        page(
          "profile",
          "AccountProfile",
          "menus.profile",
          "menuDescriptions.profile",
          () => import("@/views/account/Profile.vue"),
          "account.self.read"
        ),
        page(
          "preferences",
          "AccountPreferences",
          "menus.preferences",
          "menuDescriptions.preferences",
          () => import("@/views/account/Preferences.vue"),
          "account.preference.read"
        ),
        page(
          "notifications",
          "AccountNotifications",
          "menus.accountNotifications",
          "menuDescriptions.accountNotifications",
          () => import("@/views/account/Notifications.vue"),
          "account.notification.read"
        ),
        page(
          "security",
          "AccountSecurity",
          "menus.accountSecurity",
          "menuDescriptions.accountSecurity",
          () => import("@/views/account/Security.vue"),
          "account.security.read"
        ),
        page(
          "sessions",
          "AccountSessions",
          "menus.sessions",
          "menuDescriptions.sessions",
          () => import("@/views/account/Sessions.vue"),
          "account.security.read"
        ),
        page(
          "api-tokens",
          "AccountApiTokens",
          "menus.apiTokens",
          "menuDescriptions.apiTokens",
          () => import("@/views/account/ApiTokens.vue")
        )
      ]
    }
  ]
} satisfies RouteConfigsTable;
