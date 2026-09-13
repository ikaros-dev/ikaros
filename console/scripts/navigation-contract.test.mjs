import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

const home = await readFile(new URL("../src/router/modules/home.ts", import.meta.url), "utf8");
const sidebar = await readFile(new URL("../src/layout/components/lay-sidebar/components/SidebarItem.vue", import.meta.url), "utf8");

test("Console uses five Chinese top-level workspaces", () => {
  const matches = [...home.matchAll(/workspace\(\s*"([^"]+)",\s*"([^"]+)"/g)].map(match => [match[1], match[2]]);
  assert.deepEqual(matches, [
    ["/dashboard", "Dashboard"],
    ["/resources", "Resources"],
    ["/storage", "Storage"],
    ["/apps", "Apps"],
    ["/system", "System"]
  ]);
  for (const title of ["仪表盘", "资源", "存储", "应用", "系统"]) assert.ok(home.includes(`title: "${title}"`));
  assert.doesNotMatch(home, /workspace\(\s*"\/(library|add|activity)"/);
});

test("Resource and System menus follow the documented hierarchy", () => {
  for (const fragment of [
    '"library",\n          "ResourceLibrary"',
    '"add",\n          "AddResource"',
    '"activity",\n          "ActivityCenter"',
    'directory("access", "SystemAccess", "访问控制"',
    'directory("integrations", "SystemIntegrations", "集成"',
    'directory("communications", "SystemCommunications", "通知与审计"',
    'directory("settings", "SystemSettings", "平台配置"',
    'directory("operations", "SystemOperations", "运维"'
  ]) assert.ok(home.includes(fragment));
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
