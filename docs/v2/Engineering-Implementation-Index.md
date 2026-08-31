# Ikaros V2 Engineering Implementation Index

> 本文档作为 V2 从“领域设计”进入“工程实现”的入口索引。
>
> 产品与领域语义仍以 `Product-Requirements-Document.md`、`System-Overview-Design.md`、`Database-Overview-Design.md`、`API-Convention-Design.md` 及各 Subsystem Design 为准；本文档不重新定义业务规则。

---

## 1. 推荐进入实现前的阅读顺序

1. [`Product-Requirements-Document.md`](./Product-Requirements-Document.md)
2. [`System-Overview-Design.md`](./System-Overview-Design.md)
3. [`Database-Overview-Design.md`](./Database-Overview-Design.md)
4. [`API-Convention-Design.md`](./API-Convention-Design.md)
5. [`Implementation-Roadmap-and-Dependency-Graph.md`](./Implementation-Roadmap-and-Dependency-Graph.md)
6. [`Module-Package-Ownership-Design.md`](./Module-Package-Ownership-Design.md)
7. [`Plugin-Runtime-SDK-Lifecycle-Design.md`](./Plugin-Runtime-SDK-Lifecycle-Design.md)
8. [`database/P0-Database-Schema-Design.md`](./database/P0-Database-Schema-Design.md)
9. [`contracts/P0-Command-Query-Event-Catalog.md`](./contracts/P0-Command-Query-Event-Catalog.md)
10. [`contracts/P0-Event-Payload-Schema-Registry.md`](./contracts/P0-Event-Payload-Schema-Registry.md)
11. [`api/openapi-v2-p0.yaml`](./api/openapi-v2-p0.yaml)
12. [`testing/P0-Acceptance-Invariant-Test-Matrix.md`](./testing/P0-Acceptance-Invariant-Test-Matrix.md)
13. 目标业务对应的 Subsystem Design
14. App / CMS Interaction Design

---

## 2. 工程基线文档职责

### Implementation Roadmap and Dependency Graph

负责：

- Foundation → Core Platform → Content Infrastructure → Professional Domains → Higher-level Capabilities 的实施顺序；
- 每个 Phase 的 Deliverables；
- Phase Exit Criteria；
- Definition of Ready / Definition of Done；
- 禁止的错误实施路径。

它回答：

> “V2 第一刀从哪里开始，什么完成后才能进入下一阶段？”

### Module and Package Ownership Design

负责：

- Modular Monolith 的代码所有权；
- `api / application / domain / infrastructure / persistence` 分层；
- Allowed Dependency Matrix；
- Repository / Entity / SQL 私有边界；
- Worker 拆分规则；
- Build-time architecture test。

它回答：

> “谁拥有这份状态，模块之间允许依赖什么？”

### Plugin Runtime / SDK / Lifecycle Design

负责：

- Plugin Package / Manifest；
- Stable / Experimental / Deprecated Plugin API；
- Extension Point；
- Plugin Permission；
- install / enable / disable / upgrade / uninstall；
- Plugin-owned Data / Migration；
- Config / Secret Reference；
- Runtime Failure Isolation；
- Frontend Extension；
- SDK / Compatibility Test Kit。

它回答：

> “插件如何扩展平台，同时不获得无边界内部访问权？”

### P0 Database Schema Design

负责把系统级数据库原则下降为首批可实现的 PostgreSQL Schema Contract，包括：

- Resource Core；
- Attachment / Blob / Storage；
- Event Outbox / Consumer Inbox；
- Background Task / Attempt；
- Identity / Permission / Role / Session；
- 字段、Constraint、Index、Transaction Boundary 与首批 Flyway 顺序。

它回答：

> “第一批 Migration 和 Repository 到底要落哪些表、哪些约束？”

### P0 Command / Query / Event Catalog + OpenAPI

负责：

- P0 Application Command / Query ID；
- Permission、Idempotency、Concurrency；
- Event Type / Schema Version / Producer / Consumer；
- Payload Registry；
- HTTP operationId 映射；
- machine-readable OpenAPI 3.1 baseline。

它回答：

> “业务能力如何从 Domain Contract 映射到内部调用、事件和公开 HTTP？”

### P0 Acceptance / Invariant Test Matrix

负责：

- Architecture Boundary；
- Database Constraint；
- Resource / Storage invariant；
- Outbox / Inbox replay；
- Background Task crash recovery；
- Identity / Authorization / Session；
- OpenAPI Contract；
- Security leakage；
- Concurrency；
- Plugin boundary；
- Required E2E scenario。

它回答：

> “什么自动化证据成立后，P0 才算真的完成？”

---

## 3. 工程契约链

当前 V2 已经建立以下纵向契约链：

```text
Product Requirements
      ↓
System Overview
      ↓
Subsystem Design
      ↓
Implementation Roadmap
      ↓
Module / Package Ownership
      ↓
P0 Schema Design
      ↓
Command / Query Catalog
      ↓
Event Catalog + Payload Registry
      ↓
Permission Registry
      ↓
OpenAPI Contract
      ↓
Acceptance / Invariant Test Matrix
      ↓
Flyway Migration
      ↓
Module Skeleton
      ↓
Implementation
      ↓
Automated Contract / Integration / E2E Tests
```

设计合同已经向实现层推进到 Acceptance Gate；下一阶段的重点不再是继续横向增加 P0 设计文档，而是将这些合同转化成代码和可执行测试。

---

## 4. 当前六项工程设计完成状态

| # | 工程设计事项 | 文档状态 |
|---|---|---|
| 1 | Implementation Roadmap + Dependency Graph | ✅ 已定义 |
| 2 | Module / Package Ownership + Allowed Dependency Matrix | ✅ 已定义 |
| 3 | Plugin Runtime / SDK / Lifecycle Design | ✅ 已定义 |
| 4 | 首批 P0 Database Schema | ✅ 已定义 |
| 5 | Command / Query / Event Catalog + OpenAPI | ✅ 已定义 |
| 6 | P0 Acceptance / Invariant Test Matrix | ✅ 已定义 |

这里的“已定义”表示设计和工程契约已经进入仓库，不表示对应生产代码、Migration 和自动化测试已经全部实现。

---

## 5. 下一阶段实现产物

### P0 Implementation

- Platform Foundation module skeleton；
- Resource / Storage / Identity / Integration / Operations module skeleton；
- 首批 Flyway V2 migrations；
- Permission Registry / Built-in Role deterministic seed；
- Resource / Storage Application Command / Query handlers；
- Event Outbox dispatcher / Consumer Inbox infrastructure；
- Background Task claim / attempt / lease runtime；
- HTTP Controller / DTO 与 OpenAPI contract implementation；
- architecture boundary tests；
- Testcontainers PostgreSQL constraint tests；
- Event replay / crash recovery tests；
- P0 E2E acceptance scenarios。

### 后续设计 / 实现

- Personal Drive Schema / API / Event mapping；
- performance / capacity quantitative baseline；
- V1 → V2 migration strategy；
- Design System / Token baseline（进入 App / Console 实现时）；
- 其他专业领域的 Schema / Contract / Test expansion。

---

## 6. 工程实现 Gate

任何业务模块开始编码前至少确认：

- Owner 明确；
- Invariant 明确；
- Schema Contract 存在；
- Command / Query 存在；
- Event Producer / Consumer 已登记；
- Permission 已登记；
- API Contract 已确定；
- 并发 / 幂等 / 重试语义明确；
- Acceptance Test ID 可追踪。

如果实现过程中出现无法在上述契约中解释的新业务规则，应先回到对应设计文档修正，而不是把规则只写进 Controller、Service 或 Repository。
