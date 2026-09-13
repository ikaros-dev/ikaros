import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

const home = await readFile(new URL("../src/router/modules/home.ts", import.meta.url), "utf8");
const apps = await readFile(new URL("../src/views/apps/index.vue", import.meta.url), "utf8");
const navbar = await readFile(new URL("../src/layout/components/lay-navbar/index.vue", import.meta.url), "utf8");
const routerUtils = await readFile(new URL("../src/router/utils.ts", import.meta.url), "utf8");

test("Apps and System remain the aggregation workspaces", () => {
  assert.match(home, /workspace\("\/apps",\s*"Apps"/);
  assert.match(home, /workspace\("\/system",\s*"System"/);
  assert.doesNotMatch(home, /workspace\("\/(drive|documents|media|planning|finance|plugins)/);
});

test("App navigation is capability and enabled-state driven", () => {
  assert.match(routerUtils, /meta\?\.enabled === false/);
  assert.match(routerUtils, /meta\?\.capability/);
  assert.match(apps, /hasPerms\(app\.capability/);
  assert.match(apps, /lifecycle === "ENABLED"/);
  assert.match(apps, /插件 App/);
});

test("Account routes are hidden from the sidebar and exposed by the avatar menu", () => {
  assert.match(home, /path: "\/account"/);
  assert.match(home, /showLink: false/);
  assert.match(navbar, /\/account\/profile/);
  assert.match(navbar, /\/account\/preferences/);
  assert.match(navbar, /\/account\/security/);
});
