import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

const home = await readFile(new URL("../src/router/modules/home.ts", import.meta.url), "utf8");
const page = await readFile(new URL("../src/views/console/Dashboard.vue", import.meta.url), "utf8");
const card = await readFile(new URL("../src/views/console/PageCard.vue", import.meta.url), "utf8");

test("Dashboard is a standalone canonical page", () => {
  assert.match(home, /workspace\(\s*"\/dashboard",\s*"Dashboard"/);
  assert.match(home, /"DashboardHome"[\s\S]*?"仪表盘"[\s\S]*?汇总需要关注的事项/);
});

test("Dashboard uses the minimal page-card skeleton", () => {
  assert.match(page, /<PageCard\s*\/>/);
  assert.match(card, /<el-card/);
  assert.match(card, /route\.meta\.title/);
  assert.match(card, /route\.meta\.description/);
});
