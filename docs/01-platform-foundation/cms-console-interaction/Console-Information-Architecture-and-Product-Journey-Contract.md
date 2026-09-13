# Ikaros V2 Console 信息架构与产品旅程契约

> 状态：候选规范（本变更合并后生效）
>
> 适用范围：Ikaros Web Console / CMS Console 的全局导航、工作区边界、跨子系统任务呈现、用户语言、路由迁移与产品验收。
>
> 本文是 `cms-console-interaction/README.md` 的全局信息架构补充与覆盖规范。发生冲突时，**本文关于全局导航、一级入口、跨子系统 Activity、Apps/System 边界、路由迁移和产品旅程验收的规则优先**；各子系统文档继续负责页面内字段、状态、权限、安全和领域行为。

## 1. 背景与目标

Ikaros V2 的领域模型、API、任务、存储和权限设计保持 Resource-centric、Attachment/Blob 分离、HTTP-first 和能力可组合原则，但 Console 不得把内部子系统树、数据库对象树或后台状态机直接暴露为用户的信息架构。

Console 的首要目标不是“让每个后端能力都有一个菜单”，而是让管理员和家庭用户能够围绕真实任务完成工作：

- 找到、整理和处理自己的内容；
- 把新内容加入 Ikaros；
- 发现当前需要处理的失败、冲突或不可用状态；
- 了解数据是否安全、在哪里、是否正在恢复；
- 配置少量必要系统能力；
- 在需要时进入高级诊断，而不是日常面对实现细节。

## 2. 规范优先级

Console 设计与实现按以下顺序解释：

1. `Product-Requirements-Document.md`：产品范围、阶段和核心概念；
2. **本文**：全局 IA、跨子系统用户旅程、用户语言与迁移；
3. `cms-console-interaction/README.md`：通用组件与页面交互规则；
4. 各 `cms-console-interaction/*` 子系统文档：领域页面行为；
5. 后端子系统设计：领域状态、API、安全、不变量。

不得因为后端存在一个模块、Controller、Command、Query、Task 类型或 Issue，就自动创建一级导航、独立页面或新的“XX 中心”。

## 3. 一级信息架构

默认桌面 Console 的稳定一级入口限定为：

| 一级入口 | 核心问题 | 默认路由 |
|---|---|---|
| Overview | 现在有什么需要我关注？ | `/overview` |
| Library | 我的内容在哪里、状态如何？ | `/library` |
| Add Content | 怎么把内容加入 Ikaros？ | `/add` |
| Activity | Ikaros 现在正在做什么？ | `/activity` |
| Storage | 我的数据在哪里、是否安全？ | `/storage` |
| Apps | 我启用了哪些可选产品能力？ | `/apps` |
| System | 用户、权限、插件、配置和诊断在哪里？ | `/system` |

### 3.1 禁止按内部子系统扩张一级导航

以下类型默认不得成为新的一级 Sidebar Entry：

- `*-center` 形式的后端/工程子系统；
- Task、Worker、Execution、Attempt、Lease；
- Attachment、Blob、Placement、Replica；
- Provider、Delivery Provider、Metadata Provider；
- Search Index、Event Outbox、Webhook Delivery；
- 某个插件声明的内部模块；
- P1/P2 规划能力仅因为代码已存在就提前进入默认导航。

新增一级入口必须同时满足：

1. 是稳定、独立、持续使用的用户心智模型，而不是实现模块；
2. 无法合理归入 Library / Add Content / Activity / Storage / Apps / System；
3. 有完整产品旅程和独立 Golden Path；
4. 经过产品契约变更，不允许由功能 Issue 自行决定。

## 4. Overview：Attention-first，而非 KPI-first

Overview 的首屏必须优先展示“需要用户处理的事项”和“正在进行的工作”，而不是数据库对象数量。

### 4.1 必须支持的 Attention 类型

至少包含：

- 导入失败或存在待确认项目；
- 内容不可用、Missing、Corrupted、Restore 失败；
- 存储 Provider 不健康或未知且超过检查时限；
- 元数据冲突或外部更新待确认；
- 后台任务失败且需要人工操作；
- 备份、同步、索引重建等长期任务失败；
- 权限或配置导致关键功能不可用。

每条 Attention 必须能进入**最接近用户对象的详情页**，而不是先跳到内部任务/Blob/Provider 列表。

### 4.2 健康状态规则

- `unknown`、未探测、接口失败、数据缺失不得渲染为 Healthy/正常。
- 只有真实、近期、来源明确的健康结果才能显示为正常。
- Overview 某个 Widget 请求失败时，可保留其他 Widget，但必须显式显示该区域“未知/加载失败”。

## 5. Library：统一内容入口

`Resource` 仍是平台统一逻辑身份，但默认 UI 使用用户能理解的内容类型和名称。

Library 负责：

- 全局搜索与过滤；
- 按动画、电影、视频、漫画、图书、音乐、图片、文档等业务类型浏览；
- Collection / Tag / Lifecycle / Availability / Source 等筛选；
- 进入 canonical Resource Detail；
- 处理归档、回收站、收藏、用户状态等资源级行为。

不得把“资源中心”和“内容与媒体”作为两个平级用户入口继续扩张。媒体、阅读、图片等业务体验可以作为 Library 的类型视图或 Apps 中的专属体验，但必须回到同一 Resource 身份。

## 6. Canonical Resource Detail

Resource Detail 是调查内容问题和进行内容级操作的默认起点。

日常视图优先展示：

- 标题、类型、封面/摘要；
- 内容结构（剧集、章节、曲目、图片等）；
- 用户可理解的可用性状态；
- 元数据来源与冲突；
- 相关内容、集合、标签、活动；
- 直接操作：播放、阅读、恢复、刷新元数据、归档等。

实现对象只在 `Advanced` / `Diagnostics` 中展示：

- Resource ID / Attachment ID / Blob ID；
- SHA-256；
- Placement / Replica；
- Provider ID / Provider Key；
- Task Attempt / Worker / Lease；
- Idempotency Key；
- Raw payload / internal status code。

### 6.1 问题解释必须从业务对象出发

例如“第 07 集不可用”应解释为：

`剧集 → 内容可用性 → 当前只有归档副本 → 正在恢复/可发起恢复`

而不是要求用户手动经过：

`Resource → Attachment → Blob → Placement → Provider`

## 7. Add Content：隐藏 Source/Scan/Plan/Run 状态机

导入的用户旅程统一为：

1. 选择来源；
2. 扫描/上传；
3. 预览识别结果；
4. 处理重复、映射和需要确认项；
5. 确认导入；
6. 在 Activity 中观察执行；
7. 完成后进入 Library。

后端可以继续使用 Source / Scan / Candidate / Plan / Plan Item / Run / Run Item，但这些是实现模型，不得要求普通用户理解。

异常情况下可以显示高级详情和内部 ID，但主操作必须仍然是：`确认`、`跳过`、`重试`、`取消`、`查看资源`。

## 8. Activity：统一所有后台工作

以下概念在用户层统一进入 Activity：

- Background Task；
- AI Job；
- Operations Job；
- Automation Execution；
- Import Run；
- Metadata Sync；
- Archive / Restore；
- Blob Repair / Verify / GC；
- Backup / Export / Migration；
- Download / Cache；
- Index rebuild 等长任务。

### 8.1 Activity 列表必须使用业务语义

每一行至少包含：

- 类型（Import / Storage / Metadata / Backup / AI 等）；
- 用户可理解的动作描述；
- 关联业务对象；
- 状态；
- 进度；
- 最近更新时间；
- 失败时的下一步操作。

### 8.2 高级执行信息

Execution Attempt、Worker、Lease、重试次数、内部 Payload 只在 Activity Detail 的 Advanced 区域显示。

A04 等后台任务能力 Issue 继续验证 scheduler 契约，但 Console 产品验收统一通过 Activity 完成。

## 9. Storage 工作区

Storage 一级工作区固定分为：

- Overview；
- Providers；
- Policy；
- Archive & Restore；
- Maintenance（Advanced）。

### 9.1 Overview

回答：数据分布、容量、可用性、异常、恢复中数量和近期失败。

### 9.2 Providers

只负责配置和查看真实存储后端：健康、容量、凭据状态、启停、探测。

### 9.3 Policy

负责 HOT/WARM/COLD/ARCHIVE/DEEP 等层级策略、生命周期规则和放置目标，不与 Provider 配置混为一屏。

### 9.4 Archive & Restore

负责用户可理解的归档/恢复队列和结果。

### 9.5 Maintenance

仅面向高级维护：

- Blob integrity；
- GC candidate / physical cleanup；
- Placement/Replica diagnostics；
- Repair / Promote；
- Delivery/CDN 深度诊断。

“输入 Blob ID 查询 Placement”不得作为 Storage 默认首页的核心操作。

## 10. Apps 与 System

### 10.1 Apps

P1/P2 的产品能力默认进入 Apps，包括但不限于：

- Media；
- Drive；
- Documents；
- Planning；
- Private Notes；
- Passwords；
- Finance；
- AI；
- Sharing/Rooms；
- Analytics。

Apps 只显示当前部署已启用且当前用户有访问能力的应用。插件可声明 App Entry，但必须显示来源。

### 10.2 System

System 负责：

- Users / Roles / Permissions / Authentication；
- Audit；
- Plugins / Integrations；
- Notifications configuration；
- Platform settings；
- Diagnostics / Health；
- Advanced system events and maintenance。

当前用户自己的 Profile / Preferences / Security 从头像菜单进入，不建立独立一级“个人中心”。

## 11. 用户语言规则

默认页面标题、主导航、主按钮和主状态禁止使用只有实现人员才能理解的词作为唯一文案。

### 11.1 默认隐藏到 Advanced 的词汇

`Resource ID`、`Attachment ID`、`Blob`、`Placement`、`Replica`、`Task Attempt`、`Worker`、`Lease`、`Provider Key`、`Idempotency Key`、`Event Outbox` 等。

### 11.2 可以保留的领域词

当用户需要配置外部系统时可以使用 `Provider`，但必须附业务语义，例如：

- `存储 Provider`；
- `元数据来源`；
- `Delivery Provider` 仅在高级分发配置中出现。

## 12. 旧路由迁移契约

迁移期间不得一次性删除旧页面。新路由建立后，旧 URL 必须保留 redirect 或兼容入口，并记录迁移状态。

建议映射：

| 旧路由族 | 新路由/位置 |
|---|---|
| `/dashboard` | `/overview` |
| `/resource-center/library*` | `/library*` |
| `/resource-center/collections` | `/library/collections` |
| `/content-center/*` | `/library?...` 或 `/apps/*` |
| `/ingestion-center/import` | `/add` |
| `/ingestion-center/tasks` | `/activity?type=import` |
| `/operations-center/jobs`、`/operations-center/background` | `/activity` 或 `/system/diagnostics` |
| `/ai-center/jobs` | `/activity?type=ai` |
| `/integration-center/executions` | `/activity?type=automation` |
| `/storage-center/tiers` | `/storage` / `/storage/policy` |
| `/storage-center/providers` | `/storage/providers` |
| Blob/GC/Placement 页面 | `/storage/maintenance/*` |
| `/edge-acceleration/*` | `/storage/delivery/*` 或 Maintenance |
| `/identity-center/*` | `/system/access/*` |
| Audit | `/system/audit` |
| Plugin/Integration | `/system/integrations` |
| Platform config | `/system/settings` |
| Planning/Finance/Notes/Passwords/AI 等 | `/apps/*` |
| `/account-center/*` | 头像菜单对应页面 |

最终映射由迁移 Issue 基于实际 Router 清单逐项维护；任何移除旧 URL 的 PR 必须有对应迁移证据。

## 13. 开发与 Issue 规则

### 13.1 功能 Issue 不得自行增加一级导航

所有既有 Axx/Bxx/Cxx/Rxx 功能 Issue 默认只交付其领域能力与适用页面，不得自行：

- 新增一级 Sidebar Entry；
- 新增“XX 中心”作为默认用户入口；
- 因 API/表/Task 存在而新增独立页面；
- 将实现对象提升为用户主对象。

确需修改全局 IA，必须单独修改本文并经过 Review。

### 13.2 Capability Acceptance 与 Product Journey Acceptance 分离

原有 Issue 继续验证 API、持久化、权限、状态机和失败恢复；Console 是否可用还必须通过对应 Product Journey / Golden Path 验收。

“API 可调用”“按钮存在”“页面能打开”不等价于产品完成。

## 14. 完成定义

Console IA 重构完成至少满足：

- 默认一级导航不再按后端子系统数量增长；
- Overview 不伪造健康状态；
- Library 成为内容统一入口；
- Add Content 不要求理解内部导入状态机；
- Activity 聚合跨子系统后台工作；
- Storage 的 Provider、Policy、Restore、Maintenance 分层；
- P1/P2 应用通过 Apps 暴露，不污染核心管理导航；
- implementation vocabulary 默认进入 Advanced；
- 旧 URL 有可验证迁移策略；
- 核心 Golden Path 通过 E2E 或可重复人工验收。

参见：[Console Golden Path Acceptance Contract](./Console-Golden-Path-Acceptance-Contract.md)。
