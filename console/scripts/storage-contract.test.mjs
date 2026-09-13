import { readFile } from "node:fs/promises";
import test from "node:test";
import assert from "node:assert/strict";

const route = await readFile(new URL("../src/router/modules/home.ts", import.meta.url), "utf8");
const overview = await readFile(new URL("../src/views/storage/Overview.vue", import.meta.url), "utf8");
const maintenance = await readFile(new URL("../src/views/storage/Maintenance.vue", import.meta.url), "utf8");

test("Storage routes have independent responsibilities", () => {
  assert.match(route, /views\/storage\/Overview\.vue/);
  assert.match(route, /views\/storage\/Providers\.vue/);
  assert.match(route, /views\/storage\/Policy\.vue/);
  assert.match(route, /views\/storage\/Archive\.vue/);
  assert.match(route, /views\/storage\/Maintenance\.vue/);
  assert.match(route, /views\/storage\/Backup\.vue/);
  assert.doesNotMatch(route, /StorageHome.*Tiers\.vue/);
  assert.doesNotMatch(route, /StorageMaintenance.*Cache\.vue/);
});

test("Storage unknown and diagnostics paths are explicit", () => {
  assert.match(overview, /Unknown/);
  assert.match(overview, /Promise\.allSettled/);
  assert.match(maintenance, /Resource ID/);
  assert.match(maintenance, /Attachment ID/);
  assert.match(maintenance, /Blob ID（高级）/);
  assert.match(maintenance, /\/activity/);
});
