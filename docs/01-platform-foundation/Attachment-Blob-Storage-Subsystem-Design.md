# Ikaros V2 Attachment / Blob / Storage 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Attachment / Blob / Storage Subsystem Design |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 系统级上位约束 | `System-Overview-Design.md` |
| 产品基线 | `Product-Requirements-Document.md` |

> 本文档定义 Ikaros V2 中 Attachment、Blob、Blob Placement / Replica、Storage Provider、Storage Tier、缓存访问、完整性、迁移、归档恢复、保留与 Blob GC 的专项设计。
>
> 本文档以已经合并的 `System-Overview-Design.md` 为系统级上位约束；若本文档与 System Overview 冲突，以 System Overview 为准。本文档不使用 V1 的 Attachment/File、数据库表、路径模型、API 或存储实现作为 V2 兼容性约束。

---

## 1. 设计依据与边界

### 1.1 已读取的 V2 上下文

本专项设计除 `System-Overview-Design.md` 与 `Product-Requirements-Document.md` 外，还需要遵守以下现有 V2 文档已经确定的交叉边界：

- `Secure-Data-Foundation-Design.md`：Secure Domain 的 Attachment 必须以加密 Blob 落盘，Storage Provider 只持有密文字节；普通业务数据不因此自动升级为 Secure Domain。
- `Security-Identity-Authorization-Crypto-Subsystem-Design.md`：Authentication、Authorization、Step-up Verification 分离；高风险删除、密钥与 Secret 使用进入受控安全能力。
- `Private-Notes-Subsystem-Design.md`：Private Attachment 使用 Secure Blob；解密明文不得自动进入公共 Download / Cache 路径。
- `Password-Manager-Subsystem-Design.md`：Vault Attachment 使用 Secure Blob；Provider Credential 通过 `secret://` Reference 使用，优先 Use Without Reveal。
- `Personal-Finance-Accounting-Subsystem-Design.md`：普通财务收据、发票等继续使用普通 Attachment / Blob；真正 Secret 单独引用安全凭据，不把整个 Accounting Attachment 体系改成 Secure Blob。
- `Platform-Integration-Automation-Design.md`：Storage 子系统拥有 Placement、Replica、Cache 与恢复状态；跨子系统使用 Capability / Command / Event，不能直接修改 Storage 私有状态。
- `Platform-Administration-Operations-Subsystem-Design.md`：Storage Provider、Storage Tier、Server Cache 具有独立 Health；Provider 修改、恢复、永久删除与 GC 需要审计。
- `Data-Analytics-Statistics-Subsystem-Design.md`：Storage Analytics 是派生统计，不是存储真相源；必须区分 Logical Size、Physical Unique Size、Replica Physical Size 与 Cache Size。
- `AI-Intelligence-Subsystem-Design.md`：AI 不拥有 Attachment / Blob 状态；读取 Attachment 必须继承当前 Permission；删除 Blob Replica、停用 Storage Provider 等高风险动作默认需要人工确认。

### 1.2 本文档负责

本专项拥有以下领域设计责任：

1. Attachment 的统一业务身份、Blob 绑定与生命周期。
2. Blob 的不可变内容身份、摘要、大小、完整性与去重。
3. Blob Placement / Replica 的物理副本状态。
4. Storage Provider、Storage Tier 与 Provider Capability。
5. 上传、导入、接管已有对象、读取、Range 读取与临时访问授权的存储侧流程。
6. Server Cache 的存储侧语义与 Cache-first 读取编排。
7. 多副本、修复、迁移、Provider Drain、归档与恢复。
8. Retention Hold、逻辑删除、物理 GC 与不可逆清理。
9. Storage Policy 的匹配、期望副本与分层编排。
10. Attachment / Blob / Storage 对外公开的 Capability、Command、Event 语义。
11. Storage 自有 PostgreSQL Schema 的概念模型、约束、索引与 Migration Ownership。
12. Storage 相关的安全、审计、可观测性、故障恢复与 Provider 插件契约。

### 1.3 本文档不负责

以下内容由其他专项拥有，本文档只定义交接点：

- Resource / Collection / Relation 的完整领域模型与关系表结构。
- 视频、音乐、漫画、小说、图片、文档等专业媒体语义。
- Resource ACL、Role、Permission 的完整授权模型。
- Background Task / Scheduled Job 的通用执行框架。
- Backup / Restore 的完整实例级编排。
- Export / Import 的格式定义与 Export Format Version。
- Search、Analytics 的内部表结构与指标实现。
- Secure Data Foundation 的密码算法、密钥层级与解锁协议。
- 客户端 Download Manager 的完整本地数据库与 UI。
- 最终 REST Path、OpenAPI 字段命名、Java 包名、类名与 Storage SDK 具体接口签名。

---

## 2. 核心设计目标

Attachment / Blob / Storage 子系统需要同时解决“业务身份稳定”和“物理存储可演进”两个问题。

核心目标如下：

1. **业务与物理位置解耦**：任何业务模块都不能把文件路径、Bucket、Object Key、Provider URL 当作 Attachment 身份。
2. **内容不可变、身份明确**：Blob 以密码学摘要表达内容身份，UUIDv7 只表达 Blob 实体身份，两者严格分离。
3. **去重不破坏业务语义**：多个 Attachment 可以共享一个 Blob，但仍保持独立名称、用途、来源、权限和生命周期。
4. **物理副本可编排**：同一 Blob 可以拥有多个 Placement，并可在 Hot / Warm / Cold / Archive 间复制、迁移与恢复。
5. **故障可解释**：客户端能区分缓存命中、远程读取、处理中、恢复中、缺失、损坏与临时不可用。
6. **物理删除保守**：业务删除与物理字节删除完全解耦，任何 Blob GC 必须先证明没有仍需保留的有效引用或 Hold。
7. **对象存储事实与缓存分离**：持久化 Storage Tier 与访问 Cache Tier 是两套概念；Server / Client Cache 可随时淘汰和重建。
8. **Secure Domain 不降级**：Storage 能保存 Secure Blob，但不获得解密权限；普通缓存、日志、事件和 Analytics 不泄露 Secure 明文。
9. **自托管可落地**：单 Provider、单机部署必须简单；多 Provider、多副本、Worker、归档等作为可渐进启用能力。
10. **可验证与可修复**：任何长期 Blob 都有完整性校验路径，丢副本或损坏时有明确修复、降级和告警路径。

---

## 3. 系统级约束在本专项中的落地

本节只描述这些系统级规则在 Storage 领域中的具体落点，不重复其全局定义。

### 3.1 UUIDv7

以下具有独立生命周期的持久化实体使用 UUIDv7，并在 PostgreSQL 中使用原生 `uuid`：

- Attachment；
- Blob；
- Blob Placement；
- Storage Provider；
- Storage Policy；
- Ingest Session；
- Blob Retention Hold；
- 需要独立寻址的 Storage Operation / Request（若不直接复用 Background Task ID）。

Blob 的 `id` 不能替代内容摘要，内容摘要也不能替代 `id`。

### 3.2 时间与时区

所有真实时间点，例如 `created_at`、`updated_at`、`verified_at`、`restore_expires_at`、`retention_until`、`deleted_at`，统一使用 `timestamptz`。

Storage 本身的大多数保留时间按绝对时间点和持续时长计算；只有“每天凌晨执行校验”“每周迁移”等墙上时间计划交给 Scheduled Job 时，才使用应用配置时区，未显式指定时默认 UTC+8。

### 3.3 Instance 边界

Storage Provider、默认 Storage Policy、Server Cache 配置属于 Ikaros Instance 级能力。

V2 默认不因为存在多个用户就在 Storage 表中机械加入 `tenant_id`。不同用户的可见性由 Permission / ACL 与 Attachment 所处业务上下文决定，不由物理 Bucket 隔离伪装成 Tenant 模型。

### 3.4 Schema Ownership

Attachment / Blob / Storage 子系统拥有本文定义的 Storage Schema 与 Migration。

其他子系统：

- 可以持有稳定的 `attachment_id` / `blob_id` 引用或通过 Relation 建立关系；
- 不得直接 UPDATE Placement、Provider、Blob Lifecycle 等 Storage 私有表；
- 不得依赖 Object Key、私有索引或内部表连接来判断内容是否可访问；
- 必须通过 Storage Capability / Command 获取状态或请求变更。

### 3.5 数据敏感等级

Attachment 需要携带或解析有效数据敏感等级：`Public / Shared / Private / Sensitive / Secure Domain`。

该等级约束日志、缓存、事件、Analytics、AI 与访问 URL 的传播方式，但不替代 Permission / ACL。

### 3.6 契约版本

Storage 对外 Event 必须具有 Event Contract Version；公开 API 由 API Version 管理；Storage Provider 插件契约由 Plugin API Version 管理；Storage Schema 由 Database Schema Version 管理。

这些版本不得合并成一个模糊的 `version`。

### 3.7 异步状态

校验、复制、迁移、归档恢复、GC、Provider Drain、批量 Reconcile 等耗时动作统一使用 Background Task 的系统级状态语义：

`Pending / Running / Succeeded / Failed / Cancelled / TimedOut`。

如果需要 `Queued / Retrying / Cancelling / Throttled`，只能作为兼容扩展中间态；Cancelled、TimedOut 与 Failed 的语义不得混用。

---

## 4. 领域总览

```text
业务子系统 / Resource Core
          │
          │ 使用 attachment_id 建立业务关系
          ▼
      Attachment
          │ 1:1 当前内容
          ▼
         Blob
          │ 1:N
          ▼
Blob Placement / Replica
          │ N:1
          ▼
   Storage Provider
          │
          └── Storage Tier: HOT / WARM / COLD / ARCHIVE

访问加速：
Blob
 ├── Server Cache（可淘汰，不是持久化 Placement）
 └── Client Cache / Download（客户端语义，不是 Server 持久化 Placement）
```

领域职责分层：

| 概念 | 回答的问题 | 是否业务身份 | 是否内容身份 | 是否物理位置 |
|---|---|---:|---:|---:|
| Attachment | “这份内容在业务上是什么用途？” | 是 | 否 | 否 |
| Blob | “这组不可变字节是什么内容？” | 是 | 是，另有摘要 | 否 |
| Placement | “这组字节现在具体放在哪里？” | 否业务身份，是运维实体 | 否 | 是 |
| Storage Provider | “由哪个存储实现负责读写？” | 平台配置实体 | 否 | 提供物理命名空间 |
| Cache Entry | “最近是否已有可快速读取副本？” | 否 | 绑定 Blob | 临时 |

---

## 5. Attachment 设计

### 5.1 Attachment 的定位

Attachment 是业务层可引用的内容对象，统一用于：

- 视频文件；
- 音频文件；
- 字幕；
- 封面；
- 图片与漫画页；
- 电子书；
- 文档附件；
- 收据、发票；
- 游戏安装包、补丁、MOD、归档包；
- 导出包；
- AI 或媒体处理产生的可持久化派生内容。

Attachment 不保存“永久物理路径”。

### 5.2 一个 Attachment 对应一个当前 Blob

P0 规则：

> 一个已经完成物化的 Attachment 绑定一个不可变 Blob。

如果同一业务槽位需要多个版本，例如：

- 原视频与转码视频；
- 图片原图与预览；
- 文档不同 Revision 中的附件；
- 一个文件被用户替换；

应创建新的 Attachment，并由业务 Revision / Relation 表达版本、派生或替换关系，而不是原地修改 Blob 的字节内容。

这样可以保证：

- Blob 与缓存天然不可变；
- 内容摘要稳定；
- 历史引用可审计；
- GC 可以准确判断旧内容是否仍被引用。

### 5.3 Attachment 概念字段

建议概念字段：

```text
Attachment
├── id: uuid / UUIDv7
├── blob_id: uuid
├── filename: text?
├── media_type: text?
├── usage_kind: stable string code?
├── source_kind: stable string code
├── source_reference: protected text/json?
├── data_classification: stable string code
├── lifecycle: ACTIVE | ARCHIVED | TRASHED | PURGED
├── created_at: timestamptz
├── updated_at: timestamptz
├── deleted_at: timestamptz?
└── version: bigint
```

说明：

- `filename` 是业务显示元数据，不是物理对象名称；不同 Attachment 可以共享 Blob 但拥有不同文件名。
- `media_type` 是 Attachment 的内容声明/识别结果，不负责保存视频 Codec、音轨、EXIF 等专业领域数据。
- `usage_kind` 只用于通用用途提示；“字幕属于哪个视频”“漫画页序号”等关系仍由专业领域 / Relation Core 管理。
- `source_reference` 可能包含 URL、外部对象标识等敏感来源信息，不能无条件进入日志或公开 API。
- `data_classification` 是传播约束的输入之一，最终访问权限仍需 ACL / Permission。

### 5.4 Attachment Lifecycle

Attachment 生命周期建议统一为：

```text
ACTIVE
  ├──> ARCHIVED
  │      └──> ACTIVE
  └──> TRASHED
           ├──> ACTIVE
           └──> PURGED
```

其中：

- `ARCHIVED` 表示业务逻辑归档，不等于 Blob 放入 Storage `ARCHIVE` Tier。
- `TRASHED` 仍保留 Blob 引用，不能触发立即 GC。
- `PURGED` 表示 Attachment 业务身份已永久清理，其 Blob 引用才可释放。
- 是否允许 `ARCHIVED -> TRASHED`、Trash 保留多久由上层业务策略决定。

### 5.5 Attachment 不直接承担 ACL 真相

Storage 不应通过“某个 Blob 是 Private”来代替业务权限模型。

读取流程必须基于 Attachment 和调用主体：

```text
Principal
   ↓
目标 Attachment / 业务上下文
   ↓
Permission / Resource ACL
   ↓ allow
Storage Read Resolution
   ↓
Blob / Placement
```

禁止给普通客户端提供“知道 blob_id 就能直接读内容”的无授权旁路。

---

## 6. Blob 设计

### 6.1 Blob 是不可变内容对象

Blob 表示一组已经完成确认的不可变字节。

一旦 Blob 完成 Finalize，下列字段不得原地改变：

- Canonical Digest；
- Digest Algorithm；
- Size；
- 内容字节。

字节发生任何变化都必须产生新的 Blob。

### 6.2 UUIDv7 与内容身份分离

Blob 同时具有两类身份：

```text
Blob.id
= UUIDv7
= 系统实体身份

Blob.digest
= Cryptographic Hash
= 内容身份 / 完整性身份
```

不得使用 UUIDv7 判断内容相同，也不得把 Digest 当成数据库实体主键。

### 6.3 Canonical Digest

P0 建议将 `SHA-256` 作为普通 Blob 的 Canonical Digest Algorithm，同时保留算法字段以支持未来演进。

概念字段：

```text
Blob
├── id: uuid / UUIDv7
├── digest_algorithm: text       // P0: SHA-256
├── digest_value: bytea
├── size_bytes: bigint
├── integrity_state: VERIFIED | SUSPECT | CORRUPTED | UNKNOWN
├── lifecycle: ACTIVE | GC_CANDIDATE | PURGING | PURGED
├── gc_eligible_at: timestamptz?
├── created_at: timestamptz
├── updated_at: timestamptz
└── version: bigint
```

Canonical Digest 必须对“Storage 实际负责保存的字节”计算。

对于 Secure Blob：

- Storage 层看到的是密文 Envelope；
- Storage 的 Digest 对密文字节计算；
- 明文摘要若安全协议确实需要，应留在 Secure Data Boundary 内，不进入普通 Blob 元数据。

### 6.4 Blob 不保存业务名称

Blob 不应该保存：

- 业务标题；
- Resource 名称；
- 文件用途；
- 用户展示文件名；
- “这是某动画第 3 集”之类领域语义。

这些属于 Attachment 或上层业务。

### 6.5 Blob Lifecycle

Blob 生命周期与可用性分离：

```text
ACTIVE
  ↓ 无有效业务引用 + 无 Hold + 满足保留策略
GC_CANDIDATE
  ↓ 再次确认条件成立
PURGING
  ↓ 所有需要删除的 Placement 已处理
PURGED
```

`MISSING`、`CORRUPTED` 不是 Blob Lifecycle，而是内容可用性/完整性状态。

---

## 7. 去重模型

### 7.1 普通数据默认 Instance 内去重

普通非 Secure Blob 在同一个 Ikaros Instance 内以：

```text
digest_algorithm + digest_value + size_bytes + dedup_namespace
```

作为去重判定键。

P0 普通数据的 `dedup_namespace` 可以固定为 Instance Normal Namespace。

`size_bytes` 同时参与约束用于增加防御性校验，但不能替代 Digest。

### 7.2 并发去重必须由数据库约束兜底

两个并发 Ingest 可能同时计算到相同 Digest。

正确流程：

```text
Ingest A ─┐
          ├─ 计算相同 Digest
Ingest B ─┘
       ↓
尝试创建 Blob
       ↓
唯一约束仅允许一个成功
       ↓
另一个读取已存在 Blob 并绑定自己的 Attachment
```

不能依赖“先 SELECT 再 INSERT”作为唯一防重手段。

### 7.3 去重不合并 Attachment

以下情况仍然是两个 Attachment：

```text
Attachment A filename = poster.jpg
Attachment B filename = cover-copy.jpg
        │
        └──── 都引用 Blob X
```

它们可以拥有不同：

- 业务用途；
- Owner / ACL 上下文；
- 来源；
- 生命周期；
- 数据敏感等级。

### 7.4 Secure Domain 去重边界

Secure Domain 默认不得将明文 Digest 暴露给普通 Storage 层，也不得通过全局明文摘要实现跨 Vault / 跨 Secure Domain 去重。

规则：

1. Storage 将密文 Blob 当作不透明字节。
2. Secure Data Foundation 可以为某个安全域提供独立、不可推断明文的 `dedup_namespace`，也可以完全禁止去重。
3. 普通 Blob 与 Secure Blob 不进入同一去重命名空间。
4. 不允许为了节省空间而降低 Zero-knowledge / E2EE 的安全属性。

---

## 8. Blob Placement / Replica

### 8.1 Placement 的定位

Placement 表示一个 Blob 在某个 Storage Provider 中的一个具体物理副本。

概念字段：

```text
BlobPlacement
├── id: uuid / UUIDv7
├── blob_id: uuid
├── provider_id: uuid
├── storage_tier: HOT | WARM | COLD | ARCHIVE
├── object_key: text
├── provider_version_id: text?
├── state: stable string code
├── provider_checksum: text/bytea?
├── verified_at: timestamptz?
├── restore_expires_at: timestamptz?
├── created_at: timestamptz
├── updated_at: timestamptz
└── version: bigint
```

`object_key` 是 Storage 私有技术字段，不得出现在业务对象身份、Relation、收藏、搜索或客户端永久链接中。

### 8.2 Placement State

建议基础状态：

```text
PENDING
AVAILABLE
RESTORE_REQUIRED
RESTORING
MISSING
CORRUPTED
DELETING
DELETED
```

状态含义：

- `PENDING`：元数据已建立但副本尚未完成写入/验证，不能作为成功持久化依据。
- `AVAILABLE`：当前副本已经验证，可以按 Provider 能力立即读取。
- `RESTORE_REQUIRED`：对象存在于归档类层级，但读取前需要 Provider Restore。
- `RESTORING`：已经发起恢复，尚未可读。
- `MISSING`：元数据预期对象存在，但 Provider 确认对象不存在。
- `CORRUPTED`：对象存在，但完整性校验失败。
- `DELETING`：物理删除已开始，不得再作为读取候选。
- `DELETED`：物理副本已确认删除或按 Provider 删除语义完成。

Provider 临时 DOWN 不应把所有 Placement 批量改写为 `MISSING`；Provider Health 与对象事实必须分离。

### 8.3 Placement 不是 Cache Entry

Server Disk Cache 中的一份缓存不能计入“最小持久化副本数”。

只有明确注册为 Durable Storage Provider 的位置才是 Placement。

如果管理员将一块本地/挂载文件系统显式配置为 Durable Filesystem Provider，它可以成为 Placement；这与默认的 Server Work Directory / Cache Directory 是两种不同角色。

---

## 9. Storage Provider

### 9.1 Provider 类型

Storage Provider 可以由核心或插件提供，包括：

- S3 / S3-compatible Object Storage；
- NAS / 远程文件系统型 Provider；
- 显式启用的 Durable Local Filesystem Provider；
- 远程对象存储；
- Cold / Archive Provider。

默认 Server 本地工作目录不自动成为 Durable Provider。

### 9.2 Provider 概念模型

```text
StorageProvider
├── id: uuid / UUIDv7
├── key: stable instance-local code
├── provider_type: stable string code
├── implementation_ref: core/plugin identifier
├── mode: ENABLED | READ_ONLY | DRAINING | DISABLED
├── default_tier: HOT | WARM | COLD | ARCHIVE
├── configuration: jsonb           // 不含 Secret 明文
├── credential_ref: protected ref?
├── created_at: timestamptz
├── updated_at: timestamptz
└── version: bigint
```

### 9.3 管理模式与健康状态分离

Provider 管理模式：

- `ENABLED`：允许新写入、读取和维护。
- `READ_ONLY`：允许读取，禁止新的持久化写入。
- `DRAINING`：停止承接新副本，后台将现有副本迁移到满足策略的其他 Provider。
- `DISABLED`：业务访问编排不再使用；只有受控恢复/管理操作可访问。

Health 由 Operations 体系表达：`UP / DEGRADED / DOWN / UNKNOWN`。

因此：

```text
mode = ENABLED, health = DOWN
```

表示“管理员希望使用，但当前故障”；

```text
mode = DISABLED, health = UP
```

表示“技术上可达，但管理员禁止业务使用”。

### 9.4 Provider Credential

Access Key、Secret Key、Token、Password 等不得存在普通 Provider JSON 配置中。

Provider 只保存：

```text
credential_ref
```

实际 Secret 由 Secret / Password Manager 能力提供，并优先使用“Use Secret Without Reveal”模式。

### 9.5 Provider Capability Discovery

Provider 必须声明能力，而不是由 Storage Core 通过实现类型猜测。

建议能力集合：

```text
READ
RANGE_READ
WRITE
MULTIPART_WRITE
DELETE
HEAD
CHECKSUM
SERVER_SIDE_COPY
PRESIGNED_READ
PRESIGNED_WRITE
ARCHIVE_RESTORE
RESTORE_STATUS
OBJECT_VERSIONING
CAPACITY_REPORT
```

Storage Policy、Read Planner 与 Migration Planner 根据 Capability 决定可用策略。

---

## 10. Storage Tier 与 Cache Tier

### 10.1 持久化 Storage Tier

稳定基础层级：

```text
HOT
WARM
COLD
ARCHIVE
```

含义是访问成本、恢复要求和策略语义，不要求所有 Provider 都原生使用相同厂商 Storage Class 名称。

Provider Adapter 负责把 Ikaros Tier 映射到具体实现。

### 10.2 Cache Tier

Cache 与持久化 Tier 分开：

```text
Durable Storage
       ↓
Server Disk Cache（可选）
       ↓
Client Cache / User Download
```

规则：

- Cache 可淘汰、可重建。
- Cache 不计入持久化 Replica 数量。
- Cache 清理不得删除 Durable Placement。
- User Download 不等于 Cache。
- 客户端设备副本默认不成为 Server 可依赖的灾难恢复来源。

### 10.3 Client Download 与 Cache

显式 Download 表示用户要求一个可管理的长期离线副本；普通 Cache 是客户端实现细节。

Storage Server 只提供：

- 经过授权的下载内容；
- 内容版本/ETag；
- Range / Resume 能力；
- 必要的下载 Manifest / 校验信息。

“已下载”“下载目录”“用户删除本地下载”等设备状态由客户端专项负责。

---

## 11. Storage Policy

### 11.1 目标

Storage Policy 描述某类 Blob “应该拥有怎样的持久化副本”，而不是直接记录当前 Placement。

例如：

```text
原始媒体
- 至少 1 个 HOT/WARM 可读副本
- 至少 1 个独立 Provider 副本（可选）

可重建缩略图
- 至少 1 个 HOT 副本
- 允许空间压力时删除并重建

长期归档
- 1 个 ARCHIVE
- 可选 1 个 WARM
```

### 11.2 Policy 概念字段

```text
StoragePolicy
├── id: uuid / UUIDv7
├── key
├── priority
├── enabled
├── match_expression
├── desired_replica_spec
├── retention_spec
├── cache_spec
├── created_at
├── updated_at
└── version
```

`match_expression` 与 `desired_replica_spec` 必须使用版本化、可校验的结构，而不是任意可执行脚本。

### 11.3 可匹配维度

可逐步支持：

- Attachment usage / type；
- Resource Type（通过公开上下文，不直接 JOIN Resource 私表）；
- Blob size；
- Data Classification；
- 是否可重建 Derived Attachment；
- 访问热度聚合；
- 最近访问时间；
- 用户显式 Pin / Keep Hot；
- Provider 成本；
- 当前 Replica 数；
- Provider / Tier 容量。

### 11.4 高频 Access 不直接热写 Blob

播放大文件可能产生大量 Range 请求。

不得每个请求都 UPDATE `blob.last_accessed_at`，否则会形成数据库热点。

推荐：

```text
Read Event / Access Sample
      ↓
Analytics / Coarse Access Aggregate
      ↓
Storage Policy Reconciler
```

Storage Policy 使用粗粒度访问热度，不要求每个字节读取都形成强一致状态。

### 11.5 Policy Reconciliation

当前 Placement 与期望 Policy 之间的差异由异步 Reconciler 修复：

```text
Desired: HOT x1 + ARCHIVE x1
Actual:  HOT x1
        ↓
Create Replica Task
        ↓
ARCHIVE x1 AVAILABLE / RESTORE_REQUIRED
```

Policy Reconciliation 默认最终一致，不把复制动作塞入普通 Attachment 元数据事务。

---

## 12. Ingest / Upload / Import

### 12.1 Ingest Session

上传与导入先进入临时 Ingest Session，而不是直接创建可用 Blob。

```text
IngestSession
├── id: uuid / UUIDv7
├── actor_id
├── idempotency_key
├── state: OPEN | RECEIVING | FINALIZING | COMPLETED | ABORTED | EXPIRED
├── expected_size?
├── received_size
├── declared_media_type?
├── declared_digest?
├── temp_storage_ref
├── expires_at: timestamptz
├── created_at: timestamptz
├── updated_at: timestamptz
└── version: bigint
```

Ingest Session 是上传协议状态；如果 Finalize 需要耗时校验、远程复制等后台处理，对应执行仍使用 Background Task 的统一状态语义。

### 12.2 推荐流程

```text
1. Authorize Create Attachment
2. Begin Ingest（带 Idempotency Key）
3. 上传 / 拉取到临时区域
4. Finalize
   ├── 校验实际 Size
   ├── 计算 Canonical Digest
   ├── 检查完整性
   ├── 执行 Dedup
   ├── 确保至少一个满足最低策略的 Durable Placement
   └── 原子提交 Blob / Placement / Attachment / Outbox
5. 清理临时对象
6. 异步执行 Metadata / Derived / Policy Reconcile
```

### 12.3 Finalize 成功条件

普通长期 Attachment 的 Finalize 只有在以下条件全部满足时才可以标记成功：

1. Blob Digest 与 Size 已确定。
2. Blob 元数据可持久化。
3. 至少存在一个符合最低持久化要求的有效 Placement，或当前策略明确要求更高副本数且同步成功条件已经满足。
4. Attachment 与 Blob 绑定已提交。
5. 关键 Event 已进入 Durable Outbox。

禁止出现：

```text
HTTP 上传返回成功
但实际只有尚未管理的临时文件
```

### 12.4 Direct / Multipart Upload

对于大对象可以让客户端直接上传到支持的 Storage Provider，但仍需：

- 由 Server 发放短期 Upload Grant；
- Object Key 由 Storage 管理，不由客户端决定永久业务 Key；
- Finalize 后确认 Provider 对象存在；
- 获取或计算 Canonical Digest；
- 校验实际 Size；
- 只有验证通过后才将 Placement 标记为 `AVAILABLE`。

如果 Provider 的 Multipart ETag 不是 Canonical Digest，不能把 ETag 当 SHA-256 使用。

### 12.5 URL / Plugin Import

外部 URL、Plugin、远程文件是 **Ingest Source**，不是 Attachment 永久身份。

普通 URL Import 应将内容接入受管理的 Blob / Placement。

只有实现了 Storage Provider 契约、具备稳定 Object Identity 与完整性能力的远程存储，才能直接成为 Placement Provider；任意网页 URL 不属于 Storage Provider。

### 12.6 接管已有对象

管理员可以将已经存在于受管理 Provider 中的对象导入 Storage Catalog，但必须执行：

1. Provider / Object 存在性检查；
2. Size 获取；
3. Canonical Digest 计算或可信校验；
4. Dedup；
5. Placement 建立；
6. Attachment 创建或关联；
7. 审计与幂等处理。

不能仅凭 Object Key 在数据库中补一行就假定内容有效。

### 12.7 临时数据清理

临时上传、失败 Multipart、解压工作区、导入缓存都必须：

- 有明确 TTL；
- 有 Owner / Task / Session 关联；
- 可在 Server 重启后重新识别；
- 由 Scheduled Cleanup 清理；
- Secure Domain 临时数据遵守 Secure Data Foundation 的明文落盘限制。

---

## 13. Read Resolution 与访问路径

### 13.1 客户端读取 Attachment，不直接解析物理位置

标准读取入口语义：

```text
Principal + Attachment ID + Read Intent
        ↓
Permission / ACL
        ↓
Storage Resolve Read Capability
        ↓
Blob
        ↓
Best Cache / Placement
        ↓
Read Plan
```

Read Intent 可以包含：

- STREAM；
- DOWNLOAD；
- PREVIEW；
- INTERNAL_PROCESSING；
- RANGE；
- EXPORT。

不同 Intent 可以应用不同权限、缓存与审计策略。

### 13.2 Read Plan

Storage 可以返回内部 Read Plan，例如：

```text
READY_PROXY
READY_REDIRECT
READY_LOCAL_CACHE
RESTORE_REQUIRED
RESTORING
PROCESSING
TEMPORARILY_UNAVAILABLE
MISSING
CORRUPTED
```

外部 API 不需要暴露这些内部名字，但必须能够表达同等语义。

### 13.3 Cache-first

推荐服务器侧选择顺序：

```text
有效 Server Cache
      ↓ miss
可立即读取的 HOT / WARM Placement
      ↓
其他可在线读取 Placement
      ↓
需要 Restore 的 COLD / ARCHIVE Placement
```

客户端自己的 Cache / Download 在请求到达 Server 之前优先命中。

### 13.4 临时读取授权

如果 Provider 支持 Presigned URL，可以在 Permission 已通过后生成短期 Read Grant。

必须满足：

- URL 短时有效；
- 不包含 Storage Credential；
- 不能作为永久 Attachment 地址保存；
- 仅允许必要 HTTP Method；
- 尽可能支持 Range；
- Data Classification / Share Policy 可限制是否允许直连；
- Secure Domain 仍只返回密文字节路径，解密发生在允许的安全边界。

### 13.5 Server Proxy

以下场景应允许 Server Proxy：

- Provider 不支持 Presigned URL；
- 需要隐藏 Provider 拓扑；
- 需要 Server 侧访问控制或内容处理；
- Provider 直连不可达客户端；
- 需要统一 Range / Cache 路径。

无论 Proxy 还是 Redirect，都只是传输方式，不改变 Attachment / Blob 身份。

### 13.6 Range Read

视频、音频、大 PDF、归档预览等必须支持 Range-aware 访问路径。

Provider 不支持 Range 时：

- Storage Capability 应暴露能力限制；
- 可通过 Server Cache / Proxy 降级；
- 不得向播放器谎报支持 Range。

---

## 14. 统一 Availability

### 14.1 Availability 是派生结果

用户看到的 Availability 不应该由某一个单字段机械决定，而应综合：

- Attachment Lifecycle；
- Ingest / Derived Task；
- Blob Lifecycle；
- Server Cache；
- Placement State；
- Provider Health；
- Archive Restore；
- Integrity State。

### 14.2 基础产品状态

至少支持 PRD 定义的：

```text
AVAILABLE
CACHED
REMOTE
PROCESSING
RESTORING
MISSING
CORRUPTED
```

本专项增加：

```text
TEMPORARILY_UNAVAILABLE
```

用于“对象仍被认为存在，但当前 Provider / Network 故障，尚不能证明 Missing”的情况。

### 14.3 状态判定示例

```text
有已校验 Server Cache
→ CACHED

无 Cache，但有健康 Provider 上 AVAILABLE Placement
→ AVAILABLE / REMOTE

当前 Attachment 正在生成或 Finalize
→ PROCESSING

只有 Archive Placement，已发起 Restore
→ RESTORING

Provider 确认所有预期 Placement 对象不存在
→ MISSING

所有可用副本均校验失败
→ CORRUPTED

对象元数据仍正常，但唯一 Provider DOWN
→ TEMPORARILY_UNAVAILABLE
```

`AVAILABLE` 与 `REMOTE` 的具体 UX 映射可按 Provider / Tier 的访问类别确定，但两者都表示不需要归档恢复即可读取。

### 14.4 Availability 不替代错误详情

API 还应提供受控的：

- reason code；
- 是否可 Retry；
- 是否已创建 Background Task；
- task_id；
- 可选 estimated readiness（Provider 能可靠给出时）；
- 当前可执行动作，例如 `RESTORE` / `RETRY`。

不能只返回裸 404。

---

## 15. Derived Attachment

### 15.1 派生内容始终产生新 Attachment / Blob

以下操作产生新内容时：

- 转码；
- 缩略图；
- 图片预览；
- OCR 文件；
- 音频波形；
- 封面提取；
- 电子书格式转换；
- AI 生成的文件型 Artifact；

都创建新的 Attachment 与 Blob。

### 15.2 Lineage

派生关系必须保留：

```text
Derived Attachment
    DERIVED_FROM
Original Attachment
```

关系的持久化由 Relation Core 统一设计；Storage 只要求该关系存在并可查询，不在本专项复制一套通用 Relation 表。

### 15.3 Rebuildability

派生 Attachment 应声明是否 `rebuildable`。

如果可稳定重建：

- Storage Policy 可以只保留较少 Replica；
- Server Cache 可更积极淘汰；
- 空间紧张时可以优先清理派生 Blob，但仍必须经过受控 GC；
- 清理后业务应能解释为“需要重新生成”，而不是永久内容损坏。

原始 Attachment 默认不得仅因为“理论上可再次从第三方下载”就自动标记为 rebuildable。

---

## 16. 完整性校验

### 16.1 校验层次

完整性至少包含：

1. Ingest Finalize 校验；
2. Placement 写入后校验；
3. 定期 Scrub；
4. 迁移 / 复制后校验；
5. Restore 后校验；
6. 管理员手动校验；
7. 读路径发现异常时的即时降级与异步复核。

### 16.2 Canonical 与 Provider Checksum

Provider Checksum 可以用于快速校验，但只有算法与语义明确一致时才可作为 Canonical Digest 证据。

否则需要读取对象并重新计算 Canonical Digest。

### 16.3 Missing 与 Corrupted 分离

```text
MISSING
= Provider 确认对象不存在

CORRUPTED
= 对象存在但内容与期望 Digest / Size 不一致
```

网络超时、403、Provider DOWN 不能直接标记为 Missing。

### 16.4 损坏处理

发现某 Placement Corrupted：

```text
Mark Placement CORRUPTED
      ↓
从 Read Candidate 中移除
      ↓
Emit blob.placement.corrupted
      ↓
如果存在其他 VERIFIED Replica
    ├── 创建 Repair Task
    └── Blob 仍可服务
否则
    └── Blob Availability = CORRUPTED
```

### 16.5 自动修复

修复必须采用“复制并验证新副本”，不能直接覆盖唯一坏副本后丢失证据。

修复成功后可以按策略删除坏对象；删除动作仍需审计记录。

---

## 17. Replica Reconciliation

### 17.1 期望状态与实际状态

Storage Policy 给出 Desired State，Placement Catalog 给出 Actual State。

Reconciler 周期性计算：

```text
Missing Replica
Extra Replica
Wrong Tier
Provider Draining
Corrupted Replica
Restore State Drift
```

并生成 Background Task。

### 17.2 最小安全规则

任何“迁移”都必须实现为：

```text
Create Target Placement
        ↓
Copy / Upload
        ↓
Verify Target
        ↓
Mark Target AVAILABLE
        ↓
重新检查 Policy / Hold / Concurrency
        ↓
Delete Source（如果策略需要）
```

禁止：

```text
Delete Source
    ↓
再尝试 Copy
```

### 17.3 多 Provider 可靠性

Policy 可以要求 Replica 跨 Provider 分布，以避免一个 Provider 故障导致所有副本同时不可用。

是否默认要求跨 Provider 多副本由部署策略决定；P0 不强迫所有自托管用户配置两个对象存储。

---

## 18. Provider Drain 与停用

### 18.1 Drain

管理员希望移除 Provider 时，推荐：

```text
ENABLED
   ↓
DRAINING
   ↓
停止新 Placement
   ↓
扫描现有 Blob
   ↓
为不满足剩余策略的 Blob 创建 Migration / Replica Task
   ↓
验证所有关键 Blob 已有替代副本
   ↓
DISABLED
```

### 18.2 影响分析

进入 DISABLED 前必须显示：

- Provider 当前 Blob / Replica 数；
- 唯一副本数量；
- Physical Bytes；
- 有多少 Blob 在其他 Provider 无有效 Replica；
- Secure Blob 数量（仅可安全统计，不泄露内容）；
- Pending Migration / Restore；
- 预计会失去的能力，例如 Archive Restore。

### 18.3 强制停用

强制停用可能造成内容不可用，属于高风险管理操作，必须：

- Platform Permission；
- Step-up Verification；
- 明确影响提示；
- Audit；
- 不由 AI / Automation 静默执行。

---

## 19. Archive 与 Restore

### 19.1 Archive 是 Storage 状态，不是业务 Archive

需要区分：

```text
Attachment.lifecycle = ARCHIVED
```

与：

```text
Placement.storage_tier = ARCHIVE
Placement.state = RESTORE_REQUIRED
```

两者没有必然一一对应关系。

### 19.2 Restore Request

当读取只能命中需要恢复的 Placement 时，可以：

- 根据 Storage Policy 自动发起 Restore；
- 或向用户返回 `RESTORE_REQUIRED`，由用户确认后发起。

恢复是 Background Task。

### 19.3 Single-flight Restore

同一个 Blob / Placement 在已经 Restore 中时，重复读取请求不应重复向 Provider 发起多个高成本 Restore。

需要通过：

- 幂等键；
- 唯一 Active Restore 约束；
- Blob / Placement Lock；

实现 Single-flight。

### 19.4 Restore 完成

Provider Restore 完成后：

1. 校验对象可读取；
2. 重新验证 Size / Digest（按策略）；
3. 更新 Placement 状态；
4. 保存 `restore_expires_at`（如果 Provider 的恢复副本有过期时间）；
5. 按 Policy 可选创建 HOT / WARM Replica 或 Server Cache；
6. Emit 完成 Event。

### 19.5 Restore 失败

失败必须区分：

- Provider 暂时错误，可重试；
- 凭据错误，不应无限重试；
- 对象不存在；
- 归档对象损坏；
- 配额/费用策略拒绝；
- Task Cancelled；
- TimedOut。

不能全部压缩为 `Failed: unknown`。

---

## 20. Server Cache

### 20.1 Cache 角色

Server Cache 主要优化：

- 远程对象存储热点媒体；
- 大图片 / 漫画页；
- 弱网络 Provider；
- Range 读取；
- 多次重复读取；
- 临时 Restore 后的热点访问。

Cache 不承担长期数据真相。

### 20.2 Cache Key

Blob 不可变，因此 Cache 可以以 Blob 稳定身份构建 Key。

内部可以使用：

```text
blob_id + byte_range / representation
```

或安全的内容版本标识。

不得把用户可控文件名直接拼成不校验的本地路径。

### 20.3 Cache Validation

缓存条目必须至少能关联：

- Blob ID；
- Content Revision / Digest 版本；
- Size；
- 创建/最后验证时间；
- 本地路径或分片信息；
- Data Classification；
- 加密/安全属性。

### 20.4 Eviction

支持：

- LRU / size-aware；
- TTL；
- Pin；
- Storage Pressure；
- Derived-first Eviction；
- Sensitive / Secure 专项规则。

具体算法是实现细节，但清理 Cache 不需要修改 Blob / Placement 真相。

### 20.5 Secure Domain Cache

Secure Blob 的 Server Cache 默认只能保存密文字节。

如果某个 SERVER_ASSISTED_ENCRYPTED Profile 确实需要短时明文缓存，必须由 Secure Data Foundation 独立设计并显式允许；普通 Storage Cache 不提供此能力。

---

## 21. Retention Hold

### 21.1 为什么需要 Hold

仅靠 Attachment 引用不足以表达所有保留原因。

例如：

- Backup 正在构建一致性恢复点；
- Share 仍要求保留可访问内容；
- Document Revision 仍保留历史 Attachment；
- 导出/迁移任务正在读取 Blob；
- 法规/安全策略要求延迟物理删除；
- Secure Domain 需要完成 Crypto Erasure / Tombstone 流程。

因此 Storage 提供 Retention Hold。

### 21.2 概念模型

```text
BlobRetentionHold
├── id: uuid / UUIDv7
├── blob_id: uuid
├── holder_type: stable string code
├── holder_id: uuid/string stable reference
├── reason_code
├── expires_at: timestamptz?
├── created_at: timestamptz
├── released_at: timestamptz?
└── version: bigint
```

### 21.3 Hold 契约

其他子系统只能通过 Command 创建/释放 Hold，不能直接 INSERT Storage 表。

Holder 必须提供幂等业务键，避免重试产生无限重复 Hold。

### 21.4 Hold 不等于业务权限

Retention Hold 只回答“这个 Blob 现在不能物理删除”，不授予任何主体读取 Blob 的权限。

---

## 22. Attachment 删除与 Blob GC

### 22.1 删除分层

```text
业务删除 Resource
      ↓
上层决定 Attachment 是否进入 Trash / Purge
      ↓
Attachment TRASHED
      ↓ 保留期间仍引用 Blob
Attachment PURGED
      ↓ 释放该业务引用
Blob Reference / Hold Re-evaluation
      ↓
可能成为 GC Candidate
```

### 22.2 GC Eligibility

Blob 只有同时满足以下条件才可进入 `GC_CANDIDATE`：

1. 没有 ACTIVE / ARCHIVED / TRASHED 且仍需保留的 Attachment 引用。
2. 没有有效 Retention Hold。
3. 没有必须保留该 Blob 的 Revision / Share / Backup 等跨系统约束。
4. 已满足最小保留时间 / Grace Period。
5. 没有正在使用该 Blob 的不可中断 Storage Operation。
6. Secure Domain 的上层安全删除语义已经允许 Storage 物理回收密文。
7. 当前 Blob 未被新的 Ingest / Dedup 事务重新引用。

### 22.3 GC 两阶段确认

推荐：

```text
ACTIVE
  ↓ 初次判定
GC_CANDIDATE
  ↓ 等待 Grace Period + 再次事务内确认
PURGING
  ↓ 删除 Placement
PURGED
```

这样可以减少“刚释放引用就立即不可逆删除”的风险。

### 22.4 GC 与新增引用并发

必须防止：

```text
GC 判断 ref = 0
        │
        ├── 并发新 Attachment 引用同 Blob
        │
        └── GC 删除 Blob
```

可以使用：

- Blob Row Lock；
- PostgreSQL Advisory Lock；
- 状态条件 UPDATE；
- 引用唯一/外键约束；

确保进入 `PURGING` 后新的普通绑定不能继续使用该 Blob；如果仍在 `GC_CANDIDATE`，新引用可以原子地将其恢复为 `ACTIVE`。

### 22.5 Placement 删除失败

GC 删除多个 Placement 时，如果某个 Provider 删除失败：

- Background Task 不得错误标记 Succeeded；
- Blob 不得直接标记 PURGED；
- 已成功删除的 Placement 保留 DELETED 事实；
- 失败 Placement 保留错误与可重试状态；
- 后续 Retry 继续幂等删除；
- 管理员可以看到 Partial Progress。

### 22.6 GC Audit

每次实际 Blob GC 至少记录：

- Blob ID；
- 触发来源；
- 执行主体 / Task；
- GC Candidate 判定时间；
- 引用与 Hold 摘要；
- Policy / Retention 版本；
- 被删除的 Placement ID；
- 每个 Provider 的结果；
- 开始/结束时间；
- 最终结果；
- Error Category。

普通 Audit 中默认不记录原始 Object Key、Credential、Secure 明文或不必要的完整 Digest。

### 22.7 手动永久删除

管理员或用户主动要求不可逆 Purge 时，必须遵守 System Overview：

- 明确影响范围；
- Authorization；
- Step-up Verification；
- Audit；
- 必要时后台执行；
- 明确不可恢复提示。

---

## 23. Secure Blob 集成

### 23.1 Storage 只保存密文字节

Secure Domain 路径：

```text
Client / Secure Data Foundation
       ↓ Encrypt
Encrypted Envelope
       ↓
Attachment / Secure Blob Contract
       ↓
Blob
       ↓
Placement
       ↓
Storage Provider
```

Storage Provider 与普通 Storage Core 不需要知道密文中的标题、文件名或明文内容。

### 23.2 Storage 不拥有解密能力

Storage Core 不得：

- 请求 Vault Root Key；
- 将 Secure Blob 自动解密后写 Server Cache；
- 为内容识别而把 Secure Blob 发给普通 AI / OCR；
- 在 Event / Log 中输出受保护元数据；
- 使用明文 Digest 建立跨安全域去重。

### 23.3 Secure Attachment Viewer

Storage 只负责经过授权后传输密文字节。

流式解密、解锁状态、明文导出确认、客户端安全缓存由 Secure Data Foundation / Private Notes / Password Manager 负责。

### 23.4 Crypto Erasure 与 Blob GC

Secure Domain 的“Crypto Erasure”与 Storage 的“物理 Blob GC”不是同一动作。

上层安全域可以先使密钥不可再恢复，再释放 Storage Hold；Storage 随后按普通 GC 流程清理密文字节。

Storage 不自行决定密钥删除。

---

## 24. 并发控制与幂等

### 24.1 需要幂等的关键动作

至少包括：

- Begin / Finalize Ingest；
- Direct Upload Complete；
- Create Replica；
- Repair Replica；
- Migrate Blob；
- Restore Blob；
- Create / Release Retention Hold；
- Purge Attachment；
- GC Blob；
- Provider Drain；
- Event Consumer。

### 24.2 Idempotency Key

外部可重试 Command 应支持 Idempotency Key。

幂等记录必须绑定：

- Command Type；
- Actor / Security Context；
- Target；
- Request Semantic Hash；
- 结果引用；
- 过期策略。

同一个 Idempotency Key 携带不同业务参数时应返回 Conflict，而不是静默复用旧结果。

### 24.3 乐观并发

Attachment metadata、Storage Provider、Storage Policy 等用户可编辑实体使用 `version` / ETag 防止覆盖更新。

Placement 状态迁移使用“expected current state”条件更新，避免两个 Worker 同时把旧状态覆盖成不同结果。

### 24.4 重试不是重新发明一个新业务动作

Background Task Retry：

- 保留原逻辑操作 ID / idempotency context；
- 新 Attempt 有独立 attempt identity；
- 不重复创建已成功完成的目标 Placement；
- 不覆盖历史失败 Attempt。

---

## 25. Background Task 映射

### 25.1 Storage 任务类型

典型 Background Task：

```text
BLOB_VERIFY
BLOB_REPLICA_CREATE
BLOB_REPAIR
BLOB_MIGRATE
BLOB_RESTORE
BLOB_GC
PROVIDER_DRAIN
STORAGE_RECONCILE
CACHE_CLEANUP
ORPHAN_SCAN
INGEST_FINALIZE
```

### 25.2 状态要求

每类任务必须定义：

- 成功条件；
- Retryable Error；
- Non-retryable Error；
- Cancellation Point；
- Timeout；
- Max Attempts；
- Backoff；
- 幂等键；
- 恢复 Server 重启后遗留 `Running` 的策略。

### 25.3 Cancellation

复制 / 迁移 / Restore 取消需要协作式执行。

如果底层 Provider 不支持真正 Cancel：

- Task 进入 `Cancelling`（如实现扩展状态）；
- 等当前不可中断请求结束；
- 清理未完成目标；
- 最终标记 `Cancelled`。

不能把用户取消记录为 `Failed`。

### 25.4 Timeout

Provider 网络操作超时导致 `TimedOut`，是否 Retry 由 Error Category 与任务策略决定。

Timeout 不能伪装成用户取消。

---

## 26. Capability 契约

以下为语义级 Capability，不锁定 Java 方法名或 HTTP Path。

### 26.1 Attachment Query

输入：

- Attachment ID；
- Principal / Security Context；
- 需要的字段范围。

输出：

- 安全可见的 Attachment metadata；
- Lifecycle；
- Availability；
- 允许的操作。

### 26.2 Resolve Attachment Read

输入：

- Attachment ID；
- Read Intent；
- Range / Representation 需求；
- Principal；
- Trace Context。

输出：

- Read Plan；
- Availability；
- 临时访问 Grant 或 Proxy Handle；
- Task ID（若 Restore / Processing）；
- Error / Reason Code。

### 26.3 Resolve Blob Availability

主要面向内部子系统与管理员，返回：

- Blob overall availability；
- Replica summary；
- Restore possibility；
- 不泄露 Secret 的 Provider health summary。

### 26.4 Storage Capability Discovery

Instance 可以暴露：

```text
storage.write.available
storage.range_read.available
storage.multi_replica.available
storage.archive.available
storage.server_cache.enabled
storage.direct_upload.available
```

Capability Available 不代表当前用户有 Permission。

---

## 27. Command 契约

建议稳定业务 Command：

```text
BeginAttachmentIngest
FinalizeAttachmentIngest
AbortAttachmentIngest
TrashAttachment
RestoreAttachment
PurgeAttachment
CreateDerivedAttachment
RequestBlobVerification
RequestBlobReplica
RequestBlobMigration
RequestBlobRestore
CreateBlobRetentionHold
ReleaseBlobRetentionHold
ReconcileBlobPolicy
DrainStorageProvider
ChangeStorageProviderMode
ChangeStoragePolicy
RunBlobGC
```

规则：

1. Command 进入 Storage 子系统后重新执行 Permission / 状态校验。
2. Automation、AI、Plugin、Scheduled Job 不能绕过这些 Command 直接改表。
3. 高风险 Command 应声明 required Permission / Step-up Policy。
4. 耗时 Command 返回 Background Task 引用，而不是保持 HTTP 请求直到完成。

---

## 28. Event 契约

### 28.1 Event 基础字段

遵守平台统一 Event：

```text
event_id
schema_version
event_type
occurred_at
producer
actor
subject_id
correlation_id
causation_id
trace_id
payload
```

`event_id` 使用 UUIDv7；`occurred_at` 使用带时区时间。

### 28.2 建议 Event

Attachment：

```text
attachment.created
attachment.archived
attachment.restored_from_archive_state   // 如业务需要，避免与 storage restore 混淆时可换更明确名称
attachment.trashed
attachment.recovered
attachment.purged
```

Blob：

```text
blob.created
blob.integrity.verified
blob.missing
blob.corrupted
blob.gc.candidate
blob.purged
```

Placement：

```text
blob.placement.created
blob.placement.available
blob.placement.restore_required
blob.placement.missing
blob.placement.corrupted
blob.placement.deleted
```

异步动作：

```text
blob.restore.requested
blob.restore.completed
blob.restore.failed
blob.migration.completed
blob.replica.repaired
```

Storage：

```text
storage.provider.mode_changed
storage.provider.degraded
storage.provider.recovered
storage.policy.changed
```

最终 Event Type Registry 应避免同义重复；例如 Provider Health 原始事实可由 Storage 产生，Alert 仍由 Operations 拥有。

### 28.3 Event Payload 最小化

默认只传播下游真正需要的：

- Attachment / Blob / Placement ID；
- 安全的状态码；
- Size / Tier 等允许统计的字段；
- Task / Provider ID；
- Error Category；
- Data Classification 标记。

默认不传播：

- Credential；
- Presigned URL；
- 完整 Provider Object Key；
- Secure 明文元数据；
- 不必要的完整内容 Digest；
- 用户私有来源 URL。

---

## 29. Event 可靠性

关键 Storage Event 必须与业务状态更新采用 Durable Outbox。

例如：

```text
事务：
  UPDATE Placement = CORRUPTED
  INSERT Outbox(blob.placement.corrupted)
COMMIT
```

Event Dispatcher 可以重复投递，消费者必须幂等。

PostgreSQL LISTEN / NOTIFY 只能用于唤醒，不是唯一事实队列。

---

## 30. 概念数据库模型

本节用于明确 Storage Schema Ownership 与约束，不等同于最终 Migration DDL。

### 30.1 核心实体

建议 Storage 自有关系：

```text
attachment
blob
blob_placement
storage_provider
storage_policy
blob_retention_hold
ingest_session
```

Background Task、Outbox、Audit 复用平台公共能力，不在 Storage Schema 重复创建第二套通用任务/审计模型。

### 30.2 Attachment 约束

- `id`：UUIDv7 Primary Key。
- `blob_id`：必须引用有效 Blob；PURGED 后按实现可以保留 Tombstone 或解除内容引用。
- `lifecycle`：稳定字符串 Code / DB Check Constraint。
- `version`：乐观锁。
- `created_at / updated_at / deleted_at`：`timestamptz`。
- 普通查询索引：`blob_id`、`lifecycle`、`created_at`。

### 30.3 Blob 约束

普通去重至少需要唯一约束：

```text
(dedup_namespace, digest_algorithm, digest_value, size_bytes)
```

索引：

- `lifecycle + gc_eligible_at`；
- `integrity_state`；
- `created_at`。

Digest 建议以 `bytea` 保存，不以大小写不一致的 Hex String 作为数据库规范值；API 是否输出 Hex/Base64 由 API Convention 统一。

### 30.4 Placement 约束

至少：

- `blob_id` FK / 内部一致性约束；
- `provider_id` FK；
- Provider Namespace 内 `object_key` 唯一或加 `provider_version_id` 后满足 Provider 语义；
- `state` Check Constraint；
- `blob_id + state` 索引；
- `provider_id + state` 索引；
- `storage_tier + state` 索引。

### 30.5 Provider 约束

- `key` 在 Instance 内唯一；
- `configuration` 不能保存 Secret 明文；
- 重要更新使用 `version`；
- 删除 Provider 实体前必须先完成 Drain / 影响检查，不允许普通 FK Cascade 删除 Placement Catalog。

### 30.6 Retention Hold 约束

建议唯一业务键：

```text
(blob_id, holder_type, holder_id, reason_code)
```

保证重复 Event / Command 不会无限新增同一 Hold。

### 30.7 Ingest Session 约束

- `(actor/context, idempotency_key)` 唯一；
- 过期 Session 可被 Cleanup 查询；
- Finalize 使用 Version / State 条件确保只能完成一次。

### 30.8 不引入 tenant_id

Storage Schema 不因为多 User 机械增加 `tenant_id`。

如果未来 Instance 真正演进为 Multi-tenant，必须通过新的系统级 ADR 重新设计 Provider、Dedup Namespace、Key、Cache、GC、Audit 与配额隔离。

---

## 31. 数据库事务边界

### 31.1 Ingest Finalize

在物理对象已经安全写入后，数据库侧建议一个短事务完成：

```text
Upsert / resolve Blob
Create AVAILABLE Placement metadata（如果是新副本）
Create Attachment
Write Outbox
Commit
```

如果数据库提交失败而物理对象已经存在，会产生 Orphan Object，由 Reconcile 清理或重新接管。

### 31.2 不能把长网络 I/O 放进数据库事务

禁止：

```text
BEGIN
  上传 50GB 到 S3
  等待 Archive Restore 2 小时
COMMIT
```

网络传输在事务外执行，数据库只提交短状态迁移。

### 31.3 跨子系统不做大事务

例如创建 Resource + Upload Attachment + Search Index：

- Resource / Storage 各自保证内部一致性；
- 通过 Command / Event 协作；
- Search 最终一致。

不能要求跨所有子系统一个数据库事务锁到上传完成。

---

## 32. Object Key 与物理命名

### 32.1 Key 不属于业务契约

Object Key 可以根据 Provider 实现选择：

- 随机 UUID 路径；
- Blob ID 分层；
- 内部内容地址；
- Provider 原生命名。

但必须保证：

- 业务端不依赖 Key 格式；
- Key 格式可以 Migration；
- Secure Blob 不从明文文件名或明文 Digest 派生可泄露 Key；
- 防止路径穿越；
- Provider Adapter 负责正规化。

### 32.2 原始文件名不用于物理路径拼接

例如：

```text
../../secret.txt
```

只能作为经过安全处理的业务显示名，不能直接成为 Server Filesystem Path。

---

## 33. 故障与 Reconciliation

### 33.1 Metadata Exists, Object Missing

发现数据库有 Placement、Provider 无对象：

- 不立即删除 Placement 记录；
- 标记 `MISSING`；
- Emit Event；
- 如果有其他 Replica，触发 Repair；
- 如果无 Replica，Blob Availability = MISSING；
- 运维告警。

### 33.2 Object Exists, Metadata Missing

可能由：

- Upload 后数据库事务失败；
- Worker Crash；
- 人工向 Bucket 写入；
- 旧临时对象未清理。

处理：

1. 对受管理前缀执行 Inventory / Orphan Scan；
2. 与 Placement Catalog 对比；
3. 临时前缀按 Session TTL 清理；
4. Final Namespace 中的 Orphan 先进入 Grace / Quarantine；
5. 管理员可选择接管或删除；
6. Secure Provider 中对象仍按密文处理。

### 33.3 Provider Credential 失效

表现为 Provider Health DEGRADED / DOWN 与认证错误，不把对象批量标记 Missing。

产生 Security / Operations Event，并阻止无限重试造成账户锁定或费用浪费。

### 33.4 Server Restart

启动恢复至少扫描：

- `PENDING` / `RESTORING` / `DELETING` 的异常长期状态；
- Background Task 遗留 Running；
- 过期 Ingest Session；
- 本地 Cache Index 与文件不一致；
- Provider Drain 未完成任务。

恢复逻辑根据任务幂等语义继续、Retry 或标记异常。

---

## 34. Storage Provider 插件契约

### 34.1 插件定位

Storage Provider Plugin 只实现物理存储能力，不拥有 Attachment / Blob 业务数据库。

Plugin 不得：

- 直接 UPDATE Storage Core 表；
- 绕过 Permission 生成永久公开 URL；
- 保存 Ikaros Secret 明文副本；
- 把内部 Java Entity 当作长期公共契约；
- 假定 Provider Config JSON 永不版本升级。

### 34.2 逻辑操作

Provider SPI 至少需要能够表达：

```text
Probe / Health
Put / Multipart Put
Head
Read / Range Read
Delete
Copy（可选）
Checksum（可选）
Restore（可选）
Get Restore Status（可选）
Create Presigned Read / Write Grant（可选）
Get Capacity（可选）
```

### 34.3 Error Taxonomy

Provider Error 至少标准化为：

```text
NOT_FOUND
AUTH_FAILED
PERMISSION_DENIED
THROTTLED
TIMEOUT
NETWORK_UNAVAILABLE
CAPACITY_EXCEEDED
INTEGRITY_MISMATCH
RESTORE_REQUIRED
UNSUPPORTED_OPERATION
CONFLICT
PROVIDER_INTERNAL
```

每类 Error 需要声明默认 Retryability。

Plugin 不应只抛出一个无法分类的 `RuntimeException` 给上层。

### 34.4 Idempotency

Provider Adapter 应尽可能接受 Storage Core 提供的 Operation ID / Idempotency Context。

例如 Delete 重试时，目标已经不存在通常可以视为幂等成功，而不是制造永久 Failed Task；但必须区分“确认此前已删除”和“根本无权检查”。

### 34.5 Contract Version

Provider Plugin 使用明确 Plugin API Version。

新增 Capability 通常向后兼容；删除、改变错误含义、改变 Checksum 或 Restore 语义需要版本升级/弃用窗口。

---

## 35. 权限与安全

### 35.1 权限分层

至少区分：

```text
Attachment Read / Download
→ Resource ACL / Attachment Access Policy

Storage Provider Manage
→ Platform RBAC

Storage Policy Manage
→ Platform RBAC

Blob Inspect / Repair / GC
→ Platform Operator Permission

Force Purge / Force Disable Provider
→ High-risk Permission + Step-up
```

平台管理员权限不能自动绕过 Secure Vault 解密边界。

### 35.2 Presigned URL 泄露控制

Presigned URL 属于临时敏感访问能力：

- 不进入普通 Log；
- 不进入 Analytics Fact；
- 不进入 Event Payload；
- 不作为 Attachment metadata 返回后长期缓存；
- 过期时间与 Data Classification / Read Intent 适配。

### 35.3 Blob Digest 泄露控制

Digest 可以用于内部去重与校验，但外部公开完整 Digest 可能泄露“实例是否拥有某个已知文件”。

因此：

- 普通 Attachment API 默认不需要返回 Blob Digest；
- 管理/校验接口按 Permission 返回；
- Secure Blob 更严格限制。

---

## 36. 数据敏感等级与传播

### 36.1 Effective Classification

Attachment 的有效敏感等级不能低于其上层业务约束允许的等级。

例如：

- Public Article 的公开附件可以是 Public；
- Private Document 的附件至少是 Private；
- Password Vault Attachment 必须是 Secure Domain。

具体继承/覆盖规则由 Data Classification / Resource Core 最终设计，但 Storage 不允许调用方通过简单参数把 Secure Domain Attachment 降成 Public。

### 36.2 Blob 共享与权限

同一普通 Blob 可以被 Public Attachment 与 Private Attachment 同时引用，但：

- Blob 本身不因此变成“公开可下载”；
- 每次读取都基于 Attachment / Principal 授权；
- 对外 URL 按每次访问短期生成；
- Cache 不能缓存授权结果到所有用户共享的无上下文入口。

### 36.3 日志与 Error

日志仅记录：

- ID；
- Provider ID；
- Task ID；
- safe reason code；
- Size / Duration 等允许字段。

避免记录：

- Secret；
- Secure 明文；
- Presigned URL；
- 完整敏感来源 URL；
- 用户私密文件名（在 Sensitive / Secure 场景）。

---

## 37. Audit

必须进入 Audit 的 Storage 操作至少包括：

- Storage Provider 创建、修改、模式切换、停用；
- Credential Reference 变化；
- Storage Policy 修改；
- 手动迁移 / 修复大量 Blob；
- Archive Restore 的高成本管理动作（按部署策略）；
- Attachment 永久 Purge；
- Blob GC；
- Blob Replica 手动删除；
- Provider Drain / Force Disable；
- 大规模导入接管；
- Secure Blob 明文导出相关动作由 Secure Domain 另外审计。

Audit 与普通播放/读取 Activity 分离。

---

## 38. Observability

### 38.1 实时 Health

Operations 至少展示：

- 每个 Storage Provider Health；
- 每个 Tier 可用性；
- Server Cache Health；
- Pending Restore；
- Pending Migration；
- Missing / Corrupted Blob；
- Worker Queue；
- Provider Request latency / error rate。

一个 Archive Provider DOWN、HOT Provider 正常时，整体 Storage 可以是 `DEGRADED`，不应把整个 Ikaros 判为 DOWN。

### 38.2 Runtime Metrics

建议指标：

```text
storage.read.requests
storage.read.bytes
storage.write.requests
storage.write.bytes
storage.provider.latency
storage.provider.errors
storage.cache.hit
storage.cache.miss
storage.cache.bytes
storage.restore.pending
storage.restore.duration
storage.migration.bytes
storage.integrity.failure
storage.gc.bytes
storage.gc.failures
```

### 38.3 Analytics 口径

长期统计交给 Analytics，必须区分：

```text
Logical Size
= 所有 Attachment 逻辑引用的字节规模

Physical Unique Size
= 去重后的 Blob 内容字节规模

Replica Physical Size
= 所有 Durable Placement 实际副本总规模

Cache Size
= 可淘汰 Cache 占用
```

这些指标不能混成一个“Storage Used”。

### 38.4 Trace

一次读取/恢复/迁移链路至少能够关联：

- request_id；
- trace_id；
- actor；
- attachment_id；
- blob_id；
- placement_id；
- provider_id；
- task_id；
- source_event_id。

但 Trace 同样遵守敏感数据最小化。

---

## 39. Backup / Restore 交接边界

### 39.1 Storage 提供能力，不拥有整机备份产品

实例 Backup Orchestrator 需要 Storage 提供：

- Blob Inventory；
- Durable Placement Summary；
- 指定时间点/批次的一致性引用能力；
- Blob Stream / Copy Capability；
- 校验能力；
- Restore 后 Reconcile 能力。

但完整 Backup Job、数据库一致性点、Secret / Key Material、Plugin Config 的组合由 Backup / Operations 专项负责。

### 39.2 Backup Hold

Backup 在生成恢复点期间可以创建 Retention Hold，防止 GC 删除正在纳入备份集的 Blob。

Backup 完成并确认后按其保留策略释放或转换 Hold。

### 39.3 Restore Verification

恢复 Blob Placement 后必须重新执行完整性校验。

`Backup Succeeded` 不代表 `Restore Verified`。

---

## 40. Export / Import 交接边界

### 40.1 Export

Export 子系统可以请求：

- 某 Attachment 的授权内容流；
- Blob 引用与安全元数据；
- 校验信息。

Export Format Version 由 Export 专项拥有。

Storage 不把数据库 Dump 或 Bucket Key 列表当作用户导出格式。

### 40.2 Secure Export

Secure Domain Export 必须继续遵守对应安全设计；Storage 只提供密文字节或在受控 Secure Capability 下参与，不负责把 Vault 全量解密成普通 ZIP。

---

## 41. Search / AI / Processing 交接

### 41.1 Search

Search 可以索引被允许的 Attachment 提取文本，但：

- 读取 Attachment 需要权限；
- Extracted Text 是派生数据；
- Secure Domain 默认不进入普通明文索引。

### 41.2 AI

AI 使用 Attachment 内容时必须：

1. 继承当前 Principal 权限；
2. 经过 Data Classification / AI Policy；
3. 通过 Storage Read Capability 读取；
4. 不获得 Blob / Provider 直接数据库权限；
5. 产生文件型结果时创建 Derived Attachment 并记录 Provenance。

### 41.3 Processing Workspace

转码、OCR、解压等临时 Workspace：

- 不属于 Durable Placement；
- 必须有 Task 生命周期；
- 任务完成/失败后清理；
- Secure Domain 明文处理必须使用 Secure Data Foundation 明确允许的安全 Workspace。

---

## 42. 配置模型

### 42.1 Instance Storage Configuration

配置至少分为：

```text
Provider Configuration
Storage Policy
Server Cache Configuration
Ingest Temporary Configuration
Integrity / Scrub Policy
GC / Retention Policy
Worker Concurrency
```

### 42.2 配置元信息

每项配置明确：

- 默认值；
- 来源；
- 是否运行时可改；
- 是否需要 Reload；
- 是否需要 Restart；
- 是否 Secret；
- Validation；
- 风险等级。

### 42.3 默认安全值

P0 推荐默认：

- Blob 至少 1 个 Durable Placement 才算 Finalize 成功；
- Server Cache 可关闭；
- Cache 不是持久化来源；
- GC 使用非零 Grace Period；
- Provider 强制停用需要 Step-up；
- Secure Blob Server Cache 只缓存密文字节；
- 未配置 Archive Provider 时 `storage.archive.available = false`。

具体容量、天数和并发数属于部署默认值，不在本设计写死。

---

## 43. 一致性与正确性不变量

实现与测试必须长期保持以下不变量：

1. `Attachment` 永远不以路径 / URL 作为身份。
2. Finalized Blob 字节不可变。
3. Blob UUIDv7 与 Digest 身份分离。
4. 同一普通 Dedup Namespace 内相同 Canonical Digest + Size 不产生多个逻辑 Blob。
5. 多个 Attachment 共享 Blob 不共享业务 ACL。
6. `AVAILABLE` Placement 必须经过完成写入和必要校验。
7. Cache Entry 永远不能满足 Durable Replica 最小数量。
8. 迁移必须先验证目标，再允许删除源。
9. Provider DOWN 不等于 Placement Missing。
10. Blob GC 不能删除存在有效 Attachment 引用或 Retention Hold 的 Blob。
11. `GC_CANDIDATE` 到 `PURGING` 必须再次事务内确认。
12. Secure Blob 的普通 Storage 持久化副本只包含密文字节。
13. Provider Credential 不进入普通配置、日志、Event 或 Audit 明文。
14. 长耗时 Storage 操作不占用长数据库事务。
15. 关键状态变化与 Event Outbox 同事务提交。
16. Storage 子系统私有 Schema 不被其他子系统直接写入。
17. 所有实体主键、正式时间字段、契约版本遵守 System Overview。

---

## 44. 测试策略

### 44.1 Unit / Domain Test

覆盖：

- Attachment Lifecycle；
- Blob Lifecycle；
- Availability 计算；
- Policy Match；
- GC Eligibility；
- Error Retryability；
- Provider Mode；
- Secure Classification 传播。

### 44.2 PostgreSQL Constraint Test

至少验证：

- UUIDv7 写入；
- Digest 唯一约束；
- 并发 Ingest Dedup；
- Retention Hold 幂等唯一约束；
- Placement 状态条件更新；
- GC 与新增引用竞争；
- `timestamptz` 与时区序列化。

### 44.3 Provider Contract Test

所有核心与插件 Provider 运行统一 Contract Test Suite：

- Put / Head / Read；
- Range；
- Multipart；
- Checksum；
- Delete Idempotency；
- Presigned Grant；
- Timeout；
- Throttle；
- Auth Failure；
- Restore（支持时）；
- Capability Declaration 一致性。

### 44.4 Failure / Chaos Test

模拟：

- 上传到 99% 后断网；
- 物理对象成功但 DB Commit 失败；
- DB 成功后 Worker Crash；
- Provider 返回 500 / 429；
- Provider Credential 失效；
- 迁移 Target Verify 失败；
- 唯一 Replica 损坏；
- GC 一半 Placement 删除失败；
- Restore 超时；
- Server 重启留下 Running Task；
- Cache Index 与磁盘内容不一致。

### 44.5 Security Test

验证：

- 无 Attachment Permission 不能通过 blob_id 旁路读取；
- Presigned URL 短期有效；
- Secret 不进入日志；
- Secure Blob Provider 侧只看到密文；
- Secure Blob 不进入普通明文 Cache；
- Public / Private Attachment 共享 Blob 时 ACL 不串权；
- 强制 Purge / Provider Disable 需要 Step-up。

### 44.6 Data Recovery Test

定期演练：

- 从另一个 Replica 修复 Corrupted Placement；
- 从 Archive Restore；
- Provider Drain；
- Blob Inventory Reconcile；
- Backup Restore 后完整性校验。

---

## 45. P0 / P1 / P2 实施建议

### 45.1 P0

必须具备：

- Attachment / Blob / Placement 核心模型；
- UUIDv7 / `timestamptz` / Schema Migration；
- Canonical SHA-256；
- 普通 Instance 内 Dedup；
- 至少一个 Durable Storage Provider；
- Ingest Session 与 Finalize；
- Range-aware Read；
- Attachment Permission 交接；
- Availability；
- 完整性校验；
- 逻辑删除与基础 GC；
- Durable Event；
- Background Task 集成；
- Provider Secret Reference；
- Secure Blob 密文存储；
- 基础 Health / Metrics / Audit。

### 45.2 P1

重点增强：

- 多 Provider / 多 Replica；
- Storage Policy Reconciler；
- Server Disk Cache；
- Provider Drain；
- 自动 Repair；
- Cold / Archive Restore；
- Retention Hold；
- Orphan Reconcile；
- Direct Multipart Upload；
- Derived Attachment 分层策略；
- 更完整 Storage Analytics。

### 45.3 P2

可扩展：

- 复杂成本优化；
- 多 Worker 并行 Storage Job；
- 高级分层预测；
- 跨地域 Replica；
- 更精细的用户 Policy；
- 大规模对象 Inventory；
- Provider-specific Server-side Copy 优化；
- 更高级的灾难恢复与一致性快照能力。

P0/P1/P2 只是实现优先级，不改变本设计中的身份、权限、不可变、GC 与 Secure Boundary 不变量。

---

## 46. 验收标准

当本专项进入实现完成状态时，至少应能证明：

1. 用户上传两个内容完全相同、文件名不同的文件时，可以产生两个 Attachment，但只产生一个普通 Blob。
2. 业务数据库中不存在依赖本地绝对路径 / S3 Key 作为 Attachment 主键的设计。
3. 删除一个共享 Blob 的 Attachment 不影响其他 Attachment 读取。
4. 将 Attachment 放入 Trash 不会立即删除 Blob。
5. Blob 存在有效 Hold 时 GC 必须拒绝物理清理。
6. 迁移过程中目标校验失败时原 Replica 保持可用。
7. Provider 临时故障时内容显示临时不可用，而不是被错误标记 Missing。
8. 唯一 Replica Missing / Corrupted 时客户端获得明确状态和可修复信息。
9. Archive Restore 不重复发起同一 Provider Restore，并可以通过 Task 查询进度。
10. Server Cache 全部删除后，业务数据仍完整。
11. Secure Domain Attachment 的 Storage Provider、数据库、普通 Cache 与 Backup 中只有允许的密文形式。
12. Storage Credential 不存在普通配置明文与日志中。
13. Blob GC、Provider Force Disable、Attachment 永久 Purge 都形成高风险审计。
14. Event 重复投递不会造成重复 Replica、重复 Restore 或重复 GC。
15. Storage Provider 插件可以通过稳定 Provider Contract 接入，而不需要读取 Storage 内部数据库表。
16. 所有时间点在 API 中保留时区语义，客户端可根据应用配置时区展示，默认应用时区为 UTC+8。
17. 所有本专项独立持久化实体使用 UUIDv7，不以 UUIDv7 时间代替 `created_at`。
18. Storage 表中没有为了假设 SaaS 场景机械加入的 `tenant_id`。

---

## 47. 核心结论

Ikaros V2 的 Attachment / Blob / Storage 不是“把文件路径存进数据库”的文件管理模块，而是一套把业务内容身份、不可变内容身份、物理副本和访问加速彻底分离的基础能力。

核心链路固定为：

```text
Resource / Domain
      ↓
Attachment
      ↓
Blob
      ↓
Placement / Replica
      ↓
Storage Provider / Tier
```

并同时保持：

```text
业务身份        ≠ 物理路径
Attachment       ≠ Blob
Blob UUIDv7      ≠ Content Digest
Durable Replica  ≠ Cache
Logical Delete   ≠ Blob GC
Business Archive ≠ Storage Archive Tier
Provider Health  ≠ Placement Missing
Login / Admin    ≠ Secure Blob Decryption Authority
```

在这一分层基础上，Ikaros 才能同时获得内容去重、多副本、冷热分层、缓存优先访问、完整性校验、归档恢复、Storage Provider 插件扩展、可靠 GC 与 Secure Domain 密文字节存储，而不让媒体、文档、财务、AI 或其他业务子系统绑定某一块磁盘、某一个 Bucket 或某一家对象存储服务。