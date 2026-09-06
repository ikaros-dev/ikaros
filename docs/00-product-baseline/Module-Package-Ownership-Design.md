# Ikaros V2 Module and Package Ownership Design

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Module and Package Ownership Design |
| 适用版本 | Ikaros V2 |
| 状态 | Draft |
| 上位设计 | `System-Overview-Design.md`、`Database-Overview-Design.md` |

> 本文档将 V2 的模块化单体原则落实为代码所有权、依赖方向与可见性边界。
>
> 目标不是提前锁死最终目录名，而是确保每个领域在 Java Package / Spring / Persistence 层都有唯一 Owner，并阻止跨模块通过 Entity、Repository、SQL 或私有实现产生隐式耦合。

---

## 1. 设计目标

V2 采用 Modular Monolith。模块化单体必须同时满足：

- 一个进程可以承载多个领域；
- 每个领域拥有清晰的代码与数据所有权；
- 模块之间通过稳定 Application Contract 协作；
- 内部实现默认不可见；
- 未来拆分 Worker / 独立进程时，不需要重新发明业务契约。

本文档重点回答：

1. 哪些模块是平台 Foundation；
2. 哪些模块拥有业务真相；
3. 模块之间允许依赖什么；
4. 哪些依赖明确禁止；
5. API、Application、Domain、Persistence 如何分层；
6. Maven Multi-Module 中的 Java Package 与 Spring 组装如何表达和强制模块边界。

---

## 2. 推荐模块拓扑

推荐的逻辑模块如下：

```text
ikaros
├── application
├── common-api
├── common
├── integration-api
├── integration
├── authentication-api
├── authentication
├── authorization-api
├── authorization
├── operations-api
├── operations
├── resource-api
├── resource
├── storage-api
├── storage
├── ingestion
├── drive
├── sync-api
├── sync
├── sharing
├── search
├── backup
├── media-api
├── media
├── reading
├── music
├── photo
├── document
├── game
├── planning
├── finance
├── private-notes
└── password-manager
```

P0 以上述内容作为**逻辑模块拓扑**，统一由根 `pom.xml` 聚合为 Maven Multi-Module。每个业务或平台能力默认拆成 `<business-name>-api` 与不带后缀的业务实现模块。`application` 是唯一 Composition Root，`test-support` 只提供测试基础设施。所有模块共享根 POM 的项目版本，不声明独立模块版本。详细构建决策见 `adr/ADR-001-maven-multi-module.md`。

当前包归属决议：

- `collection`、`metadata`、`relation`、`progress`、`activity` 归属 `resource`，当前不独立拆分；
- `identity`、`security`、`verification` 归属认证与授权能力，按职责拆为 `authentication` 与 `authorization`；
- `audit` 归属 `operations`；
- `offline` 归属 `sync`；
- `planning` 归属 `productivity`；
- `notes` 归属 `private-notes`；
- `password` 归属 `password-manager`。

认证与授权的职责边界：

- `authentication`：用户身份、登录、注册、JWT、Principal、Token 签发与校验、OTP 和 Step-up Verification；
- `authorization`：Role、Permission、Access Control、Security Policy 和 Resource Authorization。

目标模块采用以下命名：

```text
resource-api          -> resource
storage-api           -> storage
authentication-api    -> authentication
authorization-api     -> authorization
```

`-api` 模块只暴露稳定契约；不带后缀的业务模块拥有完整实现。跨模块不得依赖其他模块的 Entity、Repository、Persistence Package、私有 SQL、内部 Service 或内部 Spring Bean。

`authentication-api` 对外提供 `AuthenticatedPrincipal` 与 `SecurityVerificationLevel`；`authorization` 实现可以依赖这些公开认证契约，但不得依赖 Authentication 实现模块。Authentication 实现通过 `authorization-api` 获取权限快照和角色成员查询能力。

Java 包命名与 Maven 模块保持业务语义一致：

```text
run.ikaros.authentication.api                 -> authentication-api
run.ikaros.authentication                     -> authentication
run.ikaros.authentication.verification        -> authentication
run.ikaros.authentication.security            -> authentication
run.ikaros.authorization.api                  -> authorization-api
run.ikaros.authorization                      -> authorization
run.ikaros.authorization.security             -> authorization
run.ikaros.resource.api                       -> resource-api
run.ikaros.resource                         -> resource
run.ikaros.collection                        -> resource
run.ikaros.metadata                          -> resource
run.ikaros.relation                           -> resource
run.ikaros.progress                            -> resource
run.ikaros.activity                            -> resource
run.ikaros.storage.api                       -> storage-api
run.ikaros.storage                           -> storage
run.ikaros.media.api                         -> media-api
run.ikaros.media                             -> media
run.ikaros.operations.api                    -> operations-api
run.ikaros.operations.task                   -> platform-operations
run.ikaros.common.api                         -> common-api
run.ikaros.common                             -> common
run.ikaros.sync.api                           -> sync-api
run.ikaros.sync                               -> sync
run.ikaros.password                           -> password-manager
run.ikaros.plugin                             -> plugin
run.ikaros.reading                            -> reading
run.ikaros.search                              -> search
run.ikaros.sharing                             -> sharing
```

`storage-api` 只暴露 Storage 的稳定业务契约：Attachment/Blob 的登记与查询、上传提交、归档/删除、Placement 管理、Delivery 能力、`AttachmentReferenceQuery` 和 `AttachmentAvailabilityQuery`。普通 `AttachmentView` 只返回 Resource 归属、文件元数据和业务可用状态，不返回 `blob_id`、Provider、`object_key` 或 Placement 明细；大对象流读取保留在实现侧的 HTTP 能力中。后台任务提交返回 `operations-api` 的 `TaskReference`，不暴露 Background Task 实体、Payload、Lease 或 Attempt。

`AttachmentReferenceQuery` 是带 `actorId` 的对象级授权能力，负责校验附件可读性及其与 Resource 的活动归属；`AttachmentAvailabilityQuery` 只返回稳定的五态业务结果。Blob、Placement、Provider、Restore Repository 和内部实体均属于 Storage 实现边界。

Storage 的 Season Restore 不得直接依赖 Media Entity 或 Repository；Storage 通过 `media-api` 的 `MediaRestoreTargetQuery` 获取已授权的 Episode Resource ID。后台任务的提交、生命周期、派发和 Handler 注册契约由 `operations-api` 提供；Task Entity、Claim/Lease/Attempt 和任务 Migration 由 `platform-operations` 的 `run.ikaros.operations.task` 所有，业务模块不得依赖其实现类型。

Drive 抽取阶段严格限定于 `drive` 自身的实现。Device、DeviceTrustState、DeviceRepository 和设备 HTTP 能力归属 `sync`；`sync-api` 仅暴露最小的 `DeviceTrustQuery`，Drive 与 Offline 只能通过该能力判断设备是否可用，不得引用 Sync 的 Entity、Repository 或信任状态实现。设备 HTTP 路由统一为 `/api/sync/devices`，Drive 不反向依赖 Offline。

Migration 也遵循相同的 Owner 边界：Common 的公共 UUID 数据库能力由 `common` 持有；各业务模块分别持有自己的业务表与约束迁移。Drive 迁移位于 `drive/src/main/resources/db/migration`，Planning 位于 `planning/src/main/resources/db/migration`，Sync/Backup、Password Manager、Plugin、Reading、Search、Sharing 的迁移由对应实现模块承载；`drive_device` 由 Sync 创建，Drive 通过后续自有迁移补充 `drive_sync_binding` 的设备外键。`application` 只聚合这些实现模块的运行时 classpath 并执行迁移，不持有业务 DDL。Plugin 与 Search 原先共用的初始迁移已按 Owner 拆分。

---

## 3. 每个业务模块的内部结构

推荐统一采用：

```text
<module>
├── api
├── application
├── domain
├── infrastructure
└── persistence
```

语义：

### 3.1 `api`

对其他模块公开的稳定契约：

- Capability Interface；
- Command / Query Contract；
- Public DTO；
- Event Contract Reference；
- Permission Key；
- Public Error Code。

不得暴露：

- JPA / R2DBC Entity；
- Repository；
- SQL DTO；
- 内部 service implementation；
- ORM annotation dependent model。

### 3.2 `application`

负责：

- Command Handler；
- Query Handler；
- transaction boundary；
- authorization orchestration；
- domain service coordination；
- Outbox append；
- background task submission。

Application 层可以依赖 Domain 与公开 Platform Contract；其他领域只能依赖该模块的 `-api`，不得依赖业务实现模块的内部 package。

### 3.3 `domain`

负责：

- aggregate；
- value object；
- invariant；
- state transition；
- domain policy；
- domain error。

Domain 层尽量不依赖 Spring、数据库驱动、HTTP、Redis、对象存储 SDK。

### 3.4 `infrastructure`

负责：

- external provider adapter；
- object storage adapter；
- third-party API adapter；
- runtime integration；
- scheduler adapter；
- framework bridge。

### 3.5 `persistence`

负责：

- DB Entity / Record；
- Repository implementation；
- SQL；
- row mapper；
- migration ownership linkage。

Persistence 是模块私有实现，其他模块不得直接依赖。

---

## 4. Platform Foundation 模块

### 4.1 `common`

可以提供：

- UUIDv7；
- Clock；
- timezone abstraction；
- `PrincipalContext` immutable value contract；
- shared error primitive（`ConflictException`、`ForbiddenException`、`NotFoundException`）；
- `PageResponse` common public response value；
- `IfMatchVersion`、`PreconditionFailedException` 和 `PreconditionRequiredException` 等稳定 HTTP 并发控制原语；
- correlation / request context；
- common serialization primitive；
- basic transaction abstraction。

`PrincipalContext` 属于 `common-api` 的公开契约。Reactor Context 的访问工具 `PrincipalContexts` 属于 `common` 实现模块，供需要读取请求级上下文的基础设施实现使用；它不是业务领域契约。

`UuidV7Generator` 属于 `common-api` 的公开能力，`DefaultUuidV7Generator` 由 `common` 提供实现。需要创建平台内部实体标识的模块只能依赖该契约，不得复制 UUIDv7 算法或直接依赖其他模块的生成实现。

跨模块复用的稳定错误原语和分页值类型属于 `common-api`；HTTP 异常映射器仍属于 `application`，不得反向进入 Common API。

禁止加入任何业务实体。

### 4.2 `platform.integration`

拥有：

- Event Envelope；
- `DurableEventPublisher` 公开事件发布契约；
- `EventAppendRequest` 与 `EventReference` 公开值类型；
- Outbox Runtime；
- Event Dispatcher；
- consumer idempotency；
- Correlation / Causation；
- Automation integration primitives；
- cross-domain relation infrastructure（若采用统一平台 Relation）。

其他模块只能依赖 `integration-api` 的事件发布契约。`OutboxEventEntity`、`OutboxEventRepository`、Inbox 和 Dispatcher 只能由 `integration` 实现模块使用；业务模块不得直接写 Outbox 或驱动派发。

`EventAppendRequest` 至少包含 `event_type`、`schema_version`、`producer_subsystem`、`subject_type`、`subject_id` 和结构化 Payload。Actor、request / correlation / causation context 由 Integration 从 Foundation 上下文补全。发布结果只返回 `EventReference`，不得暴露 Persistence 类型。

`event_outbox` 的 `producer_subsystem`、`subject_type` 和 `subject_id` 由 Integration Owner 的追加 Migration 建立。旧 `aggregate_type / aggregate_id` 字段在契约切换前保留，不能把兼容性回填和字段删除混在同一个 Migration 中。

不得成为一个拥有所有业务规则的“超级服务层”。

### 4.3 `platform.security`

Authentication 拥有：

- JWT Principal；
- 用户身份、认证、Token 签发与验证、Step-up Verification；
- HTTP Authentication Adapter；
- OTP / Step-up Verification；

Authorization 拥有：

- Permission Registry；
- Security Policy；
- Access Control；
- Resource Authorization；

Authentication 和 Authorization 均可依赖 `common-api` 的 `PrincipalContext`，但不得依赖 `PrincipalContexts` 的实现细节。

Authentication 在注册、登录和刷新 Token 时通过 `authorization-api` 的 `PermissionSnapshotQuery` 获取权限快照；用户视图中的角色编码通过 `RoleMembershipQuery` 获取。它不得直接访问 Authorization 实现模块、Role / Permission / Binding Entity 或 Repository。已签发 Access JWT 的权限快照在 Token 有效期内保持不变，权限变更不隐式提升 `security_version`。完整决策见 `adr/ADR-004-authentication-authorization-permission-snapshot.md`。

其他领域可以依赖 Security API，但不得直接读写 Security persistence。

### 4.4 `platform.task`

拥有：

- Background Task；
- Job Attempt；
- Lease；
- Retry；
- Progress；
- Cancellation。

业务模块只注册 Task Handler 或提交 Task，不直接修改 Task 表。

### 4.5 `platform.plugin`

拥有：

- Plugin Manifest；
- Runtime Lifecycle；
- API Compatibility；
- Permission Declaration；
- Extension Registry；
- Plugin Configuration Reference。

插件通过公开 Extension / Capability 与业务领域交互。

### 4.6 `platform.operations`

拥有：

- platform configuration；
- audit；
- notification metadata；
- health / alert；
- scheduled job management；
- operation log。

Operations 不因为拥有管理 UI 就拥有其他领域的业务数据。

---

## 5. 业务所有权地图

| 模块 | 唯一拥有的核心状态 | 不拥有 |
|---|---|---|
| `resource` | Resource、Collection、Tag、External Identity、Metadata Provenance、Resource Lifecycle、通用 User State | Blob、Drive Path、媒体专业结构 |
| `storage` | Attachment、Blob、Placement、Replica、Derived Attachment、Integrity | Resource Metadata、Drive Tree |
| `ingestion` | Source、Scan、Candidate、Match、Import Plan/Run | Resource 最终业务状态 |
| `drive` | Drive Space、Node、File Revision、Trash、Drive Conflict、Drive Quota、Sync Binding | Blob Placement、Device Runtime |
| `sync` | Device、Cursor、Change Feed Runtime、Pending Mutation Envelope、Offline Cache/Download | 各领域业务 Conflict Resolution |
| `sharing` | Share、Invite、Room、Membership、Presence/Room state | Resource ACL 真相本身 |
| `search` | Search Projection、Index Generation、Checkpoint | 业务真相 |
| `backup` | Restore Point、Manifest、Verification、Restore Run | Export Format 业务定义 |
| `media` | Work/Season/Episode、Media Release、Track、Subtitle、Playback Session/Progress | Blob 物理位置 |
| `reading` | Edition、Volume、Chapter、Page、Reading Locator/Progress | 通用 Attachment 存储 |
| `music` | Artist、Album、Track、Playlist、Queue、Lyrics relation | Blob 物理位置 |
| `photo` | Photo、EXIF、Album、Capture/Location projection | 原始 Blob 生命周期 |
| `document` | Working Copy、Revision、Publication、Comment/Annotation | Private Notes 密文 |
| `game` | Game、Edition、Asset semantic、Compatibility | Device Installed state truth |
| `productivity` | Task、Project、Goal、OKR、Habit、Time Block | Background Task |
| `finance` | Ledger、Account、Transaction、Budget、Reconciliation | Secret 明文 |
| `analytics` | Fact、Aggregate、Metric Definition、Projection State | 业务真相 |
| `ai` | Provider/Model metadata、Prompt/Persona integration、AI Run/Artifact metadata | 目标业务状态 |
| `private-notes` | Secure Note/Vault domain state | 通用 Document 明文索引 |
| `password-manager` | Vault Item / Secret domain state | 普通 Config Secret copy |

---

## 6. Allowed Dependency Rule

统一规则：

```text
Domain A
   ↓
Domain B.api
```

允许。

```text
Domain A
   ↓
Domain B.application/internal
```

默认禁止。

```text
Domain A
   ↓
Domain B.persistence / Repository / Entity / SQL
```

严格禁止。

### 6.1 示例

允许：

```text
media.application
  → storage.api.ResolveReadableAttachmentCapability
```

允许：

```text
drive.application
  → storage.api.CommitAttachmentCommand
```

禁止：

```text
drive
  → storage.persistence.BlobRepository
```

禁止：

```text
analytics
  → resource.persistence.ResourceEntity
```

允许：

```text
analytics
  ← resource Event
```

---

## 7. Dependency Matrix

`✅` 表示允许依赖公开 API；`E` 表示优先通过 Event；`❌` 表示禁止直接依赖内部实现。

| Caller | resource | storage | security | integration | task | sync | search | analytics |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| resource | — | ✅ | ✅ | ✅ | ✅ | E | E | E |
| storage | ✅ | — | ✅ | ✅ | ✅ | E | E | E |
| drive | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | E | E |
| media | ✅ | ✅ | ✅ | ✅ | ✅ | E | E | E |
| reading | ✅ | ✅ | ✅ | ✅ | ✅ | E | E | E |
| music | ✅ | ✅ | ✅ | ✅ | ✅ | E | E | E |
| photo | ✅ | ✅ | ✅ | ✅ | ✅ | E | E | E |
| document | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | E | E |
| search | ✅ Query only | ❌ private | ✅ | ✅ | ✅ | ❌ | — | ❌ |
| analytics | ✅ Query only | ❌ private | ✅ | ✅ | ✅ | ❌ | ❌ | — |
| ai | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ when required | ✅ | ✅ Query only |

此表只说明方向，不表示调用方可以读取目标领域任意数据；实际调用仍必须通过公开 Capability / Query。

---

## 8. 数据库访问规则

### 8.1 Repository 私有

每个 Repository 只能被 Owner Module 使用。

不得出现：

```java
class DriveService {
    private final BlobRepository blobRepository;
}
```

如果 Drive 需要提交内容，应调用 Storage Command。

Storage 等模块需要校验 Resource 归属时，只能依赖 `resource-api` 的最小 `ResourceOwnershipQuery` Capability；该 Capability 由 `resource` 实现并在内部使用 `ResourceRepository`。调用方不得直接注入或引用 `ResourceEntity`、`ResourceRepository`。

### 8.2 Cross-schema FK

跨领域 FK 只在确实能增强长期完整性、且不会制造不合理生命周期耦合时使用。

即使存在数据库 FK，业务写入仍必须通过 Owner API；FK 不代表调用方获得 Repository 权限。

### 8.3 Read Model

为了 UI 聚合可以建立：

- Application-level composition；
- dedicated projection；
- Search projection；
- Analytics projection。

禁止以“查询方便”为理由让一个领域的 Repository join 多个 Owner 的私有表并逐渐成为共享数据库服务层。

---

## 9. Event Ownership

Event 由拥有业务事实的模块发布。

例如：

```text
ResourceArchived
Producer = resource
```

```text
DriveFileRevisionCommitted
Producer = drive
```

```text
BlobCorrupted
Producer = storage
```

其他模块不能代替 Owner 发布“看起来一样”的事实事件。

消费者不得依赖 Event 的数据库实现细节，只依赖版本化 Event Contract。

---

## 10. HTTP API Ownership

每个 HTTP endpoint 必须映射到唯一 Owner Module。

例如：

```text
/api/resources/**      → resource
/api/attachments/**    → storage
/api/drive/**          → drive
/api/sync/**           → sync
/api/offline/**        → sync
/api/reading/**        → reading
/api/shares/**         → sharing
/api/password/**       → password-manager
/api/plugins/**        → plugin
/api/media/**          → media
/api/admin/security/** → security / operations 的明确 owner
```

Controller 可以位于统一 application adapter，但请求必须进入 Owner 的 Application API，禁止 Controller 横跨多个 Repository 完成业务事务。

---

## 11. Plugin 依赖规则

Plugin 不属于可信任的任意内部模块。

默认只能访问：

- Stable Plugin API；
- explicitly granted Capability；
- declared Command；
- Extension Point；
- safe configuration；
- Secret Reference resolution capability（按权限）。

禁止插件：

- 获取任意 Repository；
- 获取 DataSource 后执行任意 SQL；
- 扫描所有 Spring Bean 并调用内部 service；
- 直接读取 Secure Domain plaintext；
- 通过反射绕过公开 API 边界。

---

## 12. Build-time Boundary Enforcement

P0 使用 Maven Multi-Module，构建子模块依赖方向与 Java Package 规则共同阻止非法依赖。边界检查至少采用：

1. **Maven 子模块依赖方向、Java Package Ownership 与公开 `-api` 模块约定**；
2. **ArchUnit / architecture test，并接入 Maven `test` / `verify` 生命周期**。

应自动检测：

- `*.persistence.*` 被其他模块引用；
- `*.infrastructure.internal.*` 被跨模块引用；
- Repository 跨 Owner 注入；
- Domain 层依赖 Spring Web / DB Driver；
- Plugin API 依赖 Server internal package；
- `*.api.*` 反向依赖 implementation package；
- 业务模块反向依赖 Composition Root。

推荐 CI 将 Boundary Violation 作为失败条件。单 Maven 编译成功不能替代 Architecture Boundary 验收。

---

## 13. Worker 拆分规则

未来 Transcode、AI、OCR、Index、Archive Restore 等能力可以拆 Worker。

拆分时保持：

```text
Owner Domain
    ↓ creates durable work
Background Task / Command Contract
    ↓
Worker Adapter
    ↓
Result Command / Event
```

Worker 不因为独立部署而获得业务表直写权。

例如 Transcode Worker 可以读取被授权的 Attachment 并产生转码结果，但最终注册 Derived Attachment 仍进入 Storage / Media 的正式 Command。

---

## 14. 命名与可见性建议

推荐：

```text
run.ikaros.v2.resource.api
run.ikaros.v2.resource.application
run.ikaros.v2.resource.domain
run.ikaros.v2.resource.infrastructure
run.ikaros.v2.resource.persistence
```

若最终包名调整，不影响本设计原则。

对外 contract 尽量放入独立 package，并使 implementation package 不作为其他模块的合法依赖目标。

---

## 15. Review Checklist

任何新增代码 Review 时必须回答：

1. 这份状态的 Owner 是谁？
2. 调用方是否依赖了 Owner 的公开 API？
3. 是否直接使用了其他模块 Repository / Entity / SQL？
4. 这个跨领域动作应该是 Capability、Command 还是 Event？
5. 是否错误扩大了事务边界？
6. 是否把 Projection 当成业务真相？
7. 是否因为后台执行而绕过 Principal / Permission？
8. 新依赖未来拆 Worker 时是否还能保持契约？

如果答案无法明确，应先修正边界设计再合并实现。
