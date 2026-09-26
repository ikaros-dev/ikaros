# CMS Console Canonical Route 与权限矩阵

> 本文定义最终态 Router、Navigation、页面 Guard 和主要操作权限。
>
> 当前 V2 重构不承担历史 Console Route 兼容；本矩阵只列 canonical routes。用户可见菜单使用中文名称，Route、Capability 和内部标识继续使用稳定英文契约。

## 1. 权限控制原则

### Route Guard

- 未认证：进入登录流程；
- 已认证但缺少最小进入能力：403；
- Secure Domain 未解锁：进入对应 Unlock 流程，不伪装成 403；
- 后端安全策略要求隐藏实体存在性时可以返回 404。

### Navigation

- 无查看能力的页面入口不渲染；
- 目录节点只显示至少一个可访问子页面时才显示；
- 目录节点可以 redirect 到第一个可访问子页面，但不渲染独立页面；
- 菜单隐藏只属于 UX，后端 Authorization 永远是安全边界；
- 插件注册页面必须声明 Capability，不能通过前端菜单绕过服务端权限。

### Route hierarchy

- 一级目录对应一级 route prefix；
- 二级菜单页面对应二层 route；
- 三级菜单页面对应三层 route；
- 一个页面只有一个 canonical route；
- 参数化 Detail route 不进入 Sidebar；
- 页面内 Tab 只有在需要独立 deep link、权限或生命周期时才升级为 route。

## 2. 全局入口

| 页面 | Route | 导航 | 最小能力 | 主要写操作 |
|---|---|---|---|---|
| 首次初始化 | `/setup` | 登录前 | 初始化专用状态 | 创建初始管理员和基础配置 |
| 登录 | `/login` | 登录前 | 公共认证入口 | Authentication |
| 登录验证 | `/login/verify` | 认证事务 | 有效认证事务 | `security.verification.perform` |
| 账号恢复 | `/recovery/**` | 登录前 | 公共恢复入口 | `security.recovery.perform` |
| 个人资料 | `/account/profile` | 头像菜单 | `account.self.read` | `account.self.update` |
| 偏好设置 | `/account/preferences` | 头像菜单 | `account.preference.read` | `account.preference.update` |
| 个人通知 | `/account/notifications` | 头像菜单 | `account.notification.read` | `account.notification.update` |
| 当前账号安全 | `/account/security` | 头像菜单 | `account.security.read` | `account.security.update` |
| 登录会话 | `/account/sessions` | 头像菜单 | `account.security.read` | revoke session |
| API 令牌 | `/account/api-tokens` | 头像菜单 | 对应 token capability | create / revoke token |

## 3. 仪表盘

| 页面 | Route | 最小能力 | 规则 |
|---|---|---|---|
| 仪表盘 | `/dashboard` | `dashboard.read` | Widget 只显示调用者有权看到的摘要；卡片动作重新检查目标能力 |

仪表盘可以聚合 Storage、Activity、Metadata、Backup、Sync 等状态，但聚合权限不授予目标对象额外读取能力。

## 4. 资源

`/resources` 是一级目录前缀，本身不渲染页面；默认进入第一个可访问子页面，通常为 `/resources/library`。

### 4.1 资源库

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| 资源库 | `/resources/library` | `resource.read` | `resource.create` / `resource.update` / lifecycle actions |
| Resource Detail | `/resources/library/:resourceId` | `resource.read` | `resource.update` / `resource.archive` / `resource.trash` |
| Collections | `/resources/library/collections` | `collection.read` | `collection.create` / `collection.update` / `collection.delete` |
| 资源库搜索 | `/resources/library/search` | `search.use` + 目标对象 read | 搜索本身无越权写入 |

规则：

- `resource.read` 不授予 Blob Placement 管理；
- `resource.update` 不允许静默覆盖人工锁定元数据；
- 永久清理与普通 trash/archive 分离；
- Resource Detail 的 Advanced 区域仍按 Attachment / Storage 权限过滤。

### 4.2 添加资源

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| 添加资源 | `/resources/add` | `ingestion.read` 或至少一种允许的导入能力 | `ingestion.source.use` / `ingestion.run.create` / 对应上传能力 |

添加资源根据来源类型继续检查本地、NAS、对象存储、Connector 等专用能力。确认导入后生成的长期工作在 `/resources/activity` 观察。

### 4.3 活动中心

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| 活动中心 | `/resources/activity` | `activity.read` | 只展示调用者有权观察的后台工作 |
| 活动详情 | `/resources/activity/:activityId` | `activity.read` + 目标任务 scope | `task.cancel` / `task.retry` 或领域专用动作 |

规则：

- 活动中心可以聚合 Import、Storage、Metadata、Backup、AI、Automation 等，但不得扩张业务对象读取权限；
- Attempt、Worker、Lease、Raw Payload 等 Advanced 数据需要额外诊断能力时单独判断；
- 不再存在 AI Task、Operations Task、Import Task 等独立产品级任务路由。

## 5. 存储

`/storage` 是一级目录前缀；默认 redirect 到 `/storage/overview`，不渲染概览页面本身。

| 页面 | Route | 最小能力 | 主要写操作 | 高风险 |
|---|---|---|---|---|
| 存储概览 | `/storage/overview` | `storage.read` | — | — |
| 存储提供方 | `/storage/providers` | `storage.provider.read` | `storage.provider.manage` | Credential 变更可要求 Step-up |
| Provider Detail | `/storage/providers/:providerId` | `storage.provider.read` | probe / enable / disable / update | Credential rotation |
| 存储策略 | `/storage/policy` | `storage.policy.read` | `storage.policy.manage` | 大范围迁移规则可要求审批/Step-up |
| 归档管理 | `/storage/archive` | `storage.archive.read` | `storage.archive` / `storage.restore` | 大范围恢复按策略 |
| 备份管理 | `/storage/backup` | `backup.read` | `backup.run` / `backup.manage` | `backup.restore` + Step-up |
| 存储维护 | `/storage/maintenance` | `storage.maintenance.read` | verify / repair / GC decision | physical purge / destructive repair |

存储维护可以展示 Blob、Placement、Replica 等内部信息，但不因此授予 Resource 或 Drive 私有内容读取能力。

## 6. 应用

`/apps` 是一级目录前缀；默认 redirect 到首个业务应用 `/apps/drive`。应用目录不设置独立总览页。

应用只展示启用且可访问的业务产品。App route 统一位于 `/apps/<app>/**`；Console 不提供通用插件应用承载页。

### 6.1 云盘

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| 云盘 | `/apps/drive` | `drive.space.read` | `drive.file.write` / `drive.folder.write` |
| Node Detail | `/apps/drive/nodes/:nodeId` | `drive.file.read` | file/revision actions |
| Transfers | `/apps/drive/transfers` | `drive.transfer.read` | `drive.transfer.manage` |
| Sync | `/apps/drive/sync` | `drive.sync.read` | `drive.sync.manage` |
| Conflict | `/apps/drive/conflicts` | `drive.conflict.read` | `drive.conflict.resolve` |
| Trash | `/apps/drive/trash` | `drive.trash.read` | restore / permanent delete |
| Quota / Policy | `/apps/drive/settings` | 对应 drive capability | quota / policy manage |

硬规则：**Platform ADMIN 不自动等于 Drive File READ。** 运维能力与文件名、路径、预览、下载等内容读取分别判断。

### 6.2 文档

| 页面 | Route | 最小能力 | 写操作 |
|---|---|---|---|
| 文档 | `/apps/documents` | `document.read` | `document.create` / `document.update` |
| Editor | `/apps/documents/:id/edit` | `document.read` | update / publish / revision actions |

### 6.3 媒体

| 页面 | Route | 最小能力 | 写操作 |
|---|---|---|---|
| 媒体 | `/apps/media` | 对应 media/resource read | progress / queue / playback state |

### 6.4 其他应用

- 计划：`/apps/planning/**`，使用 `planning.*`；
- 财务：`/apps/finance/**`，使用 `finance.*`；
- 私密笔记：`/apps/private-notes/**`，使用 `private_note.*` 并要求 Secure Domain Unlock；
- 密码库：`/apps/passwords/**`，使用 `password.*` / canonical secret capability；
- AI：`/apps/ai/**`，使用 `ai.*`，长期工作进入 `/resources/activity`；
- 数据分析：`/apps/analytics/**`，不得扩张敏感域读取能力；
- 自动化：`/apps/automation/**`，规则编辑使用 `automation.*`，长期执行进入 `/resources/activity`；

## 7. 系统

`/system` 是一级目录前缀，本身不渲染业务页面。System 下的二级目录同样只组织三级页面。

Sidebar 的 System 菜单采用“平铺分组”表现：访问控制、集成、通知与审计、平台配置、运维是不可点击的分组标题；分组下的三级页面直接以同级菜单项平铺展示，不渲染为可展开的父子 `SubMenu`。该视觉规则不改变 canonical route，页面仍使用 `/system/<group>/<page>`。

### 7.1 访问控制 `/system/access/**`

| 页面 | Route | 最小能力 | 写操作 |
|---|---|---|---|
| 用户管理 | `/system/access/users` | `system.user.read` | `system.user.manage` |
| 角色与权限 | `/system/access/roles-permissions` | `system.role.read` | `system.role.manage` |
| 身份认证 | `/system/access/authentication` | `security.authentication.read` | policy/security actions |

“角色与权限”是一个菜单页面；若页面内部继续拆 Role / Permission Tab，则使用同一 canonical route，通过 Tab state 表达，不再制造两个 Sidebar 页面。

### 7.2 集成 `/system/integrations/**`

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| 应用管理 | `/system/integrations/apps` | 对应 integration read | install / enable / disable / uninstall |
| 外部集成 | `/system/integrations/external` | 对应 integration read | Connector / Webhook / Metadata Source 配置 |
| 事件投递 | `/system/integrations/events` | 对应 event delivery read | retry / replay / policy actions |

Secret 配置、插件授权等高风险动作继续使用专用能力和 Step-up。

### 7.3 通知与审计 `/system/communications/**`

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| 通知中心 | `/system/communications/notifications` | `notification.read` 或对应管理能力 | delivery policy / channel actions |
| 审计日志 | `/system/communications/audit` | `audit.read` | export 等按更高能力判断 |

当前用户自己的通知偏好仍在 `/account/notifications`。

### 7.4 平台配置 `/system/settings/**`

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| 系统参数 | `/system/settings/parameters` | 对应 `platform.*` read | parameter update |

产品导航结构不是运行时字典，不提供任意编辑一级菜单的产品能力。

### 7.5 运维 `/system/operations/**`

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| 系统健康 | `/system/operations/health` | `system.health.read` | — |
| 系统诊断 | `/system/operations/diagnostics` | 对应 diagnostics read | 安全诊断动作 |

System Diagnostics 只承载高级诊断；后台工作的用户入口仍是 `/resources/activity`。

## 8. 跨域权限规则

- 资源库 → 存储：查看 Resource 存储摘要不自动授予 Placement 管理；
- 存储 → Resource：存储维护 deep link 到 Resource 时重新检查 `resource.read`；
- 云盘 → 存储：Drive File READ 与 Storage 管理能力相互独立；
- 活动中心 → Business Object：进入业务对象时重新检查目标对象权限；
- 审计日志 → Business Object：不得通过审计记录泄露无权读取的字段；
- 数据分析：不得以统计能力绕过 Finance、Private Notes、Passwords、Drive 等敏感读取边界；
- Plugin：插件页面、按钮和 API 都必须映射到 canonical Capability。

## 9. Router 实施要求

- Router 只实现本文和 IA 契约定义的 canonical route tree；
- `/resources`、`/storage`、`/apps`、`/system` 等目录根只允许 redirect，不渲染业务页面；
- 当前阶段不增加旧 `/library`、`/add`、`/activity` 等 route alias 或兼容页面；
- Route meta 必须声明菜单归属和最小进入能力；
- Sidebar 由菜单树模型驱动，不从后端包 / Controller 自动生成；
- canonical route、权限、菜单层级和 redirect 行为必须有自动化测试。
