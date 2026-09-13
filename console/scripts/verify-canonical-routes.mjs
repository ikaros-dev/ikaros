import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const homeRoute = await readFile(
  fileURLToPath(new URL("../src/router/modules/home.ts", import.meta.url)),
  "utf8"
);
const entrySources = await Promise.all([
  "../src/views/attachments/index.vue",
  "../src/views/communications/Notifications.vue",
  "../src/views/operations/Health.vue",
  "../src/views/drive/index.vue"
].map(path => readFile(fileURLToPath(new URL(path, import.meta.url)), "utf8")));

const workspaces = [
  ["Dashboard", "/dashboard", "dashboard.read"],
  ["Library", "/library", "resource.read"],
  ["AddContent", "/add", "ingestion.read"],
  ["Activity", "/activity", "activity.read"],
  ["Storage", "/storage", "storage.read"],
  ["Apps", "/apps", "app.read"],
  ["System", "/system", "system.read"]
];

const workspacePaths = [...homeRoute.matchAll(/workspace\("([^"]+)",\s*"([^"]+)"/g)].map(match => match[1]);
assert.deepEqual(workspacePaths, workspaces.map(([, path]) => path));

for (const [name, path, capability] of workspaces) {
  assert.match(homeRoute, new RegExp(`workspace\\("${path}",\\s*"${name}"`));
  assert.match(homeRoute, new RegExp(`capability: "${capability}"`));
}

assert.equal(/subsystem\(|-center/.test(homeRoute), false);
for (const source of [homeRoute, ...entrySources]) {
  assert.doesNotMatch(source, /(?:resource|operations|communications|storage)-center/);
}
assert.doesNotMatch(homeRoute, /reading\/ebooks|EbookImportLegacy|redirect:\s*"\/add"/);
assert.match(entrySources[0], /`\/library\/\$\{selected\.resourceId\}`/);
assert.match(entrySources[1], /`\/activity\/\$\{row\.taskId\}`/);
assert.match(entrySources[2], /"\/activity"/);
assert.match(entrySources[2], /"\/system\/notifications"/);
assert.match(entrySources[2], /"\/storage\/providers"/);
assert.match(entrySources[3], /`\/apps\/drive\/nodes\/\$\{row\.id\}`/);
assert.match(homeRoute, /redirect:\s*"\/dashboard"/);
assert.match(homeRoute, /page\("search",\s*"LibrarySearch"/);
assert.match(homeRoute, /page\("maintenance",\s*"StorageMaintenance"/);
assert.match(homeRoute, /page\("diagnostics",\s*"SystemDiagnostics"/);
assert.match(homeRoute, /page\("drive\/nodes\/:nodeId"/);
assert.match(homeRoute, /AppsPage/);
assert.match(homeRoute, /app: true/);
assert.match(homeRoute, /source: "core"/);
assert.match(homeRoute, /path: "\/account"/);
assert.match(homeRoute, /showLink: false/);
assert.match(homeRoute, /AccountProfile/);
assert.match(homeRoute, /AccountPreferences/);
assert.match(homeRoute, /AccountSecurity/);

console.log("canonical console route assertions passed");
