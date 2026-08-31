# CMS Console 路由、菜单与权限矩阵

> 本文是 CMS Console Router、Navigation、页面级 Guard 和按钮级权限控制的实施附件。
>
> Capability Key 使用 V2 安全设计中的点分命名风格，例如 `private_note.key.reset`。本文中的 Key 用于明确交互层的能力边界；后端安全子系统形成 canonical Permission Catalog 后，前端必须一对一映射到 canonical key，不允许在各页面自行创造另一套同义权限。

## 1. 三层权限控制

### 1.1 路由层

Router 定义每个受保护 Route 所需的最小“查看/进入”能力。

- 未认证：401 → 登录/会话恢复。
- 已认证但无 Route Capability：403。
- Secure Domain 未解锁：不是 403，进入 Vault Unlock / Secure Session 流程。
- 后端策略要求隐藏实体存在性时，详情页可返回 404，但列表/菜单权限仍按 Capability 处理。

### 1.2 菜单层

- 当前用户没有页面查看能力：菜单项不渲染。
- 子系统下所有页面均不可见时：整个子系统分组不渲染。
- 子系统只剩一个可访问页面时仍保留分组结构，避免权限变化造成信息架构跳变。
- 菜单可见性只是 UX，不能代替后端鉴权。

### 1.3 操作层

- 用户可以查看页面但没有创建/编辑/删除能力时，页面保持正常可读。
- 普通无权限操作默认隐藏；当解释“为什么不能操作”具有价值时可 Disabled + Tooltip。
- 高风险操作同时满足：Authentication + Authorization + Required Verification Level + Security Policy。

## 2. 全局与当前用户页面

| 页面 | Route | 导航入口 | 查看/进入能力 | 主要写操作能力 | Step-up / 额外条件 |
|---|---|---|---|---|---|
| 首次初始化 | `/setup` | 登录前自动 | `system.initialization.execute` 或初始化专用一次性状态 | 创建初始管理员、基础配置 | 仅系统未初始化时可用；后端一次性锁 |
| 登录 | `/login` | 登录前 | 公共认证入口 | Authentication | 限流、锁定、安全策略 |
| OTP / 登录验证 | `/login/verify` | 认证事务 | 有效认证事务 | `security.verification.perform` | 满足所需 SVL |
| 账号恢复 | `/recovery/**` | 登录页 | 公共恢复入口 | `security.recovery.perform` | 恢复策略、OTP/恢复材料 |
| 个人资料 | `/console/account/profile` | 头像菜单 | `account.self.read` | `account.self.update` | 邮箱/登录标识变更可要求 Step-up |
| 偏好设置 | `/console/account/preferences` | 头像菜单 | `account.preference.read` | `account.preference.update` | 策略锁定项只读 |
| 个人通知 | `/console/account/notifications` | 头像菜单 | `account.notification.read` | `account.notification.update` | 强制安全通知不可关闭 |
| 我的安全 | `/console/account/security` | 头像菜单 | `account.security.read` | `account.security.update` | 改密、恢复、撤销会话可要求 Step-up |

## 3. 工作台

| 页面 | Route | 菜单 | 查看能力 | 页面内主要动作 |
|---|---|---|---|---|
| 概览 | `/console/dashboard` | 工作台 / 概览 | `dashboard.read` | Widget 自定义：`account.preference.update`；具体卡片动作继续检查目标能力 |
| 全局搜索 | `/console/search` | 工作台 / 全局搜索 | `search.use` | 搜索结果只返回当前用户有读取权限的实体；收藏使用 `resource.favorite.update` |
| 我的活动与收藏 | `/console/activity` | 工作台 / 我的活动与收藏 | `activity.self.read` | 收藏修改 `resource.favorite.update`；导出 `activity.self.export` |

## 4. 内容与创作

| 页面 | Route | 查看能力 | 创建/修改 | 删除/生命周期 | 其他高风险/专用能力 |
|---|---|---|---|---|---|
| 统一资源库 | `/console/resources` | `resource.read` | `resource.create` / `resource.update` | `resource.archive` / `resource.trash` | 永久清理 `resource.purge` |
| Resource 详情 | `/console/resources/{id}` | `resource.read` | `resource.update` | 同上 | 分享 `resource.share`；元数据冲突处理 `resource.metadata.resolve` |
| 集合/标签/关系 | `/console/collections` | `collection.read` | `collection.create` / `collection.update` | `collection.delete` | 标签合并 `tag.merge`；关系修改 `resource.relation.update` |
| 文章与文档 | `/console/documents` | `document.read` | `document.create` / `document.update` | `document.archive` / `document.delete` | 发布 `document.publish`；历史恢复 `document.version.restore` |
| 媒体消费 | `/console/media` | `media.history.read` | 进度/队列 `media.progress.update` | 历史删除 `media.history.delete` | 播放资源仍要求 Resource/Attachment 读取能力 |
| 分享与协作 | `/console/sharing` | `share.read` | `share.create` / `share.update` | `share.revoke` | 协作权限修改 `collaboration.permission.update` |

### 内容页面规则

- `resource.read` 不自动授予 Attachment Blob 物理管理能力。
- `resource.update` 不自动授予覆盖人工锁定元数据的能力；元数据来源冲突使用独立能力/策略。
- `resource.trash` 与 `resource.purge` 必须分离；永久清理不得因为拥有普通删除能力而出现。

## 5. Personal Drive / 个人网盘

> Personal Drive 是独立业务子系统，不归入 Attachment / Storage。Capability Key 在本矩阵中表达交互层最小边界，最终 canonical key 仍以安全子系统 Permission Catalog 为准。

| 页面 | Route | 查看/进入能力 | 写操作能力 | 高风险 / 额外条件 |
|---|---|---|---|---|
| 文件空间 / Drive Home | `/console/drive` | `drive.space.read`；展示文件名、目录结构、缩略图、预览或下载时还需 `drive.file.read` | `drive.file.write` / `drive.folder.write` | 永久删除不得由普通写权限隐含授予 |
| Drive Space 列表 / 详情 | `/console/drive/spaces`、`/console/drive/spaces/{spaceId}` | `drive.space.read` | `drive.space.manage`（仅允许的空间级配置） | 他人 Space 仍受 ACL / Scope 限制 |
| Drive Node / File Detail | `/console/drive/nodes/{nodeId}` | `drive.file.read` | `drive.file.write` | 分享/ACL 按独立能力；内容读取不能由平台管理员角色隐含获得 |
| Revision History | `/console/drive/revisions` 或 File Detail Tab | `drive.revision.read` + 目标 File READ | `drive.revision.write` | Restore as New Revision：`drive.revision.restore`；需要时 Step-up |
| 传输中心 | `/console/drive/transfers` | `drive.transfer.read` | `drive.transfer.manage` | 没有 `drive.file.read` 时仅显示安全诊断摘要，不显示文件名/路径/预览 |
| 同步关系 | `/console/drive/sync`、`/console/drive/sync/{bindingId}` | `drive.sync.read` | `drive.sync.manage` | 危险删除传播策略、Force Reconcile 可要求 Step-up；本地路径按额外设备诊断权限最小化展示 |
| 冲突处理 | `/console/drive/conflicts` | `drive.conflict.read` | `drive.conflict.resolve` | 显式丢弃一侧内容属于高影响动作；Preview 仍需 `drive.file.read` |
| 回收站 | `/console/drive/trash` | `drive.trash.read`；文件名/原路径仍受 `drive.file.read` / Scope | `drive.trash.restore` | Permanent Delete：`drive.file.purge` + Step-up / Policy |
| 配额 | `/console/drive/quota` | `drive.quota.read` | `drive.quota.manage` | 修改他人 Quota 可要求 Step-up；逻辑用量不等于物理 Storage Used |
| Drive Policies | `/console/drive/policies` | `drive.policy.read` | `drive.policy.manage` | 高风险策略修改要求 Audit / 可选 Step-up；不得管理 S3 Credential / Placement / Blob GC |

### 5.1 权限与隐私硬规则

- **Platform ADMIN ≠ Drive File READ。** `platform.*`、`permission.manage`、系统运维或其他管理员能力均不得自动推导出 `drive.file.read`。
- Drive 运维权限可以允许查看 Space ID、Owner 摘要、Quota、Transfer / Sync / Conflict 数量、Health、Error Category 等最小必要诊断信息，但不得因此展示私有 File Name、Folder Path、Thumbnail、Preview、Content 或设备 Local Path。
- `drive.space.read` / `drive.transfer.read` / `drive.sync.read` 等“治理/诊断可见性”与 `drive.file.read` 的内容读取能力必须分别判断。
- Deep Link 不能扩权：从 Storage、Background Task、Audit、Photo / Media / Document Projection 跳转到 Drive 后，仍重新执行目标 Drive Capability + ACL / Scope 判定。
- Drive File Detail 可链接 Attachment / Blob，但 `drive.file.write` 不授予 Placement、Provider、Blob GC 等 Storage 管理权限；反向亦然。

## 6. 附件与存储

| 页面 | Route | 查看能力 | 写操作能力 | 高风险能力 |
|---|---|---|---|---|
| 附件与 Blob | `/console/attachments` | `attachment.read` | `attachment.create` / `attachment.update` / `attachment.link.update` | Blob 清理 `blob.purge` |
| 持久化存储层 | `/console/storage/tiers` | `storage.read` | `storage.backend.manage` / `storage.policy.manage` | 凭据修改、迁移策略可要求 Step-up |
| 缓存与下载 | `/console/storage/cache` | `storage.cache.read` | `storage.cache.manage` | 客户端移除请求 `client.download.remove.request` |
| 归档/恢复/回收站 | `/console/storage/archive` | `storage.archive.read` | `storage.archive` / `storage.restore` | 永久清理 `resource.purge` / `blob.purge` |
| 备份与恢复 | `/console/storage/backup` | `backup.read` | `backup.run` / `backup.manage` | 覆盖恢复 `backup.restore` + Step-up |

## 7. 效率与计划

| 页面 | Route | 查看能力 | 写操作能力 | 专用能力 |
|---|---|---|---|---|
| 收集箱与今天 | `/console/planning/today` | `planning.task.read` | `planning.task.create` / `planning.task.update` | 删除 `planning.task.delete` |
| 项目与任务 | `/console/planning/projects` | `planning.project.read` / `planning.task.read` | `planning.project.write` / `planning.task.write` | 项目删除 `planning.project.delete` |
| 日历与时间块 | `/console/planning/calendar` | `planning.calendar.read` | `planning.time_block.write` | 共享/他人日历按额外 scope |
| 目标与 OKR | `/console/planning/goals` | `planning.goal.read` | `planning.goal.write` | 管理他人目标按 owner/admin scope |
| 习惯/专注/复盘 | `/console/planning/focus` | `planning.focus.read` | `planning.focus.write` | — |

## 8. 个人记账

> Finance 是私密业务域。列表、搜索、导出、通知正文都必须继续遵守 Finance 数据敏感性策略。

| 页面 | Route | 查看能力 | 写操作能力 | 高风险/额外能力 |
|---|---|---|---|---|
| 账本总览 | `/console/finance` | `finance.read` | — | 金额可见性仍按账本 scope |
| 账户 | `/console/finance/accounts` | `finance.account.read` | `finance.account.write` | 删除/迁移 `finance.account.delete` |
| 交易 | `/console/finance/transactions` | `finance.transaction.read` | `finance.transaction.write` | 批量删除 `finance.transaction.delete` |
| 预算与周期账 | `/console/finance/budgets` | `finance.budget.read` | `finance.budget.write` | — |
| 对账与导入 | `/console/finance/reconcile` | `finance.reconcile.read` | `finance.reconcile.execute` / `finance.import` | 重开已完成对账可要求 Step-up |
| 财务导出 | 各页动作 | 相应 read | `finance.export` | 大范围/明文导出可要求 Step-up |

## 9. 私密笔记

Secure Session 未解锁时即使拥有 Permission 也只显示锁定界面。

| 页面 | Route | 查看能力 | 写操作能力 | Step-up / 高风险 |
|---|---|---|---|---|
| 保险库 | `/console/private-notes` | `private_note.read` | `private_note.write` | 需要有效 Secure Session |
| 版本与冲突 | `/console/private-notes/conflicts` | `private_note.version.read` | `private_note.conflict.resolve` | Secure Session |
| 恢复与导出 | `/console/private-notes/recovery` | `private_note.recovery.read` | `private_note.export` / `private_note.restore` | 明文导出、恢复、密钥操作要求 Step-up |
| 密钥/恢复材料操作 | 页面内动作 | — | `private_note.key.reset` / 对应 canonical key | Required SVL + Policy |

## 10. 密码管理

| 页面 | Route | 查看能力 | 写操作能力 | Step-up / 高风险 |
|---|---|---|---|---|
| 密码保险库 | `/console/passwords` | `password_vault.read` | `password_vault.write` | Reveal/Copy 可按策略要求 Step-up/Secure Session |
| 生成器 | `/console/passwords/generator` | `password_generator.use` | — | 生成值不进入日志/遥测 |
| 健康与安全发送 | `/console/passwords/health` | `password_health.read` | `password_send.create` / `password_send.revoke` | 明文/发送敏感操作按策略验证 |
| 设备与访问 | `/console/passwords/devices` | `password_device.read` | `password_device.revoke` | 撤销设备、改变 Vault 策略可要求 Step-up |
| 导出/恢复 | 页面内动作 | `password_vault.read` | `password_vault.export` / `password_vault.restore` | 必须 Step-up + 显式范围确认 |

## 11. AI 智能

| 页面 | Route | 查看能力 | 写/使用能力 | 管理能力 |
|---|---|---|---|---|
| 助手 | `/console/ai/assistant` | `ai.conversation.read` | `ai.assistant.use` | 敏感 Context 仍检查数据域权限 |
| 模型与提供方 | `/console/ai/models` | `ai.model.read` | — | `ai.provider.manage` / `ai.model.manage` / `ai.routing.manage` |
| 人格 | `/console/ai/personas` | `ai.persona.read` | `ai.persona.write` | 敏感 Tool 启用按 Tool Capability |
| 上下文/隐私/记忆 | `/console/ai/privacy` | `ai.privacy.read` | `ai.memory.manage` | 隐私策略 `ai.privacy.manage` + Step-up（按策略） |
| 作业/Trace/用量 | `/console/ai/jobs` | `ai.job.read` | `ai.job.retry` | Trace 内容 `ai.trace.content.read` 与仅元数据读取分离 |

## 12. 数据分析

| 页面 | Route | 查看能力 | 写/管理能力 | 导出 |
|---|---|---|---|---|
| 个人概览 | `/console/analytics` | `analytics.read` | — | `analytics.export` |
| 内容分析 | `/console/analytics/content` | `analytics.content.read` | — | `analytics.export` |
| 存储分析 | `/console/analytics/storage` | `analytics.storage.read` | — | `analytics.export` |
| 效率分析 | `/console/analytics/planning` | `analytics.planning.read` | — | `analytics.export` |
| 系统历史 | `/console/analytics/system` | `analytics.system.read` | — | `analytics.export` |
| 指标目录 | `/console/analytics/metrics` | `analytics.metric.read` | `analytics.metric.manage` | — |
| 报表与重建 | `/console/analytics/reports` | `analytics.report.read` | `analytics.report.manage` / `analytics.rebuild` | `analytics.export` |

分析权限不能绕过源业务域权限。聚合数据若可反推出无权读取的个人/私密内容，后端必须抑制或降维。

## 13. 集成与自动化

| 页面 | Route | 查看能力 | 写/执行能力 | 高风险能力 |
|---|---|---|---|---|
| 自动化规则 | `/console/integration/automation` | `automation.rule.read` | `automation.rule.write` / `automation.rule.enable` | 真实副作用测试需要目标动作 Permission |
| 执行与 Trace | `/console/integration/executions` | `automation.execution.read` | `automation.execution.retry` | 重放必须检查每个目标 Action Capability |
| 事件与失败队列 | `/console/integration/events` | `event.read` | `event.replay` / `event.failure.manage` | Replay 属于高影响动作 |
| 导入与同步 | `/console/integration/sync` | `sync.read` | `sync.manage` / `sync.execute` | 冲突覆盖按 Resource 写权限 |
| 插件与连接器 | `/console/integration/plugins` | `plugin.read` | `plugin.manage` / `connector.manage` | 扩权、安装、Secret 变更可要求 Step-up |

## 14. 身份与安全

| 页面 | Route | 查看能力 | 写/管理能力 | Step-up |
|---|---|---|---|---|
| 用户与角色 | `/console/security/users` | `user.read` | `user.manage` / `role.assign` | 禁用管理员、凭据操作按策略 |
| 权限矩阵 | `/console/security/permissions` | `permission.read` | `permission.manage` | 权限提升/高风险 Capability 变更必须 Step-up |
| 活跃会话 | `/console/security/sessions` | `session.read` | `session.revoke` | 全局撤销可要求 Step-up |
| 认证策略 | `/console/security/authentication` | `security.policy.read` | `security.policy.manage` | Step-up |
| Keys & Secrets | 同上 Tab | `security.key.read` | `security.key.manage` | Required SVL + Policy |
| Recovery | 同上 Tab | `security.recovery.read` | `security.recovery.manage` | Required SVL + Policy |
| OAuth/API Access | 同上 Tab | `security.api_access.read` | `security.api_access.manage` | Token 创建/高权限 Scope 可 Step-up |

### 14.1 Permission 与 Verification 的硬规则

例如用户拥有 `private_note.key.reset`：

```text
Authenticated
AND Authorized(private_note.key.reset)
AND VerificationLevel >= RequiredLevel
AND SecurityPolicyAllows
```

全部满足后才允许执行。SVL 高不产生新的 Permission。

同样地，平台管理员角色或高 SVL 不产生 `drive.file.read`；Drive 内容读取必须独立授权并继续满足目标 Space / Node 的 ACL / Scope。

## 15. 平台配置

| 页面 | Route | 查看能力 | 写能力 | 高风险 |
|---|---|---|---|---|
| 参数 | `/console/platform/parameters` | `platform.parameter.read` | `platform.parameter.write` | Secret/安全参数修改按目标子系统 Step-up |
| 字典 | `/console/platform/dictionaries` | `platform.dictionary.read` | `platform.dictionary.write` | 有引用项删除/迁移需要额外确认 |
| 菜单 | `/console/platform/menus` | `platform.menu.read` | `platform.menu.write` | 不能移除后端强制 Permission |

环境变量来源的只读配置即使拥有 write Permission 也不能在 UI 中伪装成可写。

## 16. 沟通与审计

| 页面 | Route | 查看能力 | 写/管理能力 | 额外条件 |
|---|---|---|---|---|
| 公告 | `/console/communications/announcements` | `announcement.read` | `announcement.write` / `announcement.publish` | Critical 发布可按策略要求 Step-up |
| 通知中心 | `/console/communications/notifications` | `notification.read` | 当前用户状态 `notification.self.update` | 全局范围需 `notification.admin.read` |
| 投递规则/日志 | 同页 Tab | `notification.delivery.read` | `notification.delivery.manage` | 目标 Secret 不可读回 |
| 审计日志 | `/console/communications/audit` | `audit.read` | 普通 UI 不可修改 | 导出 `audit.export` |
| 安全事件 | 同页 Tab | `security.event.read` | `security.event.manage` | 响应动作继续调用目标子系统权限 |

审计记录不可因为拥有普通写权限被修改或删除。

## 17. 系统运维

| 页面 | Route | 查看能力 | 写/执行能力 | 高风险 |
|---|---|---|---|---|
| 系统健康与告警 | `/console/ops/health` | `system.health.read` | `system.alert.manage` | Silence/确认按运维策略 |
| 定时任务 | `/console/ops/jobs` | `system.job.read` | `system.job.manage` / `system.job.run` | System-owned Job 可只读 |
| 后台任务 | `/console/ops/background` | `system.task.read` | `system.task.cancel` / `system.task.retry` | 业务任务还需所属子系统能力 |

“可以看到全局后台任务”不自动意味着可以读取任务中的私密业务 payload；Drive Task 也不得因为 `system.task.read` 泄露无权访问的文件名、Path 或内容。

## 18. Capability Scope 与所有权

同一个 Capability 还应结合后端 Scope：

- `self`：仅自己的数据。
- `owned`：自己拥有/创建的实体。
- `shared`：通过协作授权可访问的实体。
- `all` / `admin`：管理范围。

Console 不应仅通过 Capability Key 推断 Scope。服务端返回的有效授权范围决定：
- 列表能看到哪些行。
- Picker 能搜索到哪些实体。
- Detail 能展示哪些字段。
- 操作影响范围。

对于 Personal Drive，即使某个治理 Capability 具有 `admin` Scope，也只表示该治理动作的范围；除非 `drive.file.read` 自身明确授权对应 Scope，否则不得推导出任意用户 Drive 内容读取能力。

## 19. 路由元数据建议

前端 Route Record 建议至少声明以下元数据：

```text
id
subsystem
navigationGroup
navigationLabel
requiredCapability
secureDomain
requiredVerificationLevel (仅静态可知时)
breadcrumb
```

页面内 Action 建议声明：

```text
actionId
requiredCapability
entityScope
requiresStepUp
destructive
```

但这些前端元数据只是渲染辅助。真正 Authorization / SVL / Policy 判定必须在后端重新执行。

## 20. 插件页面权限

插件注册 Console 页面时必须提供：
- 唯一 Route ID。
- 所属子系统/插件导航位置。
- Required Capability。
- 页面显示名称与图标。
- 是否包含高风险 Action。

插件后端 Capability 未注册或当前用户未授权时，前端不得仅因插件声明了菜单项就显示页面。

插件不能覆盖 Core Route，也不能通过自定义 Menu 配置把受保护页面降级为无需权限的普通链接。

## 21. 403 页面标准

403 页面包含：
- Shield/Lock 图标。
- 标题 `你没有权限访问此页面`。
- 简短页面名称。
- 可安全披露时展示缺失 Capability 的开发者可读 Key；普通用户模式可只显示能力说明。
- `返回上一页`。
- `返回概览`。

不得：
- 展示目标实体敏感字段。
- 提供伪造的“请求管理员权限”按钮，除非系统真的实现权限申请工作流。
- 把 401、Secure Vault Locked 或后端错误统一显示成 403。