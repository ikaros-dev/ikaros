# Ikaros V2 P0 Media Delivery / Restore Event Payload Schema Addendum

| 项目 | 内容 |
|---|---|
| 文档名称 | P0 Media Delivery / Restore Event Payload Schema Addendum |
| 状态 | Draft / Implementation Contract |
| 基线 | `P0-Event-Payload-Schema-Registry.md` |
| Catalog | `P0-Media-Delivery-Restore-Contract-Addendum.md` |

> 本文档是 P0 Event Payload Schema Registry 的规范性扩展。所有字段均为最小稳定 Contract，不得加入 Signed URL、Raw Token、Secret、Origin Credential 或 Provider Auth Header。

---

## 1. `storage.delivery-provider.created` v1

```json
{
  "delivery_provider_id": "uuid",
  "provider_type": "CDN"
}
```

Required：`delivery_provider_id`, `provider_type`。

---

## 2. `storage.delivery-provider.updated` v1

```json
{
  "delivery_provider_id": "uuid",
  "changed_fields": ["config", "enabled"],
  "version": 2
}
```

`changed_fields` 只允许字段名，不包含新旧 Secret Value。

---

## 3. `storage.delivery-provider.enabled` / `disabled` v1

```json
{
  "delivery_provider_id": "uuid"
}
```

---

## 4. `storage.delivery-provider.signing-key-rotated` v1

```json
{
  "delivery_provider_id": "uuid",
  "previous_key_version": 3,
  "new_key_version": 4,
  "emergency": false
}
```

禁止记录 key material / secret ref resolved value。

---

## 5. `storage.delivery-provider.probed` v1

```json
{
  "delivery_provider_id": "uuid",
  "health_status": "HEALTHY",
  "capability_changes": ["range", "private_origin"]
}
```

Probe 原始响应体不得进入 Durable Event。

---

## 6. `storage.delivery-provider.degraded` v1

```json
{
  "delivery_provider_id": "uuid",
  "reason_code": "origin-unreachable"
}
```

`reason_code` 必须是稳定安全码，不包含 Provider 异常全文。

---

## 7. Delivery Binding Events v1

### created

```json
{
  "binding_id": "uuid",
  "storage_provider_id": "uuid",
  "delivery_provider_id": "uuid",
  "priority": 10
}
```

### updated

```json
{
  "binding_id": "uuid",
  "changed_fields": ["cache_key_policy", "priority"],
  "version": 2
}
```

### removed

```json
{
  "binding_id": "uuid"
}
```

Cache Key Policy 具体内容默认不进入 Event；消费者需要当前配置时通过 Query 获取。

---

## 8. `storage.restore-request.requested` v1

```json
{
  "request_id": "uuid",
  "scope_type": "SEASON",
  "scope_id": "uuid",
  "item_count": 24,
  "total_bytes": 25769803776,
  "budget_decision": "ACCEPTED"
}
```

不在 Event 中展开所有 Attachment / Placement ID，避免大 Payload；消费者需要 Item 明细时查询 Projection / API。

---

## 9. Restore Request Lifecycle v1

### cancel-requested

```json
{
  "request_id": "uuid"
}
```

### retry-requested

```json
{
  "request_id": "uuid",
  "failed_item_count": 2
}
```

### completed

```json
{
  "request_id": "uuid",
  "status": "PARTIAL",
  "ready_items": 22,
  "failed_items": 2
}
```

---

## 10. `storage.restore-operation.started` v1

```json
{
  "operation_id": "uuid",
  "placement_id": "uuid",
  "restore_class": "standard",
  "size_bytes": 1073741824
}
```

`restore_class` 是 Provider-normalized stable code，不要求所有 Provider 使用相同内部枚举。

---

## 11. `storage.restore-operation.ready` v1

```json
{
  "operation_id": "uuid",
  "placement_id": "uuid",
  "restore_expires_at": "2026-09-09T12:00:00Z"
}
```

`restore_expires_at` 在恢复结果为长期 Promotion 时可以为 null / omitted，按 Event Schema optional field 处理。

---

## 12. `storage.restore-operation.failed` v1

```json
{
  "operation_id": "uuid",
  "placement_id": "uuid",
  "error_code": "provider-timeout",
  "retryable": true
}
```

禁止把 Provider Error Body 原文放入 Event。

---

## 13. `storage.restore-operation.expired` v1

```json
{
  "operation_id": "uuid",
  "placement_id": "uuid"
}
```

---

## 14. `storage.restore-budget.updated` v1

```json
{
  "policy_id": "uuid",
  "scope_type": "INSTANCE",
  "scope_id": null,
  "version": 2
}
```

预算具体数值默认不进入 Durable Event；需要配置快照时读取当前 Policy。

---

## 15. Delivery Lease Lifecycle v1

### created

```json
{
  "lease_id": "uuid",
  "attachment_id": "uuid",
  "purpose": "PLAYBACK",
  "expires_at": "2026-09-02T12:05:00Z"
}
```

### released

```json
{
  "lease_id": "uuid",
  "attachment_id": "uuid"
}
```

### expired

```json
{
  "lease_id": "uuid",
  "attachment_id": "uuid"
}
```

Lease renew/heartbeat 不产生 Durable Event。

---

## 16. Compatibility Rules

1. 所有 ID 使用 UUID 字符串表达。
2. 所有时间使用 RFC 3339 带时区时间点。
3. 未知 Optional Field 消费者必须安全忽略。
4. 新增 Optional Field 可以保持 v1；删除、改类型、改语义必须升级 schema_version。
5. Event 只表达事实，不作为获取 Signed URL / Provider Credential 的通道。
6. 同一个 Restore Operation 被多个 Request 共享时，Operation Lifecycle Event 仍只产生一次。
