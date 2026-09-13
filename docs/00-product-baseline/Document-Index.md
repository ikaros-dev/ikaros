# Ikaros 文档索引与设计覆盖矩阵

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

[`app-interaction/`](./app-interaction/) 描述用户侧 App 的信息架构和交互，包括登录、账户、应用 Shell、Resource Library、Drive、媒体消费、文档创作、分享、Offline、AI、Analytics、Automation、Productivity、Finance 和安全数据产品。

入口：[`app-interaction/README.md`](./app-interaction/README.md)

Personal Drive 独立交互规格：[`app-interaction/drive/personal-drive-file-sync.md`](./app-interaction/drive/personal-drive-file-sync.md)

### CMS / Web Console 交互

[`cms-console-interaction/`](./cms-console-interaction/) 描述 Web Console 的最终态管理交互。当前 V2 按从零重构处理，不以旧菜单或旧 Route 为兼容约束。

Console 一级工作区固定为：

- **Overview**：Attention 与正在进行的工作；
- **Library**：统一 Resource 浏览、搜索和 canonical Resource Detail；
- **Add Content**：来源、预览、确认和导入；
- **Activity**：所有长期后台工作；
- **Storage**：Overview、Providers、Policy、Archive & Restore、Maintenance、Backup；
- **Apps**：Drive、Documents、Media、Planning、Finance、Private Notes、Passwords、AI、Sharing、Analytics、Automation 等可选产品；
- **System**：Access、Audit、Integrations、Notifications、Settings、Health、Diagnostics。

关键设计文件：

- [`cms-console-interaction/README.md`](./cms-console-interaction/README.md) — 通用 Console 交互规范；
- [`cms-console-interaction/Console-Information-Architecture-and-Product-Journey-Contract.md`](./cms-console-interaction/Console-Information-Architecture-and-Product-Journey-Contract.md) — canonical IA；
- [`cms-console-interaction/Console-Product-Journey-Acceptance-Contract.md`](./cms-console-interaction/Console-Product-Journey-Acceptance-Contract.md) — Golden Path / Product Journey Acceptance；
- [`cms-console-interaction/route-permission-matrix.md`](./cms-console-interaction/route-permission-matrix.md) — canonical Route / Permission Matrix；
- [`cms-console-interaction/personal-drive/README.md`](./cms-console-interaction/personal-drive/README.md) — Drive App 管理规格。

`cms-console-interaction/*` 的历史目录名只表示领域文档容器，不代表全局 Sidebar 分组。

### 原型

[`prototypes/`](./prototypes/) 保存 V2 交互原型草稿，仅用于辅助讨论，不应作为服务端领域契约、数据库 Schema 或 API 的事实来源。

---

## 3. 设计覆盖矩阵

| 能力 | PRD / 系统概要 | 服务端详细设计 | App 交互 | CMS 交互 | 当前状态 |
|---|---|---|---|---|---|
| Resource / Collection / Relation / User State | ✅ | ✅ Core Resource | ✅ | ✅ Library / Resource Detail | 核心契约已覆盖 |
| Content Ingestion / Import / Metadata Sync | ✅ | ✅ Ingestion / Metadata | 间接 | ✅ Add Content + Resource Metadata | 核心契约已覆盖 |
| Attachment / Blob / Storage | ✅ | ✅ Storage | 间接 | ✅ Storage Workspace | 核心契约已覆盖 |
| Personal Drive / File Sync / Camera Backup | ✅ | ✅ 主设计 + P0 Semantics | ✅ | ✅ Apps / Drive | 产品、系统、服务端与交互均已覆盖 |
| Sharing / Collaboration / Room | ✅ | ✅ Sharing | ✅ | ✅ Apps / Sharing | 核心契约已覆盖 |
| Offline Cache / Device Sync | ✅ | ✅ Offline / Sync | ✅ | Apps / Drive / Media 适用入口 | 核心契约已覆盖 |
| Content Creation / Revision / Collaborative Document | ✅ | ✅ Document | ✅ | ✅ Apps / Documents | 核心契约已覆盖 |
| 视频 / 动画 / 影视专业领域 | ✅ | ✅ Media | ✅ | Library + Apps / Media | 核心契约已覆盖 |
| 漫画 / 小说 / Ebook | ✅ | ✅ Reading | ✅ | Library / 专业 App 适用入口 | 核心契约已覆盖 |
| 音乐 | ✅ | ✅ Music | ✅ | Library + Apps / Media | 核心契约已覆盖 |
| 图片 / 相册 | ✅ | ✅ Photo | ✅ | Library / 专业 App 适用入口 | 核心契约已覆盖 |
| 游戏 / 数字资产 | ✅ | ✅ Game | ✅ | Library / 专业 App 适用入口 | 核心契约已覆盖 |
| 身份 / 授权 / Crypto | ✅ | ✅ | ✅ | ✅ System / Access | 已覆盖 |
| Secure Data 基础 | ✅ | ✅ | ✅ | ✅ Apps 安全域 | 已覆盖 |
| Background Task / Scheduler | ✅ | ✅ | 间接 | ✅ Unified Activity | 已覆盖 |
| Plugin / Integration / Automation | ✅ | ✅ | ✅ | ✅ System / Integrations + Apps / Automation | 已覆盖 |
| Notification | ✅ | Platform Administration | ✅ | ✅ System / Notifications + Account Preferences | 已覆盖 |
| AI Intelligence / Persona | ✅ | ✅ | ✅ | ✅ Apps / AI | 已覆盖 |
| Analytics / Statistics | ✅ | ✅ | ✅ | ✅ Apps / Analytics | 已覆盖 |
| Productivity | ✅ | ✅ | ✅ | ✅ Apps / Planning | 已覆盖 |
| Personal Finance | ✅ | ✅ | ✅ | ✅ Apps / Finance | 已覆盖 |
| Password Manager | ✅ | ✅ | ✅ | ✅ Apps / Passwords | 已覆盖 |
| Private Notes | ✅ | ✅ | ✅ | ✅ Apps / Private Notes | 已覆盖 |
| Search / Discovery | ✅ | ✅ Search | ✅ | ✅ Library Search / Global Search | 已覆盖 |
| Backup / Restore / Data Portability | ✅ | ✅ Backup / Restore | 间接 | ✅ Storage / Backup + Activity | 已覆盖 |

### 3.1 覆盖结论

1. 当前 P0 核心领域均已有服务端设计基线。
2. Console 已增加 canonical IA 与 Product Journey Acceptance，不再把“能力页面存在”视为产品完成。
3. Personal Drive 继续保持 **Platform ADMIN ≠ Drive File READ**，Drive 内容读取、运维诊断和 Storage 管理互相分离。
4. Background Task / Scheduler 的工程模型通过统一 Activity 暴露，不再为每个子系统创建任务中心。
5. Storage 的 Provider、Policy、Archive & Restore、Maintenance 已在 Console 设计中分层。

以下方向只有在实际复杂度出现时再拆专项设计：

- Notification Delivery Provider / Template Runtime；
- Advanced Search Ranking / Recommendation；
- WebRTC / Large-scale Realtime Gateway；
- Large-scale CRDT Runtime；
- Multi-node Transcode Scheduler；
- Resource Merge / Split / Entity Resolution；
- Advanced Metadata Provider Mapping；
- Device Installation Projection；
- Photo Face / Object Organization。

---

## 4. 关键跨领域边界

1. **Resource ≠ Attachment ≠ Blob**：逻辑内容身份、关联内容对象和实际字节身份分离。
2. **Drive Node / Path ≠ Attachment / Blob**：用户文件树和文件版本不把路径重新变成内容身份。
3. **Download ≠ Cache ≠ Server Replica**。
4. **Working Copy ≠ Revision ≠ Publication**。
5. **Media Release ≠ Playback Variant**。
6. **Playlist ≠ Playback Queue**。
7. **Reading Locator ≠ Pixel Offset**。
8. **Photo Original ≠ Preview / Thumbnail**。
9. **Game Asset Available ≠ Installed**。
10. **Share / Room ≠ Resource ACL**。
11. **Sync Runtime ≠ Domain Conflict Resolver**。
12. **Search / Analytics / AI Projection ≠ Business Truth**。
13. **Backup / Restore ≠ Export / Import**。
14. **Backup Success ≠ Restore Verified**。
15. **Capability Acceptance ≠ Console Product Journey Acceptance**。

---

## 5. 文档一致性规则

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

1. 先确认产品需求是否变化；
2. 系统级原则先在 System Overview 统一；
3. 数据库与 API 跨域规则不得由单个子系统自行覆盖；
4. 子系统负责领域所有权、不变量、Command / Event 和失败语义；
5. 交互文档通过公开能力实现体验，不制造隐藏业务通道；
6. Prototype 只验证交互；
7. 详细设计新增产品域时必须在覆盖矩阵明确标记；
8. **Console 全局 IA 变化必须先修改 Console IA 契约，功能 Issue 不得自行新增一级导航。**

---

## 6. 新增子系统设计的最低内容要求

新的服务端子系统设计至少包含：

- 目标、范围与非目标；
- 领域所有权和核心实体；
- 不变量；
- 生命周期 / 状态机；
- 数据库关键约束；
- Command / Query / Event；
- 权限、隐私与审计；
- 幂等、并发、一致性与失败恢复；
- Background Task / Automation / Plugin 集成；
- Search / Analytics / AI 投影边界；
- 典型流程；
- 可观测性；
- 测试和验收基线。

实时、创作、媒体、导入同步、离线、Drive、搜索和备份等领域继续遵守各专项设计已经定义的最低契约要求。

---

## 7. 后续文档策略

当前阶段不为“看起来完整”继续无边界拆文档。后续优先：

1. 保持 Console canonical IA、Route Matrix 与各领域交互文档一致；
2. 将 P0 领域映射到模块 / Package Ownership；
3. 将不变量映射为 PostgreSQL Constraint / Transaction Boundary；
4. 将 Command / Query / Event 映射为 API 与内部接口；
5. 建立事件契约版本和 Outbox / Consumer 幂等策略；
6. 建立 Schema / Migration 和实施依赖图；
7. 只有实现问题超出现有设计边界时再新增专项设计。
