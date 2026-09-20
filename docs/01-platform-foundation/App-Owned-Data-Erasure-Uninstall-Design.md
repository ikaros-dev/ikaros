# Ikaros V2 App-owned Data Erasure / Uninstall Design

| 项目 | 内容 |
|---|---|
| 适用版本 | Ikaros V2 |
| 状态 | Draft / Implementation Gate |
| 上位决策 | ADR-005 App Lifecycle |
| 关联设计 | App Runtime / Identity / Client Architecture、Attachment / Blob / Storage、Database Overview、Backup / Restore |

> 本文档定义 `DELETE_APP_DATA` 的安全语义。
>
> 核心原则：删除 App-owned Data 不等于删除 Platform Resource / Attachment / Blob，也不等于立即从所有 Backup 中物理抹除字节。

---

## 1. 背景

当前 App Runtime Foundation：

- 支持 uninstall + `KEEP_DATA`；
- uninstall 会撤销 Server App Platform Permission Grant；
- `DELETE_APP_DATA` 返回 `app.data-delete-unsupported`。

在没有 Owner-aware Erasure Protocol 前，不能用 `DROP SCHEMA CASCADE` 代替业务删除。

## 2. 目标

Erasure 需要解决：

- 哪些数据属于 App；
- 哪些数据只是 App 引用的平台对象；
- 跨 App 引用如何发现；
- 删除顺序；
- 失败/重试；
- Audit；
- Backup / Restore；
- reinstall；
- Resource / Attachment / Blob GC；
- retention/security policy；
- 如何避免删除其他 Owner 数据。

## 3. 数据分类

### A. App-owned Domain Data

例如：

```text
app_anime.anime
app_anime.episode
app_anime.playback_state
```

`DELETE_APP_DATA` 的主要删除目标。

### B. Platform-owned Object

例如：

```text
resource.resource
storage.attachment
storage.blob
```

App 可能创建/引用，但 Owner 是 Platform，默认不删除。

### C. Other App-owned Data

例如 `app_photos.*`、`app_accounting.*`。当前 App 永远无权删除。

### D. Derived / Cache / Projection

App 自己的 search/cache projection 可由 Erasure Handler 清理，但不能把清缓存当业务删除完成。

## 4. 两种 Uninstall Policy

### KEEP_DATA

```text
disable
→ revoke platform grants
→ unregister runtime admission
→ keep app-owned data
→ UNINSTALLED
```

### DELETE_APP_DATA

```text
disable
→ revoke platform grants
→ preflight erasure
→ create Erasure Operation
→ erase app-owned data
→ reconcile platform references
→ verify
→ UNINSTALLED
```

必须异步、可重试、可审计。

## 5. 前置条件

`DELETE_APP_DATA` 只能在 App 非 ENABLED 时执行。

Enabled 时返回 `app.must-disable-first`。

App 没有注册 Erasure Handler 时返回 `app.data-delete-unsupported`。

不得 fallback 到通用 DROP SCHEMA。

## 6. Erasure Handler

App Runtime 定义：

```text
AppDataErasureHandler
```

逻辑接口：

```text
appId()
plan(context) -> ErasurePlan
execute(plan, checkpoint) -> Progress
verify(plan) -> VerificationResult
```

Handler：

- 只能删除自己 Owner Schema；
- 不能获得其他 App Repository；
- 不能直接删除 Resource / Attachment / Blob；
- 支持 retry；
- 支持 checkpoint / idempotency。

## 7. Erasure Plan

Plan 至少包含：

```text
operation_id
app_id
installation_version
requested_by
created_at
owned logical categories
estimated_rows?
platform_reference_summary
external_reference_summary
retention_blocks
strategy
```

执行前管理员应知道删什么、什么会保留、什么被外部引用阻止，以及 Backup 语义。

## 8. Cross-App Reference Preflight

检查可能来自：

- Platform Relation；
- other App stable ID reference；
- Share；
- Automation；
- Search projection；
- Notification deep link；
- Backup metadata；
- scheduled task payload。

跨 Owner 引用不能靠 App 随意 JOIN 私有表扫描，必须通过 Reference Query / Relation Infrastructure。

## 9. External Reference Policy

如果 Other App 强引用当前 App-owned Entity，默认：

```text
BLOCK_ERASURE
```

错误：

```text
app.data-delete-blocked
```

只有引用 Owner 明确支持 `DETACH_ON_TARGET_ERASURE` 时，才通过其公开 Command/Capability 解除引用。

禁止当前 App 直接 UPDATE 其他 App 表。

## 10. Platform Resource Policy

App 关联的 Platform Resource 默认：

```text
KEEP
```

即：

```text
delete app_anime.anime
does not imply
delete resource.resource
```

Resource 可能被 Collection/Tag/Relation、其他 App、Share、Attachment、Export/Restore 使用。

App unavailable 后 Resource 可以保留其 namespaced type，不自动 purge。

## 11. Explicit Platform Object Cleanup

未来若提供：

```text
DELETE_APP_DATA_AND_OWNED_PLATFORM_OBJECTS
```

必须是独立策略。

它需要 Platform Resource Owner 执行：

```text
ownership eligibility
→ external reference check
→ retention
→ explicit purge command
```

V2 首版不实现该策略。

## 12. Attachment Policy

App 删除自己的 `attachment_id` reference 不等于删除 Attachment。

Attachment Owner 决定：

- 是否仍有 Resource/reference；
- 是否被 Share 使用；
- retention；
- 是否允许 TRASH/PURGE。

`DELETE_APP_DATA` 默认只删除 App reference。

## 13. Blob Policy

Blob 永远不能因为 App uninstall 直接删除。

Blob GC 仍走：

```text
reference eligibility
→ retention hold
→ backup/snapshot
→ replica/archive
→ GC Task
```

物理字节删除是 Storage Runtime 独立 Background Task。

## 14. Erasure Operation State

建议：

```text
PLANNING
READY
BLOCKED
RUNNING
VERIFYING
SUCCEEDED
FAILED
CANCELLED
```

在 Erasure 成功前：

```text
AppInstallation != UNINSTALLED
```

可停留 `UNINSTALLING`。

当前 Foundation 直接稳定态；实现 DELETE_APP_DATA 时必须引入异步状态。

## 15. Background Task

Erasure 使用：

```text
app-runtime.erase-app-data
```

Payload 只带 Operation ID，不塞大批 row IDs。

理由：数据量、checkpoint、retry、reference reconciliation 都不适合 HTTP 大事务。

## 16. Transaction Boundary

不要一个大事务删除全部 App Data。

推荐：

```text
small deterministic batch
→ checkpoint
→ durable progress
→ next batch
```

每批由 App Owner transaction 保证，最终 verify 后才成功。

## 17. Idempotency

Handler 必须允许重复执行同一 batch，最终状态一致。

策略：

- stable cursor/range；
- tombstone then purge；
- checkpoint upsert；
- already missing = success/no-op。

禁止长任务使用不稳定 OFFSET page。

## 18. Permission Order

Uninstall 一开始就撤销普通 Server App Platform Permission Grant。

但 Erasure Handler 仍需要执行，因此 Erasure 使用受控 Platform Internal Principal，而不是普通 App Grant。

该 Principal 只能：

- 调用当前 App Erasure Handler；
- 调用明确允许的 reconciliation capability；
- 写 Audit/Task。

不能成为通用 admin bypass principal。

## 19. Client Grant

Server App uninstall 后：

- AppAuthorizationGrant 可保留历史；
- Token/refresh admission 因 App unavailable 被拒绝；
- 可异步 revoke active Grant；
- Client Registration 不因 App uninstall 自动删除。

删除 App Data 不等于删除 Client identity。

## 20. Audit

至少记录：

```text
erasure requested
plan generated
blocked reason
operation started
checkpoint/progress summary
verification
success/failure
uninstall completed
```

禁止记录被删业务正文。

Audit 是 Platform-owned retention data，不被 App 删除。

## 21. Backup / Snapshot

`DELETE_APP_DATA` 不保证历史 Backup 当场重写。

Backup 语义：

- 当前在线数据被删除；
- Backup 可按 retention 保存历史快照；
- Erasure 完成要进入 deletion ledger/tombstone；
- Restore 后必须 re-apply 已完成 Erasure，避免恢复即复活已删除 App Data。

## 22. Erasure Tombstone

Platform 保存最小：

```text
operation_id
app_id
completed_at
installation_generation
erasure_generation
data_policy
```

不保存业务正文。

用途：

- Restore reconciliation；
- 防止旧 Backup 恢复已删除数据；
- Audit；
- reinstall decision。

## 23. Reinstall after KEEP_DATA

重装同 app_id：

1. package compatibility；
2. schema version detect；
3. App migration；
4. data verification；
5. enable。

不得假设保留数据天然兼容新版本。

## 24. Reinstall after DELETE_APP_DATA

Erasure SUCCEEDED 后重装从 fresh App-owned state 开始。

Platform Resource/Attachment 可能仍在。App 可以通过显式 import、Resource discovery 或用户操作重新关联，不得自动猜旧业务数据。

## 25. Schema Drop

`DELETE_APP_DATA` 意味着 erase App-owned business data，不等于必须 `DROP SCHEMA`。

首版推荐：

- 保留 schema/table；
- 删除业务 row；
- 保留 migration baseline；
- 便于 reinstall/version detection。

只有 Package Runtime 明确支持 Owner Schema teardown 后再讨论 schema drop。

## 26. Retention / Secure Data

如果数据受 legal retention、user retention lock、Secure Domain policy 或 immutable backup 限制，Erasure Plan 可以 BLOCKED 或等待 retention expiry。

Uninstall 不得绕过 retention。

## 27. Failure Recovery

失败时：

- App 保持 disabled；
- 不恢复普通 Platform Permission Grant；
- Operation 保留 checkpoint；
- 可 retry；
- 部分删除不标 UNINSTALLED success。

如果 Handler 版本不可用，需要 exact compatible package / recovery handler / administrator intervention。

因此 Package Retention 要保留 Erasure 所需代码直到完成。

## 28. Event

建议：

```text
app-runtime.app-data-erasure.requested
app-runtime.app-data-erasure.blocked
app-runtime.app-data-erasure.started
app-runtime.app-data-erasure.completed
app-runtime.app-data-erasure.failed
```

Payload 只带 operation_id、app_id、status、reason code、counters summary。

## 29. Command

逻辑 Command：

```text
app-runtime.plan-app-data-erasure
app-runtime.execute-app-data-erasure
app-runtime.retry-app-data-erasure
app-runtime.uninstall-app DELETE_APP_DATA
```

HTTP 路由另行进入 OpenAPI / Operation Registry。

## 30. 第一实现切片

### Slice A
AppDataErasureHandler API + operation table + plan + Background Task + KEEP platform objects。

### Slice B
owner reference preflight + BLOCKED state + retry/checkpoint。

### Slice C
Backup Erasure Tombstone + restore reconciliation。

### Slice D
optional explicit Platform Resource cleanup policy。

## 31. Quiescence / Drain Barrier

`disable` 只能阻止新的普通 admission，不足以证明 App 已经不会继续写数据。

在进入 Erasure RUNNING 前必须建立独立的写入栅栏：

```text
DISABLED
→ QUIESCING
→ DRAINING
→ DRAINED
→ ERASURE RUNNING
```

这些可以是 Erasure Operation phase，不要求立即扩展全局 AppLifecycle enum。

### 31.1 Erasure Fence

创建 Erasure Operation 时记录：

```text
operation_id
app_id
expected_installation_version
erasure_generation
fence_created_at
```

并原子设置当前 App 的：

```text
erasure_fence = ACTIVE
```

之后所有 App-owned write entry 必须：

- 检查 App availability；
- 检查 erasure fence；
- 检查 invocation/data generation；
- fence ACTIVE 时拒绝新业务写入。

不能只依赖 HTTP Controller，因为 Task/Event/Scheduler/Internal Capability 同样可能写 App Data。

### 31.2 Background Task Drain

对于当前 App：

- 禁止提交新 App-owned Task；
- 未 claim 的 queued task 进入 `BLOCKED_APP_ERASURE` 或按 policy cancel；
- 已 claim/running task 请求 cooperative cancellation 或允许安全 drain；
- Erasure 不得开始，直到所有 write-capable attempt 进入 terminal 状态；
- 只读/平台级 GC Task 可以按 Owner Policy 继续。

### 31.3 Event Consumer Drain

- 停止向 App Event Consumer 分派新 Event；
- 等待已进入 handler 的 invocation 完成/取消；
- 未消费 Durable Event 保留在 Event Runtime，不伪装 ACK；
- reinstall/enable 后是否 replay 由 consumer contract 决定；
- Erasure 期间不得有 Consumer 写 App-owned Data。

### 31.4 Scheduler / Callback / Realtime

进入 QUIESCING 时：

- 禁用 App-owned Scheduled Job；
- 关闭 write-capable realtime subscription；
- Provider callback/webhook 进入 reject/ignore-safe policy；
- 不再创建新的 App-owned Background Work。

### 31.5 Outbox

Barrier 前已提交的 Outbox Event 可以继续由 Platform Publisher 发布，因为它们是已发生业务事实。

但：

- barrier 后不能再创建新的 App-owned Domain write + Outbox；
- Outbox delivery 不能回调本 App 并重新写入已进入 Erasure 的数据；
- Consumer side-effect 仍按接收方 Owner Contract 处理。

只有确认“没有仍可写 App-owned Data 的 in-flight execution”后，Operation 才能进入 `DRAINED`。

---

## 32. Concurrency / Race Control

同一 `app_id` 同时最多一个 active Erasure/Uninstall Operation。

开始执行前必须 compare-and-swap：

```text
installation.version == plan.expected_installation_version
AND erasure_generation == plan.erasure_generation
AND erasure_fence == ACTIVE
AND operation.status == READY
```

如果 Plan 生成后发生：

- package reinstall/upgrade；
- configuration mutation；
- new reference；
- retention policy change；
- installation version change；

则旧 Plan 失效，必须重新 plan/confirm。

Erasure batch 不能靠普通“先查后写”避免并发；需要 DB constraint / version token / operation lock 保证唯一 active operation。

---

## 33. App Configuration / Secret / External Side-effect Cleanup

`DELETE_APP_DATA` 不只包含业务表。

Erasure Plan 必须分类：

### 33.1 App Configuration

App-owned 普通配置属于删除范围：

```text
App Runtime config
App-specific public preferences
provider mapping owned exclusively by app
```

但 Platform 全局配置不删除。

### 33.2 Secret Reference

App 删除：

- 自己的 Secret Reference；
- App-owned secret binding metadata。

Secret Material 本身只有在满足：

```text
secret.owner_app_id == target app
AND no other active reference
AND retention policy allows
```

时，才由 Secret Owner 的公开 Command 删除。

共享 Secret 只 unlink，不物理删除。

### 33.3 External Provider Side Effects

App 可能在外部系统创建：

- webhook；
- subscription；
- provider-side folder/object metadata；
- remote index；
- API registration。

Erasure Handler 不得假装删除本地 row 就完成外部擦除。

Plan 必须为外部 side effect 标记：

```text
NONE
DETACH_ONLY
REMOTE_DELETE_REQUIRED
MANUAL_RECOVERY_REQUIRED
```

远端删除失败时：

- 按 policy retry；
- 如果数据仍可从外部反向写回 Ikaros，则必须 BLOCK completion；
- 无法自动撤销时进入 MANUAL_RECOVERY_REQUIRED，并给出不含 Credential 的管理员指引。

---

## 34. Destructive Confirmation / Step-up

`DELETE_APP_DATA` 是永久删除操作，必须满足：

```text
system.app.manage
AND required Step-up Verification
AND explicit destructive confirmation
```

确认必须绑定具体 Plan，而不是只确认“删除这个 App”。

确认请求至少携带：

```text
operation_id
plan_digest
expected_installation_version
app_id
data_policy = DELETE_APP_DATA
```

Server 在执行前重新验证：

```text
plan digest still current
AND installation version unchanged
AND blockers unchanged
AND step-up still fresh
```

任一变化都使确认失效并要求重新 Plan/确认。

UI 可以要求输入 App Name 或二次按钮，但真正的安全绑定是 Server-side `operation_id + plan_digest + version`，不能依赖自然语言确认文案。

---

## 35. Erasure Completion Gate

只有全部满足：

```text
erasure fence active
AND drain state == DRAINED
AND app-owned data verification PASS
AND required external cleanup PASS
AND platform reference reconciliation PASS
AND retention blockers == none
AND tombstone persisted
AND audit persisted
```

才能：

```text
ErasureOperation = SUCCEEDED
AppInstallation = UNINSTALLED
```

顺序上先写 Erasure Tombstone，再完成 Uninstall final state，确保 crash/restart 后不会遗漏 Restore reconciliation 依据。

---

## 36. Acceptance Invariants

1. 未注册 Erasure Handler 时 DELETE_APP_DATA fail closed。
2. Enabled App 不能开始 Erasure。
3. Erasure 不依赖普通 App Permission Grant。
4. Handler 不能访问其他 Owner Repository。
5. App-owned Data 删除不隐式删除 Resource。
6. Attachment reference 删除不直接删除 Blob。
7. Blob 物理删除只通过 Storage GC。
8. 外部强引用默认阻止 Erasure。
9. Task crash 后从 checkpoint 重试。
10. Partial failure 不得标记 UNINSTALLED success。
11. Backup restore 不复活已完成 Erasure 的 App-owned Data。
12. Reinstall after DELETE_APP_DATA 从 fresh App-owned state 开始。
13. Audit 保留且不记录被删除正文。
14. Schema 不因 DELETE_APP_DATA 默认 DROP CASCADE。
15. Disable 后仍存在 running Task/Event Handler 时，Erasure 不得进入 RUNNING。
16. Erasure fence 激活后，HTTP/Task/Event/Scheduler/Internal Command 的新 App-owned 写入都被拒绝。
17. Queued/running write-capable Task 必须完成 drain/cancel 后才能删除数据。
18. Plan 生成后 Installation Version 或引用关系变化时，旧确认失效。
19. 同一 app_id 同时最多一个 active Erasure/Uninstall Operation。
20. DELETE_APP_DATA 删除 App-owned 普通配置和 Secret Reference，但不误删共享 Secret Material。
21. 外部 webhook/subscription 等 side effect 未完成必要清理时不能伪装 Erasure Success。
22. DELETE_APP_DATA 要求 system.app.manage、fresh Step-up 与绑定 plan_digest 的显式确认。
23. Barrier 前已提交 Outbox Event 可以发布，但 barrier 后不能产生新的 App-owned Domain write。
24. Tombstone 持久化成功后才允许完成 UNINSTALLED final state。
