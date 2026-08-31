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
8. 目标业务对应的 Subsystem Design
9. App / CMS Interaction Design

---

## 2. 新增工程设计文档职责

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

---

## 3. 下一阶段工程契约

完成上述工程基线后，后续优先进入以下纵向契约，而不是继续批量增加业务 Subsystem 文档：

```text
Subsystem Design
      ↓
Schema Design
      ↓
Command / Query Catalog
      ↓
Event Catalog + Payload Schema
      ↓
Permission Registry
      ↓
OpenAPI Contract
      ↓
Flyway Migration
      ↓
Module Skeleton
      ↓
Implementation
      ↓
Contract / Integration / Acceptance Test
```

建议首批从 Foundation、Resource、Storage 开始。

---

## 4. 首批待补工程产物

### P0

- Platform Foundation module skeleton；
- Resource Schema Design；
- Storage Schema Design；
- Security / Integration foundation Schema；
- Resource Command / Query Catalog；
- Storage Command / Query Catalog；
- Event Catalog；
- Permission Registry；
- OpenAPI v2 baseline；
- Flyway V2 baseline；
- architecture boundary test。

### P1

- Personal Drive Schema / API / Event mapping；
- V2 Acceptance Matrix；
- performance / capacity baseline；
- V1 → V2 migration strategy；
- Design System / Token baseline（进入 App / Console 实现时）。

---

## 5. 工程实现 Gate

任何业务模块开始编码前至少确认：

- Owner 明确；
- Invariant 明确；
- Schema Draft 存在；
- Command / Query 存在；
- Event Producer / Consumer 已登记；
- Permission 已登记；
- API Contract 已确定；
- 并发 / 幂等 / 重试语义明确；
- Acceptance Test 可描述。

如果实现过程中出现无法在上述契约中解释的新业务规则，应先回到对应设计文档修正，而不是把规则只写进 Controller、Service 或 Repository。
