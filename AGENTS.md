# Ikaros 项目级开发规则

本文档适用于仓库根目录及其所有子目录中的开发、评审和自动化修改。它提炼自 [`docs/`](docs/README.md) 中的产品基线、系统/技术架构、数据库与 API 契约、模块所有权、路线图、插件设计和验收矩阵。

本文档只保留跨模块、可执行的硬性规则；具体领域语义以对应 Subsystem Design 和交互设计文档为准。

## 规则强度与事实来源

- 本文中的“必须 / 不得 / 禁止 / 只能”是强制规则；“默认 / 建议”只有在对应契约未另行规定时适用。
- 实现前应按需阅读：产品需求 → 系统概要 → 技术架构 → 目标子系统设计 → 实施路线图/模块所有权 → Schema/Command/Query/Event/OpenAPI → 验收矩阵。
- 领域规则以 Subsystem Design 为事实来源；Command/Query/Event Catalog 约束应用契约；OpenAPI 约束公开 HTTP 表示；Controller、Handler、Repository 和 Adapter 只能实现这些契约。
- 发现契约冲突或缺失时，必须先修改事实来源文档、ADR 和适用测试契约，再修改代码。不得用 Controller 特例、Repository 隐式规则、跨 Schema 私写或“先写代码后补契约”绕过门禁。
- `contract-deferred`、`N/A` 或尚未冻结的内容不得被解释为“实现可以自行决定”。未登记到 OpenAPI/HTTP Registry 的 Command 不得由 Controller 猜测路由。
- 长期影响架构、数据隔离、构建拓扑、事件语义、安全密钥边界或基础设施依赖的变化必须新增或修改 ADR；被替代的 ADR 标记为 Superseded，不删除历史。

## AI 辅助开发行为规范

以下规则提炼自 [andrej-karpathy-skills/CLAUDE.md](https://github.com/multica-ai/andrej-karpathy-skills/blob/main/CLAUDE.md)，用于减少 AI 辅助开发中的常见错误。一般情况下审慎优先于速度；简单任务可按实际情况从简。

- 编码前先思考：明确说明假设；存在多种解释时列出选项和权衡，不得擅自选择；发现不清楚或风险时停止并询问；发现更简单方案时应说明，必要时提出异议。
- 简洁优先：只实现需求所需的最小代码，不添加未要求的功能、单次使用的抽象或未经要求的灵活性/配置项；不要为不可能发生的场景堆叠错误处理；发现过度复杂时应主动简化。
- 手术式修改：只修改完成任务所必需的内容；不得顺手改善相邻代码、注释或格式，不得重构未损坏的代码，应遵循现有风格；发现无关死代码只报告，不删除。仅清理由本次修改产生的无用导入、变量或函数。每一处改动都必须能追溯到用户请求。
- 目标驱动并验证闭环：先定义可验证的成功标准，再实施并持续验证直至达成。新增校验应覆盖无效输入；修复缺陷应先增加可复现测试；重构应比较修改前后测试结果；多步骤任务应为每一步写明验证方式。

## 文件删除安全规则

- 禁止批量删除文件或目录。
- 不得使用：
  - `del /s`
  - `rd /s`
  - `rmdir /s`
  - `Remove-Item -Recurse`
  - `rm -rf`
- 需要删除文件时，只能一次删除一个明确路径的文件。
- 正确示例：`Remove-Item "C:\path\to\file.txt"`
- 如果需要批量删除文件，必须停止操作，并询问用户，让用户手动删除。

## Git 提交规则

- 每一步完成，如果可以提交 git commit，都提交一次 commit，但是不 push。

## 协作与实施流程

- 将具体要做的事情，拆分成子任务，一步步来，不确定的先问我，由我进行决策。
- 任一模块开始编码前，必须明确 Owner、范围与非目标、不变量、Schema/Migration、Command/Query、Event Producer/Consumer、Permission/API、失败/重试/幂等语义和验收测试。
- 推荐实现顺序：契约与边界 → Schema/Constraint/Migration → Domain/Application → Adapter/API → Outbox/Task → 自动化测试 → 文档追溯。
- 一个模块只有在 Schema Migration、Application API、授权、Event/Outbox、API/OpenAPI、可观测性、并发/重试/幂等测试、迁移测试、集成测试和文档追溯均具备后，才可视为完成。

## 总体工程基线

- V2 默认采用 Modular Monolith；当前构建使用根 `pom.xml` 聚合的 Maven Multi-Module。不得切换 Gradle 或其他构建系统；如需改变 Maven Multi-Module 拓扑，先走 ADR/设计变更。
- 后端基线为 Java 21、Spring Boot 4.x、Spring WebFlux、Project Reactor、PostgreSQL 18+、R2DBC、`r2dbc-migrate`。业务运行时不得引入 JPA/JDBC 作为第二套持久化栈，也不得为迁移引入 Flyway/JDBC 第二访问栈。
- Server 是默认且唯一的 Spring Composition Root。模块通过显式 Module Configuration 组装；禁止依赖全仓库隐式扫描来碰巧发现 Bean。
- Ikaros 是单 Instance、多 User 的默认模型，不得为假设性的 SaaS 多租户在所有业务表机械增加 `tenant_id`。未来引入 Multi-Tenant 必须先走 ADR 和完整隔离设计。
- 自托管单机是正式支持场景。Redis、Search Engine、独立 Worker 等可选基础设施失效时，不得破坏 PostgreSQL 业务真相的一致性。

## 核心领域语义

- 业务逻辑内容以 `Resource` 为统一身份基础，但不得把所有领域压缩成无业务语义的通用 JSON。
- 跨领域二进制/内容对象统一称为 `Attachment`；`File`/`Path` 只能在 Personal Drive 或底层文件系统语境中使用，不得取代 Attachment 作为跨领域内容模型。
- `Resource`、`Attachment`、`Blob`、`Blob Placement/Replica` 必须保持身份和生命周期分离。业务对象不能直接依赖物理路径或 Provider `object_key`。
- 具有独立生命周期的内部实体主键统一使用平台 ID Generator 生成的 UUIDv7，并使用 PostgreSQL `uuid` 类型；不得使用自增整数、Snowflake、UUIDv4 或 Provider ID 作为内部实体主键。
- UUIDv7 不得替代 `created_at`、业务时间、排序号或版本号；不得把 `ORDER BY id` 当作严格业务时间排序。外部 Provider ID 使用 External Identity 映射；Blob 内容身份仍由加密 Hash 表达。
- 真实时间点在 PostgreSQL 使用 `timestamptz`，API 使用带 `Z` 或 Offset 的 RFC 3339/ISO 8601；禁止传递无时区的 naive datetime。展示、计划、统计自然日按应用配置时区计算，默认 UTC+8。
- 用户人工确认或修改的元数据优先级高于第三方来源、插件、扫描器和自动同步；不得无明确规则静默覆盖，并应保留来源/冲突语义。

## 模块、依赖与所有权

- 每个领域状态和持久化 Schema 必须有唯一 Owner。逻辑边界通过 Java Package Ownership、公开 API、Spring 组装边界和 Architecture Test 强制，而不是依赖 Maven 子模块隔离。
- 模块内部默认使用 `api / application / domain / adapter / persistence / config` 分层：Controller/Adapter 进入 Application Contract，Application 编排授权、事务与端口，Domain 保持业务不变量与状态转换，Persistence/Provider 作为实现细节。
- `domain` 必须尽量保持 Plain Java：不得依赖 WebFlux、R2DBC、Redis、HTTP、Storage SDK，不启动线程、不产生网络 IO、不直接读取 Spring SecurityContext。
- 跨模块只能依赖公开 API/Capability、Command/Query 或 Durable Event；禁止依赖其他模块的 Entity、Service 实现、Repository、Persistence Package、私有 SQL 或内部 Spring Bean。
- 禁止跨模块 Repository 注入、跨 Schema 随意 JOIN、通过 `ApplicationContext.getBean()` 或 Bean Name 猜测内部组件、让业务模块反向依赖 `server`。
- 跨模块双向依赖必须通过 Event、更小的 Capability Contract、Integration 协议或重新划分所有权解决；禁止把依赖不清的类型/实体/工具全部塞进 `common`。
- Controller 不得直接写 Repository，也不得横跨多个 Repository 拼接业务事务；请求必须进入唯一 Owner 的 Application API。
- Worker 不是数据库超级用户。Worker 必须通过 Background Task Claim、Task Handler Contract、目标领域 Application API、明确拥有的数据表或 Event/Outbox 工作，不得任意执行跨领域 SQL。
- Plugin 只能通过 Plugin API、Extension Point 和公开 Capability 访问核心能力；禁止直接访问 Core Repository、Entity、内部 Bean 或任意 Core SQL。插件数据、Migration、权限和运行时故障必须保持私有与隔离。

## 数据库、事务与迁移

- PostgreSQL 是核心业务状态的唯一主要真相源。Cache、Search、Analytics、Redis Stream 和内存 Event Bus 均不得替代业务真相。
- Owner Module 只能读写自己的 Schema；其他模块不得直接写入。跨 Owner 读取通过 Query API、Capability 或 Projection；不得因数据库物理共址而扩大领域所有权。
- 一个业务 Command 默认对应一个 Owner 的本地 PostgreSQL Reactive Transaction：读取状态 → 校验版本 → Domain Transition → 持久化 → 写 Outbox（必要时写审计引用）→ 提交。
- 领域状态变更与对应 Outbox INSERT 必须在同一事务原子提交；事务内默认禁止调用不可回滚的外部 API。跨领域流程使用 Command、Durable Event、Saga/Process Manager 或 Background Task，不建立系统级超级事务。
- 并发写默认使用乐观并发与 revision/version；唯一性和关键不变量必须尽量下降为数据库 Constraint，并用真实 PostgreSQL 集成测试验证。
- Blob 内容身份不可变：不得更新已存在 Blob 的 `content_hash`/`size_bytes` 来改变其内容；Attachment 替换内容应创建新的不可变绑定。Blob GC 必须同时检查有效业务引用、Retention Hold、Placement 状态和审计条件。
- 所有生产 DDL 必须进入版本化 `src/main/resources/db/migration/V<monotonic-version>__<description>.sql`，由 `r2dbc-migrate` 执行。已进入共享/正式环境的 Migration 不得原地修改；修复只能追加新 Migration。
- Migration 与业务 Persistence 职责分离。不得由 Repository、`DatabaseClient` 或启动逻辑偷偷修改 Schema、模拟 Migration History 或执行不可控的大规模数据重写。
- Schema 变更遵循 Expand → Migrate → Contract；大规模 Backfill 不得塞进启动阻塞 DDL；Seed Permission/Built-in Role 必须 deterministic；不得提供假装安全的 destructive down migration。
- Migration 完成前普通业务不得 Ready，Outbox Dispatcher 不得消费，Worker 不得 Claim，Scheduler 不得提交依赖新 Schema 的任务；Migration 失败时必须启动失败或保持 Readiness Down，不得带旧 Schema 提供部分业务能力。

## 事件、任务与派生数据

- 关键业务事件必须使用 PostgreSQL Outbox；内存事件仅可用于丢失后不会造成业务永久错误的提示、唤醒或 telemetry，绝不能替代 Outbox。
- Event 写入后视为不可变事实，使用版本化 Event Type/Schema；Event Envelope 至少携带 `event_id`、类型、版本、发生时间、Producer、Subject/Actor（适用时）、`correlation_id`、`causation_id` 和最小化 Payload。
- Durable Event 按至少一次投递设计。每个消费者必须使用稳定 Consumer Name + Event ID 去重，并在“写消费者结果/投影 + 插入 Inbox”同一事务中完成幂等提交。
- Event、Log、DTO、Cache、配置和普通数据库字段均不得携带 Secret 或 Secure Domain 明文。
- Background Task 必须持久化任务身份、类型、输入引用、状态、优先级、可执行时间、重试策略、进度、取消状态和追踪上下文；Task 与 Attempt 分离，失败历史不得原地覆盖，Retry 必须创建新 Attempt。
- Worker 使用 Claim/Lease/Heartbeat；Lease 到期后必须可被 reconciliation 恢复。取消是 cooperative cancellation，Handler 必须在安全检查点检查取消状态。
- Scheduled Job 只负责触发 Command 或入队 Background Task，不得直接执行长业务逻辑。Controller 中的裸 `subscribe()` 不构成可靠后台任务。
- Search、Analytics、AI 都是可延迟、可重建的派生数据。Search 不得回写业务真相；AI 修改业务状态必须重新进入目标 Domain Command，AI Adapter 不得直接写表。
- Cache 必须可关闭、可丢失；失效只能导致变慢或 Cache Miss，不得导致权限错误、数据丢失、状态回退或唯一性失效。缓存 Key 需包含命名空间、身份、版本以及必要的权限范围。

## Reactive、性能与大对象

- Reactor Event Loop 上禁止 JDBC Query、`Thread.sleep`、`Future.get`、阻塞文件扫描、大文件 Hash、FFmpeg/CLI 同步等待、阻塞 SDK 以及 `.block()`/`.blockFirst()`/`.blockLast()`。
- 运行时业务代码默认不得使用 `.block*()`；例外仅限明确隔离的 Bootstrap、CLI/Migration Tool、测试或专用 Blocking Adapter，并必须在 Review 中说明。无法替换的阻塞 SDK 必须封装在专用 bounded Scheduler/Executor 中。
- 转码、OCR、Embedding、批量 Thumbnail、Full Search Rebuild、Backup/Restore、Large Import、大文件 Hash、Archive Pack/Unpack 等高成本操作必须通过 Background Task 执行，HTTP 只负责提交/查询任务。
- 大量数据必须使用分页/cursor、稳定排序、受限 `flatMap` 并发和 chunk；禁止无界 `collectList()`、无界 `IN (...)`、默认无界全表列表或把全库/完整视频/大型归档一次性聚合入内存。
- 大文件上传、下载、复制、Hash、转码输入必须使用 Streaming/Channel/File API；禁止 `readAllBytes()`、整文件 `byte[]` 或收集成巨大 DataBuffer。HTTP Range 只表达恢复后的读取范围，不得伪造 Provider 级 Range Restore。
- 连接池、任务并发、Provider、媒体处理、AI、Search 重建和 Backup/Restore 必须有可配置且有上限的资源治理；不能用无限增大连接池掩盖慢 SQL。

## 安全、授权与敏感数据

- Authentication 与 Authorization 分离。Controller/HTTP Security 只是第一道门；Application Command、Automation、Plugin、Background Task、Internal Command 和 Realtime Channel 都必须执行授权判断。
- 对象级授权必须在目标 Domain/Authorization Capability 中权威判断；Search/Cache 只能做候选过滤，不能作为最终 ACL 依据。Platform RBAC 不替代 Resource ACL、Share 或 Room Membership。
- P0 身份认证采用无状态 JWT：请求必须校验签名、标准时间/受众约束、Subject、用户状态和 `security_version`。P0 不持久化登录 Session、Access/Refresh Token 原文或 Token Digest；用户级提前失效通过提升 `security_version`。
- 业务配置只保存 Secret Reference 或加密封装。禁止在 API Response、Event Payload、普通日志、Plugin Config、Cache 或数据库普通参数中传播 Secret 明文、Credential、密码或私密领域明文。
- 错误响应不得暴露 SQL、表名、物理路径、Stack Trace、Provider Credential、Java Class Name 或无必要的内部主机名；统一使用带稳定机器错误码的 Problem Contract，客户端不得解析自然语言 message。
- 高风险权限变更、永久删除、Blob GC、恢复、导出和 Secret Reveal 必须有显式确认；按权限策略要求 Step-up Verification，并写入可靠 Audit。
- Backup/Restore 与 Export/Import 是不同语义。Backup 面向实例灾难恢复，Export 面向用户迁移；Restore Point/Manifest 一旦可用应不可变，Secure Domain 备份仍保持密文，Search/Analytics 等派生状态在恢复后重建。
- Archive Base、不可重新获取数据的正式 Replica/Backup、Restore Window、Delivery Lease、Provider Retention/Object Lock 等安全边界必须尊重对应专项设计；自动分层、Promotion、Eviction、GC 和 Provider Drain 不得静默删除 Archive Base。

## HTTP、契约与兼容性

- HTTP-first：官方客户端、插件、脚本和第三方客户端原则上使用同一套公开能力。SSE/WebSocket/WebRTC 可用于实时场景，但不得成为绕过权限的隐藏业务通道。
- 稳定 HTTP API 必须登记到 HTTP Operation Registry 并进入 OpenAPI；`operationId` 唯一，公开 API 不得暴露数据库 Schema Ownership 或内部实现。
- API 默认遵循 `/api` 入口、JSON `snake_case` 字段、UUIDv7 身份、RFC 3339 时间、稳定 Problem `code`、cursor 分页和稳定排序。具体版本路径、Admin 命名空间和 operation 映射以 API Convention/OpenAPI 为准。
- 有并发更新风险的资源使用 ETag/If-Match 与 revision 语义；创建/会产生副作用的可重试操作使用 `Idempotency-Key`，必须区分同 Key 同请求与同 Key 不同请求。
- 长耗时动作返回异步任务语义（通常为 202），明确状态、进度、失败分类、取消、重试和 Retry Attempt；不得用普通同步 200 假装长期工作已完成。
- JSON 的 `null`、`unknown`、`not_applicable` 和 `empty` 不得混用；客户端必须容忍新增未知字段/枚举，正常版本升级不得无预警破坏稳定 API 或 Plugin API。
- `Resource`/`Attachment` 的逻辑删除、Trash、Purge 与 Blob 物理 GC 必须分离；普通 Cache 不得被承诺为用户主动维护的标准 Download，Replica 也不得与 Cache 混为一谈。

## 配置、可观测性与故障隔离

- 配置必须类型安全，明确类型、默认值、来源、校验、是否动态生效、是否需要重启以及是否为 Secret。默认来源顺序为 Built-in Default → File/Environment → Persistent Application Configuration。
- 日志必须结构化并在适用时包含 `request_id`、`trace_id`、`correlation_id`、`principal_id`、module、task/event ID 和 `error_code`；跨 HTTP、Command、DB、Outbox、Consumer、Task、Provider 使用 W3C Trace Context 和 correlation/causation。
- 必须区分 Liveness 与 Readiness；Readiness 至少反映 Migration、PostgreSQL 和核心模块状态。可选 Redis/Search/AI/Plugin 故障应按其 Required/Optional 属性降级，不得扩散为核心业务故障。
- 外部 HTTP 调用必须使用统一 WebClient/Client Factory，设置 Timeout、重试边界、TLS、限流/隔离和日志脱敏；不得对所有 POST 采用无条件“失败重试三次”。
- 可信代理范围必须显式配置；不得无条件信任任意 `X-Forwarded-*`。

## 测试与合并门禁

- Domain 测试不得启动 Spring Context；Reactive 测试使用 Reactor `StepVerifier`，不得用大量 `.block()` 掩盖生产异步问题。
- PostgreSQL 约束、事务、JSONB、partial index、`SKIP LOCKED`、`timestamptz` 和隔离级别必须使用真实 PostgreSQL Testcontainers 验证，不使用 H2 代替。
- CI/合并前必须覆盖适用的 Architecture Boundary、Migration、OpenAPI、Event Schema、Permission Registry、Outbox/Inbox Replay、Task Crash Recovery、Idempotency、Optimistic Concurrency、Range/Streaming 和 Security Leakage 测试。
- Architecture Test 至少阻止：Domain 依赖 Web/DB/Redis/Storage SDK；模块依赖其他模块实现；非 Owner 访问他人 Persistence；Controller 直接依赖 Repository；Plugin 访问 Core Internal；API 依赖 Implementation；业务模块反向依赖 Server。
- 常用验证命令：`mvn compile`、`mvn test`、`mvn verify`；Console 使用 `cd console; pnpm typecheck`、`pnpm lint`、`pnpm build`。涉及 Testcontainers 的完整测试需要 Docker Desktop。

## 变更前必须重新评估的事项

以下变化不能仅通过普通功能代码完成，必须先补 ADR/设计/契约/测试：

- 改变 Maven Multi-Module、Modular Monolith、Java/Spring/WebFlux/Reactor/R2DBC/PostgreSQL 主基线；
- 引入第二套业务关系数据库、JPA/JDBC 主栈、Flyway、Kafka/RabbitMQ 替代 Outbox；
- 改变 UUIDv7、带时区时间、Instance 边界、Resource-centric、Attachment/Blob/Placement 分离；
- 改变 Owner Schema、跨模块依赖、Outbox/Inbox 至少一次投递、Consumer 幂等或 Background Task/Lease 语义；
- 让 Cache、Search、Analytics 或 AI Projection 成为业务写模型；
- 改变 Secure Domain/Key Boundary、Plugin Isolation、权限模型或 Worker 的数据写权限；
- 为尚未出现的规模问题提前微服务化，或引入第二套隐藏的 Internal HTTP Business API。

## 主要规范入口

- [文档导航](docs/README.md)
- [工程实现索引](docs/00-product-baseline/Engineering-Implementation-Index.md)
- [技术架构](docs/00-product-baseline/Technical-Architecture-Design.md)
- [实施路线图与依赖图](docs/00-product-baseline/Implementation-Roadmap-and-Dependency-Graph.md)
- [模块与包所有权](docs/00-product-baseline/Module-Package-Ownership-Design.md)
- [P0 实现基线](docs/00-product-baseline/P0-Implementation-Baseline.md)
- [数据库设计](docs/00-product-baseline/Database-Overview-Design.md)
- [API 约定](docs/00-product-baseline/API-Convention-Design.md)
- [Command/Query/Event Catalog](docs/00-product-baseline/contracts/P0-Command-Query-Event-Catalog.md)
- [P0 数据库 Schema](docs/00-product-baseline/database/P0-Database-Schema-Design.md)
- [P0 验收/不变量测试矩阵](docs/00-product-baseline/testing/P0-Acceptance-Invariant-Test-Matrix.md)
- [构建与本地开发](BUILD.md)
