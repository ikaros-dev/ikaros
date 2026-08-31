# Ikaros V2 搜索与发现子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 搜索与发现子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Core-Resource-Library-Subsystem-Design.md`、`Content-Creation-Revision-Collaborative-Document-Subsystem-Design.md`、`Security-Identity-Authorization-Crypto-Subsystem-Design.md`、`Background-Task-Scheduler-Design.md` |

> 本文档把系统概要中已经明确存在的 Search / Discovery 子系统进一步收敛为可实现契约，重点定义索引所有权、文档投影、权限感知、增量更新、重建、故障恢复与引擎替换边界。
>
> 本文档不绑定 Elasticsearch、OpenSearch、PostgreSQL FTS、Meilisearch 或其他具体引擎。搜索引擎属于可替换基础设施；搜索契约属于 Ikaros 平台能力。

---

## 1. 设计目标

搜索与发现子系统解决以下问题：

1. 不同领域对象如何进入统一搜索入口，而不复制业务真相。
2. 多标题、别名、Tag、Collection、专业领域字段与文档正文如何形成统一可检索投影。
3. 如何保证搜索结果不泄露无权限对象及敏感元数据。
4. 业务事务成功但索引更新失败时如何恢复。
5. 索引损坏、丢失、Schema 升级时如何重建。
6. 搜索引擎切换时如何避免业务领域依赖引擎私有数据结构。
7. Keyword、全文、前缀、模糊、语义检索与外部 Provider 搜索如何组合。
8. 排序、过滤、建议和 Discovery 如何保持可解释、可测试与可降级。

核心原则：

> **业务数据库保存事实，Search Index 保存可重建读取投影；搜索结果可短暂最终一致，但权限判断不能最终一致。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Search Document 契约；
- Index Schema / Mapping 的平台级版本；
- 领域对象到 Search Document 的投影；
- 增量索引消费与幂等；
- 单对象重建、分区重建、全量重建；
- 搜索 Query 的统一语义；
- Filter / Sort / Facet / Suggestion；
- 权限感知搜索；
- Search Provider / Search Enricher 扩展边界；
- 搜索运行状态、Lag、失败队列与可观测性；
- Keyword / Semantic Search 的能力发现与降级。

### 2.2 本子系统不负责

- Resource、Document、Track 等对象的业务真相；
- Resource ACL / Platform RBAC 的最终授权决策；
- AI 模型推理本身；
- 外部 Metadata Provider 的抓取协议；
- Private Notes / Password Manager 明文索引；
- 推荐系统的长期用户画像与机器学习训练流水线。

---

## 3. 核心不变量

1. **索引可丢弃**：删除整个 Search Index 后，必须能够仅依赖业务真相和稳定投影契约重新构建。
2. **索引不是写入口**：任何业务修改不得先写索引再异步回写领域数据库。
3. **权限不泄露**：无权限对象不能通过标题、摘要、封面、Facet 计数、Suggestion、命中数量或错误差异泄露存在性。
4. **Secure Domain 明文禁止进入普通索引**：Private Notes、Password Manager 等受保护内容只能按其专项安全设计提供检索能力。
5. **文档身份稳定**：每个可索引对象具有稳定 `document_key`，重复事件不得制造重复搜索文档。
6. **投影版本显式**：Search Document Schema 与 Projector Version 必须可识别，不能通过隐式字段猜测兼容性。
7. **删除可传播**：业务对象删除、隐藏、权限收紧后，索引必须最终移除或更新对应投影；在此之前查询阶段仍需实时权限收敛。
8. **领域拥有字段语义**：Search 只消费各领域公开的 Search Projection，不自行解析对方私有表并猜测业务含义。
9. **未知值可降级**：新 Resource Type、新枚举、新 Provider 字段不能导致旧 Search Consumer 崩溃。
10. **引擎可替换**：公开 API、领域事件与 Search Document 契约不得暴露底层引擎私有 Query DSL 作为平台稳定接口。

---

## 4. Search Document

### 4.1 文档身份

Search Document 建议至少包含：

```text
SearchDocument
├── document_key
├── subject_type
├── subject_id
├── schema_version
├── projector_version
├── source_version
├── title
├── alternate_titles[]
├── summary?
├── searchable_text[]
├── tags[]
├── collection_refs[]
├── external_identities[]
├── typed_fields{}
├── visibility_projection{}
├── data_classification
├── created_at
├── updated_at
└── indexed_at
```

`document_key` 建议由稳定命名空间组成，例如：

```text
resource:<uuid>
document:<uuid>
collection:<uuid>
```

不得使用搜索引擎自动生成 ID 作为唯一业务关联方式。

### 4.2 Source Version

`source_version` 表示生成本次文档时对应业务对象的版本或等价单调版本。

索引消费者收到旧事件时：

```text
incoming source_version < indexed source_version
```

必须忽略旧更新，避免事件乱序使索引回退。

如果某领域无法提供单体版本，应提供足以检测旧投影的稳定 Revision / Generation 契约。

### 4.3 Typed Fields

专业领域可以向 Search 暴露明确、受版本管理的字段，例如：

- Media：season number、episode number、air time、media kind；
- Music：artist、album、track number；
- Reading：author、volume、chapter；
- Photo：capture time、camera、可公开 location projection；
- Document：publication state、author、可索引正文；
- Game：platform、version、publisher。

`typed_fields` 允许扩展，但字段 Key 必须命名空间化且具备类型定义，不能退化成任意 JSON 垃圾桶。

---

## 5. 索引来源与投影边界

### 5.1 投影来源

推荐流程：

```text
Domain Transaction
    ├── 保存业务事实
    └── 写 Outbox Event
             ↓
      Search Projector
             ↓
     读取必要业务快照
             ↓
      Build Search Document
             ↓
          Upsert Index
```

Projector 可以消费事件中的必要字段，也可以根据稳定实体 ID 回读公开 Query Capability。

对于大字段正文，不建议把完整内容复制进 Event。

### 5.2 领域投影接口

每个领域应提供类似以下稳定语义：

```text
BuildSearchProjection(subject_id)
```

返回：

- 当前是否应该被索引；
- 当前公开可索引字段；
- 当前 Source Version；
- 数据敏感等级；
- 必要的权限投影 Hint。

如果对象已删除或不应索引，应返回明确 Tombstone / Remove 指令，而不是“查询不到就猜测删除”。

### 5.3 Event 与 Reconciliation

事件负责低延迟增量更新，但不能成为唯一修复路径。

系统必须同时提供 Reconciliation：

- 按 ID 重建；
- 按类型扫描重建；
- 按更新时间范围重建；
- 全量重建；
- 对比业务对象与索引文档数量 / 版本；
- 清理孤儿文档。

---

## 6. Query 模型

### 6.1 统一 Query

公开搜索 API 应表达平台语义，例如：

```text
query text
scope / subject types
filters
sort
page / cursor
facets
highlight preference
semantic mode
external provider mode
```

不得要求客户端提交 Elasticsearch Query DSL、SQL 或其他底层实现表达式。

### 6.2 Query 类型

P0 至少支持：

- 精确匹配；
- 标题 / Alias 文本检索；
- 前缀搜索；
- 基本全文检索；
- Filter；
- Sort；
- Pagination；
- 权限过滤。

后续可以扩展：

- Fuzzy；
- Typo tolerance；
- 拼音 / 罗马字辅助；
- Semantic / Vector Search；
- Hybrid Search；
- Query Rewrite；
- Search Suggestion；
- Search Provider Federation。

### 6.3 排序

支持的 Sort Key 必须由平台定义，例如：

- relevance；
- updated_at；
- created_at；
- title；
- domain-specific stable sort key。

Relevance Score 属于查询结果，不得写回成为业务事实。

同分结果必须提供稳定 Tie-breaker，避免翻页时重复或丢项。

---

## 7. 权限感知搜索

### 7.1 双层防护

复杂权限场景建议采用：

```text
Index Candidate Filter
        ↓
Server-side Authoritative Authorization
        ↓
Safe Search Result
```

索引中的 Visibility Projection 只能用于减少候选，不是最终 ACL 真相源。

### 7.2 权限撤销

用户权限被撤销后，即使 Search Index 尚未完成异步更新，最终授权层也必须立即阻止返回内容。

因此：

> ACL 更新可以异步刷新 Search Projection，但 Search API 不能依赖旧 ACL 投影决定最终可见性。

### 7.3 Facet 与 Suggestion

Facet Count、Autocomplete、Recent Suggestion 同样属于信息泄露面。

必须确保：

- 无权限 Resource 不贡献用户可见 Facet；
- 私有 Collection 名称不出现在 Suggestion；
- Secure Domain 未解锁时不返回明文标题；
- 结果总数不能泄露受保护对象数量。

---

## 8. Semantic Search

### 8.1 可选能力

语义检索不是 V2 核心业务运行的硬依赖。

Capability Discovery 可以暴露：

```text
search.keyword.available
search.fulltext.available
search.semantic.available
search.hybrid.available
```

客户端在 Semantic 不可用时自动降级 Keyword Search。

### 8.2 Embedding 边界

Embedding 属于派生数据：

- 必须可重建；
- 需要记录模型 / Provider / Dimension / Version；
- 模型变更不能覆盖旧 Embedding 后失去重建依据；
- 数据敏感等级和权限规则必须在送往外部 AI Provider 前检查；
- Secure Domain 明文默认不得送入普通 Embedding Pipeline。

### 8.3 Hybrid Search

Keyword 与 Vector Score 的合并算法属于 Search 实现，不应成为跨领域业务规则。

但正式对外排序行为若影响客户端分页和用户体验，应有版本化策略，避免升级后 Cursor 语义不可预测。

---

## 9. 外部 Search Provider

插件可以提供外部搜索结果，例如第三方媒体数据库。

外部结果必须与本地对象区分：

```text
LOCAL
EXTERNAL_PROVIDER
```

外部结果不得伪装为已经导入的 Ikaros Resource。

用户选择外部结果执行导入时，应进入 Content Ingestion / Metadata Synchronization 子系统，而不是 Search 直接写 Resource。

---

## 10. 索引任务与状态机

### 10.1 增量任务

单对象索引推荐作为轻量、可重试任务处理，必须幂等。

典型状态：

```text
Pending -> Running -> Succeeded
                   -> Failed
                   -> Cancelled（仅适用批量/重建）
```

### 10.2 Rebuild Job

全量重建不得直接覆盖当前可用索引导致长时间不可搜索。

推荐 Generation 模型：

```text
active generation = G1
        ↓
创建 G2
        ↓
全量 Build G2
        ↓
Reconcile Delta
        ↓
Validate G2
        ↓
Atomic Activate G2
        ↓
延迟清理 G1
```

如果引擎不支持 Alias / Atomic Switch，应实现等价的服务端 Generation Routing。

### 10.3 重建校验

激活新 Generation 前至少校验：

- 文档总数合理；
- 必选字段存在；
- Schema Version 正确；
- 关键类型覆盖；
- Projector Failure 不超过允许阈值；
- 抽样查询与权限检查通过。

校验失败必须保留旧 Generation 继续服务。

---

## 11. 一致性、幂等与失败恢复

### 11.1 最终一致性

业务写成功后 Search 更新允许短暂延迟。

UI 若需要 Read-your-write，应直接读取业务详情，不应等待搜索索引同步才确认业务操作成功。

### 11.2 幂等 Upsert

索引写入以 `document_key + source_version` 收敛：

- 相同版本重复写安全；
- 旧版本不得覆盖新版本；
- 删除 Tombstone 同样需要版本保护。

### 11.3 Dead Letter

持续失败的投影必须进入可观察的 Failed Projection / Dead Letter 状态，至少记录：

- subject ID；
- document key；
- source version；
- event ID；
- failure category；
- attempt；
- last error；
- next retry / manual action。

不能因为某一个坏对象阻塞整个索引消费流。

### 11.4 Search 不可用

Search Engine 故障时：

- Resource 详情、正常业务写入继续工作；
- Search API 返回明确 Degraded / Unavailable；
- 后台索引事件继续保留在 Durable Queue / Outbox；
- 恢复后重放并 Reconcile；
- 不允许因为搜索不可用回滚已提交业务事务。

---

## 12. 数据库与基础设施约束

PostgreSQL 至少需要保存搜索运行元数据，例如：

- index generation registry；
- projection checkpoint；
- failed projection；
- schema / projector version；
- rebuild task relation；
- optional processed-event idempotency records。

Search Engine 中的数据不要求建立回指 PostgreSQL 的强事务一致性外键。

如果 P0 使用 PostgreSQL FTS 作为实现，同样必须保持“Search Projection 是可重建读取模型”的架构边界，不能因为物理存储同库就把索引字段重新变成业务真相。

---

## 13. API 契约

搜索 API 应遵循 `API-Convention-Design.md`：

- 统一 Pagination / Cursor；
- Filter / Sort 白名单；
- Problem Details；
- RFC 3339 时间；
- Unknown Enum 安全演进；
- Request / Trace ID；
- Capability Discovery。

管理 API 还需要支持：

- 查看当前 Active Generation；
- 查看 Index Lag；
- 查看 Failed Projection；
- 单对象 Reindex；
- 启动 Rebuild；
- 查看 Rebuild Progress；
- 在安全条件下 Activate / Rollback Generation。

Rebuild 属于 Background Task，不使用长时间阻塞 HTTP 请求。

---

## 14. 安全与隐私

1. Search Worker / Projector 必须使用受控内部 Capability，不获得任意数据库超级读取权限作为架构前提。
2. Sensitive 字段只有在明确允许搜索时才进入 Search Projection。
3. Secret、Token、Credential 不进入索引。
4. Private Notes / Password Manager 明文不进入普通索引。
5. Highlight 片段也必须遵循字段级可见性，不能因为正文命中泄露受保护片段。
6. 查询日志默认不记录完整敏感 Query；需要诊断时使用脱敏 / Hash / Sampling。
7. 外部 Search / AI Provider 请求必须遵循数据出境和 Provider 权限策略。

---

## 15. 可观测性

至少提供：

- active index generation；
- document count by subject type；
- projection lag；
- event backlog；
- indexing throughput；
- indexing failure rate；
- failed projection count；
- rebuild progress；
- query latency p50 / p95 / p99；
- query error rate；
- zero-result rate；
- authorization post-filter ratio；
- semantic provider latency / failure（启用时）。

管理端必须能区分：

```text
业务无结果
Search Engine 不可用
Index 尚未同步
权限过滤后无结果
Query 不合法
```

---

## 16. 测试与验收基线

P0 至少覆盖：

1. 同一 Resource 重复事件只生成一个有效 Search Document。
2. 乱序事件不会让旧 Source Version 覆盖新版本。
3. Resource 永久删除后最终移除 Search Document。
4. ACL 撤销后即使索引未更新也无法搜索到对象。
5. 私有 Collection 不通过 Facet / Suggestion 泄露。
6. Search Engine 完全清空后可以全量重建。
7. Rebuild 失败不会破坏当前 Active Generation。
8. 新 Generation 校验成功后切换不要求业务停机。
9. Search Engine 暂时不可用不会导致业务写事务失败。
10. 新增未知 Resource Type 不会使旧 Search Consumer 崩溃。
11. Semantic Search 不可用时 Keyword Search 可正常降级。
12. 外部 Provider Result 不会被误认为本地 Resource。

---

## 17. P0 实现优先级

P0 优先完成：

1. Search Document 与稳定 `document_key`；
2. Resource / Document 等核心对象 Keyword / Full-text Projection；
3. Outbox 驱动增量索引；
4. `source_version` 幂等和乱序保护；
5. 服务端权威权限过滤；
6. 单对象 Reindex 与 Full Rebuild；
7. Generation 切换与失败保留旧索引；
8. Index Lag / Failed Projection 可观测性；
9. Search Capability Discovery。

Semantic Search、复杂 Ranking、Federated Search、个性化推荐在 P0 之后演进。
