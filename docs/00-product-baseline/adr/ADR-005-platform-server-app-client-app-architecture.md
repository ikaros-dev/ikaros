# ADR-005：Platform / Server App / Client App 三层应用架构

| 项目 | 内容 |
|---|---|
| 状态 | Accepted |
| 日期 | 2026-09-19 |
| 影响范围 | System Overview、Module Ownership、Security、App Runtime、客户端交互 |
| 替代/约束 | 约束 V2 中业务应用与 Plugin 的边界；不废弃既有 Plugin Runtime 的扩展能力 |

## 背景

Ikaros V2 已经具备 Resource、Attachment / Blob、Authentication、Authorization、Event / Outbox、Background Task、Notification、Audit、Secret、Plugin Runtime 等平台能力，同时也把 Media、Photo、Drive、Finance、Reading 等专业业务作为 Server 内部业务模块规划。

随着 V2 从“内容管理系统”继续演进为自托管个人数字平台，如果继续让 Platform 直接拥有越来越多专业业务，Platform 会逐步成为一个大而全的业务单体；同时，移动端若继续以一个统一 App Shell 承载所有业务，也会把服务端的领域边界重新耦合回客户端。

本 ADR 将 V2 的产品与运行时边界收敛为三层：

```text
Client App
    │ Public App API
    ▼
Server App
    │ Platform API / Capability
    ▼
Ikaros Platform
```

## 决策

### 1. Ikaros Platform 只拥有基础平台能力

Platform 负责通用机制和系统级事实，包括但不限于：

- Instance；
- User / Identity；
- Authentication；
- Authorization；
- Device；
- App Registry / App Runtime；
- Resource / Collection / Tag / Relation；
- Attachment / Blob / Storage；
- Event / Outbox / Integration；
- Background Task / Scheduler；
- Notification；
- Audit / Operation Log；
- Configuration / Secret；
- Search Infrastructure；
- Observability。

Anime、Accounting、Photos、Drive、Reading、Music 等专业业务不再视为 Platform Foundation。

### 2. 专业业务以 Server App 形式存在

Server App 是服务端一级业务运行单元，拥有自己的：

- App identity；
- Domain model；
- Application service；
- Persistence / Schema；
- Migration；
- Public App API；
- Permission declaration；
- Client scopes；
- Event producer / consumer；
- Background Task handler；
- Configuration；
- 生命周期。

Server App 通过 Platform API / Capability 使用平台能力，不得直接访问其他 Owner 的 Repository、Entity、私有 SQL 或内部 Spring Bean。

第一方 Server App 与第三方 Server App 应尽量遵循同一运行时契约。第一方实现不得依赖“官方后门”绕过 App Runtime、Authorization 或 Owner Boundary。

### 3. Client App 是独立软件产品

Client App 与 Server App 的安装、发布、版本和生命周期独立。

例如：

```text
Ikaros                 -> 平台管理 / 快速认证
Ikaros Anime           -> Anime Client App
Ikaros Photos          -> Photos Client App
Ikaros Drive           -> Drive Client App
Ikaros Accounting      -> Accounting Client App
```

官方 Ikaros 移动客户端不作为承载所有业务的超级 App。其主要职责是：

- Instance 管理；
- 平台登录；
- 快速认证 / 授权代理；
- Device / Session 管理；
- Server App 管理；
- 必要的平台通知与运维入口。

专业业务由独立 Client App 承载。

### 4. Server App Permission 与 Client Scope 分离

Server App 访问 Platform 使用 Platform Permission，例如：

```text
resource.read
resource.write
attachment.read
task.submit
notification.send
network.external
```

Client App 访问 Server App 使用 App Scope，例如：

```text
anime.library.read
anime.playback
anime.progress.write
```

两者不得混为一套权限。

普通业务 Client 不应直接依赖 Resource / Attachment 等 Platform Domain API 拼装业务；客户端优先调用目标 Server App 的 Public App API。

### 5. 建立五类独立身份

至少区分：

```text
Instance
Server App
Client App
Device
User / Subject
```

授权、审计、Token、事件或调用上下文在适用时应能够表达：

- `instance_id`
- `app_id`
- `client_id`
- `device_id`
- `subject_id`

不得用一个模糊的 `app` 或 `plugin` 身份同时表示服务端应用、客户端软件和执行主体。

### 6. Native Client 是 Public Client

移动端和桌面端原生 Client 不持有可视为秘密的静态 Client Secret。

Native Client 授权采用标准浏览器/系统认证通道兼容的 Authorization Code + PKCE 模型，并通过受控 Redirect URI、App Link / Universal Link 或等价机制完成回跳。

Ikaros 管理客户端可以作为认证体验的 Broker，但不得把自身 Token 复制给业务 Client。业务 Client 必须获得属于自己 `client_id`、目标 audience 和 scope 的授权结果。

### 7. App Discovery 是平台能力

Platform 提供稳定的 Instance / App Discovery，使 Client 可以确定：

- Instance identity；
- Platform compatibility；
- Authorization endpoint；
- Server App 是否安装；
- Server App 是否启用；
- Server App version；
- Public App API version；
- Feature / capability；
- Client authorization metadata。

“未安装”“已禁用”“API 不兼容”必须可区分，不应统一退化为无法解释的 404。

### 8. Server App 和 Client App 生命周期独立

Server App 至少支持：

```text
DISCOVERED
INSTALLING
INSTALLED
ENABLING
ENABLED
DISABLING
DISABLED
UPGRADING
FAILED
UNINSTALLING
UNINSTALLED
INCOMPATIBLE
```

Client App 的本地安装、Instance 连接、授权和 Token 生命周期由客户端平台与 Authentication / Authorization 共同管理，不与 Server App Package 生命周期绑定。

Server App Package 不包含 APK / IPA 等客户端二进制；客户端通过各自平台的独立分发渠道发布。

### 9. App-owned Data 与 Platform Resource 分离

Server App 拥有自己的专业业务数据。

并非所有 App Domain Entity 都必须成为 Resource。只有确实需要统一资源身份、附件、标签、关系、搜索、分享等 Platform Resource 能力的对象才建立 Resource 关联。

例如 Accounting 的 Ledger、Account、Transaction 可以完全属于 Accounting App；Receipt / Statement 等内容对象可以引用 Platform Resource / Attachment。

### 10. Plugin 不再作为“业务应用”的产品级抽象

现有 Plugin Runtime 的扩展机制仍可用于 Provider、Importer、Parser、Storage Provider、Automation Extension 等平台扩展场景，并可作为 App Runtime 实现经验与兼容层来源。

但是 Anime、Photos、Drive、Accounting 等完整业务不再以“Plugin”作为产品语义。

后续新增的业务应用设计使用 Server App / Client App / App Runtime 术语。

### 11. V2 初期不要求动态热装载任意 JVM App

V2 继续保持 Modular Monolith 和单 Server Composition Root 基线。

第一阶段允许第一方 Server App 继续以 Maven Module 随 Server 构建，但必须遵守 App API、Owner、Permission、Lifecycle 等逻辑边界。

物理动态安装可分阶段演进：

```text
逻辑 App Boundary
→ App Registry / Lifecycle
→ Server App Package
→ 重启后装载
→ 必要时再演进 Hot Loading / Out-of-process App Host
```

不得为了“像手机系统”而在首阶段引入不成熟的 ClassLoader 热卸载或任意不可信代码执行。

## 结果

该决策带来的直接变化：

1. System Overview 将 Platform 与专业 Server App 分层；
2. Module Ownership 增加 App Runtime，并把业务模块定义为 Server App Owner；
3. 新增 App Runtime / Identity / Discovery / Client Authorization 设计；
4. 移动端交互不再以统一业务 App Shell 作为长期产品模型；
5. Plugin Runtime 保留，但不再承担完整业务应用抽象；
6. 后续 Anime、Photos、Drive、Accounting 等实现必须以 Server App 的 Public API 向独立 Client App 提供业务能力。

## 非目标

本 ADR 不在当前变更中：

- 实现动态 JVM App 加载；
- 修改现有数据库 Schema；
- 实现 OAuth/OIDC Server；
- 拆分现有 Maven 仓库；
- 发布独立移动客户端；
- 删除现有 Plugin Runtime；
- 一次性迁移所有历史业务模块。

这些内容应在后续实现 Issue / PR 中依据本 ADR 分阶段完成。
