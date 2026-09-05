# Ikaros V2 P0 需求 / 契约可追溯矩阵

| 项目 | 内容 |
|---|---|
| 基线 | `v2-p0-foundation-0.2` |
| 状态 | 已接受 / 工程可追溯基线 |
| PRD | `Product-Requirements-Document.md` |
| 契约目录 | `contracts/P0-Command-Query-Event-Catalog.md` |
| 数据库 | `database/P0-Database-Schema-Design.md` |
| OpenAPI | `contracts/openapi-v2-p0.yaml` + `contracts/openapi-v2-p0-contract-convergence.yaml` |
| HTTP 注册表 | `contracts/P0-HTTP-Operation-Registry.yaml` |
| 验收测试 | `testing/P0-Acceptance-Invariant-Test-Matrix.md` |

> 本矩阵冻结 P0 Engineering Foundation / Core Platform 的首批实现切片，不提前伪造 Phase 1+ 专业领域中尚未冻结的 Schema/API。
>
> Identity 登录认证采用无状态 JWT。P0 不建立 Login Session / Security Session 持久化实体，不保存 Session / Refresh Token Digest 作为登录态；用户级 Token 提前失效统一通过 `security_version`。

## 1. 可追溯规则

```text
PRD / 系统需求
 -> 子系统不变量
 -> Command / Query
 -> 权限 / 安全策略
 -> 数据库约束 / 事务边界
 -> HTTP operation（公开时）
 -> Durable Event（产生持久事实时）
 -> Acceptance Test ID
```

所有公开 HTTP operation 都必须登记到 `contracts/P0-HTTP-Operation-Registry.yaml`。本轮新增 operation 还必须在 OpenAPI 中携带 `x-ikaros-contract-id`。

## 2. P0 可追溯切片

| 需求 | 契约 | 数据库 / 事务 | 公开 HTTP | 事件 | 验收 |
|---|---|---|---|---|---|
| `FR-LIB-01` Resource 浏览 | `resource.list-resources`, `resource.get-resource` | `resource.resource` | `listResources`, `getResource` | 只读 | `P0-RES-001`, `P0-API-004`, `P0-SEC-004/005` |
| `FR-LIB-03` Collection | Collection 创建/添加/移除/列表契约 | `resource.collection`, `resource.collection_member` | `listCollections` | `resource.collection.*` | `P0-RES-021~024`, `P0-CON-004` |
| `FR-LIB-05` 多标题 | Resource 创建 / Title 查询 | `resource.resource_title` | 初始标题内嵌在 `createResource` | `resource.resource.created` | `P0-RES-011` |
| `FR-LIB-06` External Identity | 绑定/解绑/查找 | `resource.external_identity` 唯一键 | `findResourceByExternalIdentity` | `resource.external-identity.*` | `P0-RES-016~018`, `P0-CON-002` |
| Resource 生命周期 | 归档/恢复/移入回收站 | Resource Version + Outbox 事务 | `archiveResource`, `restoreResource`, `trashResource` | 生命周期事件 | `P0-RES-003~010`, `P0-CON-001` |
| 用户 Resource 状态 | 设置/获取状态 | `resource.user_resource_state` | `getResourceUserState` | `resource.user-state.changed` | `P0-RES-027~029` |
| `FR-STORAGE-01` Attachment / Blob 解耦 | Attachment 创建/读取/内容访问 | Attachment + Blob + Placement | `getAttachment`, `getAttachmentContent` | `storage.attachment.*` | `P0-STO-001~007`, `P0-API-013` |
| `FR-STORAGE-02` 多级对象存储 | Provider/Placement 契约 | Provider + Placement | Provider、Placement 的 list/get | `storage.provider.*` | `P0-STO-006/007/018/019` |
| `FR-STORAGE-06` 内容完整性 | `storage.verify-blob` | Blob 完整性状态 | 内部 / 异步 | verified / integrity-failed | `P0-STO-008~010`, `P0-REC-007` |
| Blob GC 安全 | `storage.request-blob-gc` | 引用复核 + Background Task | `contract-deferred` | gc-requested / purged | `P0-STO-011~015`, `P0-CON-007` |
| Background Task | get/list/attempts/cancel/retry | Task + 不可变 Attempt 历史 | get/list/attempts/cancel | `operations.background-task.*` | `P0-TASK-001~012`、恢复类门禁 |
| `FR-AUTH-01` Identity | Current User / JWT Validation / `identity.invalidate-user-tokens` | User + `security_version`；无 Login Session / Token Digest 登录态表 | `getCurrentUser`, `invalidateCurrentUserTokens` | `identity.user.tokens-invalidated` + Identity 事件 | `P0-ID-009~015` |
| `FR-AUTH-02` Authorization | Role / Permission / User 契约 | Permission / Role / Binding 表 | Users/Roles/Permissions 查询 | Role/User 事件 | `P0-ID-004~008`, `P0-API-014` |
| Durable Event | Event Envelope + Producer/Consumer 矩阵 | Outbox + Inbox 原子性 | N/A | 42 个 P0 v1 Event Type | `P0-EVT-001~014`, `P0-REC-001~005` |
| `FR-PLUGIN-01/02` Plugin 边界 | Runtime/Capability/Permission 契约 | Plugin 自有持久化边界 | N/A | 仅通过 Capability 中介 | `P0-PLG-001~008`, `P0-SEC-008` |

## 3. Phase 1 前已知需补充的内容

对应功能开工前必须补齐：

- Collection hierarchy mutation；
- Resource Relation mutation/query；
- 独立 Title/Alias mutation（若首批 UI 需要）；
- 当前 `contract-deferred` 的公开 mutation surface。

Personal Drive 与各 Professional Domain 仍必须分别满足 Roadmap Definition of Ready：Schema + Command/Query + Event + Permission + OpenAPI（公开时）+ Acceptance。

## 4. 变更规则

P0 语义变更必须同步所有适用层。暂不适用时应明确写 `N/A` 或 `contract-deferred`，不得把空白解释为“实现可以自行决定”。
