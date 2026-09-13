import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

const home = await readFile(new URL("../src/router/modules/home.ts", import.meta.url), "utf8");
const overview = await readFile(new URL("../src/views/console/storage/Overview.vue", import.meta.url), "utf8");
const maintenance = await readFile(new URL("../src/views/console/storage/Maintenance.vue", import.meta.url), "utf8");

test("Storage has one page per second-level route", () => {
  for (const [path, name, title] of [
    ["overview", "StorageOverview", "存储概览"],
    ["providers", "StorageProviders", "存储提供方"],
    ["policy", "StoragePolicy", "存储策略"],
    ["archive", "StorageArchive", "归档管理"],
    ["backup", "StorageBackup", "备份管理"],
    ["maintenance", "StorageMaintenance", "存储维护"]
  ]) {
    assert.match(home, new RegExp(`page\\(\\s*"${path}",\\s*"${name}",\\s*"${title}"`));
  }
  assert.match(home, /"\/storage\/overview"/);
});

test("Storage pages are clean card skeletons", () => {
  assert.match(overview, /<PageCard\s*\/>/);
  assert.match(maintenance, /<PageCard\s*\/>/);
});
