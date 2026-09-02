# Ikaros V2 P0 Engineering Foundation Contract Baseline

| 项目 | 内容 |
|---|---|
| Baseline ID | `v2-p0-foundation-0.3` |
| Baseline Date | 2026-09-02 |
| 状态 | **Accepted for Phase 0 Engineering Foundation** |
| 目标 | 允许 V2 从设计阶段进入 Phase 0 Engineering Foundation 实现 |
| 非目标 | 不宣称 Phase 1+ 所有业务域已经冻结 |

> 本 Baseline 是“进入下一阶段”的工程 Gate 决议。它冻结 Phase 0 所需的基础契约，不把整个 V2 宣布为 Final。

## 1. Decision

```text
Phase 0 — Engineering Foundation = GO
Phase 1+ Domain Implementation = gated per module Definition of Ready
Release Readiness = NOT YET
```

下一阶段默认产出应转向 module skeleton、Flyway、Application API、Outbox/Inbox、Background Task runtime 与自动化测试，而不是继续横向增加 P0 设计文档。

## 2. Normative Baseline Set

Phase 0 实现至少以以下文档为输入：

- `Product-Requirements-Document.md`
- `System-Overview-Design.md`
- `Technical-Architecture-Design.md`
- `Database-Overview-Design.md`
- `API-Convention-Design.md`
- `Implementation-Roadmap-and-Dependency-Graph.md`
- `Module-Package-Ownership-Design.md`
- `Plugin-Runtime-SDK-Lifecycle-Design.md`
- `database/P0-Database-Schema-Design.md`
- `contracts/P0-Command-Query-Event-Catalog.md`
- `contracts/P0-Event-Payload-Schema-Registry.md`
- `contracts/schema/p0-event-v1.schema.json`
- `api/openapi-v2-p0.yaml`
- `api/openapi-v2-p0-contract-convergence.yaml`
- `contracts/P0-HTTP-Operation-Registry.yaml`
- `P0-Requirement-Traceability-Matrix.md`
- `testing/P0-Acceptance-Invariant-Test-Matrix.md`

Media Delivery / Restore 的独立 P0 Addendum 继续作为专项规范性扩展。

## 3. Frozen Foundation Rules

没有先修改设计/ADR 时，Phase 0 实现不得自行改变：

- UUID public identity policy；
- Modular Monolith + Server Composition Root；
- Gradle / Package Owner dependency boundary；
- Domain 不依赖 WebFlux / R2DBC / Redis / Storage SDK 的技术边界；
- Spring WebFlux + Reactor 作为 HTTP / IO 编排模型；
- Event Loop 禁止阻塞与运行时业务代码禁止无控制 `.block*()`；
- PostgreSQL owner schema boundary；
- R2DBC 作为业务运行时数据库访问栈；
- Flyway + JDBC 只承担 Migration，不形成第二套业务持久化栈；
- Resource / Attachment / Blob / Placement identity separation；
- optimistic concurrency semantics；
- Command / Query application boundary；
- 一个 Owner Command 的本地事务边界；
- Outbox + Inbox atomicity / at-least-once / consumer idempotency；
- Background Task / Attempt separation与 Lease / Crash Recovery；
- Permission Registry authority 与对象级授权边界；
- Cache / Search / Analytics / AI Projection 不是真相源；
- 大文件 Streaming / Range、不允许全量进入 JVM Heap；
- Secret Reference boundary；
- `/api/v2`、Problem、snake_case、Idempotency、ETag/If-Match、Range 等 API 规则；
- Plugin private persistence boundary。

## 4. Not Frozen Yet

本 Baseline 不表示以下内容可以无条件并行编码：

- Personal Drive full Schema / API / Event mapping；
- Media / Reading / Music / Photo / Game / Document 全量专业领域 contract；
- Productivity / Finance / Secure Domain 完整持久化与 public API；
- V1 → V2 migration strategy；
- production capacity / partition quantitative baseline；
- 每个 Catalog Command 的 HTTP route；
- 是否将 Search / Worker / Notification 等能力拆成独立进程；
- GPU Worker / Transcode Cluster 的生产调度参数。

```text
Application Contract exists != Public HTTP endpoint must exist
```

未进入 OpenAPI / HTTP Registry 的 Command 不允许由 Controller 自行猜测路由。

## 5. Phase 0 Implementation Outputs

建议按以下顺序推进：

```text
Gradle Module Skeleton / API-Impl Boundary
 -> Architecture Boundary Tests
 -> Flyway P0 Foundation Schema
 -> UUIDv7 / Clock / ExecutionContext / Problem / Principal
 -> Reactive Transaction Executor
 -> Outbox / Inbox Runtime
 -> Background Task / Attempt / Claim / Lease Runtime
 -> Resource / Storage / Identity Application APIs
 -> OpenAPI Controllers / DTOs
 -> Observability / Health Baseline
 -> Contract / Recovery / Concurrency / Security Tests
 -> P0 E2E Gates
```

## 6. Follow-up Gates

Phase 1 Resource Core 对应能力实现前，至少补齐 Collection hierarchy、Resource Relation、必要的 Title/Alias mutation，以及当前 `contract-deferred` 的 public mutation surface。

Personal Drive 和每个 Professional Domain 仍必须分别具备 Schema + Command/Query + Event + Permission + OpenAPI（适用时）+ Acceptance Matrix，才能进入其模块编码。

若某模块引入阻塞 SDK、独立 Worker、外部 Search Engine、Redis 强一致依赖或新的跨模块事务语义，还必须先证明其符合 `Technical-Architecture-Design.md`，必要时新增 ADR。

## 7. Change Control

实现发现契约问题时：先修改 Source of Truth / ADR 与测试契约，再改代码。禁止通过 Controller special-case、跨 Schema 私写、Repository hidden rule、裸 `subscribe()` 后台执行、Cache 隐藏状态或“先写代码后补契约”绕过 Gate。
