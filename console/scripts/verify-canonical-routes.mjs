import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const homeRoute = await readFile(
  fileURLToPath(new URL("../src/router/modules/home.ts", import.meta.url)),
  "utf8"
);

const workspaces = [
  ["Overview", "/overview", "dashboard.read"],
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
assert.match(homeRoute, /redirect:\s*"\/overview"/);
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
