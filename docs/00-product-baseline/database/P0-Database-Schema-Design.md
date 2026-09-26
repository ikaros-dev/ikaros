# Ikaros V2 P0 Database Schema Design

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 P0 Database Schema Design |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.2 |
| 状态 | Draft / Implementation Contract |
| 产品基线 | `../Product-Requirements-Document.md` |
| 系统基线 | `../System-Overview-Design.md` |
| 数据库基线 | `../Database-Overview-Design.md` |
| 模块边界基线 | `../Module-Package-Ownership-Design.md` |

> 本文档把 V2 已经确定的数据库原则向下收敛为首批可以直接映射到 `r2dbc-migrate` Migration、Repository 与 Integration Test 的 P0 Schema Contract。
>
> 本文档开始锁定 P0 的物理 Schema、Table、Column、Constraint 与关键 Index 名称。实现若需要偏离本文档，必须先修改本文档或通过明确 ADR 记录偏离原因，不能由 Repository 实现静默产生另一套数据库事实。
>
> Identity 登录认证采用无状态 JWT。数据库不持久化 Login Session / Security Session、Access Token / Refresh Token Digest、单设备登录状态或 `last_seen` 会话记录；用户级提前失效旧 JWT 通过 `platform_user.security_version` 完成。

---

## 1. 目标

首批 P0 Schema 只解决“让核心平台可以开始编码并保持边界正确”的问题。

本轮覆盖五组基础持久化：

1. Resource Core：逻辑内容身份、标题、外部身份、Collection、Tag、Relation、用户状态。
2. Storage：Attachment、Blob、Placement、Storage Provider、Retention Hold。
3. Integration：Durable Event Outbox 与 Consumer Inbox。
4. Operations：Background Task 与 Attempt。
5. Identity / Authorization：User、Permission Registry、Role、Binding、JWT Security Version。

本轮不展开：

- 专业媒体、阅读、音乐、图片、游戏 Schema；
- Document Revision / Collaborative Editing；
- Personal Drive 全量 Schema；
- Productivity / Finance / Secure Domain 业务表；
- Search / Analytics 投影表；
- Plugin-owned 自定义业务表；
- Backup Manifest / Restore Point 全量表；
- 最终容量分区策略。

这些领域只能在本 P0 基础成立后继续增加 Migration。

---

## 2. 全局数据库规则

### 2.1 PostgreSQL Schema

P0 使用以下 PostgreSQL Schema：

```text
resource
storage
integration
operations
identity
app_runtime
```

所有业务表必须显式位于 Owner Schema 中，不依赖 `public` 默认命名空间。

### 2.2 ID

所有具有独立生命周期的持久化实体：

```text
id uuid primary key
```

应用侧生成 UUIDv7。

数据库不得使用自增 bigint 作为这些领域实体的公共身份。

### 2.3 时间

真实时间点统一：

```text
timestamptz
```

普通实体至少使用：

```text
created_at timestamptz not null
updated_at timestamptz not null
```

不可变事实如 Outbox Event 不需要 `updated_at` 表达业务内容修改。

### 2.4 乐观并发

可变 Aggregate Root 默认：

```text
version bigint not null default 0
```

更新必须使用：

```sql
... where id = :id and version = :expected_version
```

并在成功时推进 `version`。

### 2.5 状态字段

公开状态使用稳定 text code，并以 `CHECK` 约束 P0 已知值。

不得持久化 Java Enum ordinal。

### 2.6 删除

默认不跨 Owner 使用 `ON DELETE CASCADE`。

跨 Owner 保存 UUID 时，只把它当成稳定 Contract Reference。

### 2.7 JSONB

允许用于：

- Event payload；
- Provider capability；
- Storage provider metadata；
- 版本化扩展配置。

P0 高频过滤字段和不变量不得隐藏在 JSONB 中。

---

# Part A — Resource Core Schema

## 3. `resource.resource`

### 3.1 Purpose

逻辑内容统一身份。

### 3.2 Columns

| Column | Type | Null | Contract |
|---|---|---:|---|
| `id` | uuid | NO | UUIDv7 |
| `type` | text | NO | 稳定 Resource Type；插件类型必须 namespaced |
| `primary_title` | text | YES | 展示缓存，不替代 title 表 |
| `summary` | text | YES | 通用摘要 |
| `lifecycle_status` | text | NO | `ACTIVE / ARCHIVED / TRASHED / PURGED` |
| `data_classification` | text | NO | `PUBLIC / SHARED / PRIVATE / SENSITIVE / SECURE` |
| `version` | bigint | NO | optimistic concurrency |
| `created_at` | timestamptz | NO | 创建时间 |
| `updated_at` | timestamptz | NO | 最近修改 |
| `deleted_at` | timestamptz | YES | 进入删除流程的时间 |

### 3.3 Constraints

```text
PK resource_pk(id)
CHECK resource_type_not_blank
CHECK resource_lifecycle_ck
CHECK resource_classification_ck
CHECK version >= 0
CHECK lifecycle_status != 'PURGED' OR deleted_at IS NOT NULL
```

`type` 一旦产生具有专业领域数据的下游引用，不允许普通 UPDATE 任意改写；类型转换必须走显式 Command。

### 3.4 Indexes

```text
resource_type_lifecycle_idx(type, lifecycle_status, updated_at desc, id)
resource_lifecycle_updated_idx(lifecycle_status, updated_at desc, id)
```

Cursor 分页必须使用稳定 `(sort_value, id)` 组合。

---

## 4. `resource.resource_title`

| Column | Type | Null | Contract |
|---|---|---:|---|
| `id` | uuid | NO | UUIDv7 |
| `resource_id` | uuid | NO | owner-local FK |
| `text` | text | NO | 标题内容 |
| `language_tag` | text | YES | BCP 47 |
| `role` | text | NO | `PRIMARY / ORIGINAL / TRANSLATED / ROMANIZED / ALIAS` |
| `source_kind` | text | NO | `USER / FILE_SCAN / IMPORT / PROVIDER / PLUGIN / SYSTEM / AI_SUGGESTION` |
| `source_ref` | text | YES | 非 Secret 来源引用 |
| `pinned` | boolean | NO | 用户锁定 |
| `priority` | integer | NO | 展示/解析优先级 |
| `created_at` | timestamptz | NO | |
| `updated_at` | timestamptz | NO | |

Constraints：

```text
FK(resource_id) -> resource.resource(id) ON DELETE CASCADE
CHECK text <> ''
CHECK role in known P0 codes
CHECK source_kind in known P0 codes
```

Indexes：

```text
resource_title_resource_idx(resource_id, priority, id)
resource_title_lookup_idx(lower(text))
```

不对 `(resource_id, language_tag)` 建唯一约束，因为同语言允许多个 Alias。

---

## 5. `resource.external_identity`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `resource_id` | uuid | NO |
| `provider` | text | NO |
| `namespace` | text | NO |
| `object_type` | text | NO |
| `external_id` | text | NO |
| `canonical_url` | text | YES |
| `verification_status` | text | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

核心约束：

```text
UNIQUE(provider, namespace, object_type, external_id)
FK(resource_id) -> resource.resource(id)
```

禁止在唯一冲突时自动 merge Resource。

---

## 6. `resource.metadata_provenance`

P0 采用字段级 provenance，不复制整个 Resource Snapshot。

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `resource_id` | uuid | NO |
| `field_key` | text | NO |
| `source_kind` | text | NO |
| `source_ref` | text | YES |
| `managed` | boolean | NO |
| `pinned` | boolean | NO |
| `source_observed_at` | timestamptz | YES |
| `candidate_value` | jsonb | YES |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

核心约束：

```text
UNIQUE(resource_id, field_key)
```

`candidate_value` 只保存非 Secret、允许被当前数据等级传播的候选值。

---

## 7. Collection

### 7.1 `resource.collection`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `owner_user_id` | uuid | YES |
| `parent_id` | uuid | YES |
| `name` | text | NO |
| `description` | text | YES |
| `kind` | text | NO |
| `mode` | text | NO |
| `visibility` | text | NO |
| `lifecycle_status` | text | NO |
| `sort_policy` | text | YES |
| `version` | bigint | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

`owner_user_id` 是跨 Identity 的稳定 Contract Reference，P0 不建立跨 Schema cascade FK。

Self FK：

```text
FK(parent_id) -> resource.collection(id) ON DELETE RESTRICT
CHECK(parent_id IS NULL OR parent_id <> id)
```

任意深度循环不能只靠上述 CHECK；MoveCollection Command 必须在事务中执行祖先检测。

### 7.2 `resource.collection_member`

| Column | Type | Null |
|---|---|---:|
| `collection_id` | uuid | NO |
| `resource_id` | uuid | NO |
| `position` | numeric | YES |
| `note` | text | YES |
| `added_by_user_id` | uuid | YES |
| `created_at` | timestamptz | NO |

```text
PRIMARY KEY(collection_id, resource_id)
FK collection_id -> resource.collection(id) ON DELETE CASCADE
FK resource_id -> resource.resource(id) ON DELETE RESTRICT
```

动态 Collection 不向该表物化普通计算结果。

---

## 8. Tag

### 8.1 `resource.tag`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `owner_user_id` | uuid | YES |
| `scope_key` | text | NO |
| `name` | text | NO |
| `normalized_name` | text | NO |
| `display_metadata` | jsonb | YES |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

唯一约束：

```text
UNIQUE NULLS NOT DISTINCT(owner_user_id, scope_key, normalized_name)
```

`normalized_name` 的生成规则必须由应用统一函数负责，并有数据库集成测试锁定。

### 8.2 `resource.resource_tag`

```text
resource_id uuid not null
 tag_id uuid not null
 added_by_user_id uuid null
 created_at timestamptz not null
PRIMARY KEY(resource_id, tag_id)
```

owner-local FK 均可使用。

重复批量打标签必须自然幂等。

---

## 9. `resource.resource_relation`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `source_resource_id` | uuid | NO |
| `target_resource_id` | uuid | NO |
| `relation_type` | text | NO |
| `source_kind` | text | NO |
| `context` | jsonb | YES |
| `created_by_user_id` | uuid | YES |
| `created_at` | timestamptz | NO |

基础约束：

```text
CHECK(source_resource_id <> target_resource_id)
UNIQUE(source_resource_id, target_resource_id, relation_type)
```

无向 Relation 在 Command 层必须 canonicalize ID 顺序后写入。

---

## 10. `resource.user_resource_state`

P0 合并通用轻量状态，不把专业播放器/阅读器状态塞入此表。

| Column | Type | Null |
|---|---|---:|
| `user_id` | uuid | NO |
| `resource_id` | uuid | NO |
| `favorite` | boolean | NO |
| `rating` | numeric | YES |
| `status_code` | text | YES |
| `progress_value` | numeric | YES |
| `progress_unit` | text | YES |
| `last_accessed_at` | timestamptz | YES |
| `version` | bigint | NO |
| `updated_at` | timestamptz | NO |

```text
PRIMARY KEY(user_id, resource_id)
FK resource_id -> resource.resource(id) ON DELETE RESTRICT
CHECK rating IS NULL OR (rating >= 0 AND rating <= 10)
CHECK progress_value IS NULL OR progress_value >= 0
```

`user_id` 不做跨 Schema cascade FK。

---

# Part B — Storage Schema

## 11. `storage.blob`

Blob 是不可变字节身份。

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `hash_algorithm` | text | NO |
| `content_hash` | text | NO |
| `size_bytes` | bigint | NO |
| `sniffed_media_type` | text | YES |
| `integrity_status` | text | NO |
| `lifecycle_status` | text | NO |
| `created_at` | timestamptz | NO |
| `last_verified_at` | timestamptz | YES |

核心约束：

```text
UNIQUE(hash_algorithm, content_hash, size_bytes)
CHECK(size_bytes >= 0)
CHECK integrity_status in ('UNKNOWN','VERIFIED','MISSING','CORRUPT')
CHECK lifecycle_status in ('ACTIVE','GC_CANDIDATE','PURGED')
```

任何业务代码不得 UPDATE `content_hash` / `size_bytes` 修改一个已存在 Blob 的内容身份。

---

## 12. `storage.attachment`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `blob_id` | uuid | NO |
| `filename` | text | YES |
| `media_type` | text | YES |
| `usage_kind` | text | YES |
| `source_kind` | text | NO |
| `source_reference` | jsonb | YES |
| `data_classification` | text | NO |
| `lifecycle_status` | text | NO |
| `version` | bigint | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |
| `deleted_at` | timestamptz | YES |

```text
FK(blob_id) -> storage.blob(id) ON DELETE RESTRICT
CHECK lifecycle_status in ('ACTIVE','ARCHIVED','TRASHED','PURGED')
```

P0 物化后的 Attachment 绑定单一不可变 Blob。替换内容创建新 Attachment，而不是修改旧 Blob 字节。

---

## 13. `storage.storage_provider`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `provider_key` | text | NO |
| `provider_type` | text | NO |
| `display_name` | text | NO |
| `credential_ref` | text | YES |
| `capabilities` | jsonb | NO |
| `configuration` | jsonb | NO |
| `tier` | text | NO |
| `enabled` | boolean | NO |
| `drain_status` | text | NO |
| `version` | bigint | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

```text
UNIQUE(provider_key)
CHECK tier in ('HOT','WARM','COLD','ARCHIVE','DEEP_ARCHIVE')
CHECK drain_status in ('NORMAL','DRAINING','DRAINED')
```

Credential 只能保存 `secret://` 或等价 Secret Reference，禁止明文凭据进入该表。

---

## 14. `storage.blob_placement`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `blob_id` | uuid | NO |
| `provider_id` | uuid | NO |
| `object_key` | text | NO |
| `state` | text | NO |
| `tier` | text | NO |
| `provider_metadata` | jsonb | YES |
| `verified_at` | timestamptz | YES |
| `restore_expires_at` | timestamptz | YES |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

```text
UNIQUE(provider_id, object_key)
UNIQUE(blob_id, provider_id, object_key)
FK blob_id -> storage.blob(id) ON DELETE RESTRICT
FK provider_id -> storage.storage_provider(id) ON DELETE RESTRICT
CHECK state in ('WRITING','AVAILABLE','VERIFYING','MISSING','CORRUPT','RESTORING','MIGRATING','DELETING','DELETED')
```

业务层不得直接依赖 `object_key`。

Indexes：

```text
blob_placement_blob_state_idx(blob_id, state)
blob_placement_provider_state_idx(provider_id, state, updated_at)
```

---

## 15. `storage.blob_retention_hold`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `blob_id` | uuid | NO |
| `hold_type` | text | NO |
| `owner_type` | text | NO |
| `owner_id` | uuid | YES |
| `reason` | text | YES |
| `retention_until` | timestamptz | YES |
| `released_at` | timestamptz | YES |
| `created_at` | timestamptz | NO |

GC eligibility 必须查询仍有效 Hold，不能只统计 Attachment 数量。

```text
blob_retention_active_idx(blob_id, retention_until)
  WHERE released_at IS NULL
```

---

# Part C — Integration Schema

## 16. `integration.event_outbox`

Event 内容一旦写入即为不可变事实。

`producer_subsystem`、`subject_type` 与 `subject_id` 是 Event Envelope 的正式持久化字段。由 Integration Owner 的追加 Migration 通过 Expand / Migrate 建立并从现有 `aggregate_type / aggregate_id` 回填；旧字段删除属于后续独立 Contract 步骤，不能与本次扩展混合。

| Column | Type | Null |
|---|---|---:|
| `event_id` | uuid | NO |
| `event_type` | text | NO |
| `schema_version` | integer | NO |
| `occurred_at` | timestamptz | NO |
| `producer_subsystem` | text | NO |
| `actor_type` | text | YES |
| `actor_id` | uuid | YES |
| `subject_type` | text | YES |
| `subject_id` | uuid | YES |
| `correlation_id` | uuid | YES |
| `causation_id` | uuid | YES |
| `payload` | jsonb | NO |
| `dispatch_status` | text | NO |
| `available_at` | timestamptz | NO |
| `attempt_count` | integer | NO |
| `last_error_class` | text | YES |
| `last_error_summary` | text | YES |
| `dispatched_at` | timestamptz | YES |
| `created_at` | timestamptz | NO |

```text
PRIMARY KEY(event_id)
CHECK schema_version >= 1
CHECK attempt_count >= 0
CHECK dispatch_status in ('PENDING','DISPATCHING','DISPATCHED','FAILED')
```

关键索引：

```text
event_outbox_dispatch_idx(dispatch_status, available_at, occurred_at, event_id)
  WHERE dispatch_status IN ('PENDING','FAILED')

event_outbox_subject_idx(subject_type, subject_id, occurred_at)
```

禁止 Event Payload 存储 Secret / Secure Domain 明文。

领域状态变更与对应 Outbox INSERT 必须在同一 Owner 本地事务完成。

---

## 17. `integration.event_consumer_inbox`

用于 At-least-once delivery 去重。

| Column | Type | Null |
|---|---|---:|
| `consumer_key` | text | NO |
| `event_id` | uuid | NO |
| `event_type` | text | NO |
| `processed_at` | timestamptz | NO |
| `result_code` | text | YES |
| `source_schema_version` | integer | NO |

```text
PRIMARY KEY(consumer_key, event_id)
```

消费者必须在“写自身投影/业务结果 + 插入 Inbox”同一事务中完成幂等提交；如果目标系统不是 PostgreSQL，则必须使用目标系统等价的原子/幂等机制。

### 17.1 Consumer Delivery 状态（ADR-008）

每个 `(consumer_id,event_id)` 独立拥有 `status(PENDING/RETRY/DELIVERED/DEAD)`、`attempt_count`、`next_attempt_at`、`last_error_classification`、`updated_at`。成功 Delivery、Inbox 与 Consumer 数据库副作用同事务提交；失败记录在该事务回滚后持久化。失败采用有上限的退避，达到阈值进入 DEAD；人工 retry 显式请求再次投递。

当前物理实现表为 Integration-owned `event_delivery`，与已有 `event_outbox` / `event_inbox` 同 namespace；全量迁入逻辑 Owner Schema 是独立迁移，不混入本次追加 Migration。`event_outbox.dispatched_at` 仅保留兼容遥测含义，不再作为 Consumer 查询条件或全局完成凭证。

Consumer 以 Inbox 为幂等权威，默认接收仍保留的历史事件；新增 Consumer 与清理历史必须先明确补投/Retention 策略。某 Consumer 跳过不相关事件，只确认自己的 Delivery，不影响其他 Consumer。

---

# Part D — Operations Schema

## 18. `operations.background_task`

Background Task 表示一次逻辑异步执行，不表示一次具体 Attempt。

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `task_type` | text | NO |
| `status` | text | NO |
| `actor_type` | text | YES |
| `actor_id` | uuid | YES |
| `subject_type` | text | YES |
| `subject_id` | uuid | YES |
| `idempotency_key` | text | YES |
| `correlation_id` | uuid | YES |
| `payload` | jsonb | NO |
| `result_summary` | jsonb | YES |
| `available_at` | timestamptz | NO |
| `timeout_at` | timestamptz | YES |
| `cancel_requested_at` | timestamptz | YES |
| `version` | bigint | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |
| `completed_at` | timestamptz | YES |

```text
CHECK status in ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED','TIMED_OUT')
UNIQUE(task_type, idempotency_key) WHERE idempotency_key IS NOT NULL
```

Claim 查询索引：

```text
background_task_claim_idx(status, available_at, created_at, id)
  WHERE status = 'PENDING'
```

---

## 19. `operations.background_task_attempt`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `task_id` | uuid | NO |
| `attempt_no` | integer | NO |
| `status` | text | NO |
| `claimed_by` | text | YES |
| `claimed_at` | timestamptz | YES |
| `lease_expires_at` | timestamptz | YES |
| `last_heartbeat_at` | timestamptz | YES |
| `started_at` | timestamptz | YES |
| `ended_at` | timestamptz | YES |
| `error_classification` | text | YES |
| `error_summary` | text | YES |
| `retryable` | boolean | YES |
| `result_summary` | jsonb | YES |
| `trace_id` | text | YES |
| `created_at` | timestamptz | NO |

```text
UNIQUE(task_id, attempt_no)
FK task_id -> operations.background_task(id) ON DELETE RESTRICT
CHECK attempt_no >= 1
```

必须保留失败 Attempt 历史，Retry 创建新 Attempt，禁止原地覆盖。

过期 Lease 的恢复由 scheduler/worker reconciliation 处理。

---

# Part E — Identity / Authorization Schema

## 20. `platform_user`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `username` | text | NO |
| `normalized_username` | text | NO |
| `email` | text | YES |
| `normalized_email` | text | YES |
| `display_name` | text | YES |
| `status` | text | NO |
| `password_hash` | text | YES |
| `security_version` | bigint | NO |
| `version` | bigint | NO |
| `is_del` | smallint | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

```text
UNIQUE(normalized_username)
UNIQUE(normalized_email) WHERE normalized_email IS NOT NULL
CHECK status in ('PENDING','ACTIVE','DISABLED','LOCKED','DEACTIVATED')
CHECK security_version >= 0
CHECK is_del in (0, 1)
```

`is_del` is a required soft-delete marker with only two values: `0` (visible)
and `1` (soft-deleted). Application-layer reads of `platform_user` must filter
`is_del = 0`; rows with `is_del = 1` remain database-only history.

只保存现代密码哈希结果，不保存密码明文、可逆密码或日志副本。

`security_version` 是用户级 Token Security Epoch。签发 JWT 时把当前值写入 Token；请求认证时除了校验签名、`exp` 等标准约束，还必须要求 Token 中的 `security_version` 与用户当前值一致。提升该值即可使此前签发的所有 JWT 提前失效，而无需枚举或持久化 Session。

---

## 21. `identity.permission_registry`

Permission 是后端权威 Action Registry，不由 CMS Route 定义。

| Column | Type | Null |
|---|---|---:|
| `permission_key` | text | NO |
| `owner_subsystem` | text | NO |
| `description` | text | NO |
| `risk_level` | text | NO |
| `minimum_svl` | integer | NO |
| `fresh_verification_required` | boolean | NO |
| `deprecated` | boolean | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

```text
PRIMARY KEY(permission_key)
CHECK minimum_svl >= 0
CHECK risk_level in ('LOW','MEDIUM','HIGH','CRITICAL')
```

P0 至少注册：

```text
resource.read
resource.create
resource.update
resource.archive
resource.delete
storage.attachment.read
storage.attachment.create
storage.provider.read
storage.provider.manage
storage.blob.gc
platform.task.read
platform.task.cancel
platform.audit.read
system.app.read
system.app.manage
system.app.client.manage
identity.user.read
identity.user.manage
identity.role.manage
```

具体 Permission Catalog 由 Command / Query Contract 文档继续补全。

其中 `system.app.read`、`system.app.manage`、`system.app.client.manage` 由 App Runtime 注册；它们只管理 App Registry / Lifecycle / Client Registration，不授予任何专业 Server App 业务数据访问权。

---

## 22. Role

### 22.1 `identity.role`

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `role_key` | text | NO |
| `display_name` | text | NO |
| `system_role` | boolean | NO |
| `version` | bigint | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

```text
UNIQUE(role_key)
```

### 22.2 `identity.role_permission`

```text
role_id uuid not null
permission_key text not null
created_at timestamptz not null
PRIMARY KEY(role_id, permission_key)
FK role_id -> identity.role(id) ON DELETE CASCADE
FK permission_key -> identity.permission_registry(permission_key) ON DELETE RESTRICT
```

### 22.3 `identity.user_role`

```text
user_id uuid not null
role_id uuid not null
created_at timestamptz not null
created_by_user_id uuid null
PRIMARY KEY(user_id, role_id)
FK user_id -> platform_user(id) ON DELETE RESTRICT
FK role_id -> identity.role(id) ON DELETE RESTRICT
```

平台 Role 只解决 Instance RBAC，不替代 Resource ACL / Share / Room Membership 等对象级授权。

---

## 23. Stateless JWT Authentication Persistence Boundary

P0 **不定义**：

```text
identity.session
security_session
login_session
```

也不持久化：

- Access Token / Refresh Token 原文；
- Access Token / Refresh Token Digest；
- Session ID / `sid` 作为登录状态主键；
- 每设备登录记录；
- `last_seen_at` 登录会话状态；
- 单 Session `revoked_at` / `revoke_reason`。

JWT 是自包含签名凭据。认证至少校验：

```text
signature
issuer / audience（启用时）
exp / nbf / iat
subject = user_id
security_version == platform_user.security_version
user status allows authentication
```

每枚 Access JWT、Refresh JWT 和 Step-up Verification Grant 都必须携带唯一 `jti`。`jti` 只用于 Token / Grant 追踪、审计关联和问题排查，不作为登录 Session 主键；服务端不持久化 `jti`，也不维护基于 `jti` 的撤销黑名单。

当前设备 Logout 由客户端删除本地 Token 与 Credential Cache 完成，不产生数据库 Session 变更。

需要让目标用户全部旧 Token 提前失效时，执行 `identity.invalidate-user-tokens`，在事务中提升：

```text
platform_user.security_version
```

并发布 `authentication.user.tokens-invalidated`。

Step-up Verification 的 OTP Challenge 可以按验证子系统需要短期持久化，但它是一次性 Challenge，不是登录 Session。验证成功后签发的 Step-up Grant 应为短期、Purpose-bound 的签名凭据；服务端不因此创建 `SecuritySession` 行。

ADR-006 定义的 `identity.app_authorization_grant` 是 Authorization Domain 的 Client Scope 授权事实，不属于登录 Session。App-scoped Token 必须绑定 Grant identity / version；Grant 表本身不得保存 Token 原文、Digest 或 `jti`。

---

# Part F — Cross-domain Transactions

## 24. Resource 创建事务

```text
BEGIN
  INSERT resource.resource
  INSERT resource.resource_title ...
  INSERT integration.event_outbox(resource.created@v1)
COMMIT
```

注意：`integration.event_outbox` 虽属于 Integration Schema，但允许由领域事务写入统一 Outbox Public Persistence Contract；领域代码不得操作 Dispatcher 私有状态。

---

## 25. Attachment 完成事务

```text
Blob 已经物化且至少一个 Placement AVAILABLE
        ↓
BEGIN
  INSERT/resolve storage.blob
  INSERT storage.attachment
  INSERT integration.event_outbox(storage.attachment.created@v1)
COMMIT
```

对象存储网络写入不得在持有数据库长事务时执行。

典型流程应为 staging → hash/verify → placement commit → short database transaction。

---

## 26. Event Consumer 事务

```text
BEGIN
  if integration.event_consumer_inbox has (consumer,event)
      return already processed

  mutate consumer-owned state
  insert integration.event_consumer_inbox
COMMIT
```

重复投递是正常输入。

---

## 27. Background Task Claim

P0 可使用 `FOR UPDATE SKIP LOCKED` 或等价短事务 Claim：

```text
BEGIN
  select next PENDING task ... for update skip locked
  create next attempt
  update task -> RUNNING
COMMIT
```

外部执行发生在事务外。

Lease/Heartbeat 更新必须是短事务。

---

# Part G — Migration Plan

## 28. 首批 `r2dbc-migrate` 顺序

建议将首批 Migration 分为以下可独立审查步骤，并使用仓库统一的 `V<monotonic-version>__<description>.sql` 命名契约：

```text
V<...001>__CREATE_SCHEMAS.sql
V<...002>__IDENTITY_FOUNDATION.sql
V<...003>__APP_RUNTIME_FOUNDATION.sql
V<...004>__APP_AUTHORIZATION_GRANT.sql
V<...005>__INTEGRATION_OUTBOX_INBOX.sql
V<...006>__OPERATIONS_BACKGROUND_TASK.sql
V<...007>__RESOURCE_CORE.sql
V<...008>__STORAGE_CORE.sql
V<...009>__SEED_PERMISSION_REGISTRY.sql
V<...010>__SEED_BUILTIN_ROLES.sql
```

具体单调版本号由提交时按仓库 Migration Version Policy 分配，但顺序依赖不得倒置。

`identity_foundation` 不创建登录 Session 表；Identity 持久化基线只保留账号、权限、角色、绑定、凭据/验证所需数据以及用户级 `security_version`。

`app_runtime_foundation` 只创建 Platform 必须掌握的 App Registry / Installation / Client Registration / Permission Grant / Scope / Dependency / Migration 状态，不创建任何 Anime、Drive、Finance 等专业业务表。

`app_authorization_grant` 由 Authorization Owner 持久化到 `identity` Schema，用于 Client / Device 级 Scope Grant 与撤销；它不是 Session Store。

### 28.1 Migration 原则

- Production 禁止 Hibernate/ORM 自动建表作为 Schema Source of Truth。
- 所有 DDL 必须进入 versioned migration。
- Seed Permission / Built-in Role 必须是 deterministic。
- 新增 `NOT NULL` 大字段遵循 Expand → Migrate → Contract。
- 大规模 Backfill 不应放在启动阻塞 DDL 中。
- 不提供“假装安全”的 destructive down migration。

---

# Part H — Required Repository Contracts

## 29. Repository 只能访问 Owner Schema

默认约束：

```text
resource persistence adapter     -> resource.*
storage persistence adapter      -> storage.*
integration persistence adapter  -> integration.*
operations persistence adapter   -> operations.*
identity persistence adapter     -> identity.*
```

例外：统一 Outbox Writer Port 可以由业务模块调用，由 Integration 提供稳定的事务内 persistence adapter。

其他跨域读取必须通过 Query / Capability，而不是直接 JOIN 私表。

---

## 30. 必须由数据库直接保证的不变量

P0 至少将以下不变量下降到 Constraint：

| Invariant | DB Enforcement |
|---|---|
| External Identity 全局唯一映射 | UNIQUE |
| Collection Member 不重复 | PK/UNIQUE |
| Resource Tag 不重复 | PK/UNIQUE |
| Blob 内容去重键稳定 | UNIQUE(hash,size) |
| Placement Object Key 在 Provider 内唯一 | UNIQUE |
| Consumer/Event 只处理一次 | PK(consumer,event) |
| Task Idempotency Key 不重复 | Partial UNIQUE |
| Attempt Number 不重复 | UNIQUE(task,attempt_no) |
| Username / Email 规范化唯一 | UNIQUE |
| Role Permission 不重复 | PK |
| User security_version 非负 | CHECK |

以下不变量不能只靠单行 Constraint，需要 Command + Transaction + Integration Test：

- Collection 无循环；
- Resource Type 受控转换；
- 用户 pinned metadata 不被 provider 覆盖；
- Blob GC 前不存在有效业务引用和 Hold；
- Background Task Lease 恢复；
- Resource ACL / RBAC / Share 的最终授权判定；
- JWT `security_version` 与当前用户版本的一致性校验；
- Cross-domain Event Consumer 最终一致性。

---

# Part I — App Runtime / Client Authorization Schema

本 Part 只定义 Platform 为运行 Server App 所必需的最小持久化事实。Server App 自己的 Domain Schema 不属于 `app_runtime`。

### Implementation Checkpoint（2026-09-20）

PR #1427 已通过 `V202609200005__DDL_APP_RUNTIME_FOUNDATION.sql` 创建：

- `app_runtime.app_definition`
- `app_runtime.app_installation`
- `app_runtime.app_scope_definition`
- `app_runtime.app_permission_grant`
- `app_runtime.app_client_registration`
- `app_runtime.app_client_redirect_uri`
- `app_runtime.app_dependency`
- `app_runtime.app_migration_history`

其中当前服务层状态：

- Registry / Installation / Scope / Permission Grant / Client Registration 已接入 `app-runtime` R2DBC Store；
- `app_dependency` 与 `app_migration_history` 当前为 **schema-only**，尚未接入 lifecycle enforcement / orchestration；
- `identity.app_authorization_grant` 与 `identity.app_authorization_grant_scope` **尚未创建 production migration**，仍属于 ADR-006 / Authorization 后续实现；
- 当前不存在 Login Session、Token Digest 或 `jti` blacklist 表。

不得因为本文已定义 AppAuthorizationGrant Schema Contract 就认为其已在主线数据库中可用。

## 31. `app_runtime.app_definition`

表达稳定 App identity 与 Manifest 中需要平台检索的最小元数据。

| Column | Type | Null |
|---|---|---:|
| `app_id` | text | NO |
| `name` | text | NO |
| `publisher` | text | NO |
| `manifest_version` | text | NO |
| `supported_api_majors` | integer[] | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

```text
PRIMARY KEY(app_id)
```

`app_id` 使用稳定 reverse-DNS 风格标识，例如 `run.ikaros.anime`。App 升级不得改变 `app_id`。

---

## 32. `app_runtime.app_installation`

表达当前 Instance 上 Server App 的安装与运行状态。

| Column | Type | Null |
|---|---|---:|
| `app_id` | text | NO |
| `package_version` | text | NO |
| `lifecycle_state` | text | NO |
| `platform_api_min` | text | NO |
| `platform_api_max` | text | YES |
| `configuration_state` | text | NO |
| `failure_code` | text | YES |
| `version` | bigint | NO |
| `installed_at` | timestamptz | NO |
| `enabled_at` | timestamptz | YES |
| `disabled_at` | timestamptz | YES |
| `updated_at` | timestamptz | NO |

```text
PRIMARY KEY(app_id)
FK app_id -> app_runtime.app_definition(app_id) ON DELETE RESTRICT
CHECK lifecycle_state in (
  'DISCOVERED','INSTALLING','INSTALLED','ENABLING','ENABLED',
  'DISABLING','DISABLED','UPGRADING','FAILED',
  'UNINSTALLING','UNINSTALLED','INCOMPATIBLE'
)
CHECK version >= 0
```

Disable 不删除 App-owned Data；Uninstall 的数据保留策略由 App Runtime Command 决定。

---

## 33. App Scope 与 Platform Permission Grant

### 33.1 `app_runtime.app_scope_definition`

```text
app_id text not null
scope_key text not null
description text not null
created_at timestamptz not null
updated_at timestamptz not null
PRIMARY KEY(app_id, scope_key)
FK app_id -> app_runtime.app_definition(app_id) ON DELETE CASCADE
```

Scope 表达 Client App → Server App 的业务授权，例如：

```text
anime.library.read
anime.playback
accounting.transaction.write
```

### 33.2 `app_runtime.app_permission_grant`

```text
app_id text not null
permission_key text not null
grant_status text not null
granted_by_user_id uuid null
granted_at timestamptz null
updated_at timestamptz not null
PRIMARY KEY(app_id, permission_key)
FK app_id -> app_runtime.app_definition(app_id) ON DELETE CASCADE
CHECK grant_status in ('GRANTED','DENIED','REVOKED')
```

该表表达 Server App → Platform 的实际 Permission Grant。

当前实现通过 Authorization-owned `PermissionCatalogQuery` 校验 Permission Key，避免 App Runtime 直接读取 `permission_registry`。Grant 记录 `granted_by_user_id`；Server App uninstall 时当前 Foundation 会清空其 Platform Permission Grant，避免重新安装静默继承旧授权。

`permission_key` 属于 Authorization Permission Registry 的稳定 Contract Key。为保持 Owner Boundary，P0 不从 `app_runtime` 对 `identity.permission_registry` 建跨 Owner FK；写入时必须通过 Authorization Capability 验证 Key 有效。

App Scope 与 Platform Permission 不得互换。

---

## 34. Client Registration

### 34.1 `app_runtime.app_client_registration`

| Column | Type | Null |
|---|---|---:|
| `client_id` | text | NO |
| `app_id` | text | NO |
| `name` | text | NO |
| `client_type` | text | NO |
| `publisher` | text | NO |
| `official` | boolean | NO |
| `status` | text | NO |
| `created_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |

```text
PRIMARY KEY(client_id)
FK app_id -> app_runtime.app_definition(app_id) ON DELETE RESTRICT
CHECK client_type in ('PUBLIC_NATIVE','PUBLIC_BROWSER','CONFIDENTIAL_SERVER')
CHECK status in ('ACTIVE','DISABLED','REVOKED')
```

`official` 仅是 Publisher / UX 元数据，不产生授权旁路。

### 34.2 `app_runtime.app_client_redirect_uri`

```text
client_id text not null
redirect_uri text not null
created_at timestamptz not null
PRIMARY KEY(client_id, redirect_uri)
FK client_id -> app_runtime.app_client_registration(client_id) ON DELETE CASCADE
```

Native / Browser Authorization 必须对 Redirect URI 做精确匹配；不得使用任意前缀或模糊 Host 匹配替代注册契约。

---

## 35. `app_runtime.app_dependency`

```text
app_id text not null
dependency_app_id text not null
api_requirement text null
required boolean not null
created_at timestamptz not null
PRIMARY KEY(app_id, dependency_app_id)
FK app_id -> app_runtime.app_definition(app_id) ON DELETE CASCADE
CHECK app_id <> dependency_app_id
```

`dependency_app_id` 不建立 FK，以允许描述尚未安装的 Required / Optional App；Enable 时通过 App Registry 解析兼容性。

---

## 36. `app_runtime.app_migration_history`

```text
app_id text not null
migration_version text not null
checksum text not null
status text not null
applied_at timestamptz null
created_at timestamptz not null
PRIMARY KEY(app_id, migration_version)
FK app_id -> app_runtime.app_definition(app_id) ON DELETE RESTRICT
CHECK status in ('PENDING','APPLIED','FAILED')
```

App-owned Migration History 与 Platform Migration History 分离。一个 App 不得通过自己的 Migration 修改其他 Owner Schema。

---

## 37. AppAuthorizationGrant

### 37.1 `identity.app_authorization_grant`

该表属于 Authorization Owner，不属于 App Runtime。

| Column | Type | Null |
|---|---|---:|
| `id` | uuid | NO |
| `subject_id` | uuid | NO |
| `client_id` | text | NO |
| `app_id` | text | NO |
| `device_id` | uuid | YES |
| `grant_version` | bigint | NO |
| `refresh_generation` | bigint | NO |
| `refresh_last_used_at` | timestamptz | YES |
| `status` | text | NO |
| `granted_at` | timestamptz | NO |
| `updated_at` | timestamptz | NO |
| `revoked_at` | timestamptz | YES |

```text
PRIMARY KEY(id)
FK subject_id -> platform_user(id) ON DELETE RESTRICT
CHECK grant_version >= 0
CHECK refresh_generation >= 0
CHECK status in ('ACTIVE','REVOKED')
UNIQUE NULLS NOT DISTINCT(subject_id, client_id, app_id, device_id)
```

`client_id`、`app_id`、`device_id` 是跨 Owner Stable Contract Reference；Authorization 不对 App Runtime / Device Persistence 建数据库 FK。

撤销 Grant：

```text
status = REVOKED
grant_version = grant_version + 1
revoked_at = now()
```

不得在该表保存 Token、Token Digest、`jti` 或 Session 状态。

`refresh_generation` 是 Grant 级 Refresh Token Rotation 代际，用于 Public Client replay detection。它不标识某一枚 Token，也不要求保存 Token 原文、Digest 或 `jti`。成功 Refresh 通过 compare-and-swap 单调递增 generation；旧 generation 再次出现时按安全策略撤销 Grant。

### 37.2 `identity.app_authorization_grant_scope`

```text
grant_id uuid not null
scope_key text not null
created_at timestamptz not null
PRIMARY KEY(grant_id, scope_key)
FK grant_id -> identity.app_authorization_grant(id) ON DELETE CASCADE
```

`scope_key` 必须来自目标 Server App 当前声明的 Scope Registry。Authorization 通过 App Runtime Capability 验证，不建立跨 Owner FK。

---

# Part J — P0 Schema Exit Criteria

## 38. Definition of Ready for `r2dbc-migrate`

该 Schema Contract 进入真实 `r2dbc-migrate` 实现前必须满足：

- [ ] 所有表有明确 Owner Module。
- [ ] 每个跨域 UUID 已声明是否建立 FK。
- [ ] 每个公开状态有稳定 string code。
- [ ] 每个 mutable aggregate 有 concurrency strategy。
- [ ] 每个关键唯一性不变量有 DB Constraint。
- [ ] 每个事件型业务写入明确 Outbox transaction boundary。
- [ ] 所有 Secret 字段均转换为 digest / secret reference / encrypted envelope。
- [ ] 索引能支持 P0 cursor pagination / claim / dispatch 查询。

## 39. Definition of Done for P0 Schema

只有以下全部成立才视为首批 P0 Schema 完成：

- [ ] `r2dbc-migrate` 可以从空 PostgreSQL 创建全部 P0 Schema。
- [ ] Migration 可在 CI 重复从零验证。
- [ ] Constraint Integration Test 覆盖本文档第 30 节全部 DB-enforced invariant。
- [ ] Repository Boundary Test 能阻止模块直接访问非 Owner Schema。
- [ ] Resource Create + Outbox 原子性测试通过。
- [ ] Consumer Inbox 重放测试通过。
- [ ] Background Task claim/lease 基础并发测试通过。
- [ ] Storage Blob/Placement 唯一性与 GC eligibility 基础测试通过。
- [ ] Permission Registry seed 与 Built-in Role seed 可重复执行且结果稳定。
- [ ] Identity 基线不存在登录 Session / Token Digest 表，旧 JWT 失效仅依赖受控 Token 验证策略与 `security_version`。

---

## 40. 后续 Schema 顺序

本文件完成后，下一批 Schema 建议按以下顺序增加：

```text
Personal Drive P0
    ↓
Content Ingestion
    ↓
Document / Revision
    ↓
Media / Reading / Music / Photo / Game
    ↓
Sharing / Offline Sync
    ↓
Search Projection / Analytics
    ↓
Productivity / Finance
    ↓
Secure Notes / Password Manager
    ↓
AI / Plugin / Automation extended persistence
```

后续领域不得破坏本文件已经锁定的 `Resource ≠ Attachment ≠ Blob`、Owner Schema、Outbox、Consumer Idempotency 与 Authorization Registry 边界。
