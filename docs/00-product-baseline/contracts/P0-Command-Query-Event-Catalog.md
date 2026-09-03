# Ikaros V2 P0 Command / Query / Event Catalog

| 项目 | 内容 |
|---|---|
| 文档名称 | P0 Command / Query / Event Catalog |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 状态 | Draft / Implementation Contract |
| API 基线 | `../API-Convention-Design.md` |
| Integration 基线 | `../Platform-Integration-Automation-Design.md` |
| Database 基线 | `../database/P0-Database-Schema-Design.md` |
| OpenAPI | `openapi-v2-p0.yaml` |

> 本文档把 P0 领域能力从“概念上存在 Command / Query / Event”收敛为可编码、可授权、可测试、可映射到 HTTP/OpenAPI 的稳定 Catalog。
>
> HTTP 不是内部业务边界。Controller 必须调用这里定义的 Application Command / Query；模块间调用同样使用公开 Application API / Capability，不允许因为模块化单体而直接写对方 Repository。

---

## 1. Contract Naming

### 1.1 Command ID

```text
<owner>.<verb>-<object>
```

例如：

```text
resource.create-resource
resource.archive-resource
storage.create-attachment
identity.disable-user
operations.cancel-background-task
```

Command ID 是内部契约标识，不直接等于 Java 类名。

### 1.2 Query ID

```text
<owner>.get-<object>
<owner>.list-<objects>
<owner>.find-<object>-by-<key>
```

### 1.3 Event Type

```text
<owner>.<aggregate>.<fact>
```

Event Type 使用过去式事实语义：

```text
resource.resource.created
resource.resource.archived
storage.attachment.created
identity.user.disabled
```

禁止使用命令式事件名：

```text
resource.resource.create     # wrong
storage.blob.verify          # wrong
```

### 1.4 Event Version

Event Schema Version 从 `1` 开始递增。

同一个 `event_type` 的兼容字段新增可以保持当前 major contract version；删除字段、改变含义、改变类型必须升级 Schema Version，并保持消费者兼容窗口。

---

# Part A — Resource Commands

## 2. Resource Command Catalog

| Command ID | Permission | Idempotency | Concurrency | Result | Events |
|---|---|---|---|---|---|
| `resource.create-resource` | `resource.create` | REQUIRED | N/A | Resource | `resource.resource.created` |
| `resource.update-resource` | `resource.update` + ACL | OPTIONAL | expected version REQUIRED | Resource | `resource.resource.updated` |
| `resource.archive-resource` | `resource.archive` + ACL | NATURAL | expected version REQUIRED | Resource | `resource.resource.archived` |
| `resource.restore-resource` | `resource.update` + ACL | NATURAL | expected version REQUIRED | Resource | `resource.resource.restored` |
| `resource.trash-resource` | `resource.delete` + ACL | NATURAL | expected version REQUIRED | Resource | `resource.resource.trashed` |
| `resource.attach-external-identity` | `resource.update` + ACL | NATURAL | resource version | ExternalIdentity | `resource.external-identity.attached` |
| `resource.detach-external-identity` | `resource.update` + ACL | NATURAL | resource version | void | `resource.external-identity.detached` |
| `resource.create-tag` | `resource.update` | REQUIRED | N/A | Tag | `resource.tag.created` |
| `resource.add-tag` | `resource.update` + ACL | NATURAL | resource version | void | `resource.tag.added` |
| `resource.remove-tag` | `resource.update` + ACL | NATURAL | resource version | void | `resource.tag.removed` |
| `resource.create-collection` | `resource.update` | REQUIRED | N/A | Collection | `resource.collection.created` |
| `resource.add-collection-member` | `resource.update` + Collection ACL | NATURAL | collection version | void | `resource.collection.member-added` |
| `resource.remove-collection-member` | `resource.update` + Collection ACL | NATURAL | collection version | void | `resource.collection.member-removed` |
| `resource.set-user-state` | authenticated user | OPTIONAL | expected state version | UserResourceState | `resource.user-state.changed` |

### 2.1 `resource.create-resource`

Input：

```text
id?                  uuid; caller-supplied UUIDv7 allowed for offline/import idempotency
resource_type        stable string
primary_title        text?
summary              text?
data_classification  PUBLIC | SHARED | PRIVATE | SENSITIVE | SECURE
titles[]             optional initial title records
```

Rules：

1. API POST 必须支持 `Idempotency-Key`。
2. 如果 caller 提供 `id`，重复创建相同 ID 且语义一致时可以返回现有结果；冲突内容返回 `409`。
3. Resource + initial titles + Outbox Event 在同一事务提交。
4. Plugin-defined Resource Type 必须是 namespaced type。

### 2.2 `resource.update-resource`

Input：

```text
resource_id
expected_version
patch
```

`patch` 只允许普通可编辑字段，不允许通过它直接修改：

- lifecycle status；
- Resource Type 的领域转换；
- External Identity；
- ACL；
- Blob / Attachment；
- 专业领域状态。

Lost Update 返回 `409 resource.version-conflict`。

### 2.3 Archive / Restore / Trash

均为显式 Command，不通过通用 PATCH 设置 `lifecycle_status`。

Command 必须：

- 检查 expected version；
- 检查最终对象级授权；
- 记录 actor / correlation；
- 同事务写 Outbox；
- 不直接删除 Blob。

---

# Part B — Resource Queries

## 3. Resource Query Catalog

| Query ID | Permission | Pagination | HTTP |
|---|---|---|---|
| `resource.get-resource` | `resource.read` + ACL | N/A | `GET /resources/{resource_id}` |
| `resource.list-resources` | `resource.read` + per-item visibility | cursor | `GET /resources` |
| `resource.find-by-external-identity` | `resource.read` + ACL | N/A | `GET /resources:by-external-identity` |
| `resource.list-resource-titles` | `resource.read` + ACL | cursor | embedded / subresource |
| `resource.list-tags` | authenticated + scope | cursor | `GET /tags` |
| `resource.list-collections` | authenticated + ACL | cursor | `GET /collections` |
| `resource.get-user-state` | same user / privileged | N/A | `GET /resources/{id}/user-state` |

### 3.1 Cursor Contract

P0 Resource List 默认：

```text
sort = updated_at:desc,id:desc
```

Cursor 必须封装最后一行的：

```text
updated_at
id
query fingerprint / filter contract version
```

Cursor 对客户端 opaque。

---

# Part C — Storage Commands

## 4. Storage Command Catalog

| Command ID | Permission | Idempotency | Async | Events |
|---|---|---|---:|---|
| `storage.create-attachment` | `storage.attachment.create` + target ACL | REQUIRED | upload bytes may be async | `storage.attachment.created` |
| `storage.archive-attachment` | owner-domain permission | NATURAL | no | `storage.attachment.archived` |
| `storage.trash-attachment` | owner-domain permission | NATURAL | no | `storage.attachment.trashed` |
| `storage.verify-blob` | `storage.provider.manage` or system | REQUIRED | yes | `storage.blob.verified`, `storage.blob.integrity-failed` |
| `storage.create-provider` | `storage.provider.manage` | REQUIRED | no | `storage.provider.created` |
| `storage.update-provider` | `storage.provider.manage` | OPTIONAL | no | `storage.provider.updated` |
| `storage.enable-provider` | `storage.provider.manage` | NATURAL | no | `storage.provider.enabled` |
| `storage.disable-provider` | `storage.provider.manage` | NATURAL | maybe drain precondition | `storage.provider.disabled` |
| `storage.request-provider-drain` | `storage.provider.manage` | REQUIRED | yes | `storage.provider.drain-requested` |
| `storage.create-delivery-provider` | `storage.provider.manage` | REQUIRED | no | `storage.delivery-provider.created` |
| `storage.update-delivery-provider` | `storage.provider.manage` | REQUIRED | no | `storage.delivery-provider.updated` |
| `storage.probe-delivery-provider` | `storage.provider.manage` | REQUIRED | yes | `storage.delivery-provider.probe-requested` |
| `storage.rotate-delivery-signing-key` | `storage.provider.manage` | REQUIRED | yes | `storage.delivery-provider.signing-key-rotation-requested` |
| `storage.request-blob-gc` | `storage.blob.gc` | REQUIRED | yes | `storage.blob.gc-requested` |

### 4.1 Create Attachment Boundary

P0 API 不允许调用方提交永久 filesystem path / object key 作为 Attachment identity。

业务结果：

```text
Attachment
  -> immutable Blob
  -> one or more Placement
```

创建操作必须接受内容摘要 / 大小校验结果，或由受控上传流程产生它们。

Storage Credential 永远使用 Secret Reference。

### 4.2 GC

`storage.request-blob-gc` 只创建受控 Background Task。

Task 执行前必须重新检查：

- Attachment references；
- Retention Hold；
- Revision / Backup / Snapshot references；
- active Placement；
- concurrent restore/migration。

API 返回 `202 Accepted + background_task_id`。

---

# Part D — Storage Queries

## 5. Storage Query Catalog

| Query ID | Permission | HTTP |
|---|---|---|
| `storage.get-attachment` | `storage.attachment.read` + source ACL | `GET /attachments/{attachment_id}` |
| `storage.get-attachment-content` | same + download policy | `GET /attachments/{attachment_id}/content` |
| `storage.get-blob` | admin/system only | internal / admin |
| `storage.list-blob-placements` | `storage.provider.read` | `GET /admin/blobs/{blob_id}/placements` |
| `storage.list-providers` | `storage.provider.read` | `GET /admin/storage-providers` |
| `storage.get-provider` | `storage.provider.read` | `GET /admin/storage-providers/{provider_id}` |

Attachment Content Query 必须支持 HTTP Range，并在返回内容前重新执行当前授权判断。

---

# Part E — Operations Commands / Queries

## 6. Background Task Commands

| Command ID | Permission | Result | Event |
|---|---|---|---|
| `operations.cancel-background-task` | task owner or `platform.task.cancel` | Task | `operations.background-task.cancel-requested` |
| `operations.retry-background-task` | `platform.task.cancel` or system policy | new Attempt / Task state | `operations.background-task.retry-requested` |

取消语义：

- `PENDING` 可以直接进入 `CANCELLED`；
- `RUNNING` 记录 cancel request，由 Handler 协作取消；
- 不支持安全取消的 Handler 可以保持 Running 到自然终止，但必须暴露 `cancellable=false`；
- `CANCELLED != FAILED`。

## 7. Background Task Queries

| Query ID | Permission | HTTP |
|---|---|---|
| `operations.get-background-task` | actor/subject permission or `platform.task.read` | `GET /background-tasks/{task_id}` |
| `operations.list-background-tasks` | `platform.task.read` for global list | `GET /background-tasks` |
| `operations.list-task-attempts` | same as task | `GET /background-tasks/{task_id}/attempts` |

---

# Part F — Identity / Authorization Commands

## 8. Identity Command Catalog

| Command ID | Permission | Step-up | Events |
|---|---|---:|---|
| `identity.create-user` | `identity.user.manage` | policy | `identity.user.created` |
| `identity.disable-user` | `identity.user.manage` | REQUIRED for privileged targets | `identity.user.disabled` |
| `identity.enable-user` | `identity.user.manage` | policy | `identity.user.enabled` |
| `identity.create-role` | `identity.role.manage` | policy | `identity.role.created` |
| `identity.replace-role-permissions` | `identity.role.manage` | REQUIRED | `identity.role.permissions-replaced` |
| `identity.assign-role` | `identity.role.manage` | policy | `identity.user.role-assigned` |
| `identity.remove-role` | `identity.role.manage` | policy | `identity.user.role-removed` |
| `identity.revoke-session` | current user or `identity.user.manage` | policy | `identity.session.revoked` |
| `identity.revoke-all-user-sessions` | current user or `identity.user.manage` | REQUIRED | `identity.user.sessions-revoked` |

Permission Registry 本身默认来自 code + deterministic migration seed。

P0 不提供任意 HTTP CRUD 修改 `permission_registry` 的能力。

---

## 9. Identity Query Catalog

| Query ID | Permission | HTTP |
|---|---|---|
| `identity.get-current-user` | authenticated | `GET /me` |
| `identity.list-users` | `identity.user.read` | `GET /admin/users` |
| `identity.get-user` | self or `identity.user.read` | `GET /admin/users/{user_id}` |
| `identity.list-roles` | `identity.user.read` | `GET /admin/roles` |
| `identity.list-permissions` | `identity.user.read` | `GET /admin/permissions` |
| `identity.list-sessions` | self or admin policy | `GET /me/sessions` / admin equivalent |

---

# Part G — P0 Event Catalog

## 10. Event Envelope v1

所有 P0 Durable Event 使用统一 Envelope：

```json
{
  "event_id": "019...",
  "event_type": "resource.resource.created",
  "schema_version": 1,
  "occurred_at": "2026-08-31T12:00:00Z",
  "producer_subsystem": "resource",
  "actor": {
    "type": "user",
    "id": "019..."
  },
  "subject": {
    "type": "resource",
    "id": "019..."
  },
  "correlation_id": "019...",
  "causation_id": null,
  "payload": {}
}
```

Rules：

- `event_id` UUIDv7；
- Envelope 不包含 dispatcher attempt；
- payload 不携带 Secret / Secure Domain plaintext；
- consumer 不依赖数据库表字段；
- unknown optional fields must be ignored safely；
- at-least-once delivery；
- consumer key + event ID idempotency。

---

## 11. Resource Events

| Event Type | v | Subject | Minimum Payload |
|---|---:|---|---|
| `resource.resource.created` | 1 | resource | `resource_id, resource_type, lifecycle_status, version` |
| `resource.resource.updated` | 1 | resource | `resource_id, changed_fields[], version` |
| `resource.resource.archived` | 1 | resource | `resource_id, previous_status, version` |
| `resource.resource.restored` | 1 | resource | `resource_id, previous_status, version` |
| `resource.resource.trashed` | 1 | resource | `resource_id, version` |
| `resource.external-identity.attached` | 1 | resource | `resource_id, provider, namespace, object_type, external_id` |
| `resource.external-identity.detached` | 1 | resource | same identity key |
| `resource.tag.created` | 1 | tag | `tag_id, scope_key` |
| `resource.tag.added` | 1 | resource | `resource_id, tag_id` |
| `resource.tag.removed` | 1 | resource | `resource_id, tag_id` |
| `resource.collection.created` | 1 | collection | `collection_id, kind, mode` |
| `resource.collection.member-added` | 1 | collection | `collection_id, resource_id` |
| `resource.collection.member-removed` | 1 | collection | `collection_id, resource_id` |
| `resource.user-state.changed` | 1 | resource | `user_id, resource_id, changed_fields[], version` |

`resource.resource.updated` 不发送完整 Resource Snapshot。

Search / Analytics 等消费者需要完整读取时，通过 Query API/Capability 获取当前授权允许的数据。

---

## 12. Storage Events

| Event Type | v | Minimum Payload |
|---|---:|---|
| `storage.attachment.created` | 1 | `attachment_id, blob_id, usage_kind, data_classification` |
| `storage.attachment.archived` | 1 | `attachment_id` |
| `storage.attachment.trashed` | 1 | `attachment_id` |
| `storage.blob.verified` | 1 | `blob_id, integrity_status, verified_at` |
| `storage.blob.integrity-failed` | 1 | `blob_id, integrity_status` |
| `storage.provider.created` | 1 | `provider_id, provider_type, tier` |
| `storage.provider.updated` | 1 | `provider_id, changed_fields[]` |
| `storage.provider.enabled` | 1 | `provider_id` |
| `storage.provider.disabled` | 1 | `provider_id` |
| `storage.provider.drain-requested` | 1 | `provider_id, task_id` |
| `storage.blob.gc-requested` | 1 | `blob_id, task_id` |
| `storage.blob.purged` | 1 | `blob_id, purged_placement_count` |

Object Key、Credential、signed URL 不进入普通 Event Payload。

---

## 13. Operations Events

| Event Type | v | Minimum Payload |
|---|---:|---|
| `operations.background-task.created` | 1 | `task_id, task_type, status` |
| `operations.background-task.started` | 1 | `task_id, attempt_no` |
| `operations.background-task.succeeded` | 1 | `task_id, attempt_no` |
| `operations.background-task.failed` | 1 | `task_id, attempt_no, error_classification, retryable` |
| `operations.background-task.cancel-requested` | 1 | `task_id` |
| `operations.background-task.cancelled` | 1 | `task_id, attempt_no?` |
| `operations.background-task.timed-out` | 1 | `task_id, attempt_no` |
| `operations.background-task.retry-requested` | 1 | `task_id, next_attempt_no` |

Error Event 只包含可安全公开的 classification / summary，不复制 stack trace。

---

## 14. Identity Events

| Event Type | v | Minimum Payload |
|---|---:|---|
| `identity.user.created` | 1 | `user_id` |
| `identity.user.disabled` | 1 | `user_id, security_version` |
| `identity.user.enabled` | 1 | `user_id, security_version` |
| `identity.role.created` | 1 | `role_id, role_key` |
| `identity.role.permissions-replaced` | 1 | `role_id, permission_keys[]` |
| `identity.user.role-assigned` | 1 | `user_id, role_id` |
| `identity.user.role-removed` | 1 | `user_id, role_id` |
| `identity.session.revoked` | 1 | `session_id, user_id` |
| `identity.user.sessions-revoked` | 1 | `user_id, security_version` |

禁止 Event 包含：

- password hash；
- token digest；
- OTP；
- credential；
- private session metadata。

---

# Part H — Producer / Consumer Matrix

## 15. Initial P0 Consumer Matrix

| Event | Producer | Required Consumers | Optional Consumers |
|---|---|---|---|
| `resource.resource.*` | Resource | Search projector | Analytics, Automation, Plugin |
| `resource.tag.*` | Resource | Search projector | Analytics |
| `resource.user-state.changed` | Resource | none | Analytics, Automation |
| `storage.attachment.*` | Storage | none | Search metadata, Analytics |
| `storage.blob.integrity-failed` | Storage | Operations alerting | Notification |
| `storage.provider.*` | Storage | Operations projection | Audit/Analytics |
| `operations.background-task.*` | Operations | none | Notification, Analytics |
| `identity.user.disabled` | Identity | session/security invalidation | Audit, Notification |
| `identity.role.permissions-replaced` | Identity | authorization cache invalidation | Audit |

“Required Consumer”失败不会回滚 producer 已提交事实，但必须进入 retry / DLQ / reconciliation 可观测流程。

---

# Part I — HTTP / OpenAPI Mapping

## 16. P0 HTTP Mapping

OpenAPI Source of Truth：

```text
docs/00-product-baseline/contracts/openapi-v2-p0.yaml
```

P0 Operation ID 必须映射到 Catalog：

| HTTP | operationId | Application Contract |
|---|---|---|
| `POST /api/resources` | `createResource` | `resource.create-resource` |
| `GET /api/resources` | `listResources` | `resource.list-resources` |
| `GET /api/resources/{resource_id}` | `getResource` | `resource.get-resource` |
| `PATCH /api/resources/{resource_id}` | `updateResource` | `resource.update-resource` |
| `POST /api/resources/{resource_id}/actions/archive` | `archiveResource` | `resource.archive-resource` |
| `POST /api/resources/{resource_id}/actions/restore` | `restoreResource` | `resource.restore-resource` |
| `GET /api/attachments/{attachment_id}` | `getAttachment` | `storage.get-attachment` |
| `GET /api/attachments/{attachment_id}/content` | `getAttachmentContent` | `storage.get-attachment-content` |
| `GET /api/background-tasks/{task_id}` | `getBackgroundTask` | `operations.get-background-task` |
| `POST /api/background-tasks/{task_id}/actions/cancel` | `cancelBackgroundTask` | `operations.cancel-background-task` |
| `GET /api/admin/storage-providers` | `listStorageProviders` | `storage.list-providers` |
| `POST /api/admin/storage-providers` | `createStorageProvider` | `storage.create-provider` |
| `GET /api/admin/users` | `listUsers` | `identity.list-users` |
| `POST /api/admin/users` | `createUser` | `identity.create-user` |
| `GET /api/admin/roles` | `listRoles` | `identity.list-roles` |
| `GET /api/admin/permissions` | `listPermissions` | `identity.list-permissions` |

后续增加 endpoint 时必须先存在对应 Query / Command Contract，禁止 Controller-first。

---

## 17. Error Codes

P0 至少稳定以下 machine-readable error codes：

```text
request.invalid
request.idempotency-conflict
authentication.required
authorization.denied
verification.required
resource.not-found
resource.version-conflict
resource.external-identity-conflict
resource.invalid-lifecycle-transition
storage.attachment-not-found
storage.blob-not-available
storage.blob-integrity-failed
storage.provider-disabled
storage.gc-blocked-by-reference
background-task.not-found
background-task.not-cancellable
identity.user-not-found
identity.username-conflict
identity.email-conflict
identity.role-not-found
identity.permission-invalid
```

HTTP status 与业务 error code 分离。

---

## 18. Idempotency Contract

需要 `Idempotency-Key` 的 POST：

- Create Resource；
- Create Tag / Collection；
- Create Attachment upload/materialization；
- Create Storage Provider；
- Request Blob GC；
- Provider Drain；
- Create User；
- Create Role。

服务端必须把 idempotency key 与：

```text
principal
operation id
canonical request fingerprint
```

绑定。

同 key + 相同 fingerprint：返回原结果或等价结果。

同 key + 不同 fingerprint：`409 request.idempotency-conflict`。

---

## 19. Concurrency Contract

普通 mutable aggregate 更新必须使用 ETag：

```http
ETag: "v:12"
If-Match: "v:12"
```

缺少 required `If-Match`：

```text
428 Precondition Required
```

版本冲突：

```text
412 Precondition Failed
```

领域内部仍统一映射为 `*.version-conflict`。

业务历史 Revision 与 ETag Version 不得混用。

---

## 20. Async Contract

异步 Command 返回：

```http
HTTP/1.1 202 Accepted
Location: /api/background-tasks/{task_id}
```

Body：

```json
{
  "background_task_id": "019...",
  "status": "PENDING"
}
```

客户端通过 Task Query / SSE（后续扩展）观察执行状态，不轮询内部数据库状态。

---

# Part J — Contract Governance

## 21. Source of Truth Order

```text
Subsystem Design
        ↓
This Command / Query / Event Catalog
        ↓
OpenAPI
        ↓
Controller / Application Handler
        ↓
Repository / Outbox Adapter
```

如果 OpenAPI 与 Catalog 冲突：

1. 先确认是否属于 HTTP representation difference；
2. 若业务语义冲突，先修 Catalog；
3. 再同步 OpenAPI；
4. 禁止通过 Controller 特例掩盖冲突。

---

## 22. CI Contract Checks

P0 CI 后续必须增加：

- OpenAPI syntax validation；
- duplicate operationId check；
- operationId -> Command/Query mapping check；
- stable error code registry check；
- Event Type + Schema Version registry uniqueness；
- Event JSON Schema compatibility check；
- permission key existence check；
- generated client compilation check。

---

## 23. Exit Criteria

P0 Catalog + OpenAPI 只有满足以下条件才算完成：

- [x] P0 Resource / Storage / Operations / Identity Command Catalog 已定义。
- [x] P0 Query Catalog 已定义。
- [x] P0 Event Type / Version / Producer / Minimum Payload 已定义。
- [x] Initial Producer / Consumer Matrix 已定义。
- [x] HTTP operationId 与 Command / Query 映射已定义。
- [x] machine-readable OpenAPI baseline 已进入仓库。
- [x] Error Code baseline 已定义。
- [x] Idempotency / ETag / Async mapping 已定义。
- [ ] 实现阶段生成 Controller/API DTO。
- [ ] 实现阶段生成 Event payload JSON Schema 文件并加入兼容检查。
- [ ] 实现阶段将 permission catalog seed 与 API Security Requirement 自动交叉验证。
