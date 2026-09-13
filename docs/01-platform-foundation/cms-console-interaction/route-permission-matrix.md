# CMS Console Canonical Route 与权限矩阵

> 本文定义最终态 Router、Navigation、页面 Guard 和主要操作权限。
>
> 当前 V2 重构不承担历史 Console Route 兼容；本矩阵只列 canonical routes。

## 1. 权限控制原则

### Route Guard

- 未认证：进入登录流程；
- 已认证但缺少最小进入能力：403；
- Secure Domain 未解锁：进入对应 Unlock 流程，不伪装成 403；
- 后端安全策略要求隐藏实体存在性时可以返回 404。

### Navigation

- 无查看能力的入口不渲染；
- 一级工作区存在，只显示用户可访问的二级入口；
- 菜单隐藏只属于 UX，后端 Authorization 永远是安全边界；
- 插件注册页面必须声明 Capability，不能通过前端菜单绕过服务端权限。

### Action

查看、创建、编辑、删除、高风险操作分别判断。高风险操作还可能要求 Step-up Verification、安全策略或领域 ACL。

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

## 3. Dashboard

| 页面 | Route | 最小能力 | 规则 |
|---|---|---|---|
| Dashboard | `/dashboard` | `dashboard.read` | Widget 只显示调用者有权看到的摘要；卡片动作重新检查目标能力 |

Dashboard 可以聚合 Storage、Activity、Metadata、Backup、Sync 等状态，但聚合权限不授予目标对象额外读取能力。

## 4. Library

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| Library | `/library` | `resource.read` | `resource.create` / `resource.update` / lifecycle actions |
| Resource Detail | `/library/:resourceId` | `resource.read` | `resource.update` / `resource.archive` / `resource.trash` |
| Collections | `/library/collections` | `collection.read` | `collection.create` / `collection.update` / `collection.delete` |
| Library Search | `/library/search` | `search.use` + 目标对象 read | 搜索本身无越权写入 |

规则：

- `resource.read` 不授予 Blob Placement 管理；
- `resource.update` 不允许静默覆盖人工锁定元数据；
- 永久清理与普通 trash/archive 分离；
- Resource Detail 的 Advanced 区域仍按 Attachment/Storage 权限过滤。

## 5. Add Content

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| Add Content | `/add` | `ingestion.read` 或至少一种允许的导入能力 | `ingestion.source.use` / `ingestion.run.create` / 对应上传能力 |

Add Content 根据来源类型继续检查本地、NAS、对象存储、Connector 等专用能力。确认导入后生成的长期工作在 `/activity` 观察。

## 6. Activity

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| Activity | `/activity` | `activity.read` | 只展示调用者有权观察的后台工作 |
| Activity Detail | `/activity/:activityId` | `activity.read` + 目标任务 scope | `task.cancel` / `task.retry` 或领域专用动作 |

规则：

- Activity 可以聚合 Import、Storage、Metadata、Backup、AI、Automation 等，但不得扩张业务对象读取权限；
- Attempt、Worker、Lease、Raw Payload 等 Advanced 数据需要额外诊断能力时单独判断；
- 不再存在 AI Task、Operations Task、Import Task 等独立产品级任务路由。

## 7. Storage

| 页面 | Route | 最小能力 | 主要写操作 | 高风险 |
|---|---|---|---|---|
| Storage Overview | `/storage` | `storage.read` | — | — |
| Providers | `/storage/providers` | `storage.provider.read` | `storage.provider.manage` | Credential 变更可要求 Step-up |
| Provider Detail | `/storage/providers/:providerId` | `storage.provider.read` | probe / enable / disable / update | Credential rotation |
| Policy | `/storage/policy` | `storage.policy.read` | `storage.policy.manage` | 大范围迁移规则可要求审批/Step-up |
| Archive & Restore | `/storage/archive` | `storage.archive.read` | `storage.archive` / `storage.restore` | 大范围恢复按策略 |
| Maintenance | `/storage/maintenance` | `storage.maintenance.read` | verify / repair / GC decision | physical purge / destructive repair |
| Backup | `/storage/backup` | `backup.read` | `backup.run` / `backup.manage` | `backup.restore` + Step-up |

Storage Maintenance 可以展示 Blob、Placement、Replica 等内部信息，但不因此授予 Resource 或 Drive 私有内容读取能力。

## 8. Apps 总入口

| 页面 | Route | 最小能力 |
|---|---|---|
| Apps | `/apps` | 已认证；按 App capability 过滤卡片 |

Apps 只展示启用且可访问的业务产品。App route 统一位于 `/apps/<app>/**`。

### 8.1 Drive

| 页面 | Route | 最小能力 | 主要写操作 |
|---|---|---|---|
| Drive Home | `/apps/drive` | `drive.space.read` | `drive.file.write` / `drive.folder.write` |
| Node Detail | `/apps/drive/nodes/:nodeId` | `drive.file.read` | file/revision actions |
| Transfers | `/apps/drive/transfers` | `drive.transfer.read` | `drive.transfer.manage` |
| Sync | `/apps/drive/sync` | `drive.sync.read` | `drive.sync.manage` |
| Conflict | `/apps/drive/conflicts` | `drive.conflict.read` | `drive.conflict.resolve` |
| Trash | `/apps/drive/trash` | `drive.trash.read` | restore / permanent delete |
| Quota / Policy | `/apps/drive/settings` | 对应 drive capability | quota / policy manage |

硬规则：**Platform ADMIN 不自动等于 Drive File READ。** 运维能力与文件名、路径、预览、下载等内容读取分别判断。

### 8.2 Documents

| 页面 | Route | 最小能力 | 写操作 |
|---|---|---|---|
| Documents | `/apps/documents` | `document.read` | `document.create` / `document.update` |
| Editor | `/apps/documents/:id/edit` | `document.read` | update / publish / revision actions |

### 8.3 Media

| 页面 | Route | 最小能力 | 写操作 |
|---|---|---|---|
| Media | `/apps/media` | 对应 media/resource read | progress / queue / playback state |

Media 是专业消费体验；资源身份仍由 Library 的 Resource 承载。

### 8.4 Planning

Canonical prefix：`/apps/planning/**`。使用 `planning.*` 能力；任务、项目、日历、目标等按领域权限判断。

### 8.5 Finance

Canonical prefix：`/apps/finance/**`。使用 `finance.*` 能力；金额、账户、交易和导出按私密域规则最小化展示。

### 8.6 Private Notes

Canonical prefix：`/apps/private-notes/**`。使用 `private_note.*` 能力，并要求有效 Secure Domain Unlock State。

### 8.7 Passwords

Canonical prefix：`/apps/passwords/**`。使用 `password.*` / canonical secret capability，并遵守最高敏感数据规则。

### 8.8 AI

Canonical prefix：`/apps/ai/**`。模型、Persona、隐私、使用量等按 `ai.*` 权限判断；AI 长任务进入全局 Activity。

### 8.9 Sharing

Canonical prefix：`/apps/sharing/**`。Share、Room、Watch、Listen 等使用 `share.*` / `room.*` 能力。

### 8.10 Analytics

Canonical prefix：`/apps/analytics/**`。只聚合调用者本来有权查看的数据，不以 Analytics 权限扩张敏感域读取能力。

### 8.11 Automation

Canonical prefix：`/apps/automation/**`。规则编辑使用 `automation.*`；执行历史和长期执行统一深链接到 `/activity`。

## 9. System 总入口

| 页面 | Route | 最小能力 |
|---|---|---|
| System | `/system` | 至少一个 system 管理能力 |

### 9.1 Access

| 页面 | Route | 最小能力 | 写操作 |
|---|---|---|---|
| Access Overview | `/system/access` | `identity.read` | — |
| Users | `/system/access/users` | `user.read` | `user.manage` |
| Roles | `/system/access/roles` | `role.read` | `role.manage` |
| Permissions | `/system/access/permissions` | `permission.read` | `permission.manage` |
| Authentication | `/system/access/authentication` | `security.authentication.read` | policy/security actions |

### 9.2 Audit

`/system/audit` 使用 `audit.read`；导出或敏感字段查看按更高能力判断。Audit deep link 不能越权展示目标对象。

### 9.3 Integrations

Canonical prefix：`/system/integrations/**`。承载 Plugin、Connector、Webhook、Metadata Source 等平台连接配置。安装、权限授权、Secret 配置按专用高风险能力判断。

### 9.4 Notifications

Canonical prefix：`/system/notifications/**`。系统通知策略、投递渠道和失败诊断使用 `notification.*` 管理能力。当前用户自己的通知偏好在 `/account/notifications`。

### 9.5 Settings

Canonical prefix：`/system/settings/**`。平台参数、运行配置和必要字典使用 `platform.*` 能力。产品导航结构不是运行时字典，不提供“任意编辑一级菜单”的产品能力。

### 9.6 Health

`/system/health` 使用 `system.health.read`。Unknown 与 Healthy 必须分离。

### 9.7 Diagnostics

Canonical prefix：`/system/diagnostics/**`。系统事件、队列、索引、Outbox 等只作为高级诊断；后台工作的用户入口仍是 Activity。

## 10. 跨域权限规则

- Library → Storage：查看内容存储摘要不自动授予 Placement 管理；
- Storage → Resource：Maintenance deep link 到 Resource 时重新检查 `resource.read`；
- Drive → Storage：Drive File READ 与 Storage 管理能力相互独立；
- Activity → Business Object：进入业务对象时重新检查目标对象权限；
- Audit → Business Object：不得通过审计记录泄露无权读取的字段；
- Analytics：不得以统计能力绕过 Finance、Private Notes、Passwords、Drive 等敏感读取边界；
- Plugin：插件页面、按钮和 API 都必须映射到 canonical Capability。

## 11. Router 实施要求

- Router 只实现本文和 IA 契约定义的 canonical route tree；
- 当前阶段不增加旧路由 alias 或 redirect；
- Route meta 必须声明工作区归属和最小进入能力；
- Sidebar 由工作区模型驱动，不从后端包/Controller 自动生成；
- canonical route、权限和导航归属必须有自动化测试。
