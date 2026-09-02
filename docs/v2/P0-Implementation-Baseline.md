# Ikaros V2 P0 Engineering Foundation Contract Baseline

| 项目 | 内容 |
|---|---|
| Baseline ID | `v2-p0-foundation-0.2` |
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
- PostgreSQL owner schema boundary；
- Resource / Attachment / Blob / Placement identity separation；
- optimistic concurrency semantics；
- Command / Query application boundary；
- Outbox + Inbox atomicity / at-least-once / consumer idempotency；
- Background Task / Attempt separation；
- Permission Registry authority 与对象级授权边界；
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
- 每个 Catalog Command 的 HTTP route。

```text
Application Contract exists != Public HTTP endpoint must exist
```

未进入 OpenAPI / HTTP Registry 的 Command 不允许由 Controller 自行猜测路由。

## 5. Phase 0 Implementation Outputs

建议按以下顺序推进：

```text
Module Skeleton
 -> Architecture Boundary Tests
 -> Flyway P0 Foundation Schema
 -> UUIDv7 / Clock / Problem / Principal
 -> Transaction + Outbox / Inbox
 -> Background Task Runtime
 -> Resource / Storage / Identity Application APIs
 -> OpenAPI Controllers / DTOs
 -> Contract / Recovery / Concurrency / Security Tests
 -> P0 E2E Gates
```

## 6. Follow-up Gates

Phase 1 Resource Core 对应能力实现前，至少补齐 Collection hierarchy、Resource Relation、必要的 Title/Alias mutation，以及当前 `contract-deferred` 的 public mutation surface。

Personal Drive 和每个 Professional Domain 仍必须分别具备 Schema + Command/Query + Event + Permission + OpenAPI（适用时）+ Acceptance Matrix，才能进入其模块编码。

## 7. Change Control

实现发现契约问题时：先修改 Source of Truth / ADR 与测试契约，再改代码。禁止通过 Controller special-case、跨 Schema 私写、Repository hidden rule 或“先写代码后补契约”绕过 Gate。