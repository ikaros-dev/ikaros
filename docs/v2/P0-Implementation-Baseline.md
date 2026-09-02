# Ikaros V2 P0 工程基础契约基线

| 项目 | 内容 |
|---|---|
| 基线 ID | `v2-p0-foundation-0.2` |
| 基线日期 | 2026-09-02 |
| 状态 | **已接受，可进入 Phase 0 工程基础实现** |
| 目标 | 允许 V2 从设计阶段进入 Phase 0 工程基础实现 |
| 非目标 | 不表示 Phase 1+ 的所有业务域已经冻结 |

> 本基线是“进入下一阶段”的工程门禁决议。它冻结 Phase 0 所需的基础契约，但不代表整个 V2 已经最终定稿。

## 1. 决策

```text
Phase 0 — 工程基础 = GO
Phase 1+ 领域实现 = 按各模块的 Definition of Ready 独立门禁
发布就绪状态 = 尚未达到
```

下一阶段的默认产出应转向模块骨架、Flyway、Application API、Outbox/Inbox、Background Task Runtime 与自动化测试，而不是继续横向增加 P0 设计文档。

## 2. 规范性基线集合

Phase 0 实现至少应以下列文档作为输入：

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

## 3. 已冻结的基础规则

如果没有先修改设计文档或 ADR，Phase 0 实现不得自行改变：

- UUID 公开身份标识策略；
- PostgreSQL Owner Schema 边界；
- Resource / Attachment / Blob / Placement 的身份分离；
- 乐观并发控制语义；
- Command / Query 应用层边界；
- Outbox + Inbox 的原子性、至少一次投递与消费者幂等语义；
- Background Task / Attempt 分离；
- Permission Registry 的权威性与对象级授权边界；
- Secret Reference 边界；
- `/api/v2`、Problem、snake_case、Idempotency、ETag/If-Match、Range 等 API 规则；
- Plugin 私有持久化边界。

## 4. 尚未冻结的内容

本基线不表示以下内容可以无条件并行编码：

- Personal Drive 的完整 Schema / API / Event 映射；
- Media / Reading / Music / Photo / Game / Document 的全量专业领域契约；
- Productivity / Finance / Secure Domain 的完整持久化与公开 API；
- V1 → V2 迁移策略；
- 生产容量 / 分区的量化基线；
- 每个 Catalog Command 对应的 HTTP 路由。

```text
存在应用契约 != 必须存在公开 HTTP 端点
```

未进入 OpenAPI / HTTP Registry 的 Command，不允许由 Controller 自行猜测路由。

## 5. Phase 0 实现产出

建议按以下顺序推进：

```text
模块骨架
 -> 架构边界测试
 -> Flyway P0 基础 Schema
 -> UUIDv7 / Clock / Problem / Principal
 -> Transaction + Outbox / Inbox
 -> Background Task Runtime
 -> Resource / Storage / Identity Application API
 -> OpenAPI Controller / DTO
 -> 契约 / 恢复 / 并发 / 安全测试
 -> P0 E2E 门禁
```

## 6. 后续门禁

Phase 1 Resource Core 对应能力实现前，至少补齐 Collection 层级关系、Resource Relation、必要的 Title/Alias 变更能力，以及当前处于 `contract-deferred` 状态的公开变更接口。

Personal Drive 和每个 Professional Domain 仍必须分别具备 Schema + Command/Query + Event + Permission + OpenAPI（适用时）+ Acceptance Matrix，才能进入对应模块的编码阶段。

## 7. 变更控制

实现过程中发现契约问题时，应先修改事实来源文档（Source of Truth）/ ADR 与测试契约，再修改代码。禁止通过 Controller 特例、跨 Schema 私写、Repository 隐式规则，或“先写代码、后补契约”的方式绕过门禁。
