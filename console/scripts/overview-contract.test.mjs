import { readFile } from "node:fs/promises";
import test from "node:test";
import assert from "node:assert/strict";

const source = await readFile(new URL("../src/views/dashboard/index.vue", import.meta.url), "utf8");

test("Overview renders the attention-first sections", () => {
  assert.match(source, /需要关注/);
  assert.match(source, /正在进行/);
  assert.match(source, /内容摘要/);
  assert.match(source, /Storage 摘要/);
});

test("Overview never treats an unavailable widget as healthy", () => {
  assert.match(source, /部分状态暂时未知/);
  assert.match(source, /Unknown/);
  assert.doesNotMatch(source, /系统状态[：:]\s*正常/);
});

test("Overview actions use canonical workspaces", () => {
  assert.match(source, /['"]\/activity['"]/);
  assert.match(source, /['"]\/library['"]/);
  assert.match(source, /['"]\/storage['"]/);
  assert.doesNotMatch(source, /\/resource-center\//);
  assert.doesNotMatch(source, /\/operations-center\//);
});
