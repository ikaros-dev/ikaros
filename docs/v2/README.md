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
- [`Offline-Cache-Device-Synchronization-Subsystem-Design.md`](./Offline-Cache-Device-Synchronization-Subsystem-Design.md) — Device、Download / Cache、Pending Mutation、Change Feed / Cursor、Tombstone、Conflict、Full Resync 与 Secure Offline Data。
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
- 视频、阅读、音乐、图片、游戏等内容消费 / 归档；
- 文档、文章、普通 Note 与创作；
- 分享、Room 与协作；
- Offline / Download / Device Sync；
- AI、Analytics、Automation；
- Productivity、Finance、Password Manager、Private Notes；
- Notification 等多端能力。

入口：[`app-interaction/README.md`](./app-interaction/README.md)

### CMS / Web Console 交互

[`cms-console-interaction/`](./cms-console-interaction/) 描述 CMS / Web Console 的管理端交互，包括：

- Workbench；
- 身份与安全；
- Attachment / Storage；
- 内容创作与内容管理；
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
| Resource / Collection / Relation / User State | ✅ | ✅ `Core-Resource-Library-Subsystem-Design.md` | ✅ | 部分 | 核心契约已覆盖 |
| Content Ingestion / Import / Metadata Sync | ✅ | ✅ `Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md` | 间接 | 部分 | 核心契约已覆盖 |
| Attachment / Blob / Storage | ✅ | ✅ | 间接 | ✅ | 已覆盖 |
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
| Search / Discovery | ✅ | 核心投影契约已覆盖；实现级设计视复杂度再拆分 | ✅ | 部分 | 基础已覆盖 |
| Backup / Restore / Data Portability | ✅ | System Overview + Database Overview 已定义核心原则 | 间接 | 运维入口 | 基础已覆盖；生产化时可拆专项设计 |

### 3.1 覆盖结论

按当前 PRD、System Overview、App Interaction 与 CMS Interaction 的产品能力进行交叉检查后，**暂未发现仍然缺少服务端领域契约、且会阻塞 V2 核心编码的大型 P0 领域**。

这不表示所有未来实现细节都应提前写成独立文档。以下方向已经有系统级 / 领域级边界，只有在实际复杂度出现时再拆专项设计更合适：

- Notification Delivery Provider / Template Runtime；
- Search Engine / Ranking / Index Operations；
- Backup / Disaster Recovery / Data Portability；
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
2. **Download ≠ Cache ≠ Server Replica**：显式离线副本、可淘汰缓存和服务端存储副本分离。
3. **Working Copy ≠ Revision ≠ Publication**：编辑态、不可变历史和已发布版本分离。
4. **Media Release ≠ Playback Variant**：源版本与播放清晰度 / 转码方案分离。
5. **Playlist ≠ Playback Queue**：长期集合与当前播放上下文分离。
6. **Reading Locator ≠ Pixel Offset**：跨设备阅读位置使用逻辑定位。
7. **Photo Original ≠ Preview / Thumbnail**：用户原始内容和可重建派生内容分离。
8. **Game Asset Available ≠ Installed**：服务器存在安装包不代表任何设备已安装。
9. **Share / Room ≠ Resource ACL**：授权入口、协作上下文和目标对象最终权限分离。
10. **Sync Runtime ≠ Domain Conflict Resolver**：同步负责可靠传播，领域负责业务合并。
11. **Search / Analytics / AI Projection ≠ Business Truth**：派生数据可重建，不反向成为业务真相源。

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
6. Prototype 只能验证交互，不自动成为接口和数据模型约束。

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

---

## 7. 后续文档策略

当前阶段不再建议为了“看起来完整”继续批量拆分设计文档。

下一步更有价值的是进入实现前设计审查：

1. 从 P0 范围提取实际模块 / Package Ownership；
2. 将领域不变量映射为 PostgreSQL Constraint / Transaction Boundary；
3. 将 Command / Query / Event 映射为 API 与内部接口；
4. 为跨域事件建立契约版本与 Outbox / Consumer 幂等策略；
5. 为每个子系统生成首批 Flyway Schema 设计；
6. 建立 V2 implementation roadmap 与依赖图；
7. 只有当某个实现问题超出现有设计边界时，再新增专项设计。

这样可以避免文档继续横向膨胀，却迟迟不进入可验证的工程实现。