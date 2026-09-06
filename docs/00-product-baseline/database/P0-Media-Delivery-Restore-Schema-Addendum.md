# Ikaros V2 P0 Media Delivery / Restore Schema Addendum

| 项目 | 内容 |
|---|---|
| 文档名称 | P0 Media Delivery / Restore Schema Addendum |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.2 |
| 基线日期 | 2026-09-06 |
| 状态 | Implementation-aligned Contract |
| 基线 | `P0-Database-Schema-Design.md` |
| 领域设计 | `../../02-domain-capabilities/Media-Delivery-CDN-Archive-Restore-Design.md` |

> 本文档把 Media Delivery / Restore 的持久化契约与当前 `run.ikaros.storage` Entity、Repository 以及 Storage Owner 模块的 `storage/src/main/resources/db/migration/` 对齐。
>
> **重要**：早期设计使用 `storage.delivery_provider`、`storage.delivery_binding` 等逻辑命名。当前工程采用单 PostgreSQL 数据库中的扁平物理表名，例如 `media_delivery_provider`、`media_delivery_binding`、`media_delivery_lease`、`storage_restore_request`。Storage 仍是这些表的领域 Owner，但文档不得再把逻辑 Schema 名误写成当前物理表名。

---

## 1. 当前物理表基线

与 Delivery / Restore 直接相关的当前表包括：

```text
media_delivery_provider
media_delivery_binding
media_delivery_grant
media_delivery_lease
storage_restore_request
storage_restore_request_item
storage_restore_operation
storage_restore_budget
```

相关基础表仍包括：

```text
attachment
blob
blob_placement
storage_provider
background_task
platform_user
```

所有结构变更必须通过 Storage Owner 模块的 `storage/src/main/resources/db/migration/V...__....sql` 演进，并由 `application` 聚合运行时 classpath 后交给 `r2dbc-migrate` 执行。已经进入共享环境的 migration 不原地修改；需要修正时追加新 migration。

---

## 2. `media_delivery_provider`

Delivery Provider 表达“如何交付已经可读的 Blob”，不拥有 Blob Placement。

当前字段：

| Column | Type / Mapping | Null | 当前语义 |
|---|---|---:|---|
| `id` | uuid | NO | `uuid_v7()` 默认 |
| `provider_key` | varchar(128) | NO | 稳定、唯一 Provider Key |
| `provider_type` | varchar(32) | NO | `DIRECT / CDN / SERVER_PROXY` |
| `display_name` | varchar(256) | NO | 管理显示名 |
| `credential_ref` | varchar(512) | YES | Credential / Secret Reference |
| `config` | jsonb | NO | 非明文 Secret 的 Provider 配置 |
| `capabilities` | jsonb | NO | 声明/探测能力 |
| `grant_revocation_mode` | varchar(48) | NO | Grant 撤销语义 |
| `signing_key_version` | bigint | NO | 当前签名 Key generation |
| `health_status` | varchar(32) | NO | `UNKNOWN / HEALTHY / DEGRADED / UNHEALTHY` |
| `enabled` | boolean | NO | 是否参与路由 |
| `idempotency_key` | varchar(256) | YES | Provider 创建幂等键 |
| `created_at` | timestamptz | NO | 创建时间 |
| `updated_at` | timestamptz | NO | 更新时间 |
| `version` | bigint | NO | Spring Data optimistic version |

当前 DB 约束包括：

```text
UNIQUE(provider_key)
provider_type IN ('DIRECT','CDN','SERVER_PROXY')
grant_revocation_mode IN (
  'IMMEDIATE',
  'KEY_VERSION_BOUND',
  'TTL_BOUNDED',
  'NOT_REVOCABLE_BEFORE_EXPIRY'
)
signing_key_version >= 1
health_status IN ('UNKNOWN','HEALTHY','DEGRADED','UNHEALTHY')
```

`idempotency_key` 通过 partial unique index 在非 null 时唯一。

### 2.1 Provider 配置归属

Provider 自己拥有：

- `provider_type`；
- `credential_ref`；
- `config.endpoint` 等交付配置；
- `capabilities`；
- `grant_revocation_mode`；
- `signing_key_version`；
- `health_status`；
- `enabled`。

这些字段不得复制到 Binding 形成第二套交付策略。

### 2.2 Endpoint 语义

当前实现读取 `config.endpoint`：

- `CDN`：必须是有 scheme + host 的有效绝对 URI；用于构建基于真实 object path 的 CDN 读取合同；
- `SERVER_PROXY`：可选；有效时作为 Ikaros attachment content path 的外部基址；未配置时返回相对 URL；
- `DIRECT`：不使用 Delivery Provider Endpoint 重写 Storage Provider 已返回的签名 URL。

---

## 3. `media_delivery_binding`

一个 Storage Provider 可以绑定多个 Delivery Provider。Binding 是**路由关系**，不是 Delivery Provider 的第二份配置。

当前字段：

| Column | Type | Null | 当前语义 |
|---|---|---:|---|
| `id` | uuid | NO | `uuid_v7()` 默认 |
| `storage_provider_id` | uuid | NO | 绑定的 Storage Provider |
| `delivery_provider_key` | varchar(128) | NO | 通过稳定 Key 关联 Delivery Provider |
| `priority` | integer | NO | 越小越优先，默认 100 |
| `enabled` | boolean | NO | Binding 是否参与候选 |
| `cache_key_policy` | varchar(32) | NO | `CONTENT_IDENTITY / FULL_REQUEST / NO_CACHE` |
| `range_policy` | varchar(32) | NO | `PASSTHROUGH / FIXED_CHUNK / UNSUPPORTED` |
| `fallback_participation` | boolean | NO | 是否参与后续 Placement fallback |
| `created_at` | timestamptz | NO | 创建时间 |
| `updated_at` | timestamptz | NO | 更新时间 |
| `version` | bigint | NO | optimistic version |

当前约束：

```text
FK storage_provider_id -> storage_provider(id)
UNIQUE(storage_provider_id, delivery_provider_key)
cache_key_policy IN ('CONTENT_IDENTITY','FULL_REQUEST','NO_CACHE')
range_policy IN ('PASSTHROUGH','FIXED_CHUNK','UNSUPPORTED')
```

当前路由索引：

```text
idx_media_delivery_binding_resolution(storage_provider_id, enabled, priority)
```

### 3.1 明确删除的旧字段

当前 migration 已明确执行：

```sql
alter table media_delivery_binding
    drop column if exists origin_type,
    drop column if exists auth_mode;
```

因此：

- `origin_type` 不再属于 Binding；交付类型由 `media_delivery_provider.provider_type` 决定；
- `auth_mode` 不再属于 Binding；认证/签名/Endpoint 等交付细节由 Provider 配置、Storage Provider 能力和 Grant Contract 协作完成；
- 早期设计中的 `origin_auth_ref`、`fallback_policy` JSON、`delivery_provider_id` 不是当前 Binding Entity 的字段，不得作为实现验收依据。

### 3.2 Binding API 当前请求模型

当前写请求对应：

```text
deliveryProviderKey
priority
enabled
cacheKeyPolicy
rangePolicy
fallbackParticipation
```

文档和 Console 表单应使用该模型，不再要求 `origin_type / auth_mode / origin_auth_ref`。

---

## 4. Provider / Binding 选择不变量

Delivery 候选必须建立在可读 Blob Placement 上。

必须排除：

```text
Storage Provider status = DISABLED / FAILED
Binding enabled = false
Delivery Provider enabled = false
Delivery Provider health_status = UNHEALTHY
```

`UNKNOWN` 代表尚未获得有效健康结论，不等于 `HEALTHY`；当前实现仅将 `UNHEALTHY` 作为硬排除状态。

### 4.1 Preview 选择

Attachment Preview 会收集可用候选：

```text
active Blob Placement
 -> readable Storage Provider
 -> enabled Binding
 -> enabled and not-UNHEALTHY Delivery Provider
```

选择规则：

- 请求 `delivery_provider={providerKey}` 且候选中存在时，选择该 Provider；
- 未请求、空值或 Key 不在候选中时，选择 `priority` 最小的 Binding；
- 响应可返回候选 Provider 列表，但只为 selected Provider 生成 URL。

### 4.2 Lease 选择

Delivery Lease 将最终选择持久化到 `binding_id`。对非首个 readable Placement，Binding 只有 `fallback_participation = true` 才参与 fallback。

---

## 5. `media_delivery_lease`

Delivery Lease 表示当前短期数据面保护，不是播放历史。

当前 Entity 字段：

| Column | Type | Null | 当前语义 |
|---|---|---:|---|
| `id` | uuid | NO | Lease ID |
| `attachment_id` | uuid | NO | Attachment |
| `blob_id` | uuid | NO | Blob |
| `owner_id` | uuid | NO | 当前授权用户 |
| `grant_id` | uuid | YES | 对应 Delivery Grant |
| `binding_id` | uuid | YES | 实际选择的 Binding |
| `selection_epoch` | bigint | NO | 选择代数，当前初始 1 |
| `selected_at` | timestamptz | YES | 选择时间 |
| `selection_reason` | varchar(64) | NO | 当前 `PRIMARY / FAILOVER` 语义 |
| `fallback_index` | integer | NO | Placement fallback 序号 |
| `health_snapshot_version` | varchar(128) | YES | 选择时健康/Provider 快照标识 |
| `lease_expires_at` | timestamptz | NO | 有限 TTL |
| `released_at` | timestamptz | YES | 主动释放 |
| `last_heartbeat_at` | timestamptz | NO | 最近续租/心跳 |
| `created_at` | timestamptz | NO | 创建时间 |
| `version` | bigint | NO | optimistic version |

基础 FK / index 由 migration 建立，包括 Attachment、Blob、Owner、Grant 与 `binding_id -> media_delivery_binding(id)`。

Active Lease：

```text
released_at IS NULL
AND lease_expires_at > now()
```

当前 TTL 由应用层限制在 1..1800 秒。GC、不可逆清理、自动 Demotion 等操作必须尊重 Active Lease 保护。

---

## 6. Delivery URL 不作为持久真相

签名 URL、CDN URL、Server Proxy URL 都是短期 Contract 结果，不是 Attachment / Blob 永久字段。

当前生成规则：

### DIRECT

```text
binding.storage_provider_id
 -> Storage Provider
 -> readable placement.object_key
 -> StorageObjectProvider.createReadIntent(...)
```

直接使用 Storage Provider 返回的 URL / method / expiry，不覆盖其签名 Host。

### CDN

```text
delivery_provider.config.endpoint
 + binding.storage_provider_id
 + placement.object_key
 -> StorageObjectProvider.createReadIntent(..., cdnEndpoint)
```

Endpoint 缺失/非法时失败，不根据 Provider Key 或 Bucket 名构造虚假 CDN URL。

### SERVER_PROXY

```text
[delivery_provider.config.endpoint]
 + /api/attachments/{attachmentId}/content?delivery_grant={token}
```

Endpoint 可省略，省略时合同允许相对地址。

不得把完整 URL / Token 写入普通审计、Analytics 或 Attachment 永久列。

---

## 7. `storage_restore_request`

Restore Request 表示业务 Restore 意图。当前 Entity 以物理表 `storage_restore_request` 持久化，主要字段：

```text
id
actor_id
scope
scope_id
status
total_items
completed_items
total_bytes
error_summary
idempotency_key
background_task_id
budget_decision
selected_attachment_ids
created_at / updated_at / version
```

Restore Request 的 Storage scope 统一为 `ATTACHMENT_SET`；`scope_id` 必须允许为空，并在该 scope 下固定为 `NULL`。Attachment 集合由 Request Item / Attachment 引用表达，不使用 Episode、Season 或其他业务领域 ID 作为 Storage scope。`background_task_id` 将外部 Provider 恢复操作放入可靠 Background Task，而不是在 HTTP/数据库长事务中同步等待。

新的批量 Command 要求 `idempotency_key` 非空。幂等唯一性至少按 `actor_id + idempotency_key` 保证；相同 Key 对应不同 Attachment 集合或恢复参数时必须返回冲突，而不是复用错误请求。

---

## 8. `storage_restore_operation`

具体 Provider Restore 操作持久化在 `storage_restore_operation`，当前 Entity 主要字段：

```text
id
placement_id
provider_restore_class
restore_generation
operation_key
status
background_task_id
provider_operation_id
restore_expires_at
error_summary
created_at / updated_at / version
```

`operation_key` 用于相同语义 Restore 的幂等/去重。`READY_TEMPORARILY` Placement 只有对应 Operation 仍有效且 `restore_expires_at` 在未来时，才可进入 Delivery 选择。

---

## 9. `storage_restore_request_item`

Request 与具体 Placement / Operation 的关联使用 `storage_restore_request_item`，当前 Entity 字段：

```text
id
request_id
placement_id
operation_id
status
error_summary
created_at / updated_at / version
```

多个 Request 可以协调到共享/去重后的 Restore Operation；业务层不得因为不同入口重复触发等价 Provider Restore。

---

## 10. `storage_restore_budget`

当前 Restore Budget 表为 `storage_restore_budget`，Entity 字段：

```text
id
max_bytes_per_request
max_items_per_request
max_concurrent_operations
max_concurrent_bytes
daily_requested_bytes
daily_provider_restore_bytes
over_budget_action
updated_at
version
```

Budget 只表达实例策略和保护阈值，不硬编码云厂商实时价格。

---

## 11. Transaction Boundary

### Issue Preview / Delivery Contract

至少保证：

```text
permission / ownership check
 -> resolve Blob + readable Placement
 -> resolve Storage Provider
 -> resolve enabled Binding
 -> resolve enabled/not-UNHEALTHY Delivery Provider
 -> issue short-lived Grant
 -> create Lease with binding_id
 -> build Provider-type-specific contract
```

合同构建失败时，已签发 Grant 应尽力撤销，不能把失败合同当成成功交付状态。

### Request Restore

至少保证：

```text
validate permission
 -> resolve scope
 -> apply budget decision
 -> persist request / items / operation intent
 -> enqueue Background Task
```

实际 Provider API 调用不得在持有数据库事务锁的长事务中执行。

---

## 12. Migration Contract

当前实现的关键 Delivery migrations 包括：

```text
V202609034500__DDL_MEDIA_DELIVERY_LEASE_P0.sql
V202609034700__DDL_MEDIA_DELIVERY_BINDING_P0.sql
V202609034900__DDL_MEDIA_DELIVERY_PROVIDER_P0.sql
V202609035000__DDL_MEDIA_DELIVERY_SELECTION_P0.sql
V202609035300__DDL_MEDIA_DELIVERY_PROVIDER_IDEMPOTENCY_P0.sql
V202609060600__DDL_MEDIA_DELIVERY_BINDING_REMOVE_UNUSED_COLUMNS.sql
```

工程规则：

1. migration 由 Storage Owner 模块持有，路径为 `storage/src/main/resources/db/migration/`；
2. 由 `application` 聚合各 Owner 模块后，在应用启动阶段统一交给 `r2dbc-migrate` 执行；
3. 不引入 Flyway/JDBC 第二数据库访问栈；
4. 已发布 migration 不原地编辑；
5. Entity / Repository / API 与 migration 冲突时，必须先明确当前代码与实际数据库状态，再通过追加 migration + 文档更新收敛；
6. Provider/Binding 相关文档不得继续引用已删除的 `origin_type / auth_mode` 或不存在的 `delivery_provider_id / origin_auth_ref / fallback_policy` 作为当前字段。

---

## 13. P0 Schema 验收

至少验证：

- `provider_key` 唯一；
- Provider Type 非法值被拒绝；
- 相同 Storage Provider + Delivery Provider Key 的重复 Binding 被拒绝；
- Binding `cache_key_policy / range_policy` 只接受当前枚举值；
- `origin_type / auth_mode` 不再是当前 Binding 字段；
- Delivery Provider 类型/Endpoint 不从 Binding 推断；
- Lease 持久化 `binding_id` 与选择信息；
- Preview 默认按 Binding priority 选择，可显式选择可用 Provider；
- disabled / `UNHEALTHY` Provider 不参与候选；
- CDN Endpoint 缺失时不能构造虚假 URL；
- DIRECT 保留 Storage Provider 的签名 URL Host；
- Active Lease 能保护正在交付的 Blob；
- Restore 外部调用由 Background Task 执行；
- 所有变更均可通过 `r2dbc-migrate` 从受支持历史版本确定性升级。
