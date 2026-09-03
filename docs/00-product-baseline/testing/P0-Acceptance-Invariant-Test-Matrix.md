# Ikaros V2 P0 Acceptance / Invariant Test Matrix

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 P0 Acceptance / Invariant Test Matrix |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 状态 | Draft / Engineering Gate |
| Roadmap 基线 | `../Implementation-Roadmap-and-Dependency-Graph.md` |
| Module 基线 | `../Module-Package-Ownership-Design.md` |
| Database 基线 | `../database/P0-Database-Schema-Design.md` |
| Contract 基线 | `../contracts/P0-Command-Query-Event-Catalog.md` |
| OpenAPI 基线 | `../contracts/openapi-v2-p0.yaml` |

> 本文档不是普通测试用例清单，而是 V2 P0 的工程验收门槛：把架构边界、领域不变量、数据库约束、API 契约、事件可靠性、安全边界和故障恢复转换为必须自动验证的测试矩阵。
>
> P0 实现只有在这里标记为 `REQUIRED` 的 Gate 全部通过时，才允许宣称对应工程阶段完成。

---

## 1. Test Levels

P0 使用以下测试层级：

| Level | Purpose |
|---|---|
| `UNIT` | 纯领域规则、value object、状态迁移 |
| `MODULE` | 单模块 Application + Domain + Persistence 契约 |
| `DB` | PostgreSQL Constraint / Transaction / Lock / Migration |
| `CONTRACT` | Command / Query / Event / OpenAPI compatibility |
| `INTEGRATION` | 两个以上模块通过公开契约协作 |
| `SECURITY` | Authentication / Authorization / Step-up / data leakage |
| `RECOVERY` | crash / retry / replay / restart / reconciliation |
| `E2E` | HTTP -> Application -> DB/Event 的关键路径 |

---

## 2. Gate Severity

```text
REQUIRED
= P0 阻塞项；失败禁止合并到实现主线或标记阶段 Done

RECOMMENDED
= 应进入 CI，但可以在明确 Issue/ADR 下暂时豁免

FUTURE
= 已知后续演进测试，不阻塞首批 P0
```

本文件默认列出的测试均为 `REQUIRED`，除非显式标记其他级别。

---

# Part A — Architecture / Module Boundary

## 3. Module Ownership Matrix

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-ARCH-001` | 每个 P0 domain package 有唯一 Owner Module | CONTRACT | 构建时扫描 package ownership，无 unmapped domain package |
| `P0-ARCH-002` | Domain 不依赖 Infrastructure | MODULE | architecture test 检测依赖方向 |
| `P0-ARCH-003` | Domain 不依赖 HTTP / Controller | MODULE | 禁止 framework web package 进入 domain |
| `P0-ARCH-004` | Resource persistence adapter 不访问其他 Owner 私有表 | DB/MODULE | SQL/repository boundary test |
| `P0-ARCH-005` | Storage persistence adapter 不直接修改 Resource 表 | DB/MODULE | repository ownership check |
| `P0-ARCH-006` | Identity persistence adapter 不直接修改业务 domain 表 | DB/MODULE | repository ownership check |
| `P0-ARCH-007` | 跨模块写操作只走公开 Command / Capability | MODULE | architecture/import rules + integration fixture |
| `P0-ARCH-008` | Search/Analytics projection 不被业务模块作为 truth source | MODULE | 禁止 domain/application 依赖 projection repository |
| `P0-ARCH-009` | Plugin 无权直接获取模块私有 Repository | SECURITY/MODULE | plugin API compile/runtime boundary test |
| `P0-ARCH-010` | Controller 不直接调用 Repository | MODULE | architecture test |

### Gate

P0 module skeleton 合并前：

```text
P0-ARCH-001 ~ P0-ARCH-010 = PASS
```

---

# Part B — Migration / Schema

## 4. Migration Matrix

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-DB-001` | 空 PostgreSQL 可从零执行全部 V2 Migration | DB | clean DB migration succeeds |
| `P0-DB-002` | Migration 执行顺序 deterministic | DB | 两次 clean build schema checksum equivalent |
| `P0-DB-003` | ORM 不自动修改 production schema | DB | prod profile schema generation disabled |
| `P0-DB-004` | 所有 P0 表位于 owner schema | DB | catalog query asserts schema ownership |
| `P0-DB-005` | P0 实体 ID 使用 uuid | DB | information_schema assertion |
| `P0-DB-006` | 时间点字段使用 timestamptz | DB | column type assertion |
| `P0-DB-007` | mutable aggregate version 非负 | DB | CHECK rejects negative version |
| `P0-DB-008` | stable public enum 不使用 ordinal | DB/CONTRACT | schema + serializer assertion |
| `P0-DB-009` | destructive cross-domain cascade 不存在 | DB | FK metadata assertion |
| `P0-DB-010` | Permission / Built-in Role seed 可重复且结果稳定 | DB | seed rerun/idempotency test |

---

# Part C — Resource Core Invariants

## 5. Resource Identity / Lifecycle

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-RES-001` | Resource 使用 UUIDv7，第三方 ID 不作为 PK | UNIT/DB | create with external identity retains independent resource ID |
| `P0-RES-002` | Resource 不保存物理 Storage Path 作为身份 | CONTRACT/DB | schema/API has no permanent path identity field |
| `P0-RES-003` | Resource Type 不能通过普通 PATCH 任意转换 | E2E | PATCH type rejected |
| `P0-RES-004` | Archive 必须通过显式 Command | E2E | lifecycle direct patch rejected; action succeeds |
| `P0-RES-005` | Archive 幂等 | MODULE | repeated archive has one final state and no duplicate semantic effect |
| `P0-RES-006` | Restore 只允许合法来源状态 | UNIT/MODULE | invalid transition rejected |
| `P0-RES-007` | Trash Resource 不直接物理删除 Blob | INTEGRATION | resource trash leaves blob/placement intact |
| `P0-RES-008` | version mismatch 阻止 Lost Update | DB/E2E | stale If-Match rejected |
| `P0-RES-009` | Resource mutation 与 Event Outbox 原子 | DB/INTEGRATION | injected commit failure cannot persist only one side |
| `P0-RES-010` | Search projector failure 不回滚 Resource 事实 | INTEGRATION | resource commits while consumer retries |

## 6. Title / Metadata Provenance

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-RES-011` | 同一语言可有多个 Alias | DB | DB accepts valid aliases |
| `P0-RES-012` | pinned 用户字段不会被 Provider 静默覆盖 | MODULE | provider refresh stores candidate or no-op, current value unchanged |
| `P0-RES-013` | AI suggestion 默认低于用户确认值 | UNIT/MODULE | merge policy test |
| `P0-RES-014` | metadata provenance 每 managed field 有唯一 current policy row | DB | UNIQUE constraint rejects duplicates |
| `P0-RES-015` | Event 不复制完整 Resource snapshot | CONTRACT | event schema payload allowlist |

## 7. External Identity

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-RES-016` | Provider + Namespace + ObjectType + ExternalID 唯一映射 | DB | duplicate mapping rejected |
| `P0-RES-017` | identity conflict 不自动 merge Resource | MODULE/E2E | conflict returns stable error |
| `P0-RES-018` | detach identity 不改变 Resource ID | MODULE | resource identity stable |

## 8. Tag / Collection / Relation

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-RES-019` | 同一 Resource/Tag 关联不重复 | DB | duplicate insert rejected/natural idempotency |
| `P0-RES-020` | Tag normalization 唯一规则稳定 | UNIT/DB | case/normalization fixture |
| `P0-RES-021` | Collection member 不重复 | DB | PK uniqueness |
| `P0-RES-022` | Collection 不能直接 parent=self | DB | CHECK rejects |
| `P0-RES-023` | Collection 不能形成深层循环 | MODULE/DB | A->B->C, move A under C rejected transactionally |
| `P0-RES-024` | Dynamic Collection 结果不写成静态 truth | MODULE | dynamic evaluation leaves member table unchanged |
| `P0-RES-025` | Resource Relation 不允许禁止型 self relation | DB/MODULE | self relation rejected |
| `P0-RES-026` | 无向 relation canonicalization 防止 A-B / B-A 重复 | MODULE/DB | reverse duplicate rejected |

## 9. User State

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-RES-027` | User state 与 Resource 公共 metadata 分离 | DB/CONTRACT | resource update does not mutate user state |
| `P0-RES-028` | Rating 越界被拒绝 | DB | negative / >10 rejected |
| `P0-RES-029` | 用户状态并发更新有 version protection | E2E | stale state version rejected |

---

# Part D — Storage Invariants

## 10. Attachment / Blob Identity

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-STO-001` | Resource ≠ Attachment ≠ Blob | CONTRACT/DB | IDs/types/tables distinct; no identity reuse |
| `P0-STO-002` | Blob 内容不可变 | MODULE/DB | existing blob hash/size cannot be changed as replacement |
| `P0-STO-003` | Blob dedupe key 为 hash algorithm + hash + size | DB | identical content resolves/violates uniqueness predictably |
| `P0-STO-004` | Attachment replacement 创建新 Attachment | MODULE | old attachment/blob remains auditable |
| `P0-STO-005` | Attachment 不以 Object Key/Path 为公共身份 | CONTRACT | API response identity independent from location |
| `P0-STO-006` | Provider 内 Object Key 唯一 | DB | duplicate placement rejected |
| `P0-STO-007` | 多 Placement 不改变 Blob identity | MODULE | migration/copy preserves blob ID |

## 11. Integrity / GC

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-STO-008` | Verify success 写 VERIFIED + timestamp | INTEGRATION | object bytes/hash fixture |
| `P0-STO-009` | hash mismatch 不能标记 VERIFIED | INTEGRATION | corrupted fixture -> CORRUPT |
| `P0-STO-010` | integrity failure 产生 durable event | DB/INTEGRATION | outbox event committed |
| `P0-STO-011` | GC 有 Attachment 引用时阻塞 | INTEGRATION | task returns blocked result |
| `P0-STO-012` | GC 有 active Retention Hold 时阻塞 | INTEGRATION | hold fixture prevents purge |
| `P0-STO-013` | GC 在执行前重新检查引用 | CONCURRENCY/INTEGRATION | reference added after request prevents purge |
| `P0-STO-014` | GC 不因 Resource trash 自动触发不可逆删除 | INTEGRATION | bytes remain recoverable |
| `P0-STO-015` | Purge 只能在受控 task 中执行 | MODULE/SECURITY | direct public API delete path absent |

## 12. Provider Security

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-STO-016` | Storage Credential 只保存 Secret Reference | SECURITY/DB | plaintext credential fixture rejected/not persisted |
| `P0-STO-017` | signed URL / Object Key 不进入普通 Event | CONTRACT/SECURITY | event payload allowlist |
| `P0-STO-018` | disabled Provider 不接受新 Placement 写入 | MODULE | create placement rejected |
| `P0-STO-019` | Provider Drain 是异步受控操作 | E2E | returns 202 + background task |
| `P0-STO-020` | Attachment download 每次重新授权 | SECURITY/E2E | revoked access cannot reuse API request authority |

---

# Part E — Event / Outbox / Consumer Reliability

## 13. Outbox Atomicity

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-EVT-001` | 关键领域事实和 Outbox 在同事务 | DB | commit/rollback fault injection |
| `P0-EVT-002` | Event 内容写入后不可被 dispatcher 改写 | DB/MODULE | dispatcher only updates delivery metadata |
| `P0-EVT-003` | Event ID 使用 UUIDv7 | CONTRACT/DB | format/version check |
| `P0-EVT-004` | Event Type + Schema Version 唯一注册 | CONTRACT | registry duplicate test |
| `P0-EVT-005` | Event payload 不包含 Secret | SECURITY/CONTRACT | schema/serialization denylist |
| `P0-EVT-006` | Secure Domain plaintext 不进入 Event | SECURITY | fixture scans serialized payload |
| `P0-EVT-007` | LISTEN/NOTIFY 丢失不会丢 Event | RECOVERY | dispatcher polling/reconcile still delivers |

## 14. At-least-once / Inbox

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-EVT-008` | 同一个 Event 可被重复投递 | INTEGRATION | duplicate delivery fixture |
| `P0-EVT-009` | 同 consumer + event 只产生一次业务结果 | DB/INTEGRATION | inbox PK + transaction test |
| `P0-EVT-010` | consumer 写结果和 Inbox 同事务 | DB | rollback injection |
| `P0-EVT-011` | consumer crash before commit 可安全重放 | RECOVERY | restart produces exactly one final result |
| `P0-EVT-012` | consumer crash after commit before ack 可安全重放 | RECOVERY | duplicate ignored |
| `P0-EVT-013` | unknown optional event field 不使旧 consumer 崩溃 | CONTRACT | forward-compatible payload fixture |
| `P0-EVT-014` | breaking event schema change 被 compatibility check 阻止 | CONTRACT | required/type/removal negative tests |

---

# Part F — Background Task / Scheduler

## 15. Task State

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-TASK-001` | Productivity Task 与 Background Task 不共用实体 | ARCH/DB | schema/module assertion |
| `P0-TASK-002` | Failed / Cancelled / TimedOut 语义分离 | UNIT/MODULE | state transition tests |
| `P0-TASK-003` | Retry 创建新 Attempt，不覆盖旧 Attempt | DB/MODULE | attempt history retained |
| `P0-TASK-004` | `(task_id, attempt_no)` 唯一 | DB | duplicate attempt rejected |
| `P0-TASK-005` | idempotency key 防止重复逻辑 task | DB/MODULE | duplicate request returns same/equivalent task |
| `P0-TASK-006` | PENDING claim 不被两个 worker 同时获取 | CONCURRENCY/DB | parallel `SKIP LOCKED` fixture |
| `P0-TASK-007` | 外部网络调用期间不持有 claim DB transaction | INTEGRATION | transaction instrumentation assertion |
| `P0-TASK-008` | expired lease 可被 reconciliation 发现 | RECOVERY | fake clock + expired lease |
| `P0-TASK-009` | restart 后 RUNNING 不永久卡死 | RECOVERY | process restart fixture |
| `P0-TASK-010` | cancel PENDING -> CANCELLED | MODULE | transition test |
| `P0-TASK-011` | cancel RUNNING 是 request/协作语义 | MODULE | handler cancellation fixture |
| `P0-TASK-012` | 不可取消 Handler 暴露 cancellable=false | CONTRACT/E2E | API representation test |

---

# Part G — Identity / Authorization / Session

## 16. User / Role

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-ID-001` | normalized username 唯一 | DB | conflict rejected |
| `P0-ID-002` | normalized email（非空）唯一 | DB | conflict rejected |
| `P0-ID-003` | password plaintext 永不持久化 | SECURITY/DB | DB/log scan fixture |
| `P0-ID-004` | Permission Registry 是后端权威，不由菜单定义 | MODULE/SECURITY | route/menu change cannot grant permission |
| `P0-ID-005` | Role Permission 不重复 | DB | PK constraint |
| `P0-ID-006` | Platform ADMIN 不自动绕过对象 ACL | SECURITY/E2E | admin without object permission denied where contract requires |
| `P0-ID-007` | Permission Key 不存在时 role update 失败 | DB/MODULE | FK/validation test |
| `P0-ID-008` | 替换高风险 role permissions 需要 Step-up | SECURITY/E2E | insufficient SVL rejected |

## 17. Session

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-ID-009` | 原始 session token 不落库 | SECURITY/DB | only digest present |
| `P0-ID-010` | revoked session 立即拒绝 | SECURITY/E2E | token rejected after revoke |
| `P0-ID-011` | security_version 提升使旧 session 失效 | SECURITY/E2E | old session rejected |
| `P0-ID-012` | disabled user 的 session 失效 | SECURITY/INTEGRATION | disable command + auth check |
| `P0-ID-013` | token/credential 不进入 Event | SECURITY/CONTRACT | serialization scan |
| `P0-ID-014` | Authentication / Authorization / Step-up 可独立失败 | SECURITY | separate fixtures verify distinct error semantics |

---

# Part H — API / OpenAPI Contract

## 18. OpenAPI Gate

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-API-001` | `openapi-v2-p0.yaml` 是合法 OpenAPI 3.1 | CONTRACT | parser validation |
| `P0-API-002` | operationId 全局唯一 | CONTRACT | duplicate operationId check |
| `P0-API-003` | public mutating endpoint 映射到 Catalog Command | CONTRACT | mapping registry test |
| `P0-API-004` | public read endpoint 映射到 Catalog Query | CONTRACT | mapping registry test |
| `P0-API-005` | API base path 为 `/api` | CONTRACT | spec validation |
| `P0-API-006` | Public JSON 使用 snake_case | CONTRACT | schema/property lint |
| `P0-API-007` | Problem response 使用 machine-readable `code` | E2E/CONTRACT | error response fixture |
| `P0-API-008` | required idempotent create 接受 `Idempotency-Key` | CONTRACT/E2E | header required fixture |
| `P0-API-009` | same idempotency key + different request -> conflict | E2E | request fingerprint test |
| `P0-API-010` | mutable aggregate update 要求 `If-Match` | E2E | missing header -> 428 |
| `P0-API-011` | stale ETag -> 412 | E2E | concurrent update fixture |
| `P0-API-012` | async command -> 202 + task Location | E2E | GC/provider drain fixture |
| `P0-API-013` | Attachment content 支持 Range | E2E | 206 + correct byte range |
| `P0-API-014` | Admin namespace 不代表自动授权 | SECURITY/E2E | authenticated non-admin denied |

---

# Part I — Security / Data Leakage

## 19. Sensitive Data Gate

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-SEC-001` | Password / OTP / Token / Credential 不进入 application log | SECURITY | log capture scan |
| `P0-SEC-002` | Storage credential 不进入 API read response | SECURITY/E2E | provider response redaction |
| `P0-SEC-003` | Secret Reference 可暴露引用但不能 reveal secret | SECURITY | use-without-reveal fixture |
| `P0-SEC-004` | unauthorized resource 不因 list facet/count 泄漏 | SECURITY | list result/filter count fixture |
| `P0-SEC-005` | 404/403 策略不泄漏不可见对象存在性 | SECURITY/E2E | hidden-resource fixture |
| `P0-SEC-006` | Secure Domain plaintext 不进入普通 Cache/Search/Event | SECURITY/INTEGRATION | cross-system leakage scan |
| `P0-SEC-007` | audit/error summary 不包含敏感原始 payload | SECURITY | audit capture scan |
| `P0-SEC-008` | Plugin capability 不能提升当前 Principal 权限 | SECURITY/PLUGIN | plugin invocation fixture |

---

# Part J — Failure / Recovery

## 20. Crash Matrix

| ID | Failure Point | Required Final State |
|---|---|---|
| `P0-REC-001` | crash before domain transaction commit | domain fact absent; event absent |
| `P0-REC-002` | crash after domain commit before event dispatch | domain fact present; outbox pending; later dispatched |
| `P0-REC-003` | dispatcher delivers then crashes before marking dispatched | duplicate delivery allowed; consumer idempotent |
| `P0-REC-004` | consumer crashes before transaction commit | no consumer result/inbox; replay succeeds |
| `P0-REC-005` | consumer commits then crashes before ack | result once; replay ignored |
| `P0-REC-006` | worker crashes with active lease | lease expires; reconciliation resumes/retries |
| `P0-REC-007` | storage provider unavailable during verify | blob not falsely VERIFIED; task retryable/failed explicitly |
| `P0-REC-008` | storage provider unavailable during GC | no DB state claims purge before bytes confirmed deleted |
| `P0-REC-009` | server restart with PENDING tasks | tasks remain claimable |
| `P0-REC-010` | server restart with stale RUNNING tasks | no permanent zombie RUNNING |

---

# Part K — Concurrency

## 21. Concurrent Mutation Matrix

| ID | Race | Acceptance |
|---|---|---|
| `P0-CON-001` | two Resource updates same version | exactly one succeeds; other conflict |
| `P0-CON-002` | two External Identity attaches same key to different resources | one succeeds; one deterministic conflict |
| `P0-CON-003` | duplicate AddTag requests | one final relation |
| `P0-CON-004` | duplicate Collection member add | one final membership |
| `P0-CON-005` | two Blob registrations same content | one content identity result / uniqueness preserved |
| `P0-CON-006` | two workers claim same Task | only one active Attempt claimant |
| `P0-CON-007` | GC races new Attachment reference | GC recheck blocks deletion |
| `P0-CON-008` | role permission update races authorization cache read | cache invalidates/uses versioned policy; no permanent stale grant |

---

# Part L — Plugin Runtime P0 Gate

## 22. Plugin Contract Tests

虽然 P0 首批实现可以不交付完整 Marketplace，但 Plugin Runtime 边界已经属于平台基础契约。

| ID | Invariant | Level | Acceptance |
|---|---|---|---|
| `P0-PLG-001` | incompatible Plugin API Version 不启用 | CONTRACT | compatibility fixture |
| `P0-PLG-002` | manifest 未声明 capability 不可调用 | SECURITY | runtime deny fixture |
| `P0-PLG-003` | manifest 未声明 permission 不可获得 | SECURITY | runtime deny fixture |
| `P0-PLG-004` | plugin exception 不导致 host process contract state corruption | INTEGRATION | faulting plugin fixture |
| `P0-PLG-005` | plugin disable 后 extension point 不再被发现 | INTEGRATION | discovery fixture |
| `P0-PLG-006` | plugin-owned migration 不得修改其他 Owner Schema | DB/SECURITY | migration boundary fixture |
| `P0-PLG-007` | plugin secret 通过 secret reference 获取 | SECURITY | no plaintext config persistence |
| `P0-PLG-008` | plugin business write 仍经目标 Command/Capability | MODULE/SECURITY | no repository bypass fixture |

---

# Part M — Required E2E Scenarios

## 23. E2E-01 Create Resource

```text
Authenticate user
  -> POST /api/resources + Idempotency-Key
  -> Permission + validation
  -> Resource transaction
       resource row
       initial titles
       outbox event
  -> 201 + ETag
  -> repeat same request/key
  -> same/equivalent resource, no duplicate event fact
  -> event delivered twice deliberately
  -> Search consumer final projection once
```

Required IDs：

```text
P0-RES-001
P0-RES-009
P0-EVT-001
P0-EVT-008
P0-EVT-009
P0-API-008
P0-API-009
```

---

## 24. E2E-02 Concurrent Resource Update

```text
GET Resource -> ETag v:3
Client A PATCH If-Match v:3
Client B PATCH If-Match v:3
A succeeds -> v:4
B -> 412
No lost update
One committed updated event for A
```

---

## 25. E2E-03 Resource Trash Does Not Delete Blob

```text
Resource -> Attachment -> Blob -> Placement AVAILABLE
Trash Resource
Assert:
  Resource TRASHED
  Attachment/Blob still exist
  Placement still AVAILABLE
  no irreversible GC triggered merely by trash
```

---

## 26. E2E-04 Blob GC Race

```text
Blob currently unreferenced
Request GC -> Task PENDING
Before worker executes, create new valid Attachment reference
Worker starts
GC re-evaluates references
Task finishes blocked/no-op
Blob bytes remain
```

---

## 27. E2E-05 Background Task Crash Recovery

```text
Worker A claims Attempt 1
Worker A crashes
Lease expires
Reconciler detects stale attempt
Attempt 1 preserved as failed/expired according to handler policy
Attempt 2 created when retryable
Worker B executes
Task reaches SUCCEEDED once
```

---

## 28. E2E-06 Disable User Revokes Effective Session

```text
User has active session security_version=5
Admin passes required authorization/step-up
DisableUser
User security_version increments
identity.user.disabled event emitted
Old session request rejected immediately/effectively
No password/token data appears in event or audit payload
```

---

# Part N — CI Execution Layers

## 29. PR Fast Gate

每个普通实现 PR 至少运行：

```text
Unit
Architecture boundary tests
OpenAPI lint
Event contract registry validation
Database migration from clean PostgreSQL
DB constraint integration tests for touched module
Module integration tests
```

目标：快速阻止结构性错误进入主线。

---

## 30. Full P0 Gate

主分支 / nightly / release candidate 运行：

```text
all PR Fast Gate
+ concurrency matrix
+ crash/recovery matrix
+ duplicate event replay
+ security leakage tests
+ plugin compatibility fixtures
+ full HTTP E2E critical paths
+ clean DB -> migrate -> seed -> start application
```

---

# Part O — Traceability

## 31. Requirement -> Contract -> Test

任何 P0 实现项必须能建立：

```text
PRD / System Requirement
        ↓
Subsystem Invariant
        ↓
Command / Query / Event
        ↓
Database Constraint / Transaction Boundary
        ↓
OpenAPI operation (if public HTTP)
        ↓
P0 Test ID
```

PR 描述中建议引用对应 `P0-*` Test ID。

---

## 32. Minimum Definition of Done

P0 工程实现不能只以“接口能调用”作为完成标准。

某项能力 Done 至少需要：

- [ ] Owner Module 明确。
- [ ] Domain invariant 已实现。
- [ ] Database constraint/transaction boundary 已实现。
- [ ] Command/Query contract 已实现。
- [ ] Durable Event contract 已实现（若需要）。
- [ ] Permission/Step-up policy 已实现。
- [ ] OpenAPI 与实现一致（若公开 HTTP）。
- [ ] Idempotency / concurrency 行为已验证。
- [ ] failure/recovery 行为已验证。
- [ ] 本 Matrix 对应 REQUIRED Test ID 自动化通过。

---

# Part P — P0 Overall Acceptance Gate

## 33. P0 Foundation Ready

当且仅当以下条件同时满足，才允许从“设计基线”进入“P0 Foundation Ready”：

```text
Architecture Gate       PASS
Migration Gate          PASS
Resource Core Gate      PASS
Storage Core Gate       PASS
Event/Outbox Gate       PASS
Background Task Gate    PASS
Identity/Security Gate  PASS
OpenAPI Contract Gate   PASS
Recovery Gate           PASS
Concurrency Gate        PASS
Plugin Boundary Gate    PASS
```

任何 REQUIRED Gate 被跳过，都必须存在：

```text
explicit issue
+ owner
+ reason
+ risk
+ expiration/removal condition
```

禁止以“后面再补测试”作为无期限豁免。

---

## 34. What This Matrix Does Not Claim

本文档完成意味着 P0 的**验收方法和不可违反的测试合同**已经定义，并不意味着测试代码已经实现。

实际编码阶段下一步应把这些 Test ID 转换为：

- architecture tests；
- Testcontainers PostgreSQL tests；
- module integration tests；
- contract/schema tests；
- fault-injection/recovery tests；
- HTTP E2E tests；
- security regression tests。

测试实现完成度必须由 CI 结果判断，不能仅凭本文档勾选状态判断。
