# Ikaros V2 P0 Requirement / Contract Traceability Matrix

| 项目 | 内容 |
|---|---|
| Baseline | `v2-p0-foundation-0.2` |
| 状态 | Accepted / Engineering Traceability Baseline |
| PRD | `Product-Requirements-Document.md` |
| Catalog | `contracts/P0-Command-Query-Event-Catalog.md` |
| Database | `database/P0-Database-Schema-Design.md` |
| OpenAPI | `api/openapi-v2-p0.yaml` + `api/openapi-v2-p0-contract-convergence.yaml` |
| HTTP Registry | `contracts/P0-HTTP-Operation-Registry.yaml` |
| Acceptance | `testing/P0-Acceptance-Invariant-Test-Matrix.md` |

> 本矩阵冻结 P0 Engineering Foundation / Core Platform 首批实现切片，不提前伪造 Phase 1+ 专业领域尚未冻结的 Schema/API。

## 1. Traceability Rule

```text
PRD / System Requirement
 -> Subsystem Invariant
 -> Command / Query
 -> Permission / Security Policy
 -> DB Constraint / Transaction Boundary
 -> HTTP operation（公开时）
 -> Durable Event（产生事实时）
 -> Acceptance Test ID
```

所有 public HTTP operation 必须进入 `contracts/P0-HTTP-Operation-Registry.yaml`。本轮新增 operation 还必须在 OpenAPI 中携带 `x-ikaros-contract-id`。

## 2. P0 Traceability Slice

| Requirement | Contract | DB / Transaction | Public HTTP | Event | Acceptance |
|---|---|---|---|---|---|
| `FR-LIB-01` Resource 浏览 | `resource.list-resources`, `resource.get-resource` | `resource.resource` | `listResources`, `getResource` | read-only | `P0-RES-001`, `P0-API-004`, `P0-SEC-004/005` |
| `FR-LIB-03` Collection | create/add/remove/list collection contracts | `resource.collection`, `resource.collection_member` | `listCollections` | `resource.collection.*` | `P0-RES-021~024`, `P0-CON-004` |
| `FR-LIB-05` 多标题 | create resource / title query | `resource.resource_title` | initial titles embedded in `createResource` | `resource.resource.created` | `P0-RES-011` |
| `FR-LIB-06` External Identity | attach/detach/find | `resource.external_identity` unique key | `findResourceByExternalIdentity` | `resource.external-identity.*` | `P0-RES-016~018`, `P0-CON-002` |
| Resource lifecycle | archive/restore/trash | resource version + outbox transaction | `archiveResource`, `restoreResource`, `trashResource` | lifecycle events | `P0-RES-003~010`, `P0-CON-001` |
| User Resource State | set/get state | `resource.user_resource_state` | `getResourceUserState` | `resource.user-state.changed` | `P0-RES-027~029` |
| `FR-STORAGE-01` Attachment / Blob 解耦 | attachment create/read/content | attachment + blob + placement | `getAttachment`, `getAttachmentContent` | `storage.attachment.*` | `P0-STO-001~007`, `P0-API-013` |
| `FR-STORAGE-02` 多级对象存储 | provider/placement contracts | provider + placement | list/get providers, placements | `storage.provider.*` | `P0-STO-006/007/018/019` |
| `FR-STORAGE-06` 内容完整性 | `storage.verify-blob` | blob integrity state | internal / async | verified / integrity-failed | `P0-STO-008~010`, `P0-REC-007` |
| Blob GC safety | `storage.request-blob-gc` | reference recheck + background task | contract-deferred | gc-requested / purged | `P0-STO-011~015`, `P0-CON-007` |
| Background Task | get/list/attempts/cancel/retry | Task + immutable Attempt history | get/list/attempts/cancel | `operations.background-task.*` | `P0-TASK-001~012`, recovery gates |
| `FR-AUTH-01` Identity | current-user/session contracts | user + session digest/security_version | `getCurrentUser`, `listSessions` | identity/session events | `P0-ID-009~014` |
| `FR-AUTH-02` Authorization | role/permission/user contracts | permission/role/binding tables | users/roles/permissions queries | role/user events | `P0-ID-004~008`, `P0-API-014` |
| Durable Event | Event Envelope + producer/consumer matrix | outbox + inbox atomicity | N/A | 43 P0 v1 event types | `P0-EVT-001~014`, `P0-REC-001~005` |
| `FR-PLUGIN-01/02` Plugin boundary | runtime/capability/permission contracts | plugin-owned persistence boundary | N/A | capability-mediated only | `P0-PLG-001~008`, `P0-SEC-008` |

## 3. Known Pre-Phase-1 Expansion

对应功能开工前必须补齐：

- Collection hierarchy mutation；
- Resource Relation mutation/query；
- 独立 Title/Alias mutation（若首批 UI 需要）；
- 当前 `contract-deferred` 的 public mutation surface。

Personal Drive 与各 Professional Domain 仍必须分别满足 Roadmap Definition of Ready：Schema + Command/Query + Event + Permission + OpenAPI（公开时）+ Acceptance。

## 4. Change Rule

P0 语义变更必须同步所有适用层。暂不适用时明确写 `N/A` 或 `contract-deferred`，不得把空白解释为实现自由。