# Ikaros V2 P0 Event Payload Schema Registry

| 项目 | 内容 |
|---|---|
| 文档名称 | P0 Event Payload Schema Registry |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 状态 | Draft / Contract Registry |
| Catalog | `P0-Command-Query-Event-Catalog.md` |

> 本文件给出 P0 Event Payload 的结构注册表，供后续拆分为独立 JSON Schema 文件和 CI compatibility check 使用。

## 1. Envelope

所有 Event 使用统一 envelope，payload 仅包含该事件自己的最小事实。

```text
EventEnvelopeV1
- event_id: uuid
- event_type: string
- schema_version: integer >= 1
- occurred_at: RFC3339 timestamp
- producer_subsystem: string
- actor?: { type, id? }
- subject?: { type, id? }
- correlation_id?: uuid
- causation_id?: uuid
- payload: object
```

## 2. Resource Payloads

### `resource.resource.created@1`

```json
{
  "resource_id": "uuid",
  "resource_type": "anime",
  "lifecycle_status": "ACTIVE",
  "version": 0
}
```

### `resource.resource.updated@1`

```json
{
  "resource_id": "uuid",
  "changed_fields": ["summary"],
  "version": 2
}
```

### `resource.resource.archived@1`

```json
{
  "resource_id": "uuid",
  "previous_status": "ACTIVE",
  "version": 3
}
```

### `resource.resource.restored@1`

```json
{
  "resource_id": "uuid",
  "previous_status": "ARCHIVED",
  "version": 4
}
```

### `resource.resource.trashed@1`

```json
{
  "resource_id": "uuid",
  "version": 5
}
```

### `resource.external-identity.attached@1`

```json
{
  "resource_id": "uuid",
  "provider": "bangumi",
  "namespace": "subject",
  "object_type": "anime",
  "external_id": "12345"
}
```

### `resource.external-identity.detached@1`

Payload 与 attached 相同 identity key。

### `resource.tag.created@1`

```json
{
  "tag_id": "uuid",
  "scope_key": "user"
}
```

### `resource.tag.added@1`

```json
{
  "resource_id": "uuid",
  "tag_id": "uuid"
}
```

### `resource.tag.removed@1`

同上。

### `resource.collection.created@1`

```json
{
  "collection_id": "uuid",
  "kind": "library",
  "mode": "STATIC"
}
```

### `resource.collection.member-added@1`

```json
{
  "collection_id": "uuid",
  "resource_id": "uuid"
}
```

### `resource.collection.member-removed@1`

同上。

### `resource.user-state.changed@1`

```json
{
  "user_id": "uuid",
  "resource_id": "uuid",
  "changed_fields": ["favorite", "rating"],
  "version": 2
}
```

## 3. Storage Payloads

### `storage.attachment.created@1`

```json
{
  "attachment_id": "uuid",
  "blob_id": "uuid",
  "usage_kind": "cover",
  "data_classification": "PRIVATE"
}
```

### `storage.attachment.archived@1`

```json
{ "attachment_id": "uuid" }
```

### `storage.attachment.trashed@1`

```json
{ "attachment_id": "uuid" }
```

### `storage.blob.verified@1`

```json
{
  "blob_id": "uuid",
  "integrity_status": "VERIFIED",
  "verified_at": "2026-08-31T12:00:00Z"
}
```

### `storage.blob.integrity-failed@1`

```json
{
  "blob_id": "uuid",
  "integrity_status": "CORRUPT"
}
```

### `storage.provider.created@1`

```json
{
  "provider_id": "uuid",
  "provider_type": "filesystem",
  "tier": "HOT"
}
```

### `storage.provider.updated@1`

```json
{
  "provider_id": "uuid",
  "changed_fields": ["display_name"]
}
```

### `storage.provider.enabled@1`

```json
{ "provider_id": "uuid" }
```

### `storage.provider.disabled@1`

```json
{ "provider_id": "uuid" }
```

### `storage.provider.drain-requested@1`

```json
{
  "provider_id": "uuid",
  "task_id": "uuid"
}
```

### `storage.blob.gc-requested@1`

```json
{
  "blob_id": "uuid",
  "task_id": "uuid"
}
```

### `storage.blob.purged@1`

```json
{
  "blob_id": "uuid",
  "purged_placement_count": 2
}
```

## 4. Operations Payloads

### `operations.background-task.created@1`

```json
{
  "task_id": "uuid",
  "task_type": "storage.blob-gc",
  "status": "PENDING"
}
```

### `operations.background-task.started@1`

```json
{
  "task_id": "uuid",
  "attempt_no": 1
}
```

### `operations.background-task.succeeded@1`

```json
{
  "task_id": "uuid",
  "attempt_no": 1
}
```

### `operations.background-task.failed@1`

```json
{
  "task_id": "uuid",
  "attempt_no": 1,
  "error_classification": "provider_unavailable",
  "retryable": true
}
```

### `operations.background-task.cancel-requested@1`

```json
{ "task_id": "uuid" }
```

### `operations.background-task.cancelled@1`

```json
{
  "task_id": "uuid",
  "attempt_no": 1
}
```

### `operations.background-task.timed-out@1`

```json
{
  "task_id": "uuid",
  "attempt_no": 1
}
```

### `operations.background-task.retry-requested@1`

```json
{
  "task_id": "uuid",
  "next_attempt_no": 2
}
```

## 5. Identity Payloads

### `identity.user.created@1`

```json
{ "user_id": "uuid" }
```

### `identity.user.disabled@1`

```json
{
  "user_id": "uuid",
  "security_version": 2
}
```

### `identity.user.enabled@1`

同上。

### `identity.role.created@1`

```json
{
  "role_id": "uuid",
  "role_key": "administrator"
}
```

### `identity.role.permissions-replaced@1`

```json
{
  "role_id": "uuid",
  "permission_keys": ["resource.read", "resource.update"]
}
```

### `identity.user.role-assigned@1`

```json
{
  "user_id": "uuid",
  "role_id": "uuid"
}
```

### `identity.user.role-removed@1`

同上。

### `identity.session.revoked@1`

```json
{
  "session_id": "uuid",
  "user_id": "uuid"
}
```

### `identity.user.sessions-revoked@1`

```json
{
  "user_id": "uuid",
  "security_version": 3
}
```

### `storage.delivery-provider.created@1`

```json
{ "delivery_provider_id": "uuid", "provider_type": "DIRECT|CDN|SERVER_PROXY" }
```

### `storage.delivery-provider.updated@1`

```json
{ "delivery_provider_id": "uuid", "changed_fields": ["config", "enabled"], "version": 2 }
```

### `storage.delivery-provider.enabled@1` / `storage.delivery-provider.disabled@1`

```json
{ "delivery_provider_id": "uuid" }
```

### `storage.delivery-provider.probe-requested@1`

```json
{ "delivery_provider_id": "uuid", "task_id": "uuid" }
```

### `storage.delivery-provider.signing-key-rotation-requested@1`

```json
{ "delivery_provider_id": "uuid", "task_id": "uuid" }
```

### `storage.delivery-provider.probed@1`

```json
{ "delivery_provider_id": "uuid", "health_status": "HEALTHY|DEGRADED|UNHEALTHY|UNKNOWN", "capability_changes": [] }
```

### `storage.delivery-provider.degraded@1`

```json
{ "delivery_provider_id": "uuid", "reason_code": "health-check-degraded" }
```

### `storage.delivery-provider.signing-key-rotated@1`

```json
{ "delivery_provider_id": "uuid", "previous_key_version": 1, "new_key_version": 2 }
```

### `storage.delivery-lease.created@1`

```json
{ "lease_id": "uuid", "attachment_id": "uuid", "purpose": "DELIVERY", "expires_at": "timestamp" }
```

### `storage.delivery-lease.released@1`

```json
{ "lease_id": "uuid", "attachment_id": "uuid" }
```

### `storage.delivery-lease.expired@1`

```json
{ "lease_id": "uuid", "attachment_id": "uuid" }
```

### `storage.restore-budget.updated@1`

```json
{ "policy_id": "uuid", "scope_type": "INSTANCE", "scope_id": null, "version": 2 }
```

### `storage.restore-request.retry-requested@1`

```json
{ "request_id": "uuid", "failed_item_count": 3 }
```

### `storage.restore.reconcile-requested@1`

```json
{
  "operation_id": "uuid"
}
```

### `storage.restore.reconciled@1`

```json
{
  "operation_id": "uuid",
  "status": "SUCCEEDED | FAILED | REQUESTED | IN_PROGRESS"
}
```

### `attachment.purged@1`

```json
{
  "attachment_id": "uuid",
  "resource_id": "uuid",
  "blob_id": "uuid",
  "purged_at": "date-time"
}
```

### `ingestion.import.started@1`

```json
{
  "run_id": "uuid",
  "plan_id": "uuid"
}
```

### `ingestion.plan.generated@1`

```json
{
  "plan_id": "uuid",
  "scan_run_id": "uuid"
}
```

### `source.item.unavailable@1`

```json
{
  "item_id": "uuid",
  "source_id": "uuid",
  "reason": "string"
}
```

### `storage.placement.promotion-requested@1` / `storage.placement.demotion-requested@1`

```json
{
  "placement_id": "uuid",
  "blob_id": "uuid",
  "target_tier": "HOT | WARM | COLD | ARCHIVE"
}
```

### `storage.placement.tiering-completed@1`

```json
{
  "placement_id": "uuid",
  "target_tier": "HOT | WARM | COLD | ARCHIVE"
}
```

## 6. Compatibility Rules

P0 payload compatibility baseline：

1. 新增 optional field：兼容。
2. optional → required：不兼容。
3. 删除 field：不兼容。
4. 修改 field type：不兼容。
5. 改变 field 业务语义：不兼容，即使 JSON type 不变。
6. Enum 新增值：消费者必须安全处理 unknown；生产者需确认旧消费者不会错误拒绝。
7. Secret / token / credential / signed URL 禁止进入 payload。
8. Secure Domain 明文禁止进入 payload。

实现阶段应将本 Registry 拆成机器可校验 JSON Schema，并在 CI 中对 Event Type + Schema Version 做唯一性和兼容性检查。
