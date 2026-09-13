import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

const read = path => readFile(new URL(path, import.meta.url), "utf8");

const [
  acceptance,
  ia,
  home,
  dashboard,
  ingestion,
  activity,
  library,
  detail,
  providers,
  archive,
  maintenance,
  apps,
  routerUtils,
  users,
  permissions,
  audit
] = await Promise.all([
  read(
    "../../docs/01-platform-foundation/cms-console-interaction/Console-Product-Journey-Acceptance-Contract.md"
  ),
  read(
    "../../docs/01-platform-foundation/cms-console-interaction/Console-Information-Architecture-and-Product-Journey-Contract.md"
  ),
  read("../src/router/modules/home.ts"),
  read("../src/views/dashboard/index.vue"),
  read("../src/views/ingestion/index.vue"),
  read("../src/views/workbench/Activity.vue"),
  read("../src/views/resources/index.vue"),
  read("../src/views/resources/Detail.vue"),
  read("../src/views/storage/Providers.vue"),
  read("../src/views/storage/Archive.vue"),
  read("../src/views/storage/Maintenance.vue"),
  read("../src/views/apps/index.vue"),
  read("../src/router/utils.ts"),
  read("../src/views/security/Users.vue"),
  read("../src/views/security/Permissions.vue"),
  read("../src/views/communications/Audit.vue")
]);

function hasAll(text, fragments, label) {
  for (const fragment of fragments) {
    if (typeof fragment === "string")
      assert.ok(text.includes(fragment), `${label}: missing ${fragment}`);
    else assert.match(text, fragment, `${label}: missing ${fragment}`);
  }
}

test("the product contract and manual script cover GP01-GP09", async () => {
  const runbook = await read("../tests/golden-path-acceptance.md");
  for (const id of Array.from({ length: 9 }, (_, index) => `GP0${index + 1}`)) {
    assert.match(acceptance, new RegExp(`^## .*${id}\\b`, "m"));
    assert.match(runbook, new RegExp(`^## .*${id}\\b`, "m"));
  }
  hasAll(
    runbook,
    [
      "入口",
      "加载",
      "空状态",
      "成功",
      "失败",
      "无权限",
      "后台进度",
      "重试",
      "最终业务结果",
      "真实服务"
    ],
    "golden-path runbook"
  );
});

test("GP09 keeps the seven canonical workspaces and route ownership", () => {
  const workspaces = [
    ["/overview", "Overview", "dashboard.read"],
    ["/library", "Library", "resource.read"],
    ["/add", "AddContent", "ingestion.read"],
    ["/activity", "Activity", "activity.read"],
    ["/storage", "Storage", "storage.read"],
    ["/apps", "Apps", "app.read"],
    ["/system", "System", "system.read"]
  ];
  const matches = [...home.matchAll(/workspace\("([^"]+)",\s*"([^"]+)"/g)].map(
    match => [match[1], match[2]]
  );
  assert.deepEqual(
    matches,
    workspaces.map(([path, name]) => [path, name])
  );
  for (const [path, name, capability] of workspaces) {
    assert.match(home, new RegExp(`workspace\\("${path}",\\s*"${name}"`));
    assert.match(
      home,
      new RegExp(`workspace\\("${path}"[\\s\\S]*?capability: "${capability}"`)
    );
  }
  assert.doesNotMatch(home, /subsystem\(|-center|path:\s*["']\/console/);
  assert.doesNotMatch(ia, /\/console\//);
});

test("GP01 Add Content exposes a real source-to-result journey", () => {
  hasAll(
    ingestion,
    [
      /选择来源/,
      /扫描或上传/,
      /预览识别/,
      /重复项和映射/,
      /确认并开始导入/,
      /查看 Activity/,
      /\/ingestion\/sources/,
      /\/ingestion\/sources\/\$\{id\}\/scans/,
      /\/ingestion\/plans\/\$\{plan\.value\.id\}\/runs/,
      /Idempotency-Key/,
      /router\.push\(['"]\/library['"]\)/
    ],
    "GP01"
  );
});

test("GP02 and GP06 keep failed work observable and retryable", () => {
  hasAll(
    dashboard,
    [/需要关注/, /后台工作失败/, /查看工作/, /\/activity\//],
    "GP02 Overview attention"
  );
  hasAll(
    activity,
    [
      /background-tasks/,
      /业务类型/,
      /关联对象/,
      /状态/,
      /进度/,
      /下一步/,
      /actions\/cancel/,
      /actions\/retry/,
      /执行尝试历史/,
      /Advanced/
    ],
    "GP02/GP06 Activity"
  );
  hasAll(ingestion, [/失败项目/, /retry\(/, /\/retry/], "GP02 import retry");
});

test("GP03 and GP07 keep availability, restore, and storage diagnostics separate", () => {
  hasAll(
    library,
    [
      /AVAILABLE/,
      /CACHED/,
      /REMOTE/,
      /PROCESSING/,
      /RESTORING/,
      /MISSING/,
      /CORRUPTED/
    ],
    "GP03 availability filters"
  );
  hasAll(
    detail,
    [/Availability/, /暂无 Attachment/, /Advanced/, /\/activity/],
    "GP03 Resource Detail"
  );
  hasAll(
    archive,
    [
      /归档、恢复/,
      /恢复是异步后台操作/,
      /\/attachments\/\$\{attachmentId\.value\.trim\(\)\}\/restore-requests/,
      /\/restore-requests\/\$\{id\}\/actions\/retry/,
      /重试/
    ],
    "GP03 restore"
  );
  hasAll(
    maintenance,
    [/Resource ID/, /Attachment ID/, /Blob ID（高级）/, /Placement/, /Replica/],
    "GP07 diagnostics"
  );
});

test("GP04 makes provider health an explicit unknown-to-verified flow", () => {
  hasAll(
    providers,
    [
      /Unknown/,
      /Health/,
      /Credential/,
      /\/storage\/providers/,
      /\/probe/,
      /Provider 探测失败/
    ],
    "GP04 Providers"
  );
  hasAll(
    dashboard,
    [/Storage 摘要/, /Storage 状态未知/, /Provider 异常/],
    "GP04 Overview attention"
  );
});

test("GP05 exposes conflict context without replacing the canonical resource", () => {
  hasAll(
    library,
    [/conflict/, /元数据冲突/, /Advanced/],
    "GP05 Library conflict filter"
  );
  hasAll(
    detail,
    [/Metadata/, /当前值/, /来源/, /人工确认/],
    "GP05 Resource metadata"
  );
  hasAll(
    ingestion,
    [/metadata\/sync-sources/, /refreshResourceId/, /refreshFieldKey/],
    "GP05 metadata sync"
  );
});

test("GP08 enforces capability filtering, denial handling, and audit evidence", () => {
  hasAll(
    routerUtils,
    [/meta\?\.capability/, /permissions\.includes\(capability\)/],
    "GP08 navigation authorization"
  );
  hasAll(
    library,
    [/status === 403/, /没有权限浏览这些资源/],
    "GP08 direct resource denial"
  );
  hasAll(
    users,
    [/admin\/users/, /admin\/roles/, /角色分配/, /角色撤销/],
    "GP08 access management"
  );
  hasAll(
    permissions,
    [/admin\/roles/, /admin\/permissions/, /保存权限/],
    "GP08 permission management"
  );
  hasAll(
    audit,
    [/audit-events/, /操作审计与安全事件/, /服务端按权限过滤/],
    "GP08 audit"
  );
});

test("GP06 and GP09 keep optional products below Apps and tasks inside Activity", () => {
  hasAll(
    apps,
    [/hasPerms\(app\.capability/, /lifecycle === "ENABLED"/, /插件 App/],
    "GP09 Apps"
  );
  assert.match(home, /app: true/);
  assert.match(home, /page\("drive\/nodes\/:nodeId"/);
  assert.doesNotMatch(
    home,
    /workspace\("\/(drive|documents|media|planning|finance|plugins)/
  );
  assert.doesNotMatch(home, /AI Task|Operations Task|Import Task/);
});
