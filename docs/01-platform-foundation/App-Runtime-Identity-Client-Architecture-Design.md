# Ikaros V2 App Runtime / Identity / Client Architecture Design

| 项目 | 内容 |
|---|---|
| 文档名称 | App Runtime / Identity / Client Architecture Design |
| 适用版本 | Ikaros V2 |
| 状态 | Draft |
| 上位决策 | `adr/ADR-005-platform-server-app-client-app-architecture.md`、`adr/ADR-006-app-client-authorization-grant-token-binding.md` |
| 关联设计 | `System-Overview-Design.md`、`Module-Package-Ownership-Design.md`、`Security-Identity-Authorization-Crypto-Subsystem-Design.md` |

> 本文档定义 Ikaros V2 的 Platform、Server App、Client App 三层关系，以及 App Registry、App Identity、Client Registration、权限、授权、发现、生命周期、数据所有权与兼容性边界。

---

## 1. 设计目标

Ikaros V2 的 App 模型需要同时满足：

1. Platform 只提供稳定基础能力，不拥有越来越多专业业务。
2. Anime、Photos、Drive、Accounting、Reading 等专业业务能够作为独立 Server App 演进。
3. 手机端、桌面端的专业 Client App 可以独立安装、发布和升级，而不是被强制塞进一个统一超级 App。
4. 官方 Ikaros 客户端能够承担 Instance 管理、快速认证和授权 Broker，但不成为业务聚合壳。
5. 第一方与第三方 App 尽可能使用同一公开契约。
6. App 的数据、权限、Migration、事件、任务和故障拥有清晰 Owner。
7. 当前 Modular Monolith 可以平滑演进，不要求首阶段实现 JVM 热装载。
8. App API、Client Scope 与 Platform Permission 各自有明确语义，避免授权边界混乱。

---

## 2. 三层模型

```text
┌───────────────────────────────────────────────┐
│ Client Applications                           │
│                                               │
│ Ikaros Admin / Anime / Photos / Drive / ...  │
│ Official or Third-party                       │
└──────────────────────┬────────────────────────┘
                       │ Public App API
                       ▼
┌───────────────────────────────────────────────┐
│ Server Apps                                   │
│                                               │
│ Anime / Photos / Drive / Accounting / ...    │
│ Domain / API / Data / Event / Task           │
└──────────────────────┬────────────────────────┘
                       │ Platform API / Capability
                       ▼
┌───────────────────────────────────────────────┐
│ Ikaros Platform                               │
│                                               │
│ Auth / Authorization / Device / App Runtime  │
│ Resource / Storage / Event / Task / Audit    │
│ Notification / Config / Secret / Search Infra│
└───────────────────────────────────────────────┘
```

### 2.1 Platform

Platform 负责机制、身份、安全和跨 App 的稳定基础能力。

Platform 不解释 Anime Episode、Accounting Transaction、Photo EXIF 等专业领域语义。

### 2.2 Server App

Server App 是服务端一级业务单元。

一个 Server App 可以拥有：

- Domain Entity；
- App-owned Schema；
- Migration；
- Application Service；
- Public App API；
- Event；
- Task Handler；
- Search projection；
- Notification event type；
- Platform Permission；
- Client Scope；
- App Configuration。

### 2.3 Client App

Client App 是独立的软件产品。

Client App 可以是：

- iOS；
- Android；
- Desktop；
- Web；
- CLI；
- 第三方客户端。

Client App 不属于 Server App Package，也不与 Server App 共用安装生命周期。

---

## 3. 身份模型

至少存在五种一级身份：

### 3.1 Instance Identity

```text
instance_id
```

表示一个 Ikaros Server Instance。

### 3.2 Server App Identity

```text
app_id
```

例如：

```text
run.ikaros.anime
run.ikaros.photos
run.ikaros.accounting
```

`app_id` 必须稳定、全局唯一，并在升级时保持不变。

### 3.3 Client App Identity

```text
client_id
```

例如：

```text
run.ikaros.anime.ios
run.ikaros.anime.android
dev.example.anime-client
```

一个 Server App 可以被多个 Client App 使用。

### 3.4 Device Identity

```text
device_id
```

表示 Client App 所运行的设备身份或平台注册的设备记录。

### 3.5 Subject Identity

```text
subject_id
```

通常表示 User，也可以在明确设计后表示 Service Principal / Automation Principal。

### 3.6 调用上下文

在适用时，审计、授权与追踪上下文至少能够表达：

```text
instance_id
app_id
client_id
device_id
subject_id
correlation_id
causation_id
```

不得将 Client App 身份当作 User，不得将 Server App 身份当作管理员。

---

## 4. App Registry

Platform App Registry 是 Server App 身份与运行状态的权威来源。

建议逻辑模型：

```text
AppDefinition
AppInstallation
AppVersion
AppPermissionGrant
AppDependency
AppMigrationState
AppClientRegistration

Authorization-owned:
AppAuthorizationGrant
```

### 4.1 AppDefinition

表达稳定身份和静态元信息：

- app_id；
- name；
- publisher；
- description；
- icon metadata；
- package format version；
- declared resource types；
- declared permissions；
- declared scopes；
- public API versions。

### 4.2 AppInstallation

表达 Instance 内安装状态：

- app_id；
- installed version；
- lifecycle state；
- installed_at；
- enabled_at；
- disabled_at；
- failure summary；
- configuration state。

### 4.3 AppVersion

记录已知版本和兼容性元信息：

- app_id；
- version；
- minimum platform API；
- maximum platform API；
- package checksum；
- publisher signature metadata；
- manifest version。

### 4.4 AppPermissionGrant

记录 Server App 实际获得的平台权限。

Manifest declaration 不等于授权。

```text
Effective Server App Permission
= Declared Permission
∩ Instance Granted Permission
∩ Platform Policy
```

### 4.5 AppClientRegistration

记录可识别 Client 的公开元信息：

- client_id；
- app_id；
- name；
- client_type；
- publisher；
- redirect URIs；
- supported platform；
- official / third-party metadata；
- status。

`official` 只能作为来源与 UX 元数据，不得产生授权旁路。

### 4.6 AppAuthorizationGrant 的 Owner

`AppAuthorizationGrant` 不属于 App Registry / App Runtime 持久化所有权，而属于 Authorization。

App Runtime 只提供：

- Client Registration；
- Server App identity / availability；
- App Scope Definition；
- App API compatibility metadata。

Authorization 保存：

```text
subject
+ client
+ target app
+ optional device
+ granted scopes
+ grant version
+ status
```

的实际授权关系，并通过公开 Capability 与 Authentication 协作。完整决策见 ADR-006。

---

## 5. Platform Permission 与 App Scope

这是两套不同的授权空间。

### 5.1 Platform Permission

方向：

```text
Server App → Platform
```

例如：

```text
resource.read
resource.write
attachment.read
attachment.create
task.submit
notification.send
secret.reference.resolve
network.external
```

### 5.2 App Scope

方向：

```text
Client App → Server App
```

例如 Anime：

```text
anime.library.read
anime.library.write
anime.playback
anime.progress.write
anime.offline
```

例如 Accounting：

```text
accounting.ledger.read
accounting.transaction.write
accounting.budget.read
```

Client 不需要获得 Server App 内部使用的 `resource.write` 或 `attachment.read`。

### 5.3 授权原则

Client 调用 Server App 时：

```text
Effective Access
= User Permission
∩ Client Granted Scope
∩ Server App Policy
∩ Target Object Policy
```

Server App 再调用 Platform 时：

```text
Effective Platform Access
= Server App Granted Permission
∩ Acting Principal Permission
∩ Target Platform Capability Policy
```

---

## 6. Public App API

专业 Client 优先调用 Server App Public API。

例如：

```text
Anime Client
    ↓
Anime Public API
    ↓
Anime Application / Domain
    ↓
Resource / Storage / Task Platform API
```

不推荐：

```text
Anime Client
    ↓
Resource API + Attachment API + Relation API
    ↓
客户端自行拼装 Anime 领域
```

原因：

- Domain Logic 会泄漏到客户端；
- Server App 无法稳定演进；
- Client 与 Platform 内部模型过度耦合；
- 权限范围被迫扩大。

Platform API 可以继续向管理员、脚本或明确的通用客户端公开，但普通专业业务 Client 不应依赖它作为业务主路径。

---

## 7. App Discovery

Platform 应提供稳定 Discovery Contract。

### 7.1 Instance Discovery

建议提供 well-known 入口，用于发现：

- instance_id；
- platform version；
- authorization endpoint；
- token endpoint；
- app registry endpoint；
- supported protocol version。

具体 URL 在 API Convention / OpenAPI 中冻结。

### 7.2 Server App Discovery

Client 应能够查询目标 Server App：

- installed / not installed；
- enabled / disabled；
- app version；
- public API versions；
- capabilities；
- authorization scopes；
- compatibility metadata。

以下情况必须可区分：

```text
APP_NOT_INSTALLED
APP_DISABLED
APP_INCOMPATIBLE
APP_API_VERSION_UNSUPPORTED
APP_PERMISSION_REQUIRED
```

---

## 8. Client Authorization

### 8.1 Native Client

iOS / Android / Desktop 原生应用属于 Public Client。

不得把固定 `client_secret` 嵌入 APK、IPA 或桌面安装包，并假设其保持秘密。

### 8.2 Authorization Code + PKCE

Native Client 应使用 Authorization Code + PKCE 一类的授权模式，并通过受控 Redirect URI、App Link、Universal Link 或等价系统机制完成回调。

具体 OAuth2 / OIDC Profile 在 Security 详细设计与 OpenAPI / Authorization Contract 中冻结。

授权成功后必须建立或更新 Authorization-owned `AppAuthorizationGrant`。Grant 是“用户允许某个 Client 以哪些 Scope 访问哪个 Server App”的持久化授权事实，不是 Login Session。

### 8.3 Ikaros 管理客户端作为 Auth Broker

官方 Ikaros 管理客户端可以优化认证体验：

```text
Business Client
    ↓ request authorization
Ikaros Admin Client
    ↓ authenticate / confirm
Ikaros Platform
    ↓ issue authorization result
Business Client
```

但是：

- Ikaros 管理客户端不得把自己的 Access Token 复制给业务 Client；
- 每个业务 Client 必须拥有独立 `client_id`；
- Token 必须绑定正确 `client_id`、audience、scope、`authorization_grant_id` 与 `authorization_grant_version`；
- 用户可以独立撤销某个 Client / Device 的 Grant；撤销后只使该 Grant 绑定的 Token 失效，不影响同用户其他 Client；
- 官方 Client 不获得绕过 Server Authorization 的隐藏能力。

### 8.4 AppAuthorizationGrant 与撤销

依据 ADR-006，App-scoped Token 至少绑定：

```text
subject_id
client_id
aud = app_id
scope
authorization_grant_id
authorization_grant_version
device_id (optional)
```

请求时除 JWT 标准校验与用户 `security_version` 外，还必须校验 Grant ACTIVE 状态、Grant Version、Client / App 匹配和 Scope 子集关系。

撤销：

```text
Grant.status = REVOKED
Grant.grant_version++
```

即可使对应 Client / Device 的旧 Access / Refresh Token 失效，不需要 Token blacklist、`jti` Persistence 或 Security Session。

---

## 9. Server App Lifecycle

建议状态：

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

### 9.1 Install

```text
Package Receive
→ Integrity Check
→ Manifest Parse
→ Compatibility Check
→ Permission Review
→ Dependency Check
→ App-owned Migration Plan
→ Install
→ Register App Metadata
→ INSTALLED
```

Install 不自动等于 Enable。

### 9.2 Enable

Enable 前至少检查：

- Platform API compatibility；
- Migration state；
- required configuration；
- Secret Reference；
- Platform Permission grant；
- required App dependency；
- Public API registration。

### 9.3 Disable

Disable：

- 停止接收新的业务请求；
- 停止新任务进入；
- 停止 Event consumer；
- 保留 App-owned Data；
- 保留配置；
- 不删除 Platform Resource / Attachment。

### 9.4 Uninstall

Uninstall 必须明确数据保留策略。

建议：

```text
KEEP_DATA
DELETE_APP_DATA
```

即使选择删除 App-owned Data，也不能隐式删除已经进入 Platform Domain 的 Resource / Attachment；这类对象必须由各自 Owner 的显式 Command 管理。

---

## 10. Client Lifecycle

Client 生命周期独立于 Server App：

```text
NOT_INSTALLED
INSTALLED
INSTANCE_CONNECTED
AUTHORIZED
TOKEN_EXPIRED
REVOKED
```

以下组合都合法：

```text
Client 已安装，Server App 未安装
Server App 已安装，Client 未安装
Client 已授权，Server App 后续被禁用
Client 版本旧于 Server App，但仍兼容某个 Public API Version
```

---

## 11. Server App Package

Server App Package 建议逻辑结构：

```text
server-app-package
├── manifest
├── executable artifact
├── app-owned migrations
├── public API schema / OpenAPI fragments
├── optional admin assets
├── static resources
└── integrity / signature metadata
```

Package 不包含：

- Android APK；
- iOS IPA；
- Desktop installer。

Client 由各自平台独立分发。

---

## 12. App Manifest

建议逻辑字段：

```yaml
app_id: run.ikaros.anime
name: Ikaros Anime
version: 3.4.1
publisher: ikaros-dev

platform:
  api:
    min: 2.1
    max: 2.x

permissions:
  - resource.read
  - resource.write
  - attachment.read
  - attachment.write
  - task.submit
  - notification.send

resource_types:
  - anime
  - season
  - episode

public_api:
  versions:
    - v3

scopes:
  - anime.library.read
  - anime.library.write
  - anime.playback
  - anime.progress.write

events:
  publish:
    - anime.created
    - anime.episode.created
  subscribe:
    - attachment.deleted

tasks:
  - anime.probe
  - anime.metadata.refresh

dependencies: []

data:
  schema: app_anime
  migration_version: 17
```

Manifest 是 App 身份和兼容性的正式入口，不通过 Classpath 扫描猜测业务能力。

---

## 13. App-owned Data

Server App 是自己专业业务数据的唯一 Owner。

例如：

```text
Anime
Season
Episode
MediaRelease
```

属于 Anime App。

```text
Ledger
Account
Transaction
Budget
```

属于 Accounting App。

其他 App 不得读取其 Persistence 作为集成手段。

---

## 14. Resource 使用原则

Resource 是可选的平台能力，不是所有 App Entity 的父类。

适合 Resource 的对象：

- 有稳定内容身份；
- 需要 Attachment；
- 需要 Tag / Collection / Relation；
- 需要 Search / Share；
- 需要跨 App 引用。

不必成为 Resource 的对象：

- Accounting Transaction；
- Ledger；
- Budget；
- App 内部 Rule；
- 临时工作状态。

例如：

```text
Accounting Transaction
    │
    └── receipt_resource_id
            ↓
        Platform Resource
            ↓
        Attachment
```

---

## 15. Resource Type Namespace

Platform 不应拥有所有专业 Resource Type 的业务解释。

Server App 声明命名空间类型：

```text
run.ikaros.anime/anime
run.ikaros.anime/season
run.ikaros.anime/episode
run.ikaros.photos/photo
```

Platform 只维护类型身份与通用资源规则。

类型的专业字段和行为属于 Server App。

---

## 16. App-to-App Dependency

Server App 之间只能通过：

- Public App API；
- Stable Capability；
- Command；
- Durable Event。

不得通过 Repository / Entity / SQL 直接耦合。

Manifest 可以声明：

```yaml
dependencies:
  - app_id: run.ikaros.document
    api: "^2"
    required: false
```

Enable 时由 App Runtime 检查 required dependency compatibility。

---

## 17. Notification

Platform 拥有：

- Notification storage；
- read / unread；
- preference；
- quiet hours；
- delivery channel；
- push infrastructure。

Server App 拥有通知语义：

```text
anime.new_episode
accounting.budget_exceeded
photos.import_completed
```

通知上下文在需要时携带 `app_id` 与业务 deep link，由对应独立 Client App 处理。

---

## 18. Search

Platform 拥有：

- index runtime；
- query runtime；
- ACL-aware filtering；
- generation / checkpoint；
- rebuild orchestration。

Server App 拥有：

- searchable field semantics；
- document projection；
- domain-specific query；
- result rendering metadata。

Platform 不解释“声优”“预算”“EXIF”等专业字段。

---

## 19. Background Task

Platform 拥有 Task / Attempt / Lease / Retry / Progress / Cancellation。

Server App 注册自己的 Task Handler，例如：

```text
anime.probe-media
anime.refresh-metadata
photos.generate-preview
accounting.import-statement
```

Task Runtime 不拥有这些业务语义。

---

## 20. Audit

Server App 的高价值动作必须进入 Platform Audit。

Audit 在适用时记录：

- subject_id；
- app_id；
- client_id；
- device_id；
- action；
- target reference；
- result；
- correlation_id。

Audit 不复制完整业务正文、Secret 或不必要的敏感 Payload。

---

## 21. 第一方 App 原则

第一方 Server App 应作为 App Runtime 的 dogfood：

- 不跨 Owner 访问 Repository；
- 不绕过 Platform Permission；
- 不依赖未公开内部 Bean；
- Public API 与第三方 Client 使用同一授权模型；
- 不因为 `official=true` 自动获得额外数据权限。

第一方 App 可以与 Server 同仓、同进程、同版本构建，但逻辑边界仍按 Server App Contract 执行。

---

## 22. Plugin 与 App 的关系

Plugin 与 Server App 是不同产品语义。

Plugin 更适合：

- Metadata Provider；
- Importer；
- Storage Provider；
- Notification Provider；
- Parser；
- Search Enricher；
- Automation Trigger / Action；
- Provider Adapter。

Server App 更适合：

- Anime；
- Photos；
- Drive；
- Accounting；
- Reading；
- Music；
- Document 等完整业务。

现有 Plugin Runtime 的以下机制可以复用或迁移到 App Runtime：

- Manifest；
- Compatibility；
- Permission Review；
- Lifecycle；
- App-owned Migration；
- Configuration；
- Secret Reference；
- Task Integration；
- Failure Isolation。

但不应继续把完整业务应用称为 Plugin。

---

## 23. V2 分阶段实施

### Phase 1：逻辑边界

- 定义 App Runtime Contract；
- 增加 App Registry；
- 建立 `app_id` / `client_id` 语义；
- 第一方业务模块按 Server App Owner 约束；
- 保持 Modular Monolith。

### Phase 2：客户端授权与发现

- Instance Discovery；
- App Discovery；
- Client Registration；
- App Scope；
- Authorization Code + PKCE；
- Ikaros 管理客户端认证 Broker。

### Phase 3：Server App Package

- Package Manifest；
- Compatibility；
- Permission Grant；
- App Migration；
- install / enable / disable / uninstall；
- 重启后装载。

### Phase 4：隔离增强

只有出现真实需求时再考虑：

- Hot Loading；
- Out-of-process App Host；
- Process Isolation；
- WASM；
- per-App resource quota；
- signed marketplace。

---

## 24. 初始验收不变量

后续实现至少应验证：

1. 一个 Server App 不能读取另一个 App 的 Repository。
2. 未授予 Platform Permission 的 Server App 调用被拒绝。
3. 未授予 App Scope 的 Client 调用被拒绝。
4. 官方 Client 不绕过 scope。
5. 一个 Client 的 Token 不能被另一个 Client 直接复用为自身身份。
6. Server App Disabled 后停止接受业务调用，但数据保留。
7. Server App Uninstall 不隐式删除 Platform Resource / Attachment。
8. App API 版本不兼容可以在 Discovery 阶段识别。
9. Native Client 不依赖静态 Client Secret。
10. Accounting 等领域可以拥有不依赖 Resource 的核心实体。
11. Resource Type 可由 Server App 通过命名空间扩展。
12. App 创建的高价值操作可关联到 app_id / client_id / subject_id 审计上下文。
