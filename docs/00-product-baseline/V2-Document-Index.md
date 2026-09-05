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
- [`Personal-Drive-File-Synchronization-Subsystem-Design.md`](./Personal-Drive-File-Synchronization-Subsystem-Design.md) — Drive Space、Drive Node、File Revision、上传下载、目录同步、Camera Backup、冲突与分享边界。
- [`Personal-Drive-File-Synchronization-P0-Semantics.md`](./Personal-Drive-File-Synchronization-P0-Semantics.md) — Drive Change Generation、Tombstone / Restore、Atomic Save、Quota、Camera Backup 与 P0 同步一致性语义。
- [`Sharing-Collaboration-Room-Subsystem-Design.md`](./Sharing-Collaboration-Room-Subsystem-Design.md) — Share、Invite、Room、Membership、Presence、实时状态、Sequence、Replay 与权限收敛。
- [`Offline-Cache-Device-Synchronization-Subsystem-Design.md`](./Offline-Cache-Device-Synchronization-Subsystem-Design.md) — Device、Download / Cache、Pending Mutation、Change Feed / Cursor、Tombstone、Conflict、Full Resync 与 Secure Offline Data。
- [`Search-Discovery-Subsystem-Design.md`](./Search-Discovery-Subsystem-Design.md) — Search Document、权限感知查询、索引投影、Generation Rebuild、失败恢复与 Semantic Search 边界。
- [`Backup-Restore-Data-Portability-Subsystem-Design.md`](./Backup-Restore-Data-Portability-Subsystem-Design.md) — Restore Point、Backup Manifest、一致恢复点、Verification、Restore、Retention、Export / Import 与安全边界。
- [`Background-Task-Scheduler-Design.md`](./Background-Task-Scheduler-Design.md) — 后台任务、调度、重试、状态与 Worker 执行模型。
- [`Platform-Integration-Automation-Design.md`](./Platform-Integration-Automation-Design.md) — Capability、Command、Event、Automation 与外部集成。
- [`Platform-Administration-Operations-Subsystem-Design.md`](./Platform-Administration-Operations-Subsystem-Design.md) — 平台配置、通知、审计、运维、管理与可观测性。
- [`Data-Analytics-Statistics-Subsystem-Design.md`](./Data-Analytics-Statistics-Subsystem-Design.md) — Activity、统计、分析投影与数据边界。

### 第三层：身份与高敏感数据

- [`Security-Identity-Authorization-Crypto-Subsystem-Design.md`](./Security-Identity-Authorization-Crypto-Subsystem-Design.md) — 身份、JWT 无状态认证、Step-up Verification、RBAC / ACL、授权与密码学边界。
- [`Secure-Data-Foundation-Design.md`](./Secure-Data-Foundation-Design.md) — Secure Domain 通用安全基础。
- [`Password-Manager-Subsystem-Design.md`](./Password-Manager-Subsystem-Design.md) — 密码管理器领域模型与安全交互。
- [`Private-Notes-Subsystem-Design.md`](./Private-Notes-Subsystem-Design.md) — 私密笔记的安全数据模型与访问边界。

### 第四层：专业内容领域

- [`Content-Creation-Revision-Collaborative-Document-Subsystem-Design.md`](./Content-Creation-Revision-Collaborative-Document-Subsystem-Design.md) — Document / Article / Note、Working Copy、不可变 Revision、Publication、Comment / Annotation 与协同编辑边界。
- [`Media-Video-Anime-Playback-Subsystem-Design.md`](./Media-Video-Anime-Playback-Subsystem-Design.md) — Work / Season / Episode、Media Release、Probe / Track / Subtitle、Playback Variant、Transcoding、Playback Session 与 Progress。
- [`Reading-Comic-Novel-Ebook-Subsystem-Design.md`](./Reading-Comic-Novel-Ebook-Subsystem-Design.md) — Comic / Novel / Ebook、Edition、Volume / Chapter / Page、稳定 Reading Locator、Progress、Annotation、OCR / Translation。
- [`Music-Library-Playback-Queue-Subsystem-Design.md`](./Music-Library-Playback-Queue-Subsystem-Design.md) — Artist / Album / Edition / Disc / Track、Audio Source、Lyrics、Playlist、Queue、Shuffle / Repeat 与播放历史。
- [`Photo-Album-Image-Asset-Subsystem-Design.md`](./Photo-Album-Image-Asset-Subsystem-Design.md) — Photo、Original Asset、EXIF、Capture Time、Location、Manual / Smart Album、Preview / Thumbnail 与 AI Artifact。
- [`Game-Digital-Asset-Archive-Subsystem-Design.md`](./Game-Digital-Asset-Archive-Subsystem-Design.md) — Game、Edition / Version / Platform、Installer / Patch / MOD / Save / Manual、Compatibility、Archive 与 Plugin Action 边界。

### 第五层：业务增强能力

- [`AI-Intelligence-Subsystem-Design.md`](./AI-Intelligence-Subsystem-Design.md) — AI Provider、Context、Tool、智能处理与安全边界。
- [`AI-Persona-System-Design.md`](./AI-Persona-System-Design.md) — AI Persona、角色配置与行为边界。
- [`Productivity-Planning-Subsystem-Design.md`](./Productivity-Planning-Subsystem-Design.md) — Task、Project、Goal、OKR 等效率规划能力。
- [`Personal-Finance-Accounting-Subsystem-Design.md`](./Personal-Finance-Accounting-Subsystem-Design.md) — 个人财务、账户、交易、预算与账本能力。

---

## 2. 客户端交互文档

### App / 多端交互

[`app-interaction/`](./app-interaction/) 描述用户侧 App 的信息架构和交互，包括：

- 登录、账户、应用 Shell、首页与统一搜索；
- Resource Library；
- Personal Drive、文件访问、目录备份与设备同步；
- 视频、阅读、音乐、图片、游戏等内容消费 / 归档；
- 文档、文章、普通 Note 与创作；
- 分享、Room 与协作；
- Offline / Download / Device Sync；
- AI、Analytics、Automation；
- Productivity、Finance、Password Manager、Private Notes；
- Notification 等多端能力。

入口：[`app-interaction/README.md`](./app-interaction/README.md)

Personal Drive 独立交互规格：[`app-interaction/drive/personal-drive-file-sync.md`](./app-interaction/drive/personal-drive-file-sync.md)

### CMS / Web Console 交互

[`cms-console-interaction/`](./cms-console-interaction/) 描述 CMS / Web Console 的管理端交互，包括：

- Workbench；
- 身份与安全；
- Attachment / Storage；
- Personal Drive、同步状态与管理入口；
- 内容创作与内容管理；
- AI、Analytics、Integration / Automation；
- 平台配置、通知、审计与系统运维；
- Secure Domain 管理入口。

入口：[`cms-console-interaction/README.md`](./cms-console-interaction/README.md)

Personal Drive 独立管理规格：[`cms-console-interaction/personal-drive/README.md`](./cms-console-interaction/personal-drive/README.md)

CMS 路由与权限矩阵：[`cms-console-interaction/route-permission-matrix.md`](./cms-console-interaction/route-permission-matrix.md)

### 原型

[`prototypes/`](./prototypes/) 保存 V2 交互原型草稿，仅用于辅助讨论，不应作为服务端领域契约、数据库 Schema 或 API 的事实来源。

---

## 3. 设计覆盖矩阵

| 能力 | PRD / 系统概要 | 服务端详细设计 | App 交互 | CMS 交互 | 当前状态 |
|---|---|---|---|---|---|
| Resource / Collection / Relation / User State | ✅ | ✅ `Core-Resource-Library-Subsystem-Design.md` | ✅ | 部分 | 核心契约已覆盖 |
| Content Ingestion / Import / Metadata Sync | ✅ | ✅ `Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md` | 间接 | 部分 | 核心契约已覆盖 |
| Attachment / Blob / Storage | ✅ | ✅ `Attachment-Blob-Storage-Subsystem-Design.md` | 间接 | ✅ | 核心契约已覆盖 |
| Personal Drive / File Sync / Camera Backup | ✅ | ✅ 主设计 + P0 Semantics | ✅ `app-interaction/drive/personal-drive-file-sync.md` | ✅ `cms-console-interaction/personal-drive/README.md` + Route / Permission Matrix | 产品、系统、服务端与 App / CMS 交互均已覆盖；根导航与权限索引已同步 |
| Sharing / Collaboration / Room | ✅ | ✅ `Sharing-Collaboration-Room-Subsystem-Design.md` | ✅ | 部分 | 核心契约已覆盖 |
| Offline Cache / Device Sync | ✅ 系统原则 | ✅ `Offline-Cache-Device-Synchronization-Subsystem-Design.md` | ✅ | 不适用 | 核心契约已覆盖 |
| Content Creation / Revision / Collaborative Document | ✅ | ✅ `Content-Creation-Revision-Collaborative-Document-Subsystem-Design.md` | ✅ | ✅ | 核心契约已覆盖 |
| 视频 / 动画 / 影视专业领域 | ✅ | ✅ `Media-Video-Anime-Playback-Subsystem-Design.md` | ✅ | 部分 | 核心契约已覆盖 |
| 漫画 / 小说 / Ebook | ✅ | ✅ `Reading-Comic-Novel-Ebook-Subsystem-Design.md` | ✅ | 部分 | 核心契约已覆盖 |
| 音乐 | ✅ | ✅ `Music-Library-Playback-Queue-Subsystem-Design.md` | ✅ | 部分 | 核心契约已覆盖 |
| 图片 / 相册 | ✅ | ✅ `Photo-Album-Image-Asset-Subsystem-Design.md` | ✅ | 部分 | 核心契约已覆盖 |
| 游戏 / 数字资产 | ✅ | ✅ `Game-Digital-Asset-Archive-Subsystem-Design.md` | ✅ | 部分 | 核心契约已覆盖 |
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
| Search / Discovery | ✅ | ✅ `Search-Discovery-Subsystem-Design.md` | ✅ | 部分 | 增量投影、权限、重建与失败恢复契约已覆盖 |
| Backup / Restore / Data Portability | ✅ | ✅ `Backup-Restore-Data-Portability-Subsystem-Design.md` | 间接 | 运维入口 | 恢复点、校验、恢复、保留与迁移契约已覆盖 |

### 3.1 覆盖结论

按当前 PRD、System Overview、App Interaction、CMS Interaction 与已合入专项设计进行交叉检查后：

1. **未发现仍然缺少服务端领域契约、且会阻塞 V2 核心编码的大型 P0 领域。**
2. Search / Discovery 原本只有分散在 System Overview 与 Core Resource 中的投影原则；现已补充独立的索引、权限、Generation Rebuild 与失败恢复契约。
3. Backup / Restore / Data Portability 原本只有系统级原则；现已补充 Restore Point、Manifest、Verification、Restore Activation、Retention 与 Export / Import 契约。
4. Personal Drive / File Synchronization 已完成 PRD、System Overview、主设计、P0 Semantics、App 独立交互规格、CMS 独立管理规格，以及 App / CMS 根导航与 CMS Route / Permission Matrix 的索引同步；当前不再存在“交互待补”的覆盖缺口。
5. Personal Drive 的权限索引继续保持 **Platform ADMIN ≠ Drive File READ**，并将 Drive 内容读取与运维诊断、Quota / Policy、Attachment / Storage 管理分离。

以下方向已经有足够的系统级 / 领域级边界，只有在实际复杂度出现时再拆专项设计更合适：

- Notification Delivery Provider / Template Runtime；
- Advanced Search Ranking / Personalized Recommendation；
- WebRTC / Large-scale Realtime Gateway；
- Large-scale CRDT / Collaborative Editing Runtime；
- Multi-node Transcode Worker / Hardware Acceleration Scheduler；
- Resource Merge / Split / Entity Resolution；
- Advanced Metadata Provider Mapping；
- Device Installation Projection / Game Launcher Integration；
- Photo Face / Object Organization（需要额外隐私设计）。

---

## 4. 关键跨领域边界

本轮补齐后，后续实现尤其应保持以下边界：

1. **Resource ≠ Attachment ≠ Blob**：逻辑内容身份、可关联内容对象和实际字节身份分离。
2. **Drive Node / Path ≠ Attachment / Blob**：用户文件树与文件版本是组织及历史语义，不把路径重新变成内容身份。
3. **Download ≠ Cache ≠ Server Replica**：显式离线副本、可淘汰缓存和服务端存储副本分离。
4. **Working Copy ≠ Revision ≠ Publication**：编辑态、不可变历史和已发布版本分离。
5. **Media Release ≠ Playback Variant**：源版本与播放清晰度 / 转码方案分离。
6. **Playlist ≠ Playback Queue**：长期集合与当前播放上下文分离。
7. **Reading Locator ≠ Pixel Offset**：跨设备阅读位置使用逻辑定位。
8. **Photo Original ≠ Preview / Thumbnail**：用户原始内容和可重建派生内容分离。
9. **Game Asset Available ≠ Installed**：服务器存在安装包不代表任何设备已安装。
10. **Share / Room ≠ Resource ACL**：授权入口、协作上下文和目标对象最终权限分离。
11. **Sync Runtime ≠ Domain Conflict Resolver**：同步负责可靠传播，领域负责业务合并。
12. **Search / Analytics / AI Projection ≠ Business Truth**：派生数据可重建，不反向成为业务真相源。
13. **Backup / Restore ≠ Export / Import**：灾难恢复保存实例恢复语义，数据迁移保存开放、版本化和可合并语义。
14. **Backup Success ≠ Restore Verified**：任务成功、恢复点发布、完整性验证和真实恢复演练必须分开表达。

---

## 5. 文档一致性规则

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
6. Prototype 只能验证交互，不自动成为接口和数据模型约束；
7. 如果详细设计新增了 PRD / System Overview 尚未定义的新产品域，必须在覆盖矩阵明确标记，不能因为“已有详细设计”就默认产品范围已经批准。

---

## 6. 新增子系统设计的最低内容要求

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

- Download / Cache / Server Replica 分离；
- Device Registration / Revocation；
- Pending Mutation ID；
- 服务端 Sync Cursor / Change Feed；
- Tombstone Retention / Full Resync；
- 冲突路由到目标领域；
- 本地加密；
- 权限撤销传播；
- Secure Domain 密文同步。

对于 Personal Drive / 文件同步功能还应补充：

- Drive Node / Path 与 Attachment / Blob 身份分离；
- File Revision 不可变与覆盖写语义；
- Trash / Restore / Permanent Delete 与 Blob GC 分离；
- Sync Binding、Local Item Mapping 与稳定服务端 Cursor；
- 单向 Backup 与双向 Sync 的删除传播边界；
- Conflict Copy 与禁止静默丢数据；
- Camera Backup 与 Photo Projection 的状态分离；
- Quota、Tombstone、Atomic Save 与失败恢复。

对于搜索功能还应补充：

- Search Document 与稳定文档身份；
- Source Version / Projector Version；
- 权限候选过滤与最终权威授权分离；
- 增量索引、Dead Letter 与 Reconciliation；
- Full Rebuild / Generation 切换；
- Facet / Suggestion 的信息泄露边界；
- Search Engine 故障时业务写入降级；
- Semantic / Embedding 的可选能力与敏感数据边界。

对于备份 / 恢复功能还应补充：

- Restore Point 与不可变 Manifest；
- PostgreSQL + Blob 的一致恢复语义；
- Full / Incremental Chain；
- Verification 与 Restore Drill；
- Restore Preflight / Activation；
- Retention / Pin / Safe Prune；
- Secure Material 独立保护；
- Backup / Restore 与 Export / Import 分离。

---

## 7. 后续文档策略

当前阶段不再建议为了“看起来完整”继续批量拆分设计文档。

Personal Drive 的产品、系统、服务端、App / CMS 交互，以及根导航 / Route Permission Matrix 的索引治理已经完成。本 PR 当前不继续进入 Database Schema、API 或 Command / Event 实现映射。

下一步更有价值的是在后续独立步骤进入实现前设计审查：

1. 持续校验 Personal Drive App / CMS 入口、Deep Link 与权限矩阵是否与独立交互规格保持一致，避免后续新增页面重新并入 Attachment / Storage 或弱化 `Platform ADMIN ≠ Drive File READ`；
2. 从 P0 范围提取实际模块 / Package Ownership；
3. 将领域不变量映射为 PostgreSQL Constraint / Transaction Boundary；
4. 将 Command / Query / Event 映射为 API 与内部接口；
5. 为跨域事件建立契约版本与 Outbox / Consumer 幂等策略；
6. 为每个子系统生成首批 Flyway Schema 设计；
7. 建立 V2 implementation roadmap 与依赖图；
8. 只有当某个实现问题超出现有设计边界时，再新增专项设计。

这样可以避免文档继续横向膨胀，却迟迟不进入可验证的工程实现。
