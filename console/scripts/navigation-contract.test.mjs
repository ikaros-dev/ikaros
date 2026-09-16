import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

const home = await readFile(new URL("../src/router/modules/home.ts", import.meta.url), "utf8");
const sidebar = await readFile(new URL("../src/layout/components/lay-sidebar/components/SidebarItem.vue", import.meta.url), "utf8");
const layoutTypes = await readFile(new URL("../src/layout/types.ts", import.meta.url), "utf8");
const zhLocale = await readFile(new URL("../locales/zh-CN.yaml", import.meta.url), "utf8");
const enLocale = await readFile(new URL("../locales/en.yaml", import.meta.url), "utf8");
const permissionStore = await readFile(new URL("../src/store/modules/permission.ts", import.meta.url), "utf8");
const verticalSidebar = await readFile(new URL("../src/layout/components/lay-sidebar/NavVertical.vue", import.meta.url), "utf8");
const horizontalSidebar = await readFile(new URL("../src/layout/components/lay-sidebar/NavHorizontal.vue", import.meta.url), "utf8");
const mixSidebar = await readFile(new URL("../src/layout/components/lay-sidebar/NavMix.vue", import.meta.url), "utf8");
const routerUtils = await readFile(new URL("../src/router/utils.ts", import.meta.url), "utf8");

test("Dashboard is the fixed localized home tab", () => {
  assert.match(layoutTypes, /path: "\/dashboard"[\s\S]*name: "DashboardHome"[\s\S]*title: "menus\.dashboard"[\s\S]*fixedTag: true/);
});

test("Console uses five localized top-level workspaces", () => {
  const matches = [...home.matchAll(/workspace\(\s*"([^"]+)",\s*"([^"]+)"/g)].map(match => [match[1], match[2]]);
  assert.deepEqual(matches, [
    ["/dashboard", "Dashboard"],
    ["/resources", "Resources"],
    ["/storage", "Storage"],
    ["/apps", "Apps"],
    ["/system", "System"]
  ]);
  for (const key of ["dashboard", "resources", "storage", "apps", "system"]) {
    assert.ok(home.includes(`title: "menus.${key}"`));
    assert.match(zhLocale, new RegExp(`^  ${key}: `, "m"));
    assert.match(enLocale, new RegExp(`^  ${key}: `, "m"));
  }
  assert.doesNotMatch(home, /workspace\(\s*"\/(library|add|activity)"/);
});

test("Resource and System menus follow the documented hierarchy", () => {
  for (const fragment of [
    /page\(\s*"library",\s*"ResourceLibrary"/,
    /page\(\s*"add",\s*"AddResource"/,
    /page\(\s*"activity",\s*"ActivityCenter"/,
    /directory\(\s*"access",\s*"SystemAccess",\s*"menus\.accessControl"/,
    /directory\(\s*"integrations",\s*"SystemIntegrations",\s*"menus\.integrations"/,
    /directory\(\s*"communications",\s*"SystemCommunications",\s*"menus\.communications"/,
    /directory\(\s*"settings",\s*"SystemSettings",\s*"menus\.platformSettings"/,
    /directory\(\s*"operations",\s*"SystemOperations",\s*"menus\.operations"/
  ]) assert.match(home, fragment);
});

test("Menu page titles and descriptions use localized resources", () => {
  assert.match(home, /title: "menus\.[a-zA-Z]+"/);
  assert.match(home, /"menuDescriptions\.[a-zA-Z]+"/);
  assert.match(zhLocale, /^menuDescriptions:/m);
  assert.match(enLocale, /^menuDescriptions:/m);
});

test("Permission-filtered menu entry points declare their canonical capabilities", () => {
  assert.match(
    home,
    /"AppsOverview"[\s\S]*?\n\s*"app\.read"/
  );
  assert.match(
    home,
    /"SystemUsers"[\s\S]*?\n\s*"system\.user\.read"/
  );
  assert.match(
    home,
    /"SystemRolesPermissions"[\s\S]*?\n\s*"system\.role\.read"/
  );
});

test("Empty permission-filtered menus stop loading after initialization", () => {
  assert.match(permissionStore, /menusReady: false/);
  assert.match(permissionStore, /this\.menusReady = true/);
  assert.match(permissionStore, /this\.menusReady = false/);
  for (const sidebar of [verticalSidebar, horizontalSidebar, mixSidebar]) {
    assert.match(sidebar, /menusReady/);
  }
});

test("Empty permission-filtered menus do not create an invalid top-menu tag", () => {
  assert.match(routerUtils, /if \(tag && topMenu\) useMultiTagsStoreHook\(\)\.handleTags/);
});

test("Directory roots redirect to canonical child pages", () => {
  for (const target of ["/resources/library", "/storage/overview", "/apps/overview", "/system/access/users"]) {
    assert.ok(home.includes(`"${target}"`));
  }
});

test("Parameterized routes stay out of the Sidebar", () => {
  assert.match(sidebar, /isParameterizedRoute/);
  assert.match(sidebar, /!isParameterizedRoute/);
  assert.match(home, /showLink: false/);
  assert.match(home, /activePath/);
});

test("System directories render as flat non-clickable menu groups", () => {
  assert.match(home, /meta: \{ title, icon, showParent: true, menuGroup: true \}/);
  assert.match(sidebar, /el-menu-item-group/);
  assert.match(sidebar, /item\.meta\?\.menuGroup/);
});
