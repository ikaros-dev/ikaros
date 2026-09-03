# Ikaros V2 P0 Media Delivery / Restore Contract Addendum

| 项目 | 内容 |
|---|---|
| 文档名称 | P0 Media Delivery / Restore Command / Query / Event / Permission Addendum |
| 适用版本 | Ikaros V2 |
| 状态 | Draft / Implementation Contract |
| 基线 | `P0-Command-Query-Event-Catalog.md` |
| Schema | `../database/P0-Media-Delivery-Restore-Schema-Addendum.md` |
| OpenAPI | `openapi-v2-p0-media-delivery-restore.yaml` |

> 本文档是 P0 Command / Query / Event Catalog 的规范性扩展，同时补充 deterministic Permission Registry seed。Controller、Media 模块、插件与 Worker 必须通过这些 Application Contract 访问 Delivery / Restore 能力，不得直接写 Storage 私有表。

---

# Part A — Permissions

## 1. Permission Registry 增量

P0 deterministic seed 增加：

| Permission Key | 适用主体 | 含义 |
|---|---|---|
| `storage.delivery.read` | admin/operator | 查看 Delivery Provider / Binding / Health |
| `storage.delivery.manage` | admin | 创建、修改、启停 Delivery Provider / Binding / Key Rotation |
| `storage.restore.request` | authenticated + Attachment ACL | 为有权读取的媒体请求 Restore |
| `storage.restore.read` | request actor / admin | 查看自己的 Restore Request / 状态 |
| `storage.restore.manage` | admin/operator | 全局查看、重试、取消、手工恢复 |
| `storage.tiering.manage` | admin/operator | Promotion / Demotion / Budget / Tier Policy |

说明：

- `IssueDeliveryGrant` 不要求普通用户拥有独立的全局 `storage.delivery.issue` 权限；它继承 `storage.attachment.read` + Source ACL + Download/Playback Policy。
- 管理 Delivery Provider 使用 `storage.delivery.manage`，不得复用过宽的 Secret 管理权限。
- 普通用户 `storage.restore.request` 仍必须经过目标 Attachment / Episode / Season 的最终 ACL 检查。
- `storage.restore.manage` 不自动授予媒体内容读取权限；管理员可以管理任务但不必能播放内容。

Built-in Role 的具体映射仍由 deterministic migration seed 决定。

---

# Part B — Commands

## 2. Delivery Provider Commands

| Command ID | Permission | Idempotency | Async | Events |
|---|---|---|---:|---|
| `storage.create-delivery-provider` | `storage.delivery.manage` | REQUIRED | no | `storage.delivery-provider.created` |
| `storage.update-delivery-provider` | `storage.delivery.manage` | OPTIONAL | no | `storage.delivery-provider.updated` |
| `storage.enable-delivery-provider` | `storage.delivery.manage` | NATURAL | no | `storage.delivery-provider.enabled` |
| `storage.disable-delivery-provider` | `storage.delivery.manage` | NATURAL | no | `storage.delivery-provider.disabled` |
| `storage.rotate-delivery-signing-key` | `storage.delivery.manage` + step-up policy | REQUIRED | maybe | `storage.delivery-provider.signing-key-rotated` |
| `storage.bind-delivery-provider` | `storage.delivery.manage` | NATURAL | no | `storage.delivery-binding.created` |
| `storage.update-delivery-binding` | `storage.delivery.manage` | OPTIONAL | no | `storage.delivery-binding.updated` |
| `storage.unbind-delivery-provider` | `storage.delivery.manage` | NATURAL | no | `storage.delivery-binding.removed` |
| `storage.probe-delivery-provider` | `storage.delivery.manage` | REQUIRED | yes | `storage.delivery-provider.probed` |

### 2.1 Key Rotation

`storage.rotate-delivery-signing-key`：

- 只接受 Secret Reference；
- 新签名立刻使用新 `key_version`；
- 可以配置短暂 overlap；
- Emergency Rotation 可以禁止旧版本；
- Event 不包含 Secret / Raw Token。

---

## 3. Restore Commands

| Command ID | Permission | Idempotency | Async | Result | Events |
|---|---|---|---:|---|---|
| `storage.request-restore` | `storage.restore.request` + target ACL | REQUIRED | yes | RestoreRequest | `storage.restore-request.requested` |
| `storage.cancel-restore-request` | request actor or `storage.restore.manage` | NATURAL | cooperative | RestoreRequest | `storage.restore-request.cancel-requested` |
| `storage.retry-restore-failed-items` | actor or `storage.restore.manage` | REQUIRED | yes | RestoreRequest | `storage.restore-request.retry-requested` |
| `storage.promote-placement` | `storage.tiering.manage` | REQUIRED | yes | BackgroundTask ref | `storage.placement.promotion-requested` |
| `storage.demote-placement` | `storage.tiering.manage` | REQUIRED | yes | BackgroundTask ref | `storage.placement.demotion-requested` |
| `storage.update-restore-budget-policy` | `storage.tiering.manage` | OPTIONAL | no | RestoreBudgetPolicy | `storage.restore-budget.updated` |

### 3.1 `storage.request-restore`

Input：

```text
scope_type: ATTACHMENT | EPISODE | SEASON | RESOURCE_SET
scope_id: uuid
restore_class?: stable provider-neutral code
idempotency_key: required at HTTP boundary
budget_override_confirmation?: token/reference
```

Rules：

1. Scope 必须解析为当前 Actor 有读取权限的 Attachment 集合。
2. 服务端先计算 Item Count / Logical Bytes，再执行 Budget Guard。
3. 相同 Idempotency Key 返回原 Restore Request。
4. 不同 Request 命中同一归档 Placement 时必须复用 Active Restore Operation。
5. 外部 Provider API 调用交给 Background Task，不在 HTTP 事务内等待恢复完成。
6. 返回 `202 Accepted`，并返回 Restore Request，而不是只返回 Background Task ID。

### 3.2 Restore Budget

Budget Decision：

```text
ACCEPTED
PARTIAL
CONFIRMED
QUEUED
REJECTED
```

自动预热超过预算时默认停止，不触发交互式确认。

### 3.3 Cancel Restore

取消 Request 时：

- Request 不再等待不需要的 Item；
- 共享 Operation 仍被其他 Request 引用时不得取消；
- Provider 不支持取消时 Operation 可以自然完成；
- Request Cancel 不得错误标记 Operation Failed。

---

## 4. Delivery Grant / Lease Commands

| Command ID | Permission | Idempotency | Async | Result | Events |
|---|---|---|---:|---|---|
| `storage.issue-delivery-grant` | `storage.attachment.read` + ACL + playback/download policy | OPTIONAL | no | DeliveryGrant | `storage.delivery-lease.created` when new lease |
| `storage.renew-delivery-lease` | same actor / lease owner | NATURAL | no | DeliveryLease | none |
| `storage.release-delivery-lease` | same actor / lease owner | NATURAL | no | void | `storage.delivery-lease.released` |

### 4.1 Issue Delivery Grant

Input：

```text
attachment_id
intent: PLAYBACK | DOWNLOAD
existing_lease_id?: uuid
requested_range_capability?: bool
client_capabilities?: object
```

Resolution：

```text
Permission
  ↓
Media/Attachment Availability
  ↓
Readable Placement
  ↓
Delivery Binding / Provider Policy
  ↓
Fallback Resolution
  ↓
Lease create/renew
  ↓
Short-lived Delivery Grant
```

Result：

```text
DeliveryGrant
├── grant_id
├── attachment_id
├── lease_id
├── delivery_provider_id
├── method
├── url
├── expires_at
├── range_supported
├── content_type
├── content_length
└── revocation_mode
```

Full URL 只存在于响应边界，不进入 Durable Event /普通日志。

### 4.2 Fallback

`PermissionDenied / ClassificationDenied / Missing / Corrupt` 不允许 fallback。

Provider / network 类错误才允许根据 Binding Policy 尝试下一个路径。

低带宽 Profile 默认：

```text
CDN -> DIRECT -> FAIL
```

禁止静默 `CDN -> SERVER_PROXY`。

---

# Part C — Queries

## 5. Availability Queries

| Query ID | Permission | HTTP |
|---|---|---|
| `storage.get-media-availability` | `storage.attachment.read` + ACL | `GET /attachments/{attachment_id}/availability` |
| `storage.get-delivery-lease` | lease actor or `storage.restore.manage` | internal / optional API |

`storage.get-media-availability` 返回稳定业务语义：

```text
READY
RESTORE_REQUIRED
RESTORING
READY_TEMPORARILY
UNAVAILABLE
MISSING
CORRUPTED
```

不得向普通客户端返回 Bucket / Object Key / Provider Credential。

---

## 6. Restore Queries

| Query ID | Permission | HTTP |
|---|---|---|
| `storage.get-restore-request` | actor or `storage.restore.read/manage` | `GET /restore-requests/{request_id}` |
| `storage.list-restore-requests` | actor-scoped or `storage.restore.manage` | `GET /restore-requests` |
| `storage.list-restore-request-items` | same as request | embedded / subresource |
| `storage.get-restore-operation` | `storage.restore.manage` | admin/internal |

Restore Request Query 可以返回：

- aggregate status；
- item count；
- ready / failed count；
- total logical bytes；
- Provider 提供的 ETA range；
- temporary copy expiry；
- per-item safe error code。

不得承诺 Provider 没有保证的精确完成时间。

---

## 7. Delivery Administration Queries

| Query ID | Permission | HTTP |
|---|---|---|
| `storage.list-delivery-providers` | `storage.delivery.read` | `GET /admin/delivery-providers` |
| `storage.get-delivery-provider` | `storage.delivery.read` | `GET /admin/delivery-providers/{id}` |
| `storage.list-delivery-bindings` | `storage.delivery.read` | admin subresource |
| `storage.get-restore-budget-policy` | `storage.delivery.read` or `storage.tiering.manage` | admin API |

---

# Part D — Durable Events

## 8. Delivery Events

| Event Type | v | Subject | Minimum Payload |
|---|---:|---|---|
| `storage.delivery-provider.created` | 1 | delivery_provider | `delivery_provider_id, provider_type` |
| `storage.delivery-provider.updated` | 1 | delivery_provider | `delivery_provider_id, changed_fields[], version` |
| `storage.delivery-provider.enabled` | 1 | delivery_provider | `delivery_provider_id` |
| `storage.delivery-provider.disabled` | 1 | delivery_provider | `delivery_provider_id` |
| `storage.delivery-provider.signing-key-rotated` | 1 | delivery_provider | `delivery_provider_id, previous_key_version, new_key_version` |
| `storage.delivery-provider.probed` | 1 | delivery_provider | `delivery_provider_id, health_status, capability_changes[]` |
| `storage.delivery-provider.degraded` | 1 | delivery_provider | `delivery_provider_id, reason_code` |
| `storage.delivery-binding.created` | 1 | delivery_binding | `binding_id, storage_provider_id, delivery_provider_id, priority` |
| `storage.delivery-binding.updated` | 1 | delivery_binding | `binding_id, changed_fields[], version` |
| `storage.delivery-binding.removed` | 1 | delivery_binding | `binding_id` |

禁止 Event Payload 包含：

- Signed URL；
- raw token；
- signing key；
- origin credential；
- full provider auth header。

---

## 9. Restore Events

| Event Type | v | Subject | Minimum Payload |
|---|---:|---|---|
| `storage.restore-request.requested` | 1 | restore_request | `request_id, scope_type, scope_id, item_count, total_bytes, budget_decision` |
| `storage.restore-request.cancel-requested` | 1 | restore_request | `request_id` |
| `storage.restore-request.retry-requested` | 1 | restore_request | `request_id, failed_item_count` |
| `storage.restore-request.completed` | 1 | restore_request | `request_id, status, ready_items, failed_items` |
| `storage.restore-operation.started` | 1 | restore_operation | `operation_id, placement_id, restore_class, size_bytes` |
| `storage.restore-operation.ready` | 1 | restore_operation | `operation_id, placement_id, restore_expires_at?` |
| `storage.restore-operation.failed` | 1 | restore_operation | `operation_id, placement_id, error_code, retryable` |
| `storage.restore-operation.expired` | 1 | restore_operation | `operation_id, placement_id` |
| `storage.restore-budget.updated` | 1 | restore_budget_policy | `policy_id, scope_type, scope_id?, version` |

多个 Restore Request 共享一个 Operation 时，Operation Event 只产生一次；消费者通过 Request Item Projection 关联业务请求。

---

## 10. Lease Events

只记录低频生命周期事实：

| Event Type | v | Subject | Minimum Payload |
|---|---:|---|---|
| `storage.delivery-lease.created` | 1 | delivery_lease | `lease_id, attachment_id, purpose, expires_at` |
| `storage.delivery-lease.released` | 1 | delivery_lease | `lease_id, attachment_id` |
| `storage.delivery-lease.expired` | 1 | delivery_lease | `lease_id, attachment_id` |

Lease Heartbeat / Renew 不产生 Durable Event，避免高频事件风暴。

---

# Part E — Error Contract

## 11. Stable Error Codes

建议补充：

```text
storage.restore-required
storage.restore-already-active
storage.restore-budget-exceeded
storage.restore-provider-unavailable
storage.restore-not-cancellable
storage.delivery-provider-unavailable
storage.delivery-no-valid-path
storage.delivery-range-not-supported
storage.delivery-grant-expired
storage.delivery-grant-revoked
storage.delivery-fallback-denied
storage.delivery-lease-expired
```

HTTP Mapping：

- `restore-required`：409 或 Availability Query 的正常状态，不返回 404；
- Budget Reject：409 / 422，Problem Details 带安全的 budget code；
- Permission：403；
- No valid delivery path：503；
- Grant expired/revoked：401/403 at delivery edge，客户端重新请求 Ikaros Grant。

---

# Part F — Security / Logging

## 12. Redaction

以下字段必须默认 Redact：

```text
DeliveryGrant.url
query token
signature
expires signature input
origin auth header
credential_ref resolved value
provider raw secret
```

日志可以记录：

```text
grant_id
provider_id
binding_id
attachment_id
lease_id
result_code
latency
bytes aggregate
```

不记录完整 URL。

---

# Part G — P0 Contract Invariants

1. `RequestRestore` 有业务 Request ID，不以 Task ID 代替。
2. 同一 Placement 的 Active Restore Operation 可以被多个 Request 共享。
3. HTTP Idempotency 与 Provider Operation 去重是两层机制。
4. `IssueDeliveryGrant` 每次都重新执行当前 Permission。
5. Delivery Fallback 不能绕过 ACL / Classification。
6. Grant Revocation Mode 必须暴露给安全策略。
7. Lease Renew 不产生高频 Durable Event。
8. Full Signed URL 不进入 Event / Audit / Analytics。
9. Restore Budget 在服务端强制执行。
10. 低带宽 Profile 的默认 Fallback 不包含 Server Proxy。
