# Ikaros V2 平台管理与系统运维子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Platform Administration & Operations Subsystem Design |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-30 |
| 状态 | 草案（Draft） |

> 本文档定义 Ikaros V2 的平台级管理与系统运行健康能力。
>
> 与内容、媒体、创作、效率规划等业务子系统不同，本子系统主要回答两个问题：
>
> 1. **管理员如何管理 Ikaros 平台本身？**
> 2. **管理员如何确认 Ikaros 当前是否健康、谁正在使用、哪些后台工作正在运行？**
>
> 本文档不提前锁定具体 Java 类、数据库表或 API 路径，但会明确领域边界、核心语义、权限关系与产品行为。

---

## 1. 子系统定位

Ikaros V2 除业务能力外，需要一个稳定、统一的 Platform Administration / Operations 层，覆盖：

- 日志管理
- 通知公告
- 参数管理
- 字典管理
- 菜单管理
- 用户管理
- 角色管理
- 权限管理
- 通知管理
- 定时任务
- 在线用户
- 服务 / 子系统监控

这些能力不是某一个业务模块的附属页面，而是所有业务子系统共同依赖的基础平台能力。

总体上拆分为：

```text
Platform Administration & Operations
│
├── Identity & Access Management
│   ├── User
│   ├── Role
│   ├── Permission
│   ├── Role Binding
│   └── Session / Online User
│
├── Platform Configuration
│   ├── Parameter
│   ├── Dictionary
│   └── Menu
│
├── Communication
│   ├── Announcement
│   ├── Notification
│   ├── Notification Template
│   ├── Notification Rule
│   └── Notification Provider
│
├── Audit & Security
│   ├── Operation Log
│   ├── Login Log
│   └── Security Event
│
└── Operations
    ├── Scheduled Job
    ├── Job Run
    ├── Subsystem Health
    ├── Runtime Metrics
    └── Alert
```

---

## 2. 设计原则

### 2.1 平台权限与资源权限分离

Ikaros V2 同时存在两种不同层级的权限：

```text
Platform RBAC
决定“用户能做哪一类平台操作”

例如：
- 管理用户
- 管理角色
- 查看操作日志
- 修改系统参数
- 执行定时任务

Resource ACL
决定“用户能否访问某一个具体资源实例”

例如：
- 阅读某篇私有文档
- 编辑某个 Note
- 下载某个 Attachment
- 管理某个 Collection
```

两者不得混为一套权限模型。

Role 主要用于组合 **平台级 Capability Permission**。

Resource、Collection、Attachment、Share、Room 等业务对象的实例级访问仍由 Resource ACL / Principal 权限体系负责。

### 2.2 Menu 不是权限源

菜单管理仅负责后台导航结构、展示顺序、路由入口、图标和可见性配置。

系统绝不能通过“用户有没有这个菜单”判断用户是否拥有后端权限。

正确关系为：

```text
Permission
   │
   ├── 后端接口授权
   │
   └── Menu 可见性派生
```

而不是：

```text
Menu
  ↓
Permission
```

隐藏菜单不能替代 API 权限校验。

### 2.3 配置不等于秘密

Parameter Management 用于管理可配置参数。

密码、Access Token、Secret Key、对象存储密钥、插件 Secret 等敏感信息必须进入独立 Secret 管理机制，不应以普通明文参数形式展示和存储。

### 2.4 字典不承载核心业务约束

Dictionary 用于支持需要动态维护的显示值、可配置分类和扩展枚举。

核心业务不变量不得为了“灵活”全部改成字典。

例如：

- Notification Channel 类型可以扩展
- 用户自定义内容分类可以使用 Dictionary
- 但 Resource Lifecycle 的关键状态不应由管理员随意删除一个字典值而改变系统语义

### 2.5 Audit Log 与普通日志分离

操作审计日志是平台治理数据，需要具备稳定、可追踪、不可被普通用户随意修改的语义。

它不同于：

- 应用 Debug 日志
- JVM 日志
- HTTP Access Log
- Activity Timeline

Audit Log 关注“谁在什么时候对什么执行了什么操作，结果如何”。

### 2.6 Scheduled Job 与其他 Task 概念分离

V2 至少存在三类“任务”语义：

```text
Productivity Task
用户的待办事项

Background Task
系统的一次后台执行实例
例如导入、转码、索引、备份

Scheduled Job
按照时间规则触发系统动作的调度定义
```

Scheduled Job 可以触发 Background Task，但它不是 Background Task 本身，更不能与用户 Todo Task 共用同一业务模型。

### 2.7 在线用户是 Session 语义

HTTP 本身是无状态的，因此“在线用户”不能简单理解为某个绝对实时的在线布尔值。

系统需要区分：

- Active Session
- 最近活动时间
- WebSocket / Room 等实时连接
- Access Token / Refresh Token 生命周期

后台展示的“在线用户”应以活跃 Session 为主要产品语义。

### 2.8 子系统监控不意味着微服务

Ikaros V2 可以保持模块化单体架构。

Subsystem Health 表示逻辑能力组件的健康状态，而不是要求每一个子系统必须部署成独立微服务。

例如一个单体进程中仍可以分别监控：

- PostgreSQL
- Object Storage
- Search
- Scheduler
- Plugin Runtime
- Notification Provider
- Background Task Worker

---

## 3. 核心领域概念

### 3.1 User

User 表示 Ikaros 平台中的用户身份。

用户管理属于平台身份层，而不是普通 Resource。

典型状态：

- ACTIVE
- DISABLED
- LOCKED
- PENDING
- DELETED / DEACTIVATED

具体实现可以进一步细化。

### 3.2 Role

Role 是一组平台 Permission 的集合。

示例：

- System Administrator
- Content Administrator
- User Administrator
- Auditor
- Operator
- Normal User

系统应支持内置角色和自定义角色。

### 3.3 Permission

Permission 表达平台级 Capability。

建议语义面向动作，而不是面向菜单名称。

例如：

```text
system.user.read
system.user.manage
system.role.read
system.role.manage
system.audit.read
system.parameter.manage
system.scheduler.execute
system.health.read
```

具体命名在 API / IAM 设计文档中确定。

### 3.4 Role Binding

Role Binding 表示 Role 与用户、用户组或其他平台 Principal 的关系。

系统应支持一个用户拥有多个 Role。

最终平台权限为角色授权能力的组合。

### 3.5 Parameter

Parameter 是可动态配置的系统参数。

需要至少包含以下语义：

- Key
- Value
- Type
- Scope
- Description
- Default Value
- Validation Rule
- Editable
- Sensitive Flag
- Restart Requirement

### 3.6 Dictionary

Dictionary 是动态维护的键值分类集合。

典型结构：

```text
Dictionary
  └── Dictionary Items
```

Dictionary Item 至少可表达：

- Value
- Label
- Sort Order
- Enabled
- Metadata

### 3.7 Menu

Menu 表达后台管理 UI 的导航结构。

Menu 可以包含：

- Group
- Page
- External Link
- Divider
- Hidden Route

Menu 与权限存在关联，但不负责真正授权。

### 3.8 Announcement

Announcement 是管理员主动向一组用户发布的平台级公告。

例如：

- 系统维护
- 版本升级
- 安全提醒
- 服务异常
- 新功能说明

Announcement 与普通 Notification 不相同。

### 3.9 Notification

Notification 是发送给某个用户或用户集合的一次消息实例。

来源可能是：

- 系统事件
- 插件
- Scheduled Job
- Announcement
- Collaboration
- Storage Health
- Goal / OKR
- Backup Result

### 3.10 Operation Log

Operation Log 记录具有审计价值的平台操作。

### 3.11 Login Log

Login Log 记录身份验证与 Session 建立相关事件。

### 3.12 Scheduled Job

Scheduled Job 描述“何时触发某个动作”。

### 3.13 Job Run

Job Run 表示 Scheduled Job 的一次具体执行记录。

### 3.14 Session

Session 表示用户的一次登录会话或授权会话。

### 3.15 Subsystem Health

Subsystem Health 表示某个系统组件在某一时刻的健康状态。

---

# Part I：Identity & Access Management

## 4. 用户管理

### FR-USER-01 用户列表

管理员应能够查看系统用户列表。

至少支持：

- 用户名
- 昵称
- 状态
- 角色
- 创建时间
- 最近登录时间
- 最近活动时间

支持搜索、筛选、分页。

### FR-USER-02 用户创建

具有权限的管理员可以创建用户。

系统需要支持：

- 用户名
- 显示名称
- Email（可选）
- 初始角色
- 初始状态

密码创建方式、邀请机制和身份 Provider 由 IAM 详细设计确定。

### FR-USER-03 用户编辑

管理员可修改非敏感用户资料。

不能直接查看用户密码。

### FR-USER-04 用户禁用

管理员可以禁用用户。

禁用后应阻止新的受保护请求，并根据策略撤销或终止现有 Session。

### FR-USER-05 用户锁定

系统应能够因安全策略自动锁定账户，也允许管理员人工锁定 / 解锁。

### FR-USER-06 重置身份凭据

管理员可以发起密码重置或凭据重置流程，但不应获得用户原始密码。

### FR-USER-07 2FA 管理

具备高权限的管理员可以在必要情况下帮助用户重置 2FA 绑定。

此类操作必须进入 Audit Log。

### FR-USER-08 用户角色

管理员可以为用户分配或移除 Role。

### FR-USER-09 用户删除

删除用户应有明确生命周期语义。

不能因为删除 User 而立即破坏其创建的 Resource、Revision、Audit Log 等历史数据。

需要在后续 IAM / Ownership 设计中明确资源归属转移与匿名化策略。

---

## 5. 角色管理

### FR-ROLE-01 角色列表

管理员可以查看系统角色。

### FR-ROLE-02 内置角色

系统可提供若干内置角色。

内置角色是否允许删除或修改需要明确限制。

### FR-ROLE-03 自定义角色

管理员可以创建自定义 Role。

### FR-ROLE-04 Role Permission

可以为 Role 分配平台 Permission。

### FR-ROLE-05 Role User

可以查看某 Role 当前绑定的用户。

### FR-ROLE-06 Role Scope

未来可以支持 Role 只在特定管理域生效，但 V2 初期不要求引入复杂组织层级。

---

## 6. 权限管理

### FR-PERM-01 Permission Registry

系统需要维护完整的平台 Permission Registry。

Permission 应由系统核心或插件声明，而不是由管理员自由创建任意字符串后期待其自动生效。

### FR-PERM-02 Permission Metadata

每个 Permission 至少应具有：

- Identifier
- Name
- Description
- Module / Subsystem
- Risk Level（可选）

### FR-PERM-03 权限查看

管理员能够按模块查看 Permission。

### FR-PERM-04 权限授权

权限通过 Role 授权给 User。

普通场景不建议直接为单个 User 添加大量平台 Permission，以免绕开 RBAC。

### FR-PERM-05 插件权限

插件注册的平台管理能力可以声明自己的 Permission。

插件卸载后对应 Permission 应进入不可用状态，而不是留下可误导的有效授权。

### FR-PERM-06 平台权限与 Resource ACL

平台 Permission 不自动绕过 Resource ACL。

如需要“系统管理员可以读取所有资源”之类能力，应通过显式的 Super Admin / Break Glass 设计处理，不能默认为所有管理员开放。

---

## 7. 在线用户与 Session 管理

### FR-SESSION-01 活跃 Session 列表

管理员应能够查看当前活跃 Session。

至少包括：

- User
- Session ID / 安全展示标识
- 登录时间
- 最近活动时间
- 登录方式
- Client / Device
- IP
- User Agent 摘要

### FR-SESSION-02 在线状态

系统可以根据最近活动窗口标记：

- Active
- Idle
- Offline / Expired

具体阈值可配置。

### FR-SESSION-03 实时连接

如果用户当前存在 WebSocket / Room / Collaboration 连接，可以额外显示实时连接状态。

该状态与 HTTP Session 活跃状态分开。

### FR-SESSION-04 强制下线

具有权限的管理员可以撤销某个 Session。

用户下一个受保护请求必须失效。

### FR-SESSION-05 全部下线

管理员可以使某个 User 的全部 Session 失效。

### FR-SESSION-06 自助设备管理

用户本人应能够查看并撤销自己的登录设备 / Session。

### FR-SESSION-07 Session Security

Session Token、Refresh Token 的实际完整值不得在管理页面显示。

---

# Part II：Platform Configuration

## 8. 参数管理

### FR-PARAM-01 参数注册

核心模块与插件可以注册 Parameter 定义。

### FR-PARAM-02 类型化参数

Parameter 不应全部作为无类型字符串处理。

至少支持：

- String
- Integer
- Decimal
- Boolean
- Duration
- Date / Time
- URL
- Enum
- JSON（仅扩展场景）

### FR-PARAM-03 参数 Scope

Parameter 可以拥有不同 Scope，例如：

- SYSTEM
- SUBSYSTEM
- PLUGIN
- USER_DEFAULT

具体 Scope 在详细设计中确定。

### FR-PARAM-04 默认值

每个参数可以声明 Default Value。

没有用户配置时使用默认值。

### FR-PARAM-05 校验

参数修改前必须经过类型与业务校验。

### FR-PARAM-06 是否需要重启

参数定义需要标识：

- Dynamic
- Reload Required
- Restart Required

后台修改后必须明确告诉管理员何时生效。

### FR-PARAM-07 参数历史

重要参数修改应记录 Audit Log。

高风险参数可以保留变更历史。

### FR-PARAM-08 敏感参数

敏感数据不能以普通参数的方式明文读取。

若某参数实际引用 Secret，应只显示引用状态或掩码。

### FR-PARAM-09 环境覆盖

系统允许部署层通过环境变量 / 配置文件覆盖部分 Parameter。

后台需要明确显示“当前值由外部部署配置锁定”，避免管理员修改后发现不生效。

---

## 9. 字典管理

### FR-DICT-01 字典集合

管理员可以查看 Dictionary 列表。

### FR-DICT-02 字典项

管理员可以增删改启停 Dictionary Item。

### FR-DICT-03 排序

字典项支持排序。

### FR-DICT-04 多语言显示

Dictionary Item 可以支持多语言 Label。

### FR-DICT-05 系统字典保护

系统关键 Dictionary 可以标记为 System Managed。

System Managed 字典不允许管理员随意删除关键值。

### FR-DICT-06 插件字典

插件可以注册自己的 Dictionary。

### FR-DICT-07 字典引用保护

如果某 Dictionary Item 已被业务数据引用，删除操作应明确处理引用问题，而不是静默造成历史数据显示异常。

---

## 10. 菜单管理

### FR-MENU-01 菜单树

管理后台使用树形 Menu 组织导航。

### FR-MENU-02 菜单类型

至少支持：

- Section / Group
- Internal Page
- External Link
- Hidden Route

### FR-MENU-03 菜单属性

Menu 可以包含：

- Name
- Icon
- Route
- Sort Order
- Parent
- Enabled
- Required Permission

### FR-MENU-04 权限派生

前端根据 User Permission 决定 Menu 是否可见。

后台 API 仍需独立执行权限校验。

### FR-MENU-05 插件菜单

插件可以注册管理页面和 Menu。

### FR-MENU-06 用户偏好

允许用户保存菜单折叠、收藏页面等 UI Preference，但这类偏好不属于 Menu Definition。

---

# Part III：Communication

## 11. 通知公告

### FR-ANNOUNCEMENT-01 公告创建

管理员可以创建 Announcement。

### FR-ANNOUNCEMENT-02 公告类型

至少支持：

- INFO
- MAINTENANCE
- SECURITY
- UPDATE
- WARNING

类型主要用于产品展示，不必直接等同于通知优先级。

### FR-ANNOUNCEMENT-03 生命周期

Announcement 建议具备：

- DRAFT
- SCHEDULED
- PUBLISHED
- EXPIRED
- WITHDRAWN

### FR-ANNOUNCEMENT-04 定时发布

管理员可以设置发布时间和结束时间。

### FR-ANNOUNCEMENT-05 目标用户

公告可以发送给：

- All Users
- Role
- Selected Users

未来可以扩展其他 Audience。

### FR-ANNOUNCEMENT-06 强提醒

重要安全 / 维护公告可以标记为 High Priority。

### FR-ANNOUNCEMENT-07 已读状态

系统可以记录用户是否已读某个 Announcement。

### FR-ANNOUNCEMENT-08 撤回

管理员可以撤回已经发布的公告。

### FR-ANNOUNCEMENT-09 公告与 Notification

Announcement 是发布内容。

系统可以根据 Announcement 生成 Notification，但 Announcement 本身不等于一条 Notification Delivery。

---

## 12. 通知管理

### FR-NOTIFY-MGMT-01 Notification Center

用户拥有统一 Notification Center。

### FR-NOTIFY-MGMT-02 通知状态

至少支持：

- Unread
- Read
- Archived

### FR-NOTIFY-MGMT-03 Notification Source

Notification 需要知道来源，例如：

- SYSTEM
- ANNOUNCEMENT
- STORAGE
- BACKUP
- SECURITY
- COLLABORATION
- PRODUCTIVITY
- PLUGIN

### FR-NOTIFY-MGMT-04 Channel

通知可以通过多个 Channel 送达：

- In-App
- Email
- Webhook
- Push
- Plugin Provider

### FR-NOTIFY-MGMT-05 Notification Template

系统应支持模板化通知。

模板负责内容格式，不负责判断业务事件是否应该发送。

### FR-NOTIFY-MGMT-06 Notification Rule

规则负责把 Event 映射为 Notification。

例如：

```text
backup.failed
    ↓
High Priority Notification
    ↓
In-App + Email
```

### FR-NOTIFY-MGMT-07 用户偏好

用户可以配置不同类别通知的 Channel Preference。

管理员可以为安全类通知设置不可完全关闭的强制策略。

### FR-NOTIFY-MGMT-08 Provider 管理

管理员可以查看 Notification Provider：

- Enabled
- Health
- Last Delivery
- Error

### FR-NOTIFY-MGMT-09 Delivery Log

系统应记录通知投递结果：

- Pending
- Sent
- Failed
- Retrying
- Skipped

### FR-NOTIFY-MGMT-10 重试

失败通知可以根据 Provider 策略重试。

### FR-NOTIFY-MGMT-11 防通知风暴

健康监控、插件异常等高频事件需要支持合并、降噪或冷却时间，避免同一故障每秒发送大量通知。

---

# Part IV：Audit & Security

## 13. 操作日志

### FR-AUDIT-01 审计范围

具有管理或安全意义的操作需要进入 Operation Log。

典型操作包括：

- 用户创建 / 禁用 / 删除
- Role 修改
- Permission 修改
- Session 强制下线
- 系统参数修改
- Secret 轮换
- 公告发布
- Scheduled Job 修改 / 手动运行
- Storage Provider 修改
- 插件安装 / 启停
- Share / Permission 高风险操作
- 数据恢复 / 删除 / GC

### FR-AUDIT-02 记录内容

Operation Log 至少需要表达：

- Actor
- Action
- Target Type
- Target ID
- Result
- Timestamp
- Request / Trace ID
- Client / IP 摘要

需要时可记录变更前后摘要，但敏感字段必须脱敏。

### FR-AUDIT-03 操作结果

至少区分：

- SUCCESS
- FAILURE
- DENIED

### FR-AUDIT-04 查询

管理员可以按以下条件筛选：

- User
- Action
- Target
- Result
- Time Range

### FR-AUDIT-05 不可普通修改

Audit Log 不能通过普通 CRUD 页面编辑。

### FR-AUDIT-06 Retention

系统允许配置审计日志保留周期。

删除历史 Audit Log 本身也应具备严格权限，并记录管理行为。

### FR-AUDIT-07 导出

具备权限的管理员可以导出选定范围 Audit Log。

### FR-AUDIT-08 隐私

IP、User Agent 等安全数据需考虑隐私与保留期限，避免无限期无目的保存。

---

## 14. 登录日志

### FR-LOGIN-LOG-01 登录成功

记录成功登录事件。

### FR-LOGIN-LOG-02 登录失败

记录失败认证事件。

可以包括经过归类的失败原因：

- INVALID_CREDENTIAL
- ACCOUNT_DISABLED
- ACCOUNT_LOCKED
- MFA_FAILED
- TOKEN_EXPIRED
- PROVIDER_ERROR

不能记录原始密码或敏感 Token。

### FR-LOGIN-LOG-03 登录方式

记录 Authentication Method：

- Password
- 2FA
- OAuth / OIDC
- API Token
- Device Flow
- 其他 Provider

### FR-LOGIN-LOG-04 客户端信息

可以记录：

- Client Type
- Device
- IP
- User Agent

### FR-LOGIN-LOG-05 Session 关联

成功登录事件可以关联产生的 Session。

### FR-LOGIN-LOG-06 异常检测基础

Login Log 为未来以下功能提供数据：

- 暴力登录检测
- 异常登录提醒
- 新设备提醒

V2 初期不要求实现复杂风控模型。

---

## 15. Security Event

Operation Log 和 Login Log 之外，系统可以统一生成 Security Event。

例如：

- 多次登录失败
- 新设备登录
- 2FA 被重置
- 高权限 Role 被授予
- Share Token 大量访问
- Secret 被轮换
- Storage Credential 失效

Security Event 可以触发 Notification。

---

# Part V：Operations

## 16. 定时任务

### 16.1 概念关系

建议使用：

```text
Scheduled Job
“每天凌晨 03:00 执行 Blob Integrity Scan”
        │
        ↓ trigger
Background Task / Command
“一次实际扫描执行”
        │
        ↓
Job Run / Task Run
“2026-08-31 03:00 这次执行结果”
```

### FR-SCHED-01 Job 列表

管理员可以查看全部 Scheduled Job。

### FR-SCHED-02 Job 类型

系统核心与插件都可以注册 Job Definition。

### FR-SCHED-03 Schedule

至少支持：

- Cron
- Fixed Interval
- One-shot

### FR-SCHED-04 Timezone

Schedule 必须明确 Timezone，不能默认所有 Cron 都按服务器本地时区解释而不展示。

### FR-SCHED-05 启用 / 停用

管理员可以启停允许管理的 Job。

### FR-SCHED-06 手动执行

管理员可以手动触发一次 Job。

手动触发必须记录 Actor 和 Audit Log。

### FR-SCHED-07 并发策略

Job 需要能够声明：

- Allow Concurrent
- Forbid Concurrent
- Replace / Skip

具体策略在调度设计中确定。

### FR-SCHED-08 Misfire

系统需要定义错过调度时间后的行为：

- Ignore
- Run Once Immediately
- Catch Up

不同 Job 可以选择不同策略。

### FR-SCHED-09 Retry

Job 失败时可根据策略重试。

### FR-SCHED-10 Execution History

管理员可以查看 Job Run：

- Scheduled Time
- Start Time
- End Time
- Duration
- Result
- Error Summary
- Trigger Type

### FR-SCHED-11 插件 Job

插件可以注册 Scheduled Job。

插件停用后，其 Job 不能继续盲目执行。

### FR-SCHED-12 高风险任务

数据删除、GC、存储迁移等高风险 Job 可以要求额外权限或禁止管理员随意修改 Schedule。

---

## 17. 系统健康总览

管理员需要一个统一 System Health Dashboard。

目标不是替代 Grafana / Prometheus，而是让普通自托管用户无需额外部署监控平台，也能回答：

- Ikaros 当前是否健康？
- 哪个子系统有问题？
- 是 PostgreSQL、对象存储、插件还是后台任务出现异常？
- 问题持续了多久？
- 最近一次成功是什么时候？

---

## 18. Subsystem Health

### FR-HEALTH-01 Health Component

系统需要维护可注册的 Health Component。

核心至少包括：

- Application Runtime
- PostgreSQL
- Object Storage Provider
- Storage Tier
- Server Disk Cache
- Search
- Background Task Worker
- Scheduler
- Notification Provider
- Plugin Runtime
- Realtime / WebSocket

根据实际架构可以增减。

### FR-HEALTH-02 Health Status

统一状态建议至少包括：

- UP
- DEGRADED
- DOWN
- UNKNOWN
- MAINTENANCE

### FR-HEALTH-03 Status Reason

Health Status 必须尽可能给出可理解的原因，而不是只显示红灯。

例如：

```text
Object Storage / Archive
DEGRADED
Last request latency: 8.2s
3 requests failed in last 5m
```

### FR-HEALTH-04 Last Check

每个组件显示最近检查时间。

### FR-HEALTH-05 Last Success

必要组件显示最近一次成功时间。

### FR-HEALTH-06 Dependency

Subsystem 可以表达依赖关系。

例如：

```text
Media Playback
 ├── PostgreSQL
 ├── Object Storage
 └── Transcode Worker
```

当底层依赖异常时，业务 Health 可以表现为 DEGRADED。

### FR-HEALTH-07 Provider 多实例

如果配置多个 Storage Provider / Notification Provider，应分别展示健康状态，而不是只显示一个“存储正常”。

### FR-HEALTH-08 Plugin Health

插件可以暴露自己的 Health Check。

插件异常不能拖垮整个平台健康检查。

### FR-HEALTH-09 Health History

平台可以保留有限时间的 Health 状态变化历史，用于判断故障何时发生和恢复。

---

## 19. Runtime Metrics

### FR-METRIC-01 基础运行指标

系统健康页可以展示：

- CPU
- Memory
- JVM Heap / Non-Heap
- Thread
- Uptime
- GC 摘要

### FR-METRIC-02 PostgreSQL

可展示：

- Connection Pool
- Active Connections
- Query Error 摘要
- Database Latency

不要求在 Ikaros 内实现完整数据库性能分析器。

### FR-METRIC-03 Storage

至少关注：

- Blob Count
- Logical Size
- Physical Size
- Storage Tier Capacity（如 Provider 可提供）
- Cache Size
- Cache Hit Rate
- Missing / Corrupted Blob Count
- Pending Restore Count

### FR-METRIC-04 Background Task

关注：

- Queue Depth
- Running Count
- Failed Count
- Average Duration

### FR-METRIC-05 Scheduler

关注：

- Enabled Jobs
- Running Jobs
- Failed Runs
- Misfire Count

### FR-METRIC-06 Notification

关注：

- Pending Delivery
- Failed Delivery
- Provider Error

### FR-METRIC-07 API

可以提供：

- Request Rate
- Error Rate
- Latency

Ikaros 内置 UI 只需提供面向管理员的摘要。

### FR-METRIC-08 Export

高级用户应能够将 Metrics 暴露给外部监控体系。

后续可考虑兼容：

- Prometheus
- OpenTelemetry

具体集成放到可观测性设计文档。

---

## 20. 服务 / 子系统监控页面

### FR-MONITOR-01 总览

提供统一状态卡片：

```text
System               UP
PostgreSQL           UP
Object Storage Hot   UP
Object Storage Cold  DEGRADED
Server Cache         UP
Search               UP
Scheduler            UP
Notification Email   DOWN
Plugin: Bangumi      UP
```

### FR-MONITOR-02 详情

点击组件可以查看：

- Current Status
- Last Check
- Last Success
- Error Summary
- Key Metrics
- Dependencies
- Recent Status Changes

### FR-MONITOR-03 手动检查

管理员可以主动触发允许手动检查的 Health Check。

### FR-MONITOR-04 Maintenance

管理员可以把某个 Provider / Subsystem 标记为 Maintenance，避免计划维护期间不断产生错误告警。

Maintenance 不应伪装成 UP。

### FR-MONITOR-05 DEGRADED

系统应优先使用 DEGRADED 表达“核心功能仍可运行但部分能力受损”的情况。

例如：

- Archive Storage 不可用，但 Hot Storage 正常
- Email Provider 异常，但 In-App Notification 正常
- 某个 Metadata Plugin 异常，但本地 Resource 可正常访问

不应把所有局部故障都显示成整个平台 DOWN。

---

## 21. Alert

### FR-ALERT-01 Health Alert

Health 状态变化可以产生 Alert。

### FR-ALERT-02 Alert Lifecycle

Alert 至少包含：

- OPEN
- ACKNOWLEDGED
- RESOLVED

### FR-ALERT-03 Alert Severity

可以区分：

- INFO
- WARNING
- ERROR
- CRITICAL

### FR-ALERT-04 Notification Integration

Alert 可以通过 Notification System 通知管理员。

### FR-ALERT-05 Deduplication

同一根因反复失败时应合并 Alert，避免通知风暴。

### FR-ALERT-06 Recovery Notification

组件恢复时可以发送恢复通知。

---

# Part VI：跨子系统组合

## 22. Announcement → Notification

```text
Announcement Published
        ↓
Audience Resolver
        ↓
Notification
        ↓
In-App / Email / Push
```

公告内容和通知投递相互分离。

---

## 23. Health → Alert → Notification

```text
Health Check
     ↓
Subsystem DOWN
     ↓
Alert OPEN
     ↓
Notification Rule
     ↓
管理员收到通知
```

恢复时：

```text
Subsystem UP
     ↓
Alert RESOLVED
     ↓
Recovery Notification
```

---

## 24. Parameter → Audit

```text
管理员修改参数
      ↓
Validation
      ↓
Parameter Update
      ↓
Operation Log
```

如果参数需要重启：

```text
Update
  ↓
Pending Restart
  ↓
管理后台明确提示
```

---

## 25. Scheduled Job → Background Task

例如：

```text
Scheduled Job
每天 02:00 备份数据库
       ↓
Background Task
Backup
       ↓
Task Result
       ↓
Notification
```

调度层只负责触发。

真正耗时的工作交给 Background Task 执行系统。

---

## 26. User Disable → Session Revocation

```text
User Disabled
     ↓
Revoke Sessions
     ↓
Audit Log
     ↓
Security Notification（可选）
```

---

## 27. Role Change → Audit / Security Event

高权限 Role 授予属于安全敏感操作。

```text
Grant Admin Role
      ↓
Audit Log
      ↓
Security Event
      ↓
Optional Notification
```

---

# Part VII：管理后台信息架构

## 28. 建议导航结构

Material Design 3 后台可以组织为：

```text
System
│
├── Dashboard
│
├── Identity & Access
│   ├── Users
│   ├── Roles
│   ├── Permissions
│   └── Online Sessions
│
├── Configuration
│   ├── Parameters
│   ├── Dictionaries
│   └── Menus
│
├── Communication
│   ├── Announcements
│   ├── Notifications
│   ├── Templates
│   └── Providers
│
├── Audit
│   ├── Operation Logs
│   ├── Login Logs
│   └── Security Events
│
└── Operations
    ├── Scheduled Jobs
    ├── Background Tasks
    ├── System Health
    ├── Subsystems
    ├── Storage Health
    └── Plugins
```

这只是产品信息架构建议，不要求后端代码按照菜单树拆模块。

---

## 29. Dashboard

System Dashboard 可以聚合：

- Overall Health
- Active Users / Sessions
- Running Background Tasks
- Failed Jobs
- Storage Health
- Recent Alerts
- Recent Security Events
- Pending Notifications
- Version / Uptime

Dashboard 应以“快速判断是否需要管理员处理”为目标，不堆砌大量无意义数字。

---

# Part VIII：权限建议

## 30. 管理权限域

建议至少形成以下 Capability Domain：

```text
USER
ROLE
PERMISSION
SESSION
PARAMETER
DICTIONARY
MENU
ANNOUNCEMENT
NOTIFICATION
AUDIT
SCHEDULER
HEALTH
ALERT
```

每个 Domain 根据实际需要拥有：

- READ
- CREATE
- UPDATE
- DELETE
- EXECUTE
- MANAGE

不要求每个对象机械拥有全部动作。

例如 Health 通常只需要 READ / CHECK，而 Scheduler 需要 READ / MANAGE / EXECUTE。

---

## 31. 高风险操作

以下操作应考虑单独 Permission 或二次确认：

- 禁用管理员账户
- 强制所有用户下线
- 授予 Super Admin
- 修改关键 Storage 参数
- 修改 Secret
- 手动执行 GC
- 手动执行 Restore / Disaster Recovery
- 删除 Audit Log
- 执行高风险 Scheduled Job

---

# Part IX：数据与生命周期原则

## 32. 管理数据生命周期

平台管理对象同样需要生命周期设计。

例如：

```text
Announcement
Draft → Scheduled → Published → Expired / Withdrawn
```

```text
Session
Active → Idle → Expired / Revoked
```

```text
Alert
Open → Acknowledged → Resolved
```

```text
Scheduled Job
Enabled ↔ Disabled
        ↓
Archived / Removed
```

---

## 33. Audit 数据保留

不同数据可以配置不同保留周期：

- Login Log
- Operation Log
- Health History
- Metrics Rollup
- Job Run
- Notification Delivery Log

高频 Metrics 不应与安全 Audit Log 使用相同长期保存策略。

---

## 34. 可搜索性

管理后台需要能够搜索：

- User
- Role
- Operation Log
- Login Log
- Notification
- Announcement
- Scheduled Job
- Alert

大量日志查询必须具有明确时间范围和索引策略，具体由数据库设计确定。

---

# Part X：API 与插件原则

## 35. Admin API

平台管理 API 与普通 Resource API 应在权限模型上明确区分。

第三方客户端不应因为能够访问 Resource API 就自然获得 Admin API。

---

## 36. Plugin Integration

插件可以扩展：

- Parameter
- Dictionary
- Menu
- Permission
- Notification Provider
- Scheduled Job
- Health Check
- Metrics

插件必须声明扩展项归属。

插件卸载后，平台需要能够识别这些项已经失效。

---

## 37. Event Integration

平台事件建议包括：

```text
user.created
user.disabled
user.login.succeeded
user.login.failed
role.assigned
parameter.changed
announcement.published
notification.failed
scheduled_job.failed
health.degraded
health.down
health.recovered
alert.opened
alert.resolved
```

事件名称只作为产品级示例，正式命名由 Event 设计确定。

---

# Part XI：非功能需求

## 38. 安全

### NFR-PLATFORM-01 Least Privilege

平台管理功能必须遵循最小权限原则。

### NFR-PLATFORM-02 Secret Redaction

日志、通知和错误信息不得泄露 Password、Token、Secret、Authorization Header 等敏感内容。

### NFR-PLATFORM-03 Audit Coverage

高风险管理操作必须可审计。

### NFR-PLATFORM-04 Session Revocation

管理员撤销 Session 后需要可靠生效。

---

## 39. 可用性

### NFR-PLATFORM-05 管理能力不影响业务主链路

健康监控、日志查询等管理功能发生故障时，不应轻易拖垮正常媒体播放、阅读或 Resource API。

### NFR-PLATFORM-06 Health Check 隔离

外部 Provider Health Check 必须有 Timeout 和隔离机制。

一个失效插件不能让整个 `/health` 永久阻塞。

---

## 40. 性能

### NFR-PLATFORM-07 大日志量

Operation Log / Login Log / Job Run 等应支持长期大量数据下的时间范围查询。

### NFR-PLATFORM-08 Metrics Aggregation

高频 Metrics 可以通过聚合、降采样等方式保存趋势，不要求永久保存每个采样点。

---

## 41. 可观测性开放

### NFR-PLATFORM-09 External Observability

内置管理后台提供开箱即用的基础监控，但不能把高级用户锁死在 Ikaros 自有 UI。

应允许外部可观测性平台消费标准 Metrics / Trace / Log 数据。

---

# Part XII：MVP 优先级

## 42. P0

V2 Platform P0 应至少包含：

- User Management
- Role Management
- Permission Registry
- Platform RBAC
- Session / Online User
- Operation Log
- Login Log
- Parameter Management
- Announcement
- Notification Center
- Scheduled Job
- Job Run History
- System Health
- PostgreSQL Health
- Object Storage Health
- Plugin Health
- Background Task Health

---

## 43. P1

- Dictionary Management
- Menu Management
- Notification Template
- Notification Provider Management
- Security Event
- Alert
- Runtime Metrics Dashboard
- Storage Metrics
- Scheduler Metrics
- Session Device Management

---

## 44. P2

- Advanced Alert Rules
- Metrics History / Rollup
- Prometheus Export
- OpenTelemetry Integration
- Advanced Security Detection
- Organization / Group IAM 扩展
- More Advanced Policy Control

---

# Part XIII：需要继续拆分的设计文档

## 45. IAM 详细设计

后续单独确定：

- User
- Authentication
- Session
- Role
- Permission
- Platform RBAC
- Resource ACL
- 2FA
- Token

---

## 46. Notification 详细设计

确定：

- Notification Model
- Template
- Rule
- Channel
- Provider
- Delivery
- Retry
- Preference
- Deduplication

---

## 47. Scheduler / Background Task 设计

明确：

- Scheduled Job
- Trigger
- Background Task
- Worker
- Retry
- Concurrency
- Distributed Lock（如需要）
- Job Run

---

## 48. Observability 设计

明确：

- Health
- Metrics
- Log
- Trace
- Alert
- Prometheus / OpenTelemetry
- Retention

---

## 49. Platform Configuration 设计

明确：

- Parameter
- Secret
- Dictionary
- Menu
- Scope
- Reload / Restart
- Plugin Registration

---

# 50. 最终边界总结

Ikaros V2 的平台系统可以概括为：

```text
业务系统回答：
“我的内容、计划、文档、媒体和目标怎么管理？”

平台管理回答：
“谁可以做什么，系统应该如何配置和治理？”

系统运维回答：
“Ikaros 现在是否健康，出了什么问题，谁正在使用，后台正在做什么？”
```

三者共同构成完整的 V2：

```text
Ikaros V2
│
├── Business Domains
│   ├── Content / Media
│   ├── Creation / Documents
│   ├── Productivity / Planning
│   └── Collaboration
│
├── Platform Administration
│   ├── Identity
│   ├── RBAC
│   ├── Configuration
│   ├── Announcement
│   ├── Notification
│   └── Audit
│
└── System Operations
    ├── Scheduler
    ├── Session
    ├── Health
    ├── Metrics
    └── Alert
```

其中：

- Menu 永远不是权限源。
- Platform RBAC 与 Resource ACL 永远分层。
- Parameter 与 Secret 永远分层。
- Announcement 与 Notification 永远分层。
- Scheduled Job、Background Task、Productivity Task 永远使用不同领域语义。
- Audit Log、Activity、Runtime Log 永远区分用途。
- Subsystem Health 不要求微服务化。
- 内置监控提供开箱即用体验，同时保持对外部可观测性体系开放。
