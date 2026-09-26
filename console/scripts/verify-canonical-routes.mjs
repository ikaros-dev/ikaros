import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const home = await readFile(new URL("../src/router/modules/home.ts", import.meta.url), "utf8");
const escape = value => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
const workspaces = [
  ["Dashboard", "/dashboard"],
  ["Resources", "/resources"],
  ["Storage", "/storage"],
  ["Apps", "/apps"],
  ["System", "/system"]
];
const pages = [
  ["DashboardHome", ""],
  ["ResourceLibrary", "library"], ["ResourceDetail", "library/:resourceId"], ["ResourceCollections", "library/collections"], ["ResourceSearch", "library/search"], ["AddResource", "add"], ["ActivityCenter", "activity"], ["ActivityDetail", "activity/:activityId"],
  ["StorageOverview", "overview"], ["StorageProviders", "providers"], ["StorageProviderDetail", "providers/:providerId"], ["StoragePolicy", "policy"], ["StorageArchive", "archive"], ["StorageBackup", "backup"], ["StorageMaintenance", "maintenance"],
  ["Drive", "drive"], ["DriveNodeDetail", "drive/nodes/:nodeId"], ["DriveTransfers", "drive/transfers"], ["DriveSync", "drive/sync"], ["DriveConflicts", "drive/conflicts"], ["DriveTrash", "drive/trash"], ["DriveSettings", "drive/settings"], ["Documents", "documents"], ["DocumentEditor", "documents/:id/edit"], ["Media", "media"], ["Planning", "planning"], ["Finance", "finance"], ["PrivateNotes", "private-notes"], ["Passwords", "passwords"], ["AI", "ai"], ["Sharing", "sharing"], ["Analytics", "analytics"], ["Automation", "automation"],
  ["SystemUsers", "users"], ["SystemRolesPermissions", "roles-permissions"], ["SystemAuthentication", "authentication"], ["SystemApps", "apps"], ["SystemExternalIntegrations", "external"], ["SystemEventDelivery", "events"], ["SystemNotifications", "notifications"], ["SystemAudit", "audit"], ["SystemParameters", "parameters"], ["SystemHealth", "health"], ["SystemDiagnostics", "diagnostics"],
  ["AccountProfile", "profile"], ["AccountPreferences", "preferences"], ["AccountNotifications", "notifications"], ["AccountSecurity", "security"], ["AccountSessions", "sessions"], ["AccountApiTokens", "api-tokens"]
];

const workspaceMatches = [...home.matchAll(/workspace\(\s*"([^"]+)",\s*"([^"]+)"/g)].map(match => [match[2], match[1]]);
assert.deepEqual(workspaceMatches, workspaces);

for (const [name, path] of pages) {
  const pattern = new RegExp(`page\\(\\s*"${escape(path)}",\\s*"${escape(name)}"`);
  assert.match(home, pattern, `missing page ${name} at ${path}`);
}

for (const target of ["/resources/library", "/storage/overview", "/apps/drive", "/system/access/users", "/system/integrations/apps", "/system/communications/notifications", "/system/settings/parameters", "/system/operations/health"]) {
  assert.ok(home.includes(`"${target}"`), `missing redirect target ${target}`);
}

assert.doesNotMatch(home, /workspace\(\s*"\/(library|add|activity)"/);
assert.doesNotMatch(home, /\/apps\/overview|AppsOverview|appsOverview/);
assert.doesNotMatch(home, /page\(\s*"plugins",\s*"PluginApps"/);
assert.doesNotMatch(home, /plugins\/:appId|PluginApp|pluginApp/);
assert.doesNotMatch(home, /\/system\/integrations\/plugins/);
assert.doesNotMatch(home, /@\/views\/(dashboard|resources|storage|apps|drive|documents|media|planning|finance|notes|password|ai|sharing|analytics|ingestion|integration|security|communications|operations|platform|workbench)\//);
assert.match(home, /redirect: "\/dashboard"/);
console.log("canonical console route assertions passed");
