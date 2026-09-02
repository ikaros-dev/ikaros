# Ikaros V2 Database Overview

| 项目 | 内容 |
|---|---|
| 文档名称 | Database Overview |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 系统级上位约束 | `System-Overview-Design.md` |
| 产品基线 | `Product-Requirements-Document.md` |

> 本文档定义 Ikaros V2 的数据库总体设计：PostgreSQL 在系统中的职责、数据库边界、Schema Ownership、实体身份与公共数据类型、跨领域引用、事务与并发、事件与异步执行持久化、Migration 与 Schema Version、索引、数据敏感等级、生命周期、备份恢复以及派生数据的数据库边界。
>
> 本文档是 `System-Overview-Design.md` 在数据库领域的专项展开。凡本文档与系统概要设计冲突，以系统概要设计为准；各子系统后续 PostgreSQL 详细模型与 DDL 必须同时满足本文档和各自领域设计。
>
> V2 是整体重构。本文档不以 V1 数据库表、字段、Repository、Flyway 历史版本、主键类型或兼容实现为设计输入，也不要求 V2 Schema 兼容 V1。V1 → V2 应作为独立数据迁移/导入问题处理。

---

## 1. 设计目标

Database Overview 需要回答以下问题：

1. PostgreSQL 在 Ikaros V2 中保存什么，不保存什么。
2. 一个 Ikaros Instance 与数据库边界是什么关系。
3. 多个子系统共享一个 PostgreSQL 时，如何仍然保持明确的 Schema Ownership。
4. UUIDv7、时间、版本、金额、枚举、JSONB、密文等数据如何统一表达。
5. 哪些关系应由数据库外键保证，哪些跨领域关系只应保存稳定 ID 或使用 Relation。
6. 一个领域事务如何同时可靠地产生 Event，而不把其他领域拉进同一事务。
7. Background Task、Job Run、Automation Run 等异步执行如何持久化状态、Attempt 与幂等信息。
8. Database Schema Version 如何独立于 API、Event、Plugin、Export 等其他契约版本演进。
9. 生产 Schema 如何迁移，长时间 Backfill 如何与短 DDL 分离。
10. Public / Shared / Private / Sensitive / Secure Domain 数据在数据库中的处理边界有何不同。
11. Search、Analytics、Embedding、Materialized View、Cache 等派生数据如何避免成为业务真相源。
12. 删除、归档、Tombstone、Purged、Blob GC、审计保留之间如何避免错误级联。
13. 数据库如何支持备份、恢复、可观测性与未来 Worker / 多节点演进。

---

## 2. 设计输入与约束优先级

本文档主动承接当前 `docs/v2/` 中与数据库有关的设计，主要包括：

- `System-Overview-Design.md`
- `Product-Requirements-Document.md`
- `Platform-Integration-Automation-Design.md`
- `Platform-Administration-Operations-Subsystem-Design.md`
- `Productivity-Planning-Subsystem-Design.md`
- `Personal-Finance-Accounting-Subsystem-Design.md`
- `Secure-Data-Foundation-Design.md`
- `Security-Identity-Authorization-Crypto-Subsystem-Design.md`
- `Private-Notes-Subsystem-Design.md`
- `Password-Manager-Subsystem-Design.md`
- `Data-Analytics-Statistics-Subsystem-Design.md`
- `AI-Intelligence-Subsystem-Design.md`
- `AI-Persona-System-Design.md`

设计约束优先级为：

```text
Product Requirements
        ↓
System Overview Design
        ↓
Database Overview
        ↓
Subsystem Database / Schema Design
        ↓
Migration / DDL / Repository Implementation
```

其中：

- PRD 定义产品目标与产品边界；
- System Overview 定义系统级不可违背的架构规则；
- 本文档只在数据库专项内展开这些规则；
- 具体表名、字段全集、DDL、索引组合、分区阈值等由对应领域详细设计继续确定。

---

## 3. 数据库总体定位

### 3.1 PostgreSQL 是普通业务关系数据的主要真相源

V2 只面向 PostgreSQL 设计。

PostgreSQL 负责保存：

- 领域实体与领域状态；
- Resource / Collection / Relation 等平台关系数据；
- Attachment / Blob / Placement 的元数据与状态；
- Identity / Permission / Session / Security Policy；
- Productivity、Accounting 等普通业务数据；
- Background Task / Scheduled Job / Job Run；
- Event Outbox 与幂等消费记录；
- Audit / Login / Security Event；
- Plugin / Automation / AI 等平台配置和执行元数据；
- Analytics Fact / Aggregate 等允许落在 PostgreSQL 的派生分析数据；
- Secure Domain 的最小公开元数据、密文 Envelope 与密钥引用信息。

PostgreSQL 可以合理使用：

- ACID Transaction；
- Foreign Key；
- Unique / Check Constraint；
- JSONB；
- Array；
- CTE / Window Function；
- Partial / Expression Index；
- Advisory Lock；
- LISTEN / NOTIFY；
- Full Text Search；
- 必要时的向量扩展。

但使用 PostgreSQL 高级能力的前提是它确实表达清晰、稳定的业务约束或性能目标，而不是把数据库变成不可理解的技巧集合。

### 3.2 PostgreSQL 不保存所有内容字节

大 Attachment、媒体、图片、电子书、压缩包、游戏包、Secure Attachment 等内容字节不应作为普通大 `bytea` 长期塞入业务表。

标准路径为：

```text
Business Entity / Resource
        ↓
Attachment Metadata
        ↓
Blob Metadata
        ↓
Blob Placement / Replica
        ↓
Object Storage / NAS / Filesystem Provider
```

PostgreSQL 保存内容身份、摘要、大小、类型、引用与可用状态；Blob Storage 保存实际内容字节。

Secure Domain 仍遵守同一逻辑分层，只是进入存储前内容已经被加密。

### 3.3 Search、Cache、Analytics、AI 派生数据不是业务真相源

```text
Business Truth                PostgreSQL Domain Data
Content Bytes                 Blob Storage
Search Projection             Search Index / PG Search Projection
Analytics Projection          Fact / Aggregate / Materialized View
AI Projection                 Embedding / Summary / Transcript / AI Cache
Cache                         Memory / Redis / Disk / Client Cache
Secure Domain Payload         Ciphertext-only Persistence
```

任何派生数据损坏后都应存在从更低层事实重建的路径。

---

## 4. Instance 与数据库边界

### 4.1 一个 Ikaros Instance 是默认数据边界

V2 默认模型为：

```text
One Ikaros Instance
        ↓
One logical PostgreSQL database
        ↓
Multiple subsystem-owned schemas / namespaces
        ↓
Multiple users inside the same Instance
```

V2 默认不是 SaaS Multi-tenant 系统。

因此数据库设计明确禁止：

- 为了假设性的 SaaS 场景给所有表机械增加 `tenant_id`；
- 把 `user_id` 误认为 Tenant；
- 把 Ledger、Vault、Collection、Project 等领域 Scope 误认为系统 Tenant；
- 在应用层用 Tenant 路由掩盖真实的 User / RBAC / ACL 语义。

### 4.2 不机械增加 `instance_id`

一个运行中的业务数据库本身已经位于当前 Instance 边界内，因此普通业务表也不需要机械增加 `instance_id`。

系统可以维护独立的 Instance Identity，用于：

- Backup / Restore 元数据；
- Export 来源标识；
- 节点发现；
- 诊断与 Trace；
- 未来明确设计的 Federation / Multi-instance 能力。

但 Instance Identity 不是所有业务行的分区键。

### 4.3 多 User 的隔离由授权模型负责

同一 Instance 内多用户访问边界由：

- User；
- Platform RBAC；
- Resource ACL；
- Owner / Member；
- Share；
- Secure Vault 权限；
- 其他明确领域 Scope；

共同决定。

数据库中的 `owner_id`、`user_id`、`member_id` 等字段表达真实业务关系，不承担虚构的 Tenant 语义。

---

## 5. PostgreSQL Schema Ownership

### 5.1 单数据库不等于共享所有权

V2 初期采用模块化单体并共享一个 PostgreSQL，但每一份业务数据必须有且只有一个明确 Owner Subsystem。

默认采用：

> **单 PostgreSQL Database + 按领域所有权划分 PostgreSQL Schema / Namespace。**

具体物理 Schema 名称由后续工程规范统一确定，本文档不锁定最终 identifier；但“谁拥有这张表、谁拥有它的 Migration、谁能够改变它的业务状态”必须在设计阶段就明确。

### 5.2 Ownership 的含义

一个子系统拥有某数据结构意味着：

1. 它定义该实体/关系的业务语义。
2. 它拥有对应 Schema Migration 的设计责任。
3. 它负责该表的约束、索引、生命周期与数据修复策略。
4. 只有它的 Repository / Persistence Adapter 可以把该表作为内部写模型直接操作。
5. 其他子系统只能通过公开 Capability / Command / Event / Relation 等契约使用其能力。
6. 数据库同处一个实例并不授予其他模块直接 SQL 修改权限。

### 5.3 逻辑所有权地图

下表只确定领域所有权，不锁定最终表名。

| 所有权域 | 主要数据 | 数据性质 |
|---|---|---|
| Resource / Content Core | Resource、Title/Alias、Metadata、Metadata Provenance、External Identity、Collection、Tag、Lifecycle、User State 等 | 普通业务真相 |
| Storage | Attachment、Blob、Placement/Replica、Derived Attachment、完整性与恢复状态 | 普通业务真相 + 内容元数据 |
| Integration | Durable Event / Outbox、跨领域 Generic Relation、Automation Rule/Run、消费幂等记录、Integration Trace 元数据 | 平台联动真相/执行状态 |
| Identity / Security | User Identity、Role、Permission Registry、Role Binding、Session、Verification Challenge、Security Policy、KeyRing / KeyVersion / WrappedKeySnapshot / RecoveryOperation | 身份、安全真相；部分高敏感 |
| Administration / Operations | Application Parameter、Dictionary、Menu、Announcement、Notification 元数据、Audit、Login Log、Security Event、Scheduled Job、Job Run、Health History、Alert | 平台管理与运维真相/历史 |
| Productivity / Planning | Task、Project、Goal、OKR、Milestone、Calendar、Time Block、Reminder、Habit、Focus、Review | 普通业务真相 |
| Personal Finance / Accounting | Ledger、Account、Transaction、Split、Category、Payee、Budget、Scheduled Transaction、Reconciliation、Exchange Rate | Sensitive 普通业务真相，不默认 E2EE |
| Private Notes | Vault、Note、Revision、Notebook/Tag 安全结构、同步元数据、Tombstone | Secure Domain 密文业务真相 |
| Password Manager | Vault、Vault Item、Folder/Shared Vault、Revision/History、同步元数据、Tombstone | Secure Domain 密文业务真相 |
| Analytics | Fact、Aggregate、Metric Definition、Snapshot、Lineage、Report 元数据 | 派生数据；Metric Definition 是定义真相 |
| Search | Search Projection、Index Checkpoint、Rebuild State | 派生数据 |
| AI Intelligence | Provider/Model Registry、Prompt Definition/Version、Conversation Policy、AI Artifact 元数据、Agent Run、Usage、Embedding 状态 | 配置/执行真相 + 派生数据 |
| AI Persona | Persona、Persona Version、Scenario Profile、User Preference、Conversation Persona Snapshot | AI 配置真相 |
| Plugin | Plugin Registry、Capability/Permission Declaration、Plugin-owned Configuration Reference | 平台配置真相 |

### 5.4 管理界面不等于数据库所有者

CMS 中“用户管理”“权限管理”“安全设置”可以都位于 Platform Administration 页面，但 UI 所在位置不决定数据库 Owner。

例如 Identity / Authorization / Crypto 的核心记录应由 Security / Identity 领域作为唯一规则所有者；Administration 只是管理入口，不能再维护一套重复 User / Role / Key 表。

### 5.5 Generic Relation 与领域内部关系分离

不是所有关系都应该塞入一张通用 Relation 表。

- 跨领域、跨类型、可扩展的长期关系可以进入平台 Generic Relation 能力；
- Episode → Series、Transaction → Account、Task → Parent Task 等强领域关系应由领域模型自己保存；
- 通用 Relation 不能替代领域不变量和真实外键；
- 关系记录不得要求调用者理解目标子系统私有表结构。

---

## 6. 数据身份与主键规则

### 6.1 核心实体统一 UUIDv7

所有具有独立生命周期、可以被独立引用的持久化实体，主键统一使用 UUIDv7。

PostgreSQL 类型：

```text
uuid
```

禁止核心实体使用：

- Auto Increment Integer；
- Snowflake ID；
- UUIDv4；
- 第三方 Provider ID；
- 文件路径；
- Object Storage Key；
- 内容 Hash；

作为内部主键。

### 6.2 UUIDv7 由统一 ID Generator 产生

UUIDv7 生成必须由平台统一能力提供，并处理：

- 同毫秒高并发；
- 系统时间回拨；
- 多 Worker / 多节点未来扩展；
- 可测试性；
- 实现升级一致性。

各子系统不能自行选择 UUID 库、生成顺序或回拨策略。

### 6.3 UUIDv7 不承担其他语义

UUIDv7 只表达实体身份和有利于索引局部性的时间有序特征。

明确禁止：

```text
created_at = timestamp_from_uuid(id)
ORDER BY id 作为严格业务时间排序
id 作为 Revision Number
id 作为业务 Sequence
```

真实时间、业务排序、版本、序号都必须拥有独立字段。

### 6.4 三类身份必须分离

```text
Internal Entity Identity
        → UUIDv7

External Provider Identity
        → Provider / Namespace + External ID Mapping

Content Identity
        → Cryptographic Hash
```

External Identity 可以变化或被修正，Blob Hash 表达内容身份，二者都不能替代内部 UUIDv7。

### 6.5 非实体关联不机械增加 UUID

以下数据不要求为了“统一”强制增加独立 UUID 主键：

- 纯多对多关联表；
- 不可独立寻址的 Value Object；
- 统计 Bucket；
- 排序位置；
- Revision Sequence；
- 某些复合唯一映射。

这类结构可以使用复合主键、业务唯一约束或整数序号，只要它们没有被提升成系统独立实体身份。

---

## 7. 实体公共字段与版本语义

### 7.1 普通持久化实体基线

具有独立生命周期的普通实体原则上包含：

```text
id           uuid        // UUIDv7
created_at   timestamptz
updated_at   timestamptz
version      bigint      // 需要乐观并发控制时
```

按领域需要增加：

```text
created_by   uuid
updated_by   uuid
deleted_at   timestamptz
```

这些字段不是机械模板：

- 关联表不必复制全部公共字段；
- Immutable Event 不需要 `updated_at`；
- Append-only Revision 可能没有普通实体式 `version`；
- `deleted_at` 只属于真正采用软删除语义的对象。

### 7.2 不同 Version 不得混为一个字段

V2 中存在多种完全不同的“版本”：

| 版本类型 | 语义 |
|---|---|
| Entity `version` | 乐观并发控制，避免 Lost Update |
| Revision Sequence / Revision ID | 文档、私密笔记、Vault Item 等内容版本历史 |
| Database Schema Version | 当前数据库结构与 Server 的兼容状态 |
| Event Contract Version | Event Payload Schema 版本 |
| API Version | 对外 HTTP 契约版本 |
| Plugin API Version | 插件公共扩展契约版本 |
| Export Format Version | 导出包结构版本 |
| Crypto Version | 密文 Envelope / 算法格式版本 |
| Key Version | KeyRing 中实际使用的密钥版本 |
| Metric Version | Analytics 指标口径版本 |
| Prompt Version | AI Prompt 定义版本 |
| Persona Version | AI Persona 配置版本 |

任何实现都不得用一个通用 `version` 字段承载上述全部语义。

---

## 8. 时间、日期与时区

### 8.1 所有真实时间点使用 `timestamptz`

以下字段都属于真实时间点：

- `created_at` / `updated_at`；
- `occurred_at`；
- `started_at` / `ended_at`；
- `expires_at`；
- `scheduled_at`；
- `completed_at`；
- `last_success_at`；
- `next_retry_at`；
- `deleted_at`；
- 登录、审计、Event、Trace 等时间。

PostgreSQL 中统一使用：

```text
timestamptz
```

禁止用 `timestamp without time zone` 保存实际发生时刻。

### 8.2 默认应用时区为 UTC+8

数据库保存绝对时间点；客户端展示与自然日计算读取 Instance 的 Application Timezone。

默认：

```text
UTC+8
```

浏览器、手机或桌面设备当前系统时区不能静默覆盖应用时区。

### 8.3 `date`、Local Time 与时间点必须区分

以下数据不是同一种时间语义：

```text
Instant                 → timestamptz
Calendar Date           → date
Wall Clock Time         → time / structured local time
Schedule Timezone       → explicit timezone identifier
Recurrence Rule         → rule + timezone context
```

例如“每月 1 日 09:00”不是一个固定 UTC Instant。Scheduled Job、Reminder、Recurring Task、Scheduled Transaction、账期等需要保存：

- 规则；
- 当地墙上时间；
- Timezone；
- 实际每次触发产生的 `scheduled_at` / `started_at` 等 `timestamptz`。

未显式指定 Timezone 时使用应用配置时区。

### 8.4 Timezone 应使用稳定标识

需要长期表达地区时区规则的场景应优先保存 IANA Timezone ID，而不是只保存当前 UTC Offset。

例如：

```text
Asia/Shanghai
America/Los_Angeles
```

UTC Offset 适合表达某个已发生时间点的序列化，不足以表达未来 DST 规则。

### 8.5 Analytics 使用事实时间而不是入库时间

Late-arriving Fact、离线客户端延迟同步等场景必须按原始事实 `occurred_at` 和应用时区归属历史窗口，不能把“三天前发生、今天同步”的行为全部计入今天。

---

## 9. 基础数据类型规范

### 9.1 String

默认优先使用 PostgreSQL `text`。

只有字段长度本身属于业务不变量时才使用明确长度限制，不使用习惯性的 `varchar(255)` 代替真实约束设计。

### 9.2 Boolean

真实二值语义使用 `boolean`。

如果业务存在 `unknown`、`not_applicable` 等第三状态，不得用 nullable boolean 模糊表达，应该设计显式状态。

### 9.3 Enum / Status Code

数据库和外部契约中的枚举值使用稳定字符串 Code，不使用 Java ordinal 或其他语言内部数值序号。

推荐：

- 跨 API / Event / Export 的状态使用稳定字符串；
- 领域内可通过 `CHECK` 约束已知写入值；
- PostgreSQL ENUM 不是默认选择，只有真正封闭、领域内部、Migration 成本可接受的集合才考虑使用；
- 读取端必须对未来未知公共枚举值具备安全降级能力。

### 9.4 Numeric / Money

金额、余额、汇率等不能使用浮点数表示精确财务事实。

Accounting 应使用：

- 精确 `numeric`；
- 独立 Currency Code；
- 明确精度/舍入规则；
- 历史 Exchange Rate 与 Provenance。

具体 precision / scale 由 Accounting PostgreSQL 详细设计确定。

### 9.5 Duration / Quantity

时长、字节数、计数等应保存明确单位的数值，并由字段契约声明单位。

不得让同名字段在不同表中一会儿表示秒、一会儿表示毫秒而没有显式语义。

### 9.6 JSONB

JSONB 适合：

- Provider 可扩展 Metadata；
- Event Payload；
- 能力声明；
- 不同 Plugin 的扩展属性；
- Snapshot / Policy 中真正半结构化且需要整体版本化的部分。

JSONB 不适合：

- 把 Resource 所有核心业务字段塞进一个 `metadata`；
- 用自由 JSON 替代重要外键；
- 把高频过滤字段藏进 JSON；
- 逃避字段约束与 Migration；
- 保存未分类的 Secret Payload。

如果某 JSONB 属性成为稳定、高频查询、唯一约束或领域不变量，应升级为显式字段或领域结构。

### 9.7 Array

PostgreSQL Array 只用于真正属于单个实体、没有独立生命周期和关系语义的小型原子集合。

Tag、Member、Permission Binding、Attachment Relation 等有独立关系语义的数据不能为了少建表而塞进 Array。

### 9.8 Binary / Ciphertext

Secure Domain 的密文可以在 PostgreSQL 中使用 `bytea` 或适合的编码形式保存，但必须具有自描述 Encrypted Envelope 元数据。

禁止只保存一个裸密文字节数组并依赖“当前代码知道算法”。

至少需要可解析：

- format_version；
- algorithm；
- key reference / key version；
- nonce；
- ciphertext；
- authentication tag 语义；
- AAD version；
- 必要的压缩/编码信息。

大型 Secure Attachment 仍走 Encrypted Blob，而不是数据库 LOB。

---

## 10. Null、Unknown 与 Partial Update

数据库设计必须保留系统概要中的语义区分：

```text
NULL
≠
UNKNOWN
≠
NOT_APPLICABLE
≠
EMPTY
```

统一规则：

1. `NULL` 只表示字段契约明确允许的“当前无值”。
2. `UNKNOWN` 是业务上存在该属性，但当前无法确认，必要时使用显式状态。
3. `NOT_APPLICABLE` 表示对当前对象不适用。
4. Empty Collection 是存在但为空，不等于未提供。
5. PATCH 中“字段缺失”和“字段显式 null”在 API 层必须可区分，持久化层不得把两者都变成同一个无条件 UPDATE。
6. 不允许为了减少列而让一个 `status` / `metadata` 字段承担所有未知语义。

---

## 11. 数据敏感等级与数据库处理边界

### 11.1 数据库采用系统级五级分类

Database Overview 统一沿用 System Overview 的：

```text
Public
Shared
Private
Sensitive
Secure Domain
```

说明：`AI-Intelligence-Subsystem-Design.md` 中的 `PUBLIC / INTERNAL / PRIVATE / SECRET / LOCAL_ONLY` 是 AI Provider 路由和数据外发策略层的分类，不建立第二套数据库级敏感等级体系。

其中：

- AI `SECRET` 应映射到 Secret / Secure Domain 的受控数据处理，而不是允许新增普通明文 `SECRET` 列；
- AI `LOCAL_ONLY` 表达数据驻留/处理策略，不等价于关系数据库中的一个普通数据等级；
- 当专项文档术语不同，以 System Overview 的系统级分类作为数据库治理基准。

### 11.2 敏感等级不替代 ACL

Public 数据的修改仍需要 Permission。

Private / Sensitive 数据的读取仍需要 Owner / ACL / Scope 判断。

数据库敏感等级只决定：

- 是否可进入 Log；
- 是否可进入 Search；
- 是否可进入 Analytics；
- 是否可发送 AI Provider；
- 是否可进入 Cache；
- 是否可导出；
- 是否需要脱敏；
- 是否需要额外审计。

### 11.3 Secure Domain 不是一个普通 Flag

以下实现是错误的：

```text
is_secure = true
payload = plaintext
```

Secure Domain 必须真正进入 Secure Data Foundation：

```text
Plaintext
   ↓
Client / Authorized Crypto Boundary
   ↓
Encrypted Envelope
   ↓
PostgreSQL / Encrypted Blob / Backup
```

服务端持久层只能看到：

- 密文；
- Key / Crypto Version；
- 最小同步元数据；
- 安全允许公开的状态字段。

Private Notes 和 Password Manager 的标题、正文、标签、URI、Secret、附件文件名等是否公开，必须服从各自 Secure Domain 设计，不能因为查询方便擅自明文化。

### 11.4 Secret 永远不作为普通配置字段散落

以下内容不得普通明文持久化：

- Password；
- OTP；
- TOTP Seed；
- API Token；
- OAuth Refresh Token；
- Storage Credential；
- AI Provider Credential；
- Plugin Secret；
- Private Key；
- Recovery Code；
- Key Material。

普通子系统保存：

```text
credential_ref / secret_ref
```

并通过 Password Manager / Secure Credential Capability 使用 Secret。

### 11.5 Accounting 是 Sensitive，但默认不是 Secure Domain

Ledger、Account、Transaction、Budget、Reconciliation 等为了支持查询、统计、自动化与多端同步，默认使用普通 PostgreSQL 持久化。

完整银行卡号、银行 Credential、PIN 等高敏感值使用：

- Masked Value；
- Tokenized Reference；
- Field-level protected value；
- Secret Reference；

而不是把整个账本强制变成 USER_LOCKED_E2EE。

---

## 12. 关系、Foreign Key 与跨领域引用

### 12.1 同一所有权域优先使用数据库约束

同一 Owner Subsystem 内的真实关系应优先使用：

- Foreign Key；
- Unique Constraint；
- Check Constraint；
- Exclusion / Partial Unique 等适合的 PostgreSQL 约束。

不要把所有完整性检查都留给 Java 代码。

### 12.2 跨 Owner Foreign Key 不是默认方案

跨子系统直接建立 FK 会形成：

- Migration 顺序耦合；
- 删除语义耦合；
- Schema 内部结构暴露；
- 未来拆分困难。

因此默认策略为：

1. 保存目标系统公开的稳定 UUIDv7 ID；
2. 通过 Capability / Command 校验实时业务条件；
3. 长期、跨类型关系使用 Generic Relation；
4. 不通过 JOIN 对方私表完成核心业务逻辑。

### 12.3 允许的跨域 FK 例外

对极少数稳定的平台根实体，可以在满足全部条件时建立跨域 FK：

- 被引用身份是正式公共稳定身份；
- 生命周期和删除语义已经明确；
- 两个 Owner 明确接受 Migration 依赖；
- FK 只保证存在性，不成为调用方读取对方私有字段的借口；
- 不使用跨域 `ON DELETE CASCADE` 隐式删除对方业务数据。

例如是否对某些 `user_id` / `resource_id` 建立物理 FK，应由对应数据库详细设计根据上述条件决定，而不是全局机械统一。

### 12.4 多态引用

跨类型引用如果目标可能属于多个 Domain，应使用：

```text
target_type + target_id
```

或平台 Relation 契约，而不是创建几十个 nullable FK 列。

但多态引用必须有稳定类型 Registry / Contract，不允许自由文本目标类型。

### 12.5 Cascade 删除严格受限

`ON DELETE CASCADE` 只适合：

- 同一 Owner；
- 子记录没有独立生命周期；
- 删除语义确实是 Aggregate 内部强从属。

Blob、Audit、Revision、Share、Backup、Secure Snapshot、跨领域 Relation 等不能因为某个上游行被删除就被数据库级联无条件清除。

---

## 13. Resource、Attachment、Blob 的数据库边界

### 13.1 Resource 是逻辑身份

数据库中的 Resource 不保存“永久物理路径”作为业务身份。

资源可以拥有：

- 多语言 Title / Alias；
- Metadata；
- Provenance；
- External Identity；
- Lifecycle；
- Collection / Tag / Relation；
- User State；

但实际内容通过 Attachment / Blob 体系管理。

### 13.2 Attachment 是业务内容对象

Attachment 负责表达：

- 用途；
- MIME / Media Type；
- 来源；
- 与 Resource 的业务关系；
- Derived / Original 语义；
- 当前业务可用状态。

Attachment 不以文件路径作为永久身份。

### 13.3 Blob 是内容身份

Blob 元数据至少应能够表达：

- UUIDv7 实体身份；
- Cryptographic Hash；
- Hash Algorithm；
- Size；
- Integrity State；
- Content Type / Sniffed Type（按专项设计）；
- 生命周期/保留语义。

Blob 去重以内容 Hash 与必要校验为基础，不以文件名、URL 或 Path 为基础。

### 13.4 Placement / Replica 是物理位置

同一 Blob 可以有多个 Placement。

Placement 负责表达：

- Storage Provider；
- Tier；
- Object Key / Physical Locator；
- Replica State；
- Verified At；
- Restore / Migration State；
- Storage-specific Metadata。

物理位置可以变化，Blob Identity 不变。

### 13.5 Blob GC 是独立生命周期

Resource / Attachment 进入 Trash 或 Purged 不等于 Blob 可以立即删除。

GC 必须检查至少：

- 其他 Attachment 引用；
- Revision；
- Derived Relationship；
- Share；
- Backup / Snapshot；
- Archive / Replica；
- Secure Snapshot；
- Retention Hold。

真正物理删除应通过受控 Background Task / GC Run 执行，并形成 Audit 与可追踪结果。

---

## 14. 事务边界

### 14.1 单领域事务保证强一致

同一个 Owner Subsystem 的业务不变量可以且应使用 PostgreSQL 本地事务保证。

例如：

```text
Create Task
├── Insert Task
├── Insert domain-owned dependency/section metadata
└── Insert Outbox Event
```

或者：

```text
Create Transfer
├── Create one Transfer business fact
├── Update/derive required account state according to Accounting rules
└── Insert finance.transaction.created Outbox Event
```

### 14.2 跨领域不建立系统级大事务

禁止默认使用一个 ACID 事务完成：

```text
Publish Article
+ Complete Productivity Task
+ Update Analytics
+ Update Search
+ Send Notification
```

正确模型：

```text
Article Domain Commit
        ↓
article.published Event
        ↓
Search / Analytics / Automation / Notification eventually process
```

后续消费者失败不回滚已经成立的源业务事实。

### 14.3 补偿而不是跨域回滚

跨域业务需要撤销时使用显式补偿 Command，例如：

```text
CreateShare
...
RevokeShare
```

不要依赖跨多个 Owner Schema 的数据库 Rollback 模拟业务补偿。

---

## 15. 并发控制

### 15.1 乐观并发是默认优先策略

适合：

- Resource 编辑；
- Parameter 修改；
- Task / Goal 编辑；
- Persona / Prompt 配置；
- 普通可编辑业务实体。

通过：

- Entity Version；
- ETag；
- Revision；

避免 Lost Update。

### 15.2 Revision 与乐观锁分离

Document Revision、Private Note Revision、Vault Item History 是业务历史；`version` 是并发控制。

一次业务内容更新可以同时：

- 生成新的 Revision；
- 推进实体 `version`；

二者不能互相替代。

### 15.3 Pessimistic / Advisory Lock 只用于明确场景

对于：

- 唯一 Background Task Claim；
- 单实例调度抢占；
- 某些 GC / Migration 互斥；
- 需要串行化的极少数领域操作；

可以使用 Row Lock 或 PostgreSQL Advisory Lock。

但锁必须：

- 有明确粒度；
- 有超时；
- 不跨长时间外部网络调用；
- 不持有到 AI / Object Storage / Provider 请求结束；
- 可观测。

---

## 16. Durable Event / Outbox

### 16.1 关键 Event 必须持久化

影响以下消费者的关键 Event 不得只存在于内存：

- Search；
- Analytics；
- Automation；
- Notification；
- Storage Migration / Restore；
- Audit 衍生流程；
- Plugin / Worker。

推荐事务模式：

```text
BEGIN
  update domain state
  insert durable outbox event
COMMIT
        ↓
Dispatcher
        ↓
At-least-once delivery
```

### 16.2 Event Envelope

持久化 Event 至少包含语义：

```text
event_id            UUIDv7
event_type          stable string code
schema_version      event contract version
occurred_at         timestamptz
producer_subsystem
actor / principal reference
subject / aggregate reference
correlation_id
causation_id
payload
```

派发系统可以附加：

- dispatch status；
- available_at；
- attempt count / attempt history；
- last error summary；
- dispatched_at；

但这些派发元数据不能改变 Event 已经发生的事实内容。

### 16.3 Event Payload 的边界

Event Payload：

- 使用稳定 Contract；
- 保存最小必要事实；
- 不复制完整业务表快照作为默认做法；
- 不携带 Secret；
- 不携带 Secure Domain 明文；
- Sensitive 字段按数据等级最小化；
- 可使用 JSONB，但必须有 `schema_version`。

### 16.4 LISTEN / NOTIFY 只用于唤醒

PostgreSQL LISTEN / NOTIFY 可以提示 Dispatcher“有新 Outbox 记录”，但不能成为唯一事件存储。

持久化 Outbox 才是可靠来源。

---

## 17. Event Consumer 幂等

平台按 At-least-once 设计，因此重复投递是正常情况，不是异常情况。

消费者应使用以下一种或多种方式幂等：

- `event_id`；
- `idempotency_key`；
- `(consumer_key, event_id)` Unique Constraint；
- processed-event / inbox record；
- 领域天然唯一约束；
- Command 自身的业务幂等规则。

Analytics 必须保证同一个 Event 不形成重复 Fact。

Automation 不得因一个重复 Event 创建多个相同 Task。

Notification、Webhook、外部 Sync 等也必须定义自己的重复投递语义。

幂等记录的 Retention 不能短于系统可能发生的有效重放窗口；清理策略由对应执行系统详细设计。

---

## 18. Background Task / Job Run / Automation Run 持久化

### 18.1 三种 Task 不共享业务实体

必须继续区分：

```text
Productivity Task
= 用户待办

Background Task
= 系统一次异步执行

Scheduled Job
= 何时触发系统动作的规则
```

数据库中不能因为名称都叫 Task 就做成一张大表。

### 18.2 异步执行基础状态

Background Task、Job Run、Automation Run、Webhook Delivery、异步 Plugin Action 等至少遵循：

```text
PENDING
RUNNING
SUCCEEDED
FAILED
CANCELLED
TIMED_OUT
```

可以根据专项增加：

```text
QUEUED
CANCELLING
RETRYING
THROTTLED
WAITING_FOR_USER
WAITING_FOR_TOOL
```

但必须保持系统级语义：

- Failed = 本次执行因错误终止；
- Cancelled = 明确取消，不等于失败；
- TimedOut = 超过允许时间，不等于用户取消；
- Succeeded = 达到业务定义成功条件；
- 等待资源不能伪装成 Failed。

Automation 多 Action 场景可以另外表达 `PARTIAL` 聚合结果，但不能因此抹掉每个 Action 的真实执行状态。

### 18.3 Logical Run 与 Attempt 分离

一次逻辑执行重试时，不覆盖上一次失败记录。

建议语义：

```text
Logical Run
├── Attempt 1 FAILED
├── Attempt 2 TIMED_OUT
└── Attempt 3 SUCCEEDED
```

Attempt 至少记录：

- Attempt Number；
- Started At；
- Ended At；
- Worker / Executor Identity；
- Error Classification；
- Error Summary；
- Retryable；
- Trace / Correlation；
- Result Summary。

Retry 继承原逻辑执行的：

- Actor；
- Permission Context；
- Idempotency Context；
- Correlation / Trace；
- Business Association。

### 18.4 Claim / Lease

未来独立 Worker 或多节点执行时，任务获取应支持受控 Claim / Lease 语义，避免两个 Worker 同时把同一 Attempt 当成唯一执行者。

Lease 数据需要明确：

- claimed_by；
- claimed_at；
- lease_expires_at；
- heartbeat / last_seen；

具体字段由 Background Task 详细设计确定。

### 18.5 遗留 Running 恢复

Server / Worker 重启后不能永久留下 Running。

执行系统必须根据：

- Lease 是否过期；
- Handler 是否支持 Resume；
- 幂等能力；
- Attempt 历史；
- Retry Policy；

决定恢复、创建新 Attempt、标记失败或进入人工处理。

---

## 19. Scheduled Job 的数据库时间语义

Scheduled Job 至少持久化：

- Schedule Type（Cron / Fixed Interval / One-shot）；
- Schedule Definition；
- Timezone；
- Enabled State；
- Misfire Policy；
- Concurrency Policy；
- Retry Policy；
- Next Fire / Last Fire 等运行派生信息；
- Definition Version / Optimistic Version。

每次触发产生独立 Job Run / Background Task。

Job Run 保存实际：

- scheduled_at；
- started_at；
- ended_at；
- trigger_type；
- result；
- error summary；
- actor（手动触发时）；

不能只更新 Scheduled Job 上的一个 `last_status` 而丢失历史。

---

## 20. Domain-specific 数据约束示例

本文档不锁定全部 DDL，但以下已有 V2 语义必须被后续 Schema 明确表达。

### 20.1 Productivity

- Task Lifecycle 与 Overdue 分离；Overdue 是派生状态。
- Scheduled Time 与 Deadline 分离。
- Task / Calendar Event / Time Block / Habit / Recurring Task 分离。
- Fixed Recurrence 与 Completion-based Recurrence 分离。
- Focus Session 保存真实 Started/Ended/Duration，不用 Task `created_at → completed_at` 推算实际耗时。
- Offline 修改需要 Revision / Version / Idempotency 支持。

### 20.2 Accounting

- Transfer 是单一业务事实，不建成两条互不关联的收入/支出事实。
- Split Transaction 具有明确子项约束。
- Scheduled Transaction Rule 与已发生 Transaction 分离。
- Reconciliation 不通过静默篡改历史交易“对平”。
- Adjustment 应保留来源和 Audit。
- Exchange Rate 保留 Effective Time 与 Provenance，历史报表不能因今天汇率变化无声改变。
- Amount / Balance / Rate 使用精确数值类型。

### 20.3 Security

Verification Challenge 至少承载：

- User；
- Method；
- Purpose；
- Target Reference；
- OTP Digest；
- Issued / Expired / Consumed Time；
- Attempt Count / Maximum Attempts；
- Status。

OTP 明文永不持久化。

Key Management 需要明确：

```text
KeyRing
  └── KeyVersion
      ├── ACTIVE
      ├── DECRYPT_ONLY
      ├── REVOKED
      ├── COMPROMISED
      ├── DESTROY_PENDING
      └── DESTROYED
```

WrappedKeySnapshot 保存包装后的 Key Material，禁止保存旧密钥明文。

每份 Ciphertext 必须能够定位：

- Crypto Version；
- Key Version / Key Reference。

### 20.4 Private Notes

服务端持久化默认只看到：

- Note / Vault / Owner ID；
- Crypto Version；
- Ciphertext Size；
- Revision Sequence；
- Sync Metadata；
- 最小时间戳；
- Tombstone。

Title、Body、Tag、Notebook Name、Attachment Filename 默认加密。

Conflict 不能简单 Last Write Wins，应保留 Conflict Revision。

### 20.5 Password Manager

Vault Item 除最小同步元数据外默认密文。

Login URI 也可能泄露用户使用的网站，默认属于 Protected Metadata。

Password History、TOTP Seed、Passkey Private Material、SSH Private Key、Recovery Code 等均属于 Secret Payload。

其他子系统只保存 `secret://...` Reference，不复制明文。

### 20.6 Analytics

Fact 必须保留：

- Source Event / Source Identity；
- Fact Time；
- Dimension；
- Schema / Metric Definition 关联；
- Idempotency 信息。

Aggregate / Materialized View 可重建。

Metric Definition 是正式口径定义，需要：

- Key；
- Version；
- Owner；
- Time Semantics；
- Null Semantics；
- Privacy Level；
- Source Facts；
- Dimensions。

### 20.7 AI / Persona

AI Provider Credential 只保存 Secret Reference。

Embedding、Summary、Transcript、Translation、AI Cache 是派生数据；源 Resource 修改后可以进入 STALE 并重建。

Prompt、Persona、Metric、Model Profile 都有各自版本，不能把“当前配置行被 UPDATE 了”当作可复现版本历史。

Conversation / Agent Trace 应保存实际使用的 Persona Version、Prompt Version、Model / Provider 等 Provenance，但不默认永久保存所有敏感 Prompt 明文。

---

## 21. 索引设计原则

### 21.1 Primary Key

UUIDv7 `uuid` 主键使用 PostgreSQL B-tree 即可作为默认基线。

UUIDv7 的时间局部性有利于追加型写入，但不因此取消独立业务排序索引。

### 21.2 Foreign Key Index

PostgreSQL 不会为 FK 自动创建所有必要的引用侧索引。

高频：

- Parent / Owner；
- Resource / Collection；
- Blob / Placement；
- Ledger / Transaction；
- Project / Task；
- Vault / Item；

等关系应根据查询和删除检查路径显式设计索引。

### 21.3 Unique Constraint 优先表达业务唯一性

例如：

- External Identity 的 Provider/Namespace + External ID 映射唯一性；
- 幂等 Key；
- Event Consumer 去重；
- 某些领域内唯一 Binding；

应优先由数据库 Unique Constraint 兜底，而不是只执行“先查后插”。

### 21.4 Partial Index

适合：

- Active / Pending / Unread / Not Deleted 等高频子集；
- Outbox 待派发；
- Background Task 待执行；
- Alert OPEN；
- Scheduled Job Enabled。

但索引条件必须对应稳定业务语义，不能为了当前一条 SQL 临时堆叠大量索引。

### 21.5 JSONB / GIN

只有对 JSONB 内字段确实存在稳定查询需求时才建立 GIN / Expression Index。

如果查询已成为核心路径，应重新评估该属性是否应该结构化。

### 21.6 Search Index 与业务索引分离

普通业务数据库索引负责：

- 精确查询；
- 范围查询；
- 关系导航；
- 约束执行。

全文搜索、语义检索、复杂 Ranking 属于 Search Subsystem。

Search 可以在 V2 初期使用 PostgreSQL FTS / Vector Extension，但这些 Projection 仍然是可重建派生数据，不应反向成为 Resource 真相。

### 21.7 不使用 `ORDER BY id` 替代业务排序

稳定分页需要明确排序键，例如：

```text
ORDER BY created_at DESC, id DESC
```

或者领域自己的：

- occurred_at；
- scheduled_at；
- sort_order；
- revision_sequence；
- title sort key。

UUIDv7 可以作为 Tie-breaker，但不是严格业务时间。

---

## 22. Pagination 与大数据量

PRD 目标至少覆盖十万级 Resource，更大规模 Attachment / Event / Activity / Fact。

因此：

1. 常用列表必须有与 Filter / Sort 匹配的索引。
2. 深分页默认优先 Keyset / Cursor Pagination，而不是无限 `OFFSET`。
3. Cursor 必须包含稳定排序所需的全部键。
4. 大表查询不得默认 `SELECT *` 返回大型 JSON / Ciphertext / Payload。
5. Event、Audit、Activity、Fact、Job Run 等历史表应从设计阶段考虑时间范围查询与 Retention。
6. 是否 Partition 必须由真实数据量、保留周期和运维收益驱动，不能在 V2 初期机械给每张历史表分区。

适合未来评估 Partition 的候选包括：

- 高体量 Event / Outbox Archive；
- Audit / Login Log；
- Analytics Fact；
- 高频 Activity；
- 长期 Job / Run History。

但 Partition Key 必须与查询和 Retention 策略一致。

---

## 23. Database Schema Version 与兼容性

### 23.1 Database Schema Version 是独立契约

必须明确区分：

```text
Database Schema Version
API Version
Plugin API Version
Event Contract Version
Export Format Version
```

Server 版本号不能替代 Database Schema Version。

### 23.2 Schema Compatibility Check

Server 启动前必须能判断当前数据库至少属于以下状态之一：

```text
READY
MIGRATION_REQUIRED
MIGRATING / MAINTENANCE
INCOMPATIBLE_NEWER
MIGRATION_FAILED
VALIDATION_REQUIRED
```

具体实现状态名称可调整，但必须能够防止：

- 老 Server 对新 Schema 继续写入；
- 未完成 Migration 的数据库正常对外提供写服务；
- Migration 失败后应用假装健康启动；
- 无法判断当前数据库版本。

### 23.3 Overall Version 与 Owner Migration State

由于数据库由多个 Owner Subsystem 共同演进，兼容判断需要同时知道：

- 整体 Database Schema compatibility；
- 每个 Owner 的 Migration 历史是否完整；
- 当前 Server 需要的最低/最高支持状态。

可以由统一 Migration Framework 维护历史，也可以有额外兼容元数据；具体工具由工程实施阶段确定。

本文档不因 V1 使用过某 Migration 工具而锁定 V2 工具。

---

## 24. Migration Ownership 与生产迁移

### 24.1 所有生产 DDL 必须版本化

禁止生产环境依赖：

- ORM auto create；
- ORM auto update；
- 启动时根据 Entity 自动猜测 Schema；
- 管理员临时手工改表后不形成 Migration。

### 24.2 每个 Owner 拥有自己的 Migration

Migration 必须能追溯：

- Owner Subsystem；
- Migration Version / ID；
- Change Purpose；
- Compatibility Requirement；
- 是否包含 DDL；
- 是否需要独立 Backfill；
- 是否需要 Maintenance / Feature Gate。

### 24.3 全局执行顺序必须确定

即使各 Owner 独立维护 Migration，同一个 Database 的最终执行顺序也必须确定、可重复、可审计。

不能让“模块扫描顺序”决定生产数据库最终结构。

### 24.4 Expand → Migrate → Contract

对需要兼容滚动升级、长 Backfill 或风险较高的变更，优先采用：

```text
Expand
  添加兼容结构
        ↓
Application Compatibility Window
        ↓
Backfill / Data Migration
        ↓
Validate
        ↓
Contract
  删除旧结构 / 收紧约束
```

例如新增一个最终必须 `NOT NULL` 的字段：

1. 先增加 nullable / default-compatible 字段；
2. 新代码开始写新字段；
3. Background Data Migration 分批回填历史数据；
4. 校验无缺失；
5. 后续 Migration 再增加 `NOT NULL` / Unique 等最终约束；
6. 经过弃用窗口后再删除旧字段。

### 24.5 长时间数据迁移不得塞进短 DDL

以下工作不得作为一个长时间持锁的启动 Migration 强行执行：

- 百万级历史数据重算；
- 大型 Ciphertext Re-encryption；
- 全量 Embedding；
- Search Index Rebuild；
- Analytics Rebuild；
- Blob Hash Recheck；
- 大规模 Resource/Attachment 转换；
- 大规模对象存储迁移。

它们应成为：

- Background Task；
- 专用 Data Migration Job；
- 可暂停、可重试、可观测的 Batch Process。

### 24.6 生产恢复不以 Destructive Rollback 为主

生产升级发生问题时，主要恢复路径应是：

- 修复后向前 Migration；
- Feature Gate；
- 应用版本兼容窗口；
- 必要时从经过验证的 Backup 恢复。

不能把“自动执行 DROP/反向 Migration”作为默认灾难恢复策略。

---

## 25. Migration 的锁与在线变更

生产 Migration 应显式评估：

- DDL Lock；
- Table Rewrite；
- Index Build；
- Constraint Validation；
- Backfill Write Amplification；
- Object / Row Count；
- 连接池与 Worker 影响。

对可能长时间阻塞的操作应：

- 拆分 Migration；
- 使用 PostgreSQL 支持的低锁策略；
- 设置合理 Lock Timeout；
- 将 Index Build / Validation 设计为独立步骤；
- 在运维 UI 中暴露 Migration / Maintenance 状态。

Database Overview 不锁定每种 DDL 的最终 SQL，但要求任何生产 DDL 在提交前说明锁风险。

---

## 26. Lifecycle、Soft Delete、Tombstone 与 Purge

### 26.1 不建立全局统一 Soft Delete

并非每张表都需要 `deleted_at`。

需要根据领域区分：

- Archive；
- Trash；
- Tombstone；
- Disable；
- Revoke；
- Purge；
- Physical Delete。

这些语义不能全部压缩成 `deleted = true`。

### 26.2 Resource 生命周期

Resource 至少保持：

```text
ACTIVE
ARCHIVED
TRASHED
PURGED
```

具体状态 Code 与迁移由领域详细设计确定，但逻辑删除与 Blob GC 必须分离。

### 26.3 Tombstone 用于同步删除事实

Private Notes、Password Manager、Offline Planning 等需要增量同步的领域可以使用 Tombstone，确保离线设备知道对象已经被删除。

Tombstone 的 Retention 必须覆盖合理的离线同步窗口或设备 Checkpoint 语义，不能删除业务对象后立刻把 Tombstone 也删掉。

### 26.4 Purge 是高风险业务动作

Purge 应：

- 权限校验；
- Step-up Verification（适用时）；
- 影响范围计算；
- Audit；
- 必要时异步执行；
- 不直接跨域数据库级联删除所有关联对象。

### 26.5 User 删除不破坏历史事实

User Deactivation / Deletion 不能直接导致：

- Resource 无 Owner 而损坏；
- Revision 丢失；
- Audit Actor 丢失；
- Accounting 历史失去来源；
- Security Event 无法解释。

后续 Identity / Ownership 详细设计必须明确：

- Deactivate；
- Ownership Transfer；
- Anonymize；
- Historical Actor Snapshot；
- Retention。

---

## 27. Audit、Activity 与 Application Log 的数据库边界

三者保持独立：

```text
Application Log
= Runtime diagnosis

Audit Log
= 谁对什么执行了什么高价值操作

Activity
= 用户/业务行为时间线
```

### 27.1 Audit

Audit 记录至少包含：

- Actor；
- Action；
- Target Type / ID；
- Result；
- Occurred At；
- Request / Trace ID；
- 必要 Client / IP 摘要。

Audit：

- 不提供普通 CRUD 修改；
- Retention 由独立策略控制；
- 删除 Audit 本身也属于受控高风险管理动作；
- 不保存 Password、Token、Secure Payload 或完整敏感请求体。

### 27.2 Activity

Activity 可以由用户按产品规则删除或清理，不得用 Audit 代替 Activity，也不得为了用户可删除历史而删除安全 Audit。

### 27.3 Runtime Log

生产 SQL / R2DBC Debug 日志不得默认记录完整 Bind Parameter，尤其是：

- Secret；
- Secure Domain Ciphertext 之外的明文；
- 完整财务敏感字段；
- OTP；
- AI 敏感 Prompt；
- Authorization Header。

---

## 28. Search / Analytics / AI 派生数据

### 28.1 Search Projection

Search Document / Vector / FTS Projection：

- 可以与业务 PostgreSQL 物理同库；
- 也可以未来迁移到独立 Search Engine；
- 必须可以从业务数据重建；
- 必须记录必要的 Source Version / Checkpoint；
- 不能扩大原业务可见集合。

### 28.2 Analytics Fact

Fact 是标准化统计事实，不是业务当前状态。

Fact 可以从：

- Event；
- Activity；
- Audit；
- Job Run；
- Health Snapshot；
- 明确 Analytics Event；

生成。

同一 Source Event 只能形成一次相同 Fact。

### 28.3 Aggregate / Materialized View

Aggregate 与 Materialized View：

- 允许增量更新；
- 支持 Late-arriving Fact；
- 支持指定范围 Rebuild；
- 具有 Source / Metric Version；
- 不能回写业务状态。

### 28.4 Source 删除后的派生数据处理

当用户删除可删除 Activity 或业务源数据后，相关 Analytics 不能永久保留与源数据矛盾的结果。

应支持：

```text
Delete / Forget Source
        ↓
Invalidate affected fact/aggregate
        ↓
Rebuild affected range
```

### 28.5 AI Derived Data

Embedding、Summary、Transcript、Translation、Generated Metadata Candidate、AI Cache 等必须保留：

- Source Identity；
- Source Version / Hash；
- Provider / Model；
- Prompt Version；
- Created At；
- Provenance；
- State（例如 STALE）；

并随源数据生命周期清理或重建。

---

## 29. Configuration 与 Secret 的数据库边界

### 29.1 Persistent Application Configuration

Persistent Configuration 可以保存在 PostgreSQL，并遵循系统配置优先级：

```text
Built-in Default
        ↓
Configuration File / Environment
        ↓
Persistent Application Configuration
        ↓
Effective Configuration
```

每个可持久化配置至少需要表达：

- Key；
- Type；
- Current Value；
- Source；
- Editable；
- Validation Rule；
- Apply Mode；
- Reload / Restart Requirement；
- Sensitivity Metadata；
- Updated At / Updated By；
- Optimistic Version。

### 29.2 Secret 不进入普通 Parameter Value

普通 Parameter 即使拥有 `sensitive=true`，也不意味着可以保存明文 Password / Token。

Secret 只能保存：

- Secret Reference；
- 非敏感 Masked Metadata；
- 是否已配置等状态。

---

## 30. Backup / Restore 的数据库要求

### 30.1 PostgreSQL Backup 不是完整实例 Backup

完整恢复点至少涉及：

- PostgreSQL；
- Blob Storage；
- Storage Provider Configuration；
- Plugin Configuration；
- Secret / Key Material 的安全备份；
- 必要 Instance Metadata。

Search Index、Analytics Aggregate、Embedding 等派生数据默认可以重建，不要求为了恢复正确性必须备份。

### 30.2 Backup 必须携带 Schema Compatibility 信息

Backup 元数据至少应能够判断：

- Database Schema Version；
- Ikaros Version / Build 信息；
- Backup Created At；
- Blob / Database 一致性点语义；
- Secure Key Material 是否包含、如何恢复；
- 是否已经 Restore Verified。

### 30.3 Secure Domain Backup 仍然是密文

Backup 不得成为绕过 Secure Data Foundation 的明文导出通道。

Key Backup 与普通配置不能混放成一个无保护文件。

### 30.4 Restore 后重新构建派生状态

典型顺序：

```text
Restore PostgreSQL
        ↓
Restore / Attach Blob Storage
        ↓
Restore Secure Key / Secret Material
        ↓
Validate Schema / Referential Integrity
        ↓
Rebuild Search
        ↓
Rebuild Analytics / AI Derived Data as needed
        ↓
Run Integrity Check
```

### 30.5 Backup / Restore 与 Export / Import 分离

Export Format 是用户可迁移契约，不能只是数据库 Dump。

因此：

- 内部表名变化不应强迫 Export Format 同步破坏；
- Export 保存独立 Format Version；
- Import 根据公开数据语义写入领域 Command / Import Pipeline；
- External Identity 作为映射导出，不成为内部主键；
- Secure Domain Export 继续遵守加密与显式明文导出规则。

---

## 31. 数据库可观测性

### 31.1 Operations 必须能看到数据库健康

至少暴露：

- PostgreSQL Availability；
- Connection Pool；
- Query Error 摘要；
- Database Latency；
- Migration / Schema Version 状态；
- Outbox Backlog；
- Background Task Queue Depth；
- Long-running Data Migration；
- Storage Metadata Consistency Error；
- 必要的 Lock / Deadlock 摘要。

### 31.2 Query Trace

关键业务请求可以通过：

- request_id；
- trace_id；
- task_id；
- event_id；
- automation_run_id；

关联数据库写入和后续异步链路。

Trace 不应记录敏感参数本身。

### 31.3 慢查询治理

慢查询处理优先顺序：

1. 确认查询是否跨越了错误的领域边界；
2. 确认 Filter / Sort 是否有正确索引；
3. 确认是否应该使用派生 Search / Analytics；
4. 再评估 SQL、索引和数据布局优化；
5. 最后才考虑复杂缓存或额外基础设施。

不能通过增加一个缓存掩盖错误的数据所有权或全表扫描架构。

---

## 32. Security Database 特殊要求

### 32.1 OTP

OTP 只保存不可逆 Digest，且 Digest 仍受访问限制。

Challenge 必须支持：

- Purpose Binding；
- User Binding；
- Expiration；
- Attempt Limit；
- One-time Consume；
- Rate Limit 关联。

### 32.2 Key Snapshot

`WrappedKeySnapshot` 永远只保存 Wrapped Key Material。

销毁旧 Key Version 前必须证明：

```text
no ciphertext reference
AND no backup/revision reference
AND retention allows
AND no recovery hold
AND no security incident hold
```

### 32.3 Recovery Operation

每次 Key Recovery / Reset 是独立业务实体，不能只在 Key 表上 UPDATE 一个状态然后丢失过程历史。

至少可追踪：

- Requester；
- Target KeyRing；
- Required / Achieved Verification Level；
- Verification Method；
- Policy Version；
- Old / New Active Key Version；
- Status；
- Requested / Completed Time。

### 32.4 Biometric Evidence

Raw Face Image、Face Template、Embedding、Liveness Evidence、Verification Video 等最高敏感身份材料：

- 不进入普通 Blob；
- 不进入普通 Search；
- 不进入 Analytics；
- 不进入 AI Context；
- 不进入 Runtime Log；
- 不暴露 Plugin API；
- 若未来确需持久化，必须进入专门 Secure Domain 并有严格 Retention。

---

## 33. 命名与 Schema 编码规范

具体表名由领域详细设计确定，但数据库对象统一遵循：

- PostgreSQL identifier 使用 `lower_snake_case`；
- 主键统一 `id`；
- 外键/稳定引用使用 `<entity>_id` 或明确 `<role>_id`；
- 时间点使用 `*_at`；
- 纯日期使用 `*_date`；
- 乐观锁使用 `version`；
- Contract 版本明确使用 `schema_version` / `format_version` / `contract_version` 等有语义名称；
- Secret 引用使用 `*_secret_ref` / `*_credential_ref` 等明确命名；
- Hash 同时保存 Algorithm 语义，不把未知摘要塞进模糊 `hash` 字段后永久假定 SHA-256；
- Constraint / Index 名称应可识别 Owner、表和用途，便于 Migration 与故障诊断。

禁止：

- `data1` / `data2` 等无语义字段；
- `status int` + Java ordinal；
- `ext` JSONB 承担所有未来需求；
- `time bigint` 但不说明单位；
- `is_deleted` 作为所有生命周期的统一替代品。

---

## 34. 数据库访问边界

### 34.1 Repository 只能访问所属 Owner Schema

模块内部 Repository / DAO / R2DBC Adapter 应默认只直接访问本 Owner 的表。

禁止：

```text
MediaRepository
→ SELECT / UPDATE productivity private tables
```

或者：

```text
AutomationRepository
→ UPDATE finance transaction
```

### 34.2 Cross-domain Read 也不能成为隐式契约

即使只是 `SELECT`，长期依赖对方私有字段也会形成 Schema 耦合。

同步读取使用 Capability；批量分析使用 Event / Fact / Snapshot；长期关系使用 Relation / stable reference。

### 34.3 数据库账号权限是纵深防御

模块化单体初期不强制每个 Java 模块都使用不同 PostgreSQL Login，但数据库权限规划应允许未来：

- Worker 只访问其任务和公开写入路径；
- 独立 Search / Analytics Worker 只读取必要 Projection / Event；
- 运维工具使用受限账号；
- Migration 使用专门高权限账号；

不能因为初期只有一个 Server 进程就把“任意组件拥有超级数据库账号”写成永久架构假设。

---

## 35. Worker / 多节点演进

Database Overview 保持单机自托管优先，但数据库模型不能阻止未来：

- 独立 Transcode Worker；
- AI / OCR Worker；
- Search Rebuild Worker；
- Archive Restore Worker；
- Data Migration Worker。

为此需要：

- UUIDv7 可多节点生成；
- Background Task Claim / Lease；
- Outbox / Event 可持久重放；
- Idempotent Handler；
- 不依赖单进程内存作为唯一任务状态；
- 不使用 Server 本地时间代替数据库中的正式时间；
- Worker 仍通过公开 Command / Task Contract 改变业务状态，而不是获得跨域任意 SQL 写权限。

---

## 36. 明确禁止的数据库设计模式

V2 数据库层明确禁止以下方向：

1. **直接复刻 V1 Schema。**
2. **所有表统一加 `tenant_id`。**
3. **使用第三方 Provider ID 作为内部主键。**
4. **使用 UUIDv7 的时间部分替代 `created_at`。**
5. **用 `ORDER BY id` 表达严格业务时间。**
6. **用一个巨大 JSONB Resource 表承载所有专业领域。**
7. **插件直接读取/修改任意内部表。**
8. **Automation / AI 直接 UPDATE 目标领域表。**
9. **Search / Analytics / Cache 结果反向成为业务事实。**
10. **用 LISTEN / NOTIFY 代替 Durable Event Store。**
11. **将 Event 只发在内存中并认为数据库事务已经保证投递。**
12. **无限重试但不保存 Attempt 历史。**
13. **把 Failed / Cancelled / TimedOut 全部压缩成 FAILED。**
14. **把 Secret 塞进 Parameter、Event Payload、Log 或普通业务 JSON。**
15. **Secure Domain 只打一个 `is_secure` 标记却保存明文。**
16. **生产环境依赖 ORM Auto DDL。**
17. **把大规模 Backfill 放进一个长时间启动 Migration。**
18. **跨 Owner 使用 `ON DELETE CASCADE` 批量删除业务数据。**
19. **删除 Resource 时同步直接删除所有 Blob 字节。**
20. **用数据库 Dump 代替公开 Export Format。**

---

## 37. 后续数据库详细设计拆分

Database Overview 之后，各领域应继续补充自己的 PostgreSQL 详细设计，例如：

- Resource / Metadata / External Identity / Provenance Schema；
- Attachment / Blob / Placement / Retention / GC Schema；
- Identity / RBAC / ACL / Share / Audit Schema；
- Security Verification / KeyRing / KeySnapshot / Recovery Schema；
- Event Outbox / Consumer Inbox / Automation / Background Task Schema；
- Productivity / Calendar / Recurrence / Offline Sync Schema；
- Accounting Ledger / Transaction / Reconciliation Schema；
- Private Notes / Password Vault Ciphertext / Sync Schema；
- Analytics Fact / Aggregate / Metric Catalog Schema；
- Search Projection / Rebuild Checkpoint Schema；
- AI Provider / Prompt / Artifact / Agent Run / Persona Version Schema；
- Backup / Restore / Migration Metadata Schema。

这些详细设计可以决定具体表名、列、Constraint、Index 和 DDL，但不能反向破坏本文档定义的数据库边界。

---

## 38. Database Design Review Checklist

任何新的 V2 数据模型进入实现前，至少检查：

### Identity

- [ ] 独立实体是否使用 UUIDv7 / PostgreSQL `uuid`？
- [ ] 是否误用了 External ID、Hash、Path、Object Key 作为主键？
- [ ] 非实体关联是否避免了无意义的 UUID？

### Time

- [ ] 所有实际时间点是否为 `timestamptz`？
- [ ] 是否把 Date、Wall Time、Instant 正确区分？
- [ ] Recurrence / Schedule 是否显式保存 Timezone？
- [ ] 默认应用时区是否遵循 UTC+8？

### Ownership

- [ ] 该表只有一个明确 Owner Subsystem？
- [ ] Migration 是否由 Owner 维护？
- [ ] 是否出现跨域 Repository / SQL？
- [ ] Cross-domain Reference 是否只依赖稳定公开身份？

### Integrity

- [ ] 领域不变量是否尽可能有 FK / Unique / Check 等数据库兜底？
- [ ] 是否存在错误的跨域 Cascade？
- [ ] Idempotency 是否有 Unique / Processed Event 等可靠机制？

### Version

- [ ] Entity Version、Revision、Contract Version、Schema Version 是否区分？
- [ ] Event / Export / Crypto 等持久化 Payload 是否保存明确版本？
- [ ] Schema 变更是否具备兼容升级路径？

### Security

- [ ] 数据敏感等级是否按 Public / Shared / Private / Sensitive / Secure Domain 分类？
- [ ] Secret 是否只保存安全 Reference / 密文？
- [ ] Secure Domain 是否真正密文落盘？
- [ ] Log / Event / Analytics / AI 是否不会复制不必要敏感字段？

### Async

- [ ] 关键 Event 是否与业务事务一起写 Durable Outbox？
- [ ] Consumer 是否幂等？
- [ ] Background Task 是否区分 Cancelled / TimedOut / Failed？
- [ ] Retry 是否形成独立 Attempt？
- [ ] 重启后遗留 Running 是否有恢复策略？

### Lifecycle

- [ ] Archive / Trash / Tombstone / Purge / Physical Delete 是否区分？
- [ ] 删除业务对象是否不会错误同步删除共享 Blob？
- [ ] Retention / Audit / Backup / Revision 引用是否参与 Purge/GC 判断？

### Performance

- [ ] Filter / Sort / FK 是否有合理索引？
- [ ] 是否避免深 OFFSET？
- [ ] JSONB 是否被滥用？
- [ ] 是否把应交给 Search / Analytics 的查询强塞给业务表？
- [ ] 是否满足至少十万级 Resource 的基本使用目标？

### Migration / Recovery

- [ ] Production DDL 是否全部版本化？
- [ ] 长 Backfill 是否与短 Migration 分离？
- [ ] 变更是否评估锁和表重写风险？
- [ ] Backup 是否记录 Database Schema Version？
- [ ] Restore 是否能够重建 Search / Analytics / AI 派生数据？

---

## 39. 结论

Ikaros V2 数据库总体设计可以概括为：

```text
                     Ikaros Instance
                            │
                            ▼
                 PostgreSQL Business DB
                            │
            ┌───────────────┼────────────────┐
            │               │                │
            ▼               ▼                ▼
   Domain-owned Schema   Durable State   Derived State
   业务真相与约束         Event / Task     Analytics / Search
            │               │                │
            │               │                └── 可重建
            │               │
            │               └── 可重放 / 幂等
            │
            ├── UUIDv7 identity
            ├── timestamptz instant
            ├── explicit version semantics
            ├── local ACID transaction
            └── explicit lifecycle

Content Bytes
    ↓
Attachment → Blob → Placement → Object Storage

Secure Domain
    ↓
Secure Data Foundation
    ↓
Ciphertext-only Persistence
```

核心原则不是“把所有子系统都放进同一个数据库方便 JOIN”，而是：

> **在保持单 Instance、自托管、单 PostgreSQL 运维简单度的同时，用明确的 Schema Ownership、稳定实体身份、本地事务、Durable Event、版本化 Migration 和安全数据边界，维持各子系统可独立演进的领域边界。**

这样 V2 才能同时获得：

- PostgreSQL 的强约束与事务能力；
- 模块化单体的部署简单度；
- 跨领域最终一致性的可扩展性；
- Secure Domain 的密文安全边界；
- Search / Analytics / AI 派生数据的可重建性；
- 清晰的 Schema Version 与升级路径；
- 面向未来 Worker、多节点和专项 Schema 细化的演进空间。
