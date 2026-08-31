# Ikaros V2 Plugin Runtime / SDK / Lifecycle Design

| 项目 | 内容 |
|---|---|
| 文档名称 | Plugin Runtime / SDK / Lifecycle Design |
| 适用版本 | Ikaros V2 |
| 状态 | Draft |
| 上位设计 | `System-Overview-Design.md`、`Platform-Integration-Automation-Design.md`、`Security-Identity-Authorization-Crypto-Subsystem-Design.md` |

> 本文档定义 Ikaros V2 插件运行时、插件包、Manifest、权限、扩展点、兼容性、生命周期、配置、Secret、迁移、前端扩展与故障隔离。
>
> 插件是 V2 的主要扩展机制之一，但插件不是拥有任意 Server 内部访问权的“动态模块”。插件只能通过稳定 Plugin API、Capability、Command、Event 和明确 Extension Point 与平台协作。

---

## 1. 设计目标

插件系统需要同时满足：

- 支持 Metadata Provider、Importer、Storage Provider、Notification Provider、Automation、AI Provider、Parser、Search Enricher 等扩展；
- 不把插件变成任意数据库访问后门；
- 插件权限可声明、可审计、可撤销；
- 插件安装、升级、禁用、卸载有明确生命周期；
- Stable / Experimental / Deprecated API 有独立版本策略；
- 插件故障不会破坏 Server 核心业务一致性；
- 插件自身配置、Secret 与数据有清晰所有权；
- 插件可以在未来独立进程化，但 V2 初期不强制复杂沙箱。

---

## 2. Non-goals

V2 初期不要求：

- 任意第三方不可信代码的强安全沙箱；
- 每个插件独立容器；
- WASM-only 插件模型；
- 动态下载任意 native binary 并无条件执行；
- 插件直接拥有 Server DataSource；
- 插件直接注册任意 Spring Bean 覆盖核心 Bean；
- 插件修改核心数据库 Schema。

若未来要支持真正不可信第三方插件，应单独设计 Process Isolation / WASM / Sandbox。

---

## 3. Plugin Package

插件包至少包含：

```text
plugin package
├── manifest
├── executable artifact
├── optional frontend assets
├── optional migrations for plugin-owned data
└── optional static resources
```

Manifest 是插件身份与兼容性的唯一入口，不允许通过扫描类名猜测插件能力。

---

## 4. Plugin Manifest

逻辑字段至少包括：

```text
plugin_id
name
version
publisher
plugin_api_version
minimum_server_version
maximum_server_version (optional)
entrypoint
capabilities
permissions
extension_points
configuration_schema_version
frontend_extensions
migration_version
signature / integrity metadata (optional by distribution channel)
```

### 4.1 `plugin_id`

必须稳定且全局唯一。

建议采用反向域名或组织命名：

```text
run.ikaros.plugin.bgmtv
run.ikaros.plugin.jellyfin
```

插件升级不得改变 `plugin_id`。

### 4.2 Version

至少区分：

- Plugin Package Version；
- Plugin API Version；
- Plugin Configuration Schema Version；
- Plugin-owned Data Migration Version。

不能用一个版本号同时承担所有兼容性判断。

---

## 5. Plugin API Lifecycle

Plugin API 分为：

```text
STABLE
EXPERIMENTAL
DEPRECATED
REMOVED
```

规则：

- Stable API 在同一 Major Plugin API Version 内必须优先保持向后兼容；
- Experimental API 可以更快演进，但插件必须显式声明使用；
- Deprecated API 必须提供替代路径和弃用窗口；
- Removed 只能出现在允许的 Major compatibility break 中。

Server 必须能够拒绝加载明显不兼容的插件，而不是运行后随机失败。

---

## 6. Extension Point Model

Extension Point 是插件能够注册能力的正式入口。

典型类型：

- Metadata Provider；
- Content Importer；
- Storage Provider；
- Notification Provider；
- AI Provider；
- Automation Trigger；
- Automation Action；
- Parser；
- Search Enricher；
- Renderer / Frontend Extension；
- Background Task Handler；
- External Identity Provider。

每个 Extension Point 必须定义：

- stable identifier；
- input/output contract；
- timeout / cancellation semantics；
- permission requirement；
- error model；
- concurrency model；
- lifecycle callback；
- compatibility version。

---

## 7. Plugin Permission Model

插件权限遵循：

> **Least Privilege + Explicit Grant**

插件 Manifest 声明需要的权限，但声明不等于自动获得。

例如：

```text
resource.read
resource.metadata.suggest
attachment.read
attachment.create
network.external
secret.reference.resolve
storage.provider.register
notification.send
```

安装时管理员应看到权限摘要。

高风险权限需要明显标记：

- 读取所有私有 Resource；
- 读取 Attachment 内容；
- 访问外部网络；
- Secret Reference resolve；
- 执行写 Command；
- 注册 Storage Provider；
- 处理 Secure Domain 数据。

### 7.1 明确禁止

插件默认不得：

- 访问任意 Repository；
- 注入 DataSource 后执行任意 SQL；
- 读取其他插件配置表；
- 获取 Root Key / KEK；
- 读取 Private Notes / Password Manager 明文；
- 绕过 ACL；
- 伪造管理员 Principal。

---

## 8. Principal and Actor

插件执行必须携带明确 Actor Context。

至少区分：

```text
User-triggered Plugin Action
System-triggered Plugin Action
Automation-triggered Plugin Action
Scheduled Plugin Action
```

插件本身不天然等于管理员。

当插件代表用户执行 Command：

```text
Effective Permission
= User Permission
∩ Plugin Granted Permission
∩ Target Command Policy
```

Automation / Scheduled 场景则使用对应 Service Principal 或 Rule Principal，并保留 Audit。

---

## 9. Plugin Lifecycle

状态建议：

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

安装流程：

```text
Package Receive
→ Integrity Check
→ Manifest Parse
→ Compatibility Check
→ Permission Review
→ Plugin-owned Migration Plan
→ Install
→ Register Extension Metadata
→ INSTALLED
```

安装完成不自动意味着 Enabled。

### 9.2 Enable

Enable 前必须检查：

- Server compatibility；
- Plugin API compatibility；
- migration state；
- required configuration；
- required secret references；
- permission grant；
- dependency availability。

### 9.3 Disable

Disable 必须：

- 停止新任务进入；
- 取消或等待可安全结束的运行任务；
- 注销 Extension；
- 保留插件数据和配置；
- 不自动删除插件生成的业务事实。

### 9.4 Uninstall

Uninstall 与 Disable 分离。

卸载时必须明确：

- 是否保留 Plugin-owned Data；
- 是否保留 Configuration；
- 是否保留 Secret References；
- 是否保留由插件创建的 Resource / Attachment；
- 是否存在 orphan cleanup plan。

插件创建的核心业务对象一旦进入目标 Domain，就属于目标 Domain，不应因为插件卸载自动删除。

---

## 10. Upgrade

Upgrade 流程：

```text
Compatibility Preflight
→ Stop New Invocations
→ Drain / Cancel Runtime Tasks
→ Backup Plugin State Metadata
→ Run Plugin-owned Migration
→ Activate New Version
→ Validate Extension Registration
→ Resume
```

升级失败时优先恢复到可运行旧版本或进入 Disabled/Failed，不允许留下“新代码 + 旧数据半迁移”的模糊状态。

---

## 11. Plugin-owned Data

插件可以拥有自己的持久化数据，但必须满足：

- 与核心 Domain Schema 分离；
- Migration 有独立 owner；
- 不直接修改核心表；
- 数据格式版本明确；
- 卸载保留策略明确。

建议采用独立 PostgreSQL Schema / namespace 或平台管理的 Plugin Data Store abstraction。

插件如果要改变 Resource、Attachment、Task 等核心业务状态，必须调用对应 Command。

---

## 12. Plugin Migration

插件 Migration 与 Core Migration 必须分离。

每次 migration 至少包含：

```text
plugin_id
migration_id
plugin_version
from_schema_version
to_schema_version
checksum
applied_at
status
```

Server 在插件启用前必须判断 Plugin-owned Schema 是否兼容。

禁止插件在 runtime 任意执行无法审计的 DDL。

---

## 13. Configuration

插件配置分为：

```text
Public Configuration
Sensitive Configuration Reference
Runtime Ephemeral State
```

普通配置可以由平台配置存储统一管理。

Secret 只保存 Reference：

```text
secret://...
```

插件只有在声明并获得对应权限后才能通过 Secret Capability 使用 Secret，且默认不应把 Secret 明文重新写入自己的配置。

---

## 14. Network Access

插件外部网络访问应视为明确 Capability。

平台应支持：

- domain / endpoint allow policy（未来可选）；
- timeout；
- proxy；
- request tracing；
- rate limit；
- credential reference；
- TLS validation。

插件不得通过自建无限制 HTTP Client 绕过平台策略成为隐蔽数据外传通道。

V2 初期若无法强制网络隔离，至少需要在权限模型、审计和 SDK 指南中明确声明。

---

## 15. Runtime Invocation

统一调用结构：

```text
Core / User / Automation
        ↓
Plugin Extension Registry
        ↓
Permission + Availability Check
        ↓
Invocation Context
        ↓
Plugin Extension
        ↓
Result / Error / Event
```

Invocation Context 至少携带：

- invocation_id；
- plugin_id / version；
- principal；
- correlation_id；
- causation_id；
- deadline / timeout；
- cancellation token；
- locale / timezone when needed。

---

## 16. Failure Isolation

插件调用失败不得默认导致 Server 进程不可用。

至少区分：

```text
Plugin Domain Error
Transient External Error
Timeout
Rate Limited
Plugin Bug
Plugin Incompatible
Plugin Disabled
```

平台应支持：

- timeout；
- bounded concurrency；
- retry only when safe；
- circuit breaker（适用时）；
- failure metrics；
- disable unhealthy plugin；
- task-level error reporting。

对于 Storage Provider 等关键插件，禁用前需要检查是否仍有 Blob Placement 依赖该 Provider。

---

## 17. Background Task Integration

长时间插件操作必须进入 Background Task Runtime。

例如：

- 全量元数据刷新；
- 大型导入；
- Storage migration；
- Search enrichment；
- AI batch processing。

插件 Task Handler 不拥有 Background Task 表，只实现注册的执行契约。

Task 必须保留 plugin_id + plugin_version，以便升级后诊断历史任务。

---

## 18. Event Integration

插件可以：

- 订阅明确允许的 Event；
- 发布插件自有 Event；
- 通过目标 Domain Command 改变核心状态。

插件不得发布假冒核心 Domain 的事实事件。

例如插件不能自行发布：

```text
resource.deleted
```

它只能调用 Resource Delete/Purge Command，由 Resource Owner 发布正式事件。

---

## 19. Frontend Extension

插件可选注册前端扩展，但必须使用稳定 UI Extension Contract。

建议支持：

- navigation entry；
- detail tab；
- action contribution；
- settings page；
- dashboard card；
- provider-specific configuration surface。

前端扩展必须声明：

- extension id；
- target slot；
- permission；
- route；
- plugin source identity；
- compatibility version。

插件 UI 不能伪装成 Core 页面；用户应能识别来源。

---

## 20. SDK

官方 Plugin SDK 应提供：

- Manifest model；
- Extension interfaces；
- Capability client；
- Command client；
- Event subscription API；
- Background Task API；
- Configuration API；
- Secret Reference API；
- observability context；
- test harness。

SDK 不应暴露：

- Server Entity；
- Repository；
- DataSource；
- internal Spring ApplicationContext；
- internal encryption key material。

---

## 21. Compatibility Test Kit

官方应提供插件兼容测试：

```text
Manifest Validation
Plugin API Compatibility
Permission Declaration
Configuration Migration
Plugin Data Migration
Enable / Disable
Upgrade
Event Consumer Idempotency
Timeout / Cancellation
No Internal API Dependency
```

插件发布前建议运行该 Test Kit。

---

## 22. Distribution and Integrity

V2 初期允许手动安装插件包。

长期可以支持 Plugin Registry / Marketplace，但安装前必须至少校验：

- package integrity；
- manifest；
- publisher metadata；
- version compatibility。

未来若引入签名，应支持 publisher signature / trusted source policy。

签名不等于自动授予高风险权限。

---

## 23. Observability

每个插件至少暴露：

- version；
- state；
- invocation count；
- failure count；
- timeout count；
- last error summary；
- active task count；
- extension registrations；
- migration state。

日志必须携带：

```text
plugin_id
plugin_version
invocation_id
correlation_id
```

Secret 和高敏感 payload 禁止写日志。

---

## 24. Security Review Checklist

插件设计 Review 必须检查：

1. 是否声明最小权限；
2. 是否绕过目标 Domain Command；
3. 是否直接访问数据库；
4. 是否读取不必要的 Attachment 内容；
5. 是否可能接触 Secure Domain 明文；
6. Secret 是否仅通过 Reference；
7. 外部网络访问是否明确；
8. Event 是否冒充 Core Producer；
9. 卸载是否会删除用户核心数据；
10. 升级失败是否可恢复；
11. Runtime failure 是否会拖垮 Server；
12. 前端扩展是否清楚标识来源。

---

## 25. V2 Initial Scope

首批 Plugin Runtime 只需要支持：

- Java/JVM Plugin；
- Manifest；
- Stable Plugin API v2 baseline；
- install / enable / disable / uninstall；
- permission declaration；
- configuration + Secret Reference；
- Metadata Provider；
- Importer；
- Storage Provider；
- Automation action / trigger；
- Background Task Handler；
- basic frontend extension；
- migration + compatibility check。

不要求首发提供完整 Marketplace、强隔离 Sandbox 或跨进程插件运行。

---

## 26. Long-term Evolution

只有当出现真实需求时，再考虑：

- Out-of-process Plugin Host；
- WASM Plugin；
- capability-secured network proxy；
- signed marketplace；
- resource quotas；
- per-plugin CPU / memory isolation；
- hot reload；
- multi-version plugin side-by-side。

这些能力不得反向破坏当前 Stable Plugin API 的领域契约。
