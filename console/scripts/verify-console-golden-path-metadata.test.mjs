import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

const script = await readFile(
  new URL("./verify-console-golden-path-metadata.mjs", import.meta.url),
  "utf8"
);

test("metadata gate requires every product journey field", () => {
  for (const fragment of [
    "Golden Path",
    "旅程覆盖",
    "用户概念",
    "成功",
    "失败",
    "无权限",
    "后台进度",
    "内部实现词汇"
  ]) {
    assert.match(script, new RegExp(fragment));
  }
});

test("metadata gate only applies to Console or Console-contract changes", () => {
  assert.match(script, /startsWith\("console\/"\)/);
  assert.match(script, /cms-console-interaction/);
});
