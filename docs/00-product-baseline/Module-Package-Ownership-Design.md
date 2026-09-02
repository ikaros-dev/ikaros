# Ikaros V2 Module and Package Ownership Design

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Module and Package Ownership Design |
| 适用版本 | Ikaros V2 |
| 状态 | Draft |
| 上位设计 | `System-Overview-Design.md`、`Database-Overview-Design.md` |

> 本文档将 V2 的模块化单体原则落实为代码所有权、依赖方向与可见性边界。
>
> 目标不是提前锁死最终目录名，而是确保每个领域在 Java / Gradle / Persistence 层都有唯一 Owner，并阻止跨模块通过 Entity、Repository、SQL 或私有实现产生隐式耦合。

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
6. Gradle Module 与 Java Package 如何表达边界。

---

## 2. 推荐模块拓扑

推荐的逻辑模块如下：

```text
ikaros-v2
├── platform
│   ├── foundation
│   ├── integration
│   ├── security
│   ├── task
│   ├── plugin
│   └── operations
│
├── resource
├── storage
├── ingestion
├── drive
├── sync
├── sharing
├── search
├── backup
│
├── media
├── reading
├── music
├── photo
├── document
├── game
│
├── productivity
├── finance
├── analytics
├── ai
├── private-notes
└── password-manager
```

具体是否每个逻辑模块都对应独立 Gradle subproject，可以在实施时根据编译隔离收益决定；但无论是否独立 subproject，**逻辑边界与依赖规则都必须保持一致**。

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

Application 层可以依赖 Domain 与公开 Platform Contract，但不应被其他领域直接依赖其 implementation package。

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

### 4.1 `platform.foundation`

可以提供：

- UUIDv7；
- Clock；
- timezone abstraction；
- shared error primitive；
- correlation / request context；
- common serialization primitive；
- basic transaction abstraction。

禁止加入任何业务实体。

### 4.2 `platform.integration`

拥有：

- Event Envelope；
- Outbox Runtime；
- Event Dispatcher；
- consumer idempotency；
- Correlation / Causation；
- Automation integration primitives；
- cross-domain relation infrastructure（若采用统一平台 Relation）。

不得成为一个拥有所有业务规则的“超级服务层”。

### 4.3 `platform.security`

拥有：

- Principal；
- Authentication；
- Permission Registry；
- Security Policy；
- Step-up Verification；
- Secure Session；
- crypto capability / key management。

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
| `drive` | Drive Space、Node、File Revision、Trash、Drive Conflict、Drive Quota | Blob Placement、Device Runtime |
| `sync` | Device、Cursor、Change Feed Runtime、Pending Mutation Envelope | 各领域业务 Conflict Resolution |
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
/api/v2/resources/**      → resource
/api/v2/attachments/**    → storage
/api/v2/drive/**          → drive
/api/v2/media/**          → media
/api/v2/admin/security/** → security / operations 的明确 owner
```

Controller 可以位于统一 server adapter，但请求必须进入 Owner 的 Application API，禁止 Controller 横跨多个 Repository 完成业务事务。

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

建议至少采用两种机制之一：

1. **Gradle subproject dependency isolation**；
2. **ArchUnit / architecture test**。

应自动检测：

- `*.persistence.*` 被其他模块引用；
- `*.infrastructure.internal.*` 被跨模块引用；
- Repository 跨 Owner 注入；
- Domain 层依赖 Spring Web / DB Driver；
- Plugin API 依赖 Server internal package。

推荐 CI 将 Boundary Violation 作为失败条件。

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

对外 contract 尽量放入独立 package，并使 implementation package 不作为其他模块编译依赖。

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
