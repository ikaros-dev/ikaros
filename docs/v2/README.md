# Ikaros V2 文档索引与设计覆盖矩阵

> 本目录保存 Ikaros V2 从产品需求、系统架构、数据库与 API 约定，到各业务 / 平台子系统及客户端交互的设计文档。
>
> V2 为从零重构设计。阅读具体子系统文档前，应优先理解 PRD、系统概要、数据库概要与 API 约定，避免局部设计重新定义系统级规则。

---

## 1. 推荐阅读顺序

### 第一层：产品与系统基线

1. [`Product-Requirements-Document.md`](./Product-Requirements-Document.md) — 产品愿景、核心概念、功能需求与边界。
2. [`System-Overview-Design.md`](./System-Overview-Design.md) — 总体架构、子系统边界、平台联动、一致性与部署原则。
3. [`Database-Overview-Design.md`](./Database-Overview-Design.md) — PostgreSQL-first 数据模型、实体关系与数据库约束。
4. [`API-Convention-Design.md`](./API-Convention-Design.md) — HTTP API、错误、分页、幂等、并发、任务与实时接口约定。

### 第二层：平台与内容底座

- [`Core-Resource-Library-Subsystem-Design.md`](./Core-Resource-Library-Subsystem-Design.md) — Resource、Collection、Relation、Tag、External Identity、Metadata Provenance、用户状态、生命周期与搜索投影。
- [`Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md`](./Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md) — Source、Scan、Candidate、Match、Import Plan、Metadata Refresh、Provenance、幂等与失败恢复。
- [`Attachment-Blob-Storage-Subsystem-Design.md`](./Attachment-Blob-Storage-Subsystem-Design.md) — Attachment / Blob / Replica / 分层存储与内容生命周期。
- [`Sharing-Collaboration-Room-Subsystem-Design.md`](./Sharing-Collaboration-Room-Subsystem-Design.md) — Share、Invite、Room、Membership、Presence、实时状态、Sequence、Replay 与权限收敛。
- [`Background-Task-Scheduler-Design.md`](./Background-Task-Scheduler-Design.md) — 后台任务、调度、重试、状态与 Worker 执行模型。
- [`Platform-Integration-Automation-Design.md`](./Platform-Integration-Automation-Design.md) — Capability、Command、Event、Automation 与外部集成。
- [`Platform-Administration-Operations-Subsystem-Design.md`](./Platform-Administration-Operations-Subsystem-Design.md) — 平台配置、通知、审计、运维、管理与可观测性。
- [`Data-Analytics-Statistics-Subsystem-Design.md`](./Data-Analytics-Statistics-Subsystem-Design.md) — Activity、统计、分析投影与数据边界。

### 第三层：身份与高敏感数据

- [`Security-Identity-Authorization-Crypto-Subsystem-Design.md`](./Security-Identity-Authorization-Crypto-Subsystem-Design.md) — 身份、会话、RBAC / ACL、授权与密码学边界。
- [`Secure-Data-Foundation-Design.md`](./Secure-Data-Foundation-Design.md) — Secure Domain 通用安全基础。
- [`Password-Manager-Subsystem-Design.md`](./Password-Manager-Subsystem-Design.md) — 密码管理器领域模型与安全交互。
- [`Private-Notes-Subsystem-Design.md`](./Private-Notes-Subsystem-Design.md) — 私密笔记的安全数据模型与访问边界。

### 第四层：专业内容领域

- [`Content-Creation-Revision-Collaborative-Document-Subsystem-Design.md`](./Content-Creation-Revision-Collaborative-Document-Subsystem-Design.md) — Document / Article / Note、Working Copy、不可变 Revision、Publication、Comment / Annotation 与协同编辑边界。
- [`Media-Video-Anime-Playback-Subsystem-Design.md`](./Media-Video-Anime-Playback-Subsystem-Design.md) — Work / Season / Episode、Media Release、Probe / Track / Subtitle、Playback Variant、Transcoding、Playback Session 与 Progress。

### 第五层：业务增强能力

- [`AI-Intelligence-Subsystem-Design.md`](./AI-Intelligence-Subsystem-Design.md) — AI Provider、Context、Tool、智能处理与安全边界。
- [`AI-Persona-System-Design.md`](./AI-Persona-System-Design.md) — AI Persona、角色配置与行为边界。
- [`Productivity-Planning-Subsystem-Design.md`](./Productivity-Planning-Subsystem-Design.md) — Task、Project、Goal、OKR 等效率规划能力。
- [`Personal-Finance-Accounting-Subsystem-Design.md`](./Personal-Finance-Accounting-Subsystem-Design.md) — 个人财务、账户、交易、预算与账本能力。

---

## 2. 客户端交互文档

### App / 多端交互

[`app-interaction/`](./app-interaction/) 描述用户侧 App 的信息架构和交互，包括：

- 登录与账户；
- 首页、统一资源库与搜索；
- 视频、阅读、音乐、图片、游戏等内容消费；
- 文档、文章、普通 Note 与创作；
- 分享、Room 与协作；
- AI、Analytics、Automation；
- Productivity、Finance、Password Manager、Private Notes；
- Notification、Offline Cache 等多端能力。

入口：[`app-interaction/README.md`](./app-interaction/README.md)

### CMS / Web Console 交互

[`cms-console-interaction/`](./cms-console-interaction/) 描述 CMS / Web Console 的管理端交互，包括：

- Workbench；
- 身份与安全；
- Attachment / Storage；
- 内容创作与媒体管理；
- AI、Analytics、Integration / Automation；
- 平台配置、通知、审计与系统运维；
- Secure Domain 管理入口。

入口：[`cms-console-interaction/README.md`](./cms-console-interaction/README.md)

### 原型

[`prototypes/`](./prototypes/) 保存 V2 交互原型草稿，仅用于辅助讨论，不应作为服务端领域契约、数据库 Schema 或 API 的事实来源。

---

## 3. 设计覆盖矩阵

| 能力 | PRD / 系统概要 | 服务端详细设计 | App 交互 | CMS 交互 | 当前状态 |
|---|---|---|---|---|---|
| Resource / Collection / Relation / User State | ✅ | ✅ `Core-Resource-Library-Subsystem-Design.md` | ✅ | 部分 | 核心契约已补齐 |
| Content Ingestion / Import / Metadata Sync | ✅ | ✅ `Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md` | 间接 | 部分 | 核心导入与同步契约已补齐 |
| Attachment / Blob / Storage | ✅ | ✅ | 间接 | ✅ | 已覆盖 |
| Sharing / Collaboration / Room | ✅ | ✅ `Sharing-Collaboration-Room-Subsystem-Design.md` | ✅ | 部分 | 核心分享与实时协作契约已补齐 |
| Content Creation / Revision / Collaborative Document | ✅ | ✅ `Content-Creation-Revision-Collaborative-Document-Subsystem-Design.md` | ✅ | ✅ | 核心创作、版本、发布与协作契约已补齐 |
| 视频 / 动画 / 影视专业领域 | ✅ | ✅ `Media-Video-Anime-Playback-Subsystem-Design.md` | ✅ | 部分 | 核心媒体与播放契约已补齐 |
| 身份 / 授权 / Crypto | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Secure Data 基础 | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Background Task / Scheduler | ✅ | ✅ | 间接 | 运维入口 | 已覆盖 |
| Plugin / Integration / Automation | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Notification | ✅ | 核心能力已由 Platform Administration & Operations 设计覆盖 | ✅ | ✅ | 基础已覆盖；复杂度增长时再拆专项文档 |
| AI Intelligence | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| AI Persona | ✅ | ✅ | ✅ | 间接 | 已覆盖 |
| Analytics / Statistics | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Productivity | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Personal Finance | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Password Manager | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Private Notes | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| 漫画 / 小说专业领域 | ✅ | 待独立详细设计 | ✅ | 部分 | **待补充** |
| 音乐专业领域 | ✅ | 待独立详细设计 | ✅ | 部分 | **待补充** |
| 图片 / 相册专业领域 | ✅ | 待独立详细设计 | ✅ | 部分 | **待补充** |
| Offline Cache / Device Sync | 系统原则 | 待独立详细设计 | ✅ | 不适用 | **待补充** |
| Search / Discovery | ✅ | 核心投影契约已覆盖；实现级设计视复杂度再拆分 | ✅ | 部分 | 基础已覆盖 |
| Backup / Restore / Data Portability | ✅ | System Overview + Database Overview 已定义核心原则 | 间接 | 运维入口 | 基础已覆盖；生产化时可拆专项设计 |

说明：

- “待独立详细设计”不表示当前 PRD 或系统概要完全没有描述，而是缺少与现有 Storage / Security / AI 等同层级的服务端领域设计文档。
- 交互文档可以先定义用户体验，但不能替代领域所有权、事务边界、数据不变量、Command / Event、权限与失败语义。
- Content Creation 设计明确区分 Working Copy、不可变 Revision 与 Publication；autosave 不等于 Revision，恢复旧版本通过创建新 Revision 完成。
- Media 设计明确区分 Media Release / Version 与 Playback Variant / Quality，避免把不同源版本和转码清晰度混成同一概念。
- Notification 已在 `Platform-Administration-Operations-Subsystem-Design.md` 中覆盖 Notification Center、状态、来源、Channel、Template、Rule、用户偏好、Provider、Delivery Log、重试与通知风暴控制，因此不再视为“完全缺失”。
- Search 的业务边界已在核心资源库设计中补齐；如果后续从 PostgreSQL FTS 演进到独立搜索引擎，再增加专门的索引、Ranking 与运维设计。
- Backup / Restore / Data Portability 已在 System Overview 与 Database Overview 中定义一致恢复点、Schema Compatibility、Secure Key Material、派生数据重建等系统级要求；真正进入生产恢复演练和跨版本迁移实现时再拆专项文档更合适。

---

## 4. 文档一致性规则

新增或修改 V2 设计时应遵守以下优先级：

```text
Product Requirements Document
        ↓
System Overview Design
        ↓
Database Overview + API Convention
        ↓
Subsystem Design
        ↓
App / CMS Interaction
        ↓
Prototype / Implementation
```

发生冲突时：

1. 先确认是否是产品需求发生变化；
2. 系统级原则必须先在 System Overview 中统一；
3. 数据库与 API 的跨域规则不得由单个子系统自行覆盖；
4. 子系统负责定义自己的领域所有权、不变量、Command / Event 和失败语义；
5. 交互文档通过公开能力实现体验，不反向制造隐藏业务通道；
6. Prototype 只能验证交互，不自动成为接口和数据模型约束。

---

## 5. 新增子系统设计的最低内容要求

为避免设计文档只描述“功能清单”，新的服务端子系统设计至少应包含：

- 目标、范围与非目标；
- 领域所有权和核心实体；
- 必须长期成立的不变量；
- 生命周期 / 状态机；
- 数据库关键约束；
- Command / Query / Event 契约；
- 权限、隐私与审计；
- 幂等、并发、一致性与失败恢复；
- Background Task / Automation / Plugin 集成；
- 与 Search / Analytics / AI 的投影边界；
- 典型流程；
- 可观测性；
- 测试和验收基线。

对于实时功能还应补充：

- HTTP / WebSocket / SSE / WebRTC 的职责边界；
- reconnect / replay / sequence / presence 语义；
- 服务端权威状态与客户端临时状态的区别；
- 实时连接中的权限撤销传播。

对于创作 / Revision 功能还应补充：

- Working Copy 与正式 Revision 分离；
- Revision 不可变；
- Restore 产生新 Revision；
- autosave / revision checkpoint 策略；
- Publication 固定 Revision；
- Comment / Annotation Anchor；
- Merge / Conflict；
- realtime operation 与持久 Revision 的边界。

对于媒体功能还应补充：

- Work / Season / Episode 专业结构；
- Release / Source Version 与 Playback Variant 分离；
- Probe / Track / Subtitle；
- Direct Play / Direct Stream / Transcode；
- Derived Attachment 与 Transcode Profile；
- Playback Session / Progress / History；
- Progress 乱序合并；
- Room 权威播放状态与个人 Progress 分离。

对于导入 / 同步功能还应补充：

- Source 与 Credential 边界；
- Scan / Candidate / Match / Plan / Run 分层；
- Dry Run / Preview；
- Checkpoint / Retry / Cancel；
- 幂等和去重；
- 外部来源删除与内部生命周期分离；
- Metadata Provenance 与人工修改优先级。

对于离线功能还应补充：

- 可离线数据范围；
- 本地加密；
- 同步游标；
- 冲突策略；
- 删除传播；
- 设备撤销与密钥失效。

---

## 6. 当前建议的后续补充顺序

经过 Core Resource、Ingestion、Collaboration、Content Creation 与 Media 设计补齐后，后续优先级调整为：

1. **Offline Cache / Device Sync** — 已经横跨 Document、Media、Private Notes 与多端 Progress；需要尽快明确同步游标、冲突、删除传播、本地加密与设备撤销。
2. **Reading Domain** — Comic / Novel / Ebook 的 Volume / Chapter / Page / Layout / Reading Progress 专业模型。
3. **Music Domain** — Artist / Album / Disc / Track / Queue / Lyrics / Audio Fingerprint 等专业模型。
4. **Photo Domain** — Photo / Album / EXIF / Preview / Original / Burst / Live Photo 等专业模型。

后续视实现复杂度再决定是否拆分：

- Notification Delivery 专项设计；
- Search Engine / Ranking 专项设计；
- Backup / Disaster Recovery / Data Portability 专项设计；
- WebRTC / 大规模 Realtime Gateway；
- Resource Merge / Split 与专业 Metadata Mapping；
- 大规模 CRDT / Collaborative Editing Runtime；
- 多节点 Transcode Worker / Hardware Acceleration Scheduler。

该顺序不是版本承诺，只表示从当前文档依赖关系看，优先补充这些设计能减少后续重复定义和返工。