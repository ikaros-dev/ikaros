# Ikaros V2 P0 Media Delivery / Restore Schema Addendum

| 项目 | 内容 |
|---|---|
| 文档名称 | P0 Media Delivery / Restore Schema Addendum |
| 适用版本 | Ikaros V2 |
| 状态 | Draft / Implementation Contract |
| 基线 | `P0-Database-Schema-Design.md` |
| 领域设计 | `../Media-Delivery-CDN-Archive-Restore-Design.md` |
| 工程补充 | `../Media-Delivery-CDN-Archive-Restore-Engineering-Addendum.md` |

> 本文档是 `P0-Database-Schema-Design.md` 的规范性扩展。它不修改既有 `storage.blob`、`storage.attachment`、`storage.storage_provider`、`storage.blob_placement`、`operations.background_task` 的语义，而是在其上补齐 Delivery Provider、Restore Request / Operation、Delivery Lease 与 Restore Budget 的 P0 持久化契约。

---

## 1. Schema Ownership

新增表仍由 Storage 子系统拥有，使用现有 `storage` Schema：

```text
storage.delivery_provider
storage.delivery_binding
storage.restore_request
storage.restore_request_item
storage.restore_operation
storage.delivery_lease
storage.restore_budget_policy
```

Background Task 继续由 `operations.background_task` / `operations.background_task_attempt` 拥有。

Media 子系统只能通过 Storage Application Contract 消费 Availability / Restore 状态，不直接 UPDATE 这些表。

---

## 2. `storage.delivery_provider`

Delivery Provider 表示“如何把已经可读的 Blob 交付给客户端”，不拥有 Blob Placement。

| Column | Type | Null | Contract |
|---|---|---:|---|
| `id` | uuid | NO | UUIDv7 |
| `provider_key` | text | NO | stable instance-local key |
| `provider_type` | text | NO | `DIRECT / CDN / SERVER_PROXY` |
| `display_name` | text | NO | admin display |
| `credential_ref` | text | YES | only `secret://` or equivalent |
| `config` | jsonb | NO | non-secret provider config |
| `capabilities` | jsonb | NO | probed / declared capabilities |
| `grant_revocation_mode` | text | NO | revocation contract |
| `signing_key_version` | bigint | NO | current key generation |
| `enabled` | boolean | NO | routing eligibility |
| `health_status` | text | NO | `UNKNOWN / HEALTHY / DEGRADED / UNHEALTHY` |
| `version` | bigint | NO | optimistic concurrency |
| `created_at` | timestamptz | NO | |
| `updated_at` | timestamptz | NO | |

Constraints：

```text
UNIQUE(provider_key)
CHECK provider_type in ('DIRECT','CDN','SERVER_PROXY')
CHECK grant_revocation_mode in (
  'IMMEDIATE',
  'KEY_VERSION_BOUND',
  'TTL_BOUNDED',
  'NOT_REVOCABLE_BEFORE_EXPIRY'
)
CHECK health_status in ('UNKNOWN','HEALTHY','DEGRADED','UNHEALTHY')
CHECK signing_key_version >= 1
CHECK version >= 1
```

安全规则：

- `credential_ref` 禁止保存明文 Secret；
- `config` 禁止保存完整 CDN token、签名 key 或 Storage Credential；
- `capabilities` 是能力描述，不是业务授权真相。

Indexes：

```text
delivery_provider_enabled_health_idx(enabled, health_status, provider_type)
```

---

## 3. `storage.delivery_binding`

一个 Storage Provider 可以绑定多个 Delivery Provider。

| Column | Type | Null | Contract |
|---|---|---:|---|
| `id` | uuid | NO | UUIDv7 |
| `storage_provider_id` | uuid | NO | origin provider |
| `delivery_provider_id` | uuid | NO | delivery path |
| `priority` | integer | NO | smaller first |
| `enabled` | boolean | NO | |
| `origin_auth_ref` | text | YES | Secret Reference |
| `cache_key_policy` | jsonb | NO | normalized cache identity policy |
| `range_policy` | jsonb | NO | Range capability / limits |
| `fallback_policy` | jsonb | NO | allowed next path classes |
| `version` | bigint | NO | optimistic concurrency |
| `created_at` | timestamptz | NO | |
| `updated_at` | timestamptz | NO | |

Constraints：

```text
UNIQUE(storage_provider_id, delivery_provider_id)
FK storage_provider_id -> storage.storage_provider(id) ON DELETE RESTRICT
FK delivery_provider_id -> storage.delivery_provider(id) ON DELETE RESTRICT
CHECK priority >= 0
CHECK version >= 1
```

`cache_key_policy` 至少能够表达：

```json
{
  "exclude_auth_query": true,
  "auth_query_names": ["token", "signature", "expires"],
  "query_allowlist": [],
  "vary_header_allowlist": []
}
```

任何把授权 Token 默认纳入 Cache Identity 的配置必须显式标记并在 Console 中产生性能风险提示。

Indexes：

```text
delivery_binding_origin_priority_idx(storage_provider_id, enabled, priority, id)
delivery_binding_delivery_idx(delivery_provider_id, enabled)
```

---

## 4. `storage.restore_request`

Restore Request 表示一次用户 / 系统业务意图，不等于具体 Provider Restore API 调用。

| Column | Type | Null | Contract |
|---|---|---:|---|
| `id` | uuid | NO | UUIDv7 |
| `actor_type` | text | NO | user / system / automation |
| `actor_id` | uuid | YES | |
| `scope_type` | text | NO | `ATTACHMENT / EPISODE / SEASON / RESOURCE_SET` |
| `scope_id` | uuid | NO | opaque cross-domain contract reference |
| `requested_restore_class` | text | YES | provider-neutral requested class |
| `status` | text | NO | aggregate request state |
| `idempotency_key` | text | YES | API idempotency |
| `item_count` | integer | NO | resolved item count |
| `total_bytes` | bigint | NO | logical requested bytes |
| `ready_items` | integer | NO | |
| `failed_items` | integer | NO | |
| `budget_decision` | text | NO | applied guard result |
| `correlation_id` | uuid | YES | |
| `error_summary` | text | YES | safe summary only |
| `created_at` | timestamptz | NO | |
| `updated_at` | timestamptz | NO | |
| `completed_at` | timestamptz | YES | |

Constraints：

```text
CHECK scope_type in ('ATTACHMENT','EPISODE','SEASON','RESOURCE_SET')
CHECK status in (
  'PENDING','ACTIVE','PARTIAL','SUCCEEDED','FAILED',
  'CANCEL_REQUESTED','CANCELLED'
)
CHECK item_count >= 0
CHECK total_bytes >= 0
CHECK ready_items >= 0 AND ready_items <= item_count
CHECK failed_items >= 0 AND failed_items <= item_count
CHECK budget_decision in ('ACCEPTED','PARTIAL','CONFIRMED','QUEUED','REJECTED')
UNIQUE(actor_type, actor_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL
```

跨 Schema `scope_id` P0 不建立强 FK；Application Command 必须在解析 Scope 时验证引用。

Indexes：

```text
restore_request_actor_created_idx(actor_type, actor_id, created_at, id)
restore_request_scope_idx(scope_type, scope_id, created_at, id)
restore_request_status_idx(status, updated_at, id)
```

---

## 5. `storage.restore_operation`

Restore Operation 是对具体归档 Placement 的去重后 Provider 操作。

| Column | Type | Null | Contract |
|---|---|---:|---|
| `id` | uuid | NO | UUIDv7 |
| `placement_id` | uuid | NO | concrete archive placement |
| `operation_key` | text | NO | deterministic idempotency identity |
| `restore_class` | text | NO | provider-normalized restore mode |
| `restore_generation` | bigint | NO | monotonic generation per placement |
| `status` | text | NO | operation state |
| `background_task_id` | uuid | YES | execution identity |
| `provider_request_ref` | text | YES | protected non-secret provider request ref |
| `size_bytes` | bigint | NO | actual provider restore bytes basis |
| `restore_expires_at` | timestamptz | YES | temporary readable copy expiry |
| `started_at` | timestamptz | YES | |
| `completed_at` | timestamptz | YES | |
| `created_at` | timestamptz | NO | |
| `updated_at` | timestamptz | NO | |

Constraints：

```text
UNIQUE(operation_key)
UNIQUE(placement_id, restore_generation)
FK placement_id -> storage.blob_placement(id) ON DELETE RESTRICT
CHECK restore_generation >= 1
CHECK size_bytes >= 0
CHECK status in (
  'PENDING','RUNNING','READY_TEMPORARILY','SUCCEEDED','FAILED',
  'CANCEL_REQUESTED','CANCELLED','EXPIRED'
)
```

`background_task_id` 是跨 Operations Schema 的稳定 Contract Reference；P0 可不建立跨 Schema FK，但 Application 层必须保持引用完整性。

核心不变量：

> 同一 Placement 的语义等价 Restore 只能复用同一个 active `operation_key`；不同业务 Request 不得造成重复 Provider Restore。

Indexes：

```text
restore_operation_placement_status_idx(placement_id, status, updated_at, id)
restore_operation_task_idx(background_task_id) WHERE background_task_id IS NOT NULL
restore_operation_expiry_idx(restore_expires_at, id)
  WHERE status = 'READY_TEMPORARILY'
```

---

## 6. `storage.restore_request_item`

连接业务 Restore Request 与去重后的 Restore Operation。

| Column | Type | Null | Contract |
|---|---|---:|---|
| `request_id` | uuid | NO | |
| `placement_id` | uuid | NO | resolved placement |
| `operation_id` | uuid | YES | null when immediately ready / rejected |
| `attachment_id` | uuid | YES | business traceability |
| `item_status` | text | NO | |
| `size_bytes` | bigint | NO | |
| `error_code` | text | YES | stable safe code |
| `created_at` | timestamptz | NO | |
| `updated_at` | timestamptz | NO | |

Constraints：

```text
PRIMARY KEY(request_id, placement_id)
FK request_id -> storage.restore_request(id) ON DELETE CASCADE
FK placement_id -> storage.blob_placement(id) ON DELETE RESTRICT
FK operation_id -> storage.restore_operation(id) ON DELETE RESTRICT
FK attachment_id -> storage.attachment(id) ON DELETE RESTRICT
CHECK item_status in (
  'PENDING','ATTACHED_TO_EXISTING','RESTORING','READY',
  'READY_TEMPORARILY','FAILED','CANCELLED','REJECTED'
)
CHECK size_bytes >= 0
```

一个 `operation_id` 可以被多个 Request Item 引用，这是 Restore 合并的正式数据模型。

Indexes：

```text
restore_request_item_operation_idx(operation_id, request_id)
restore_request_item_attachment_idx(attachment_id, request_id)
```

---

## 7. `storage.delivery_lease`

Delivery Lease 是当前数据面活动的短期保护租约，不是用户观看历史。

| Column | Type | Null | Contract |
|---|---|---:|---|
| `id` | uuid | NO | UUIDv7 |
| `attachment_id` | uuid | NO | |
| `blob_id` | uuid | NO | |
| `placement_id` | uuid | YES | selected readable placement when known |
| `actor_type` | text | NO | |
| `actor_id` | uuid | YES | |
| `purpose` | text | NO | `PLAYBACK / DOWNLOAD` |
| `delivery_provider_id` | uuid | YES | selected path |
| `grant_key_version` | bigint | YES | token signing generation |
| `expires_at` | timestamptz | NO | finite TTL |
| `last_renewed_at` | timestamptz | NO | |
| `released_at` | timestamptz | YES | explicit release |
| `created_at` | timestamptz | NO | |

Constraints：

```text
FK attachment_id -> storage.attachment(id) ON DELETE RESTRICT
FK blob_id -> storage.blob(id) ON DELETE RESTRICT
FK placement_id -> storage.blob_placement(id) ON DELETE RESTRICT
FK delivery_provider_id -> storage.delivery_provider(id) ON DELETE RESTRICT
CHECK purpose in ('PLAYBACK','DOWNLOAD')
CHECK expires_at > created_at
CHECK grant_key_version IS NULL OR grant_key_version >= 1
```

Active Lease 定义：

```text
released_at IS NULL AND expires_at > now()
```

GC、自动 Demotion、Restore 临时副本清理、Provider Drain 在执行不可逆动作前必须重新检查 Active Lease。

Indexes：

```text
delivery_lease_blob_active_idx(blob_id, expires_at)
  WHERE released_at IS NULL

delivery_lease_placement_active_idx(placement_id, expires_at)
  WHERE released_at IS NULL

delivery_lease_actor_idx(actor_type, actor_id, expires_at)
```

---

## 8. `storage.restore_budget_policy`

P0 使用结构化 Budget Policy，不把云厂商实时价格写入核心 Schema。

| Column | Type | Null | Contract |
|---|---|---:|---|
| `id` | uuid | NO | UUIDv7 |
| `scope_type` | text | NO | `INSTANCE / PROVIDER / STORAGE_POLICY` |
| `scope_id` | uuid | YES | null for INSTANCE |
| `max_bytes_per_request` | bigint | YES | |
| `max_items_per_request` | integer | YES | |
| `max_concurrent_operations` | integer | YES | |
| `max_concurrent_bytes` | bigint | YES | |
| `daily_requested_bytes` | bigint | YES | |
| `daily_provider_restore_bytes` | bigint | YES | |
| `auto_prefetch_max_bytes_per_trigger` | bigint | YES | |
| `auto_prefetch_max_items_per_trigger` | integer | YES | |
| `overflow_action` | text | NO | |
| `enabled` | boolean | NO | |
| `version` | bigint | NO | |
| `created_at` | timestamptz | NO | |
| `updated_at` | timestamptz | NO | |

Constraints：

```text
CHECK scope_type in ('INSTANCE','PROVIDER','STORAGE_POLICY')
CHECK (
  (scope_type = 'INSTANCE' AND scope_id IS NULL)
  OR
  (scope_type <> 'INSTANCE' AND scope_id IS NOT NULL)
)
CHECK overflow_action in ('REJECT','REQUIRE_CONFIRMATION','QUEUE_AFTER_BUDGET_RESET','PARTIAL_ACCEPT')
CHECK version >= 1
CHECK max_bytes_per_request IS NULL OR max_bytes_per_request >= 0
CHECK max_items_per_request IS NULL OR max_items_per_request >= 0
CHECK max_concurrent_operations IS NULL OR max_concurrent_operations >= 0
CHECK max_concurrent_bytes IS NULL OR max_concurrent_bytes >= 0
CHECK daily_requested_bytes IS NULL OR daily_requested_bytes >= 0
CHECK daily_provider_restore_bytes IS NULL OR daily_provider_restore_bytes >= 0
```

唯一性：

```text
UNIQUE(scope_type, scope_id)
```

Instance scope 的 null 唯一语义需要通过 partial unique index 保证：

```text
restore_budget_instance_uk(scope_type)
  WHERE scope_type = 'INSTANCE'
```

---

## 9. 不持久化完整 Delivery Grant URL

P0 不新增 `delivery_grant` 明文 URL 表。

原因：

- URL 通常含短期 Token / Signature；
- 高频播放会制造大量短命记录；
- 完整 URL 不应进入普通审计 / Analytics。

如果需要审计，只保存：

```text
grant_id
attachment_id
delivery_provider_id
key_version
expires_at
actor reference
result code
```

不得保存完整签名 URL 或原始 Token。

---

## 10. Transaction Boundaries

### Request Restore

一个事务内至少完成：

```text
validate permission
resolve scope snapshot
apply budget decision
insert restore_request
insert request_items
attach existing operation OR create operation intent
append Outbox Event
```

Provider API 调用不得放在持有数据库事务锁的长事务中；实际外部调用由 Background Task 执行。

### Issue Delivery Grant

至少保证：

```text
permission re-check
availability resolution
delivery provider resolution
lease create/renew
issue short-lived grant
```

如果签名 Provider 调用失败，Lease 不得被错误续租为长时间 Active。

---

## 11. Migration Order

建议在既有 P0 Storage Migration 之后增加：

```text
1. storage.delivery_provider
2. storage.delivery_binding
3. storage.restore_budget_policy
4. storage.restore_request
5. storage.restore_operation
6. storage.restore_request_item
7. storage.delivery_lease
8. indexes / partial unique constraints
9. deterministic permission seed expansion
```

所有 DDL 继续进入 versioned migration；不允许生产启动时动态建表。
