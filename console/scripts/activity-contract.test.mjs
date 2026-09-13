import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const source = await readFile(new URL("../src/views/workbench/Activity.vue", import.meta.url), "utf8");

test("Activity is the unified background-work entry", () => {
  assert.match(source, /background-tasks/);
  for (const label of ["导入", "存储", "元数据", "备份", "下载 \/ 缓存", "AI", "自动化"]) assert.match(source, new RegExp(label));
  for (const label of ["业务类型", "动作", "关联对象", "状态", "进度", "更新时间", "下一步"]) assert.match(source, new RegExp(label));
});

test("Activity exposes only contract-supported cancel and retry actions", () => {
  assert.match(source, /actions\/cancel/);
  assert.match(source, /actions\/retry/);
  assert.match(source, /查看 Advanced/);
  assert.match(source, /执行尝试历史/);
  assert.match(source, /Raw Payload/);
  assert.doesNotMatch(source, /我的活动与收藏|清除本地历史|media\/playback\/history|reading\/history/);
});
