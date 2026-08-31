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

### 第二层：平台底座

- [`Core-Resource-Library-Subsystem-Design.md`](./Core-Resource-Library-Subsystem-Design.md) — Resource、Collection、Relation、Tag、External Identity、Metadata Provenance、用户状态、生命周期与搜索投影。
- [`Attachment-Blob-Storage-Subsystem-Design.md`](./Attachment-Blob-Storage-Subsystem-Design.md) — Attachment / Blob / Replica / 分层存储与内容生命周期。
- [`Background-Task-Scheduler-Design.md`](./Background-Task-Scheduler-Design.md) — 后台任务、调度、重试、状态与 Worker 执行模型。
- [`Platform-Integration-Automation-Design.md`](./Platform-Integration-Automation-Design.md) — Capability、Command、Event、Automation 与外部集成。
- [`Platform-Administration-Operations-Subsystem-Design.md`](./Platform-Administration-Operations-Subsystem-Design.md) — 平台配置、运维、管理与可观测性。
- [`Data-Analytics-Statistics-Subsystem-Design.md`](./Data-Analytics-Statistics-Subsystem-Design.md) — Activity、统计、分析投影与数据边界。

### 第三层：身份与高敏感数据

- [`Security-Identity-Authorization-Crypto-Subsystem-Design.md`](./Security-Identity-Authorization-Crypto-Subsystem-Design.md) — 身份、会话、RBAC / ACL、授权与密码学边界。
- [`Secure-Data-Foundation-Design.md`](./Secure-Data-Foundation-Design.md) — Secure Domain 通用安全基础。
- [`Password-Manager-Subsystem-Design.md`](./Password-Manager-Subsystem-Design.md) — 密码管理器领域模型与安全交互。
- [`Private-Notes-Subsystem-Design.md`](./Private-Notes-Subsystem-Design.md) — 私密笔记的安全数据模型与访问边界。

### 第四层：业务增强能力

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
- AI、Analytics、Automation；
- Productivity、Finance、Password Manager、Private Notes；
- Notification、Offline Cache 等多端能力。

入口：[`app-interaction/README.md`](./app-interaction/README.md)

### CMS / Web Console 交互

[`cms-console-interaction/`](./cms-console-interaction/) 描述 CMS / Web Console 的管理端交互，包括：

- Workbench；
- 身份与安全；
- Attachment / Storage；
- 内容创作；
- AI、Analytics、Integration / Automation；
- 平台配置与系统运维；
- Secure Domain 管理入口。

入口：[`cms-console-interaction/README.md`](./cms-console-interaction/README.md)

### 原型

[`prototypes/`](./prototypes/) 保存 V2 交互原型草稿，仅用于辅助讨论，不应作为服务端领域契约、数据库 Schema 或 API 的事实来源。

---

## 3. 设计覆盖矩阵

| 能力 | PRD / 系统概要 | 服务端详细设计 | App 交互 | CMS 交互 | 当前状态 |
|---|---|---|---|---|---|
| Resource / Collection / Relation / User State | ✅ | ✅ `Core-Resource-Library-Subsystem-Design.md` | ✅ | 部分 | 核心契约已补齐 |
| Attachment / Blob / Storage | ✅ | ✅ | 间接 | ✅ | 已覆盖 |
| 身份 / 授权 / Crypto | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Secure Data 基础 | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Background Task / Scheduler | ✅ | ✅ | 间接 | 运维入口 | 已覆盖 |
| Plugin / Integration / Automation | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| AI Intelligence | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| AI Persona | ✅ | ✅ | ✅ | 间接 | 已覆盖 |
| Analytics / Statistics | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Productivity | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Personal Finance | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Password Manager | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| Private Notes | ✅ | ✅ | ✅ | ✅ | 已覆盖 |
| 视频 / 动画 / 影视专业领域 | ✅ | 待独立详细设计 | ✅ | 部分 | **待补充** |
| 漫画 / 小说专业领域 | ✅ | 待独立详细设计 | ✅ | 部分 | **待补充** |
| 音乐专业领域 | ✅ | 待独立详细设计 | ✅ | 部分 | **待补充** |
| 图片 / 相册专业领域 | ✅ | 待独立详细设计 | ✅ | 部分 | **待补充** |
| 内容创作 / Revision / 协作文档 | ✅ | 待独立详细设计 | ✅ | ✅ | **待补充** |
| Sharing / Collaboration / Room | ✅ | 待独立详细设计 | ✅ | 部分 | **待补充** |
| Notification | ✅ | 待独立详细设计 | ✅ | 部分 | **待补充** |
| Offline Cache / Device Sync | 系统原则 | 待独立详细设计 | ✅ | 不适用 | **待补充** |
| Search / Discovery | ✅ | 核心投影契约已覆盖；实现级设计视复杂度再拆分 | ✅ | 部分 | 基础已覆盖 |

说明：

- “待独立详细设计”不表示当前 PRD 或系统概要完全没有描述，而是缺少与现有 Storage / Security / AI 等同层级的服务端领域设计文档。
- 交互文档可以先定义用户体验，但不能替代领域所有权、事务边界、数据不变量、Command / Event、权限与失败语义。
- Search 的业务边界已在核心资源库设计中补齐；如果后续从 PostgreSQL FTS 演进到独立搜索引擎，再增加专门的索引、Ranking 与运维设计。

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

- WebSocket / SSE / WebRTC 的职责边界；
- reconnect / replay / sequence / presence 语义；
- 服务端权威状态与客户端临时状态的区别。

对于离线功能还应补充：

- 可离线数据范围；
- 本地加密；
- 同步游标；
- 冲突策略；
- 删除传播；
- 设备撤销与密钥失效。

---

## 6. 当前建议的后续补充顺序

按对其他模块的依赖程度，建议后续优先级：

1. **Sharing / Collaboration / Room** — 同时影响视频、音乐、文档、权限和实时协议。
2. **Content Creation / Revision / Collaborative Document** — PRD 已明确创作与协作，CMS 交互也已存在，需要服务端契约承接。
3. **Media Domain** — 细化作品 / Season / Episode / Playback Variant / Track / Subtitle / Derived Attachment 与转码任务。
4. **Notification** — 统一站内通知、推送渠道、偏好、去重、已读状态和事件订阅。
5. **Offline Cache / Device Sync** — 多端客户端落地前必须明确一致性、删除传播与 Secure Domain 限制。
6. **Reading / Music / Photo** — 在核心 Resource 与 Attachment 契约稳定后分别补齐专业模型。

该顺序不是版本承诺，只表示从当前文档依赖关系看，优先补充这些设计能减少后续重复定义和返工。
