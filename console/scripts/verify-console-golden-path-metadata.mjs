import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { execFileSync } from "node:child_process";

function argument(name) {
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] : undefined;
}

function changedFiles(base, head) {
  if (!base || !head) return [];
  return execFileSync("git", ["diff", "--name-only", `${base}..${head}`], {
    encoding: "utf8"
  })
    .split(/\r?\n/)
    .map(value => value.trim())
    .filter(Boolean);
}

const eventFile = argument("--event-file");
if (!eventFile) {
  console.log(
    "Console Golden Path metadata gate skipped: no pull request event was supplied."
  );
  process.exit(0);
}

const event = JSON.parse(await readFile(eventFile, "utf8"));
const pullRequest = event.pull_request;
if (!pullRequest) {
  console.log(
    "Console Golden Path metadata gate skipped: event is not a pull request."
  );
  process.exit(0);
}

const body = String(pullRequest.body || "");
const files = changedFiles(
  argument("--base-sha") || pullRequest.base?.sha,
  argument("--head-sha") || pullRequest.head?.sha
);
const touchesConsole = files.some(
  file =>
    file.startsWith("console/") ||
    file.startsWith("docs/01-platform-foundation/cms-console-interaction/")
);
if (!touchesConsole) {
  console.log(
    "Console Golden Path metadata gate passed: pull request does not touch Console scope."
  );
  process.exit(0);
}

const requirements = [
  ["Golden Path 关联", /Golden Path[\s\S]{0,180}GP0[1-9]/i],
  ["旅程覆盖段落", /旅程覆盖|Journey coverage/i],
  ["用户概念说明", /用户概念|user concept/i],
  ["成功验证", /成功.*验证|success.*verification/i],
  ["失败验证", /失败.*验证|failure.*verification/i],
  ["无权限验证", /无权限.*验证|unauthori[sz]ed.*verification/i],
  ["后台进度验证", /后台进度.*验证|background progress.*verification/i],
  ["内部实现词汇说明", /内部实现词汇|internal implementation terms/i]
];
const missing = requirements
  .filter(([, pattern]) => !pattern.test(body))
  .map(([label]) => label);
assert.equal(
  missing.length,
  0,
  `Console pull request is missing Golden Path metadata: ${missing.join(", ")}`
);
console.log(
  `Console Golden Path metadata gate passed for ${files.length} changed files.`
);
