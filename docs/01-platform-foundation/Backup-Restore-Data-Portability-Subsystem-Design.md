# Ikaros V2 备份、恢复与数据可迁移性子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 备份、恢复与数据可迁移性子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Attachment-Blob-Storage-Subsystem-Design.md`、`Secure-Data-Foundation-Design.md`、`Security-Identity-Authorization-Crypto-Subsystem-Design.md`、`Background-Task-Scheduler-Design.md`、`Platform-Administration-Operations-Subsystem-Design.md` |

> 本文档把系统概要中已经定义的 Backup / Restore / Export / Import 原则进一步收敛为可实现契约，重点定义恢复点、Manifest、备份一致性、校验、保留策略、安全边界、恢复激活和数据可迁移格式。
>
> Backup / Restore 面向 Ikaros 实例灾难恢复；Export / Import 面向用户数据自主和跨实例迁移。两者可以复用底层序列化或对象复制能力，但不能共享成同一个模糊概念。

---

## 1. 设计目标

本子系统解决以下问题：

1. PostgreSQL、Blob、配置、插件数据和 Secure Domain 如何形成可解释的恢复点。
2. 如何证明一次 Backup 不只是“任务执行成功”，而是真正具备可恢复性。
3. Full / Incremental Backup 如何保留依赖关系并支持安全清理。
4. Restore 如何在破坏现有实例前完成兼容性、完整性和安全预检。
5. Search / Analytics 等可重建数据是否需要进入备份，以及恢复后如何重建。
6. Secret / Key Material 如何与普通配置和密文内容分离保护。
7. Backup Retention 与业务 Blob GC 如何避免相互误删。
8. Export / Import 如何形成稳定、版本化、可理解的数据可迁移格式。
9. 恢复、导入、迁移过程中如何处理 ID、External Identity、重复对象和冲突。
10. 如何为管理员提供可审计、可重试、可验证的生产运维流程。

核心原则：

> **Backup Success ≠ Restore Verified；只有具备完整 Manifest、校验结果和可执行恢复路径的备份，才是可信恢复点。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Backup Policy；
- Backup Run；
- Restore Point；
- Backup Manifest；
- Backup Artifact / Object Inventory；
- Full / Incremental Backup Chain；
- Backup Verification；
- Restore Run；
- Restore Preflight；
- Restore Activation；
- Retention / Prune；
- Restore Drill；
- Export Package / Manifest；
- Import Validation / Conflict Plan；
- Backup / Restore / Export Format Version；
- 对 Background Task、Audit、Storage、Security 的编排契约。

### 2.2 本子系统不负责

- PostgreSQL 引擎自身的物理备份工具实现；
- S3、Filesystem、NAS 等 Provider 的底层复制协议；
- Blob Storage 的普通副本调度；
- Search Index 与 Analytics Projection 的业务重建逻辑；
- 各业务领域 Import Conflict 的最终业务合并规则；
- Secure Domain 内部密钥派生算法；
- 操作系统或虚拟机整机镜像备份。

---

## 3. 核心不变量

1. **Backup ≠ Export**：Backup 保持实例恢复语义；Export 保持用户可迁移语义。
2. **Restore Point 不可变**：一旦标记可用，其 Manifest 和已纳入对象集合不得原地修改。
3. **恢复点必须可解释**：必须知道数据库版本、应用版本、格式版本、Blob 覆盖范围和 Secure Material 状态。
4. **业务与内容要有一致恢复语义**：不能声称数据库恢复到 T1，却在没有声明的情况下使用 T2/T3 才出现或已删除的 Blob 集合。
5. **Secure Domain 始终保持安全边界**：备份不能为了方便把密文自动解密成普通文件。
6. **Key Material 与普通备份分离保护**：加密密钥、Recovery Material、Credential 不得与普通明文配置无差别存放。
7. **可重建数据默认不成为恢复真相**：Search Index、Analytics Aggregate 默认通过恢复后重建获得。
8. **Restore 默认不是 Merge**：灾难恢复目标是恢复一个实例状态；把数据并入现有实例属于 Import / Migration 流程。
9. **Restore 保留内部身份**：实例恢复应保留 UUIDv7 等内部实体 ID；Import 才允许按冲突策略映射或创建新 ID。
10. **备份引用阻止误 GC**：被有效 Restore Point 引用且策略要求保留的内容不能因业务侧删除而被物理清理。
11. **Prune 必须保守**：删除增量链中仍被后继 Restore Point 依赖的 Artifact 属于非法操作。
12. **恢复未验证不得激活**：数据库能启动不等于业务可恢复；必须完成定义的完整性和兼容性检查。

---

## 4. 领域模型

### 4.1 Backup Policy

Backup Policy 描述：

- scope；
- schedule；
- target provider；
- full / incremental strategy；
- retention；
- verification level；
- encryption policy；
- include blob policy；
- include plugin data policy；
- secure material strategy；
- bandwidth / concurrency limits；
- owner / enabled state。

Policy 是长期配置；一次实际执行形成独立 Backup Run。

### 4.2 Backup Run

建议字段：

```text
BackupRun
├── id: UUIDv7
├── policy_id?
├── kind: FULL | INCREMENTAL
├── base_restore_point_id?
├── state
├── stage
├── started_at
├── finished_at?
├── source_instance_id
├── app_version
├── schema_version
├── bytes_processed
├── objects_processed
├── manifest_id?
├── verification_status
├── initiated_by
└── background_task_id
```

Backup Run 是执行记录，不等同于 Restore Point。

### 4.3 Restore Point

只有满足成功条件的 Backup Run 才能发布 Restore Point。

Restore Point 至少包含：

- immutable ID；
- created_at；
- Backup Format Version；
- Source Instance Identity；
- Server Version；
- Database Schema Version；
- manifest digest；
- base Restore Point（增量时）；
- blob coverage；
- secure material availability descriptor；
- verification level / result；
- retention deadline / pin state。

### 4.4 Backup Manifest

Manifest 是恢复点的核心可验证清单。

建议至少描述：

```text
BackupManifest
├── format_version
├── restore_point_id
├── source_instance_id
├── created_at
├── database_artifact
├── database_schema_version
├── blob_inventory / blob_set_descriptor
├── config_artifacts[]
├── plugin_artifacts[]
├── secure_material_descriptor
├── external_dependencies[]
├── checksums[]
├── base_restore_point_id?
└── manifest_digest
```

Manifest 自身必须可校验，并与 Artifact 的内容摘要绑定。

---

## 5. 备份范围

### 5.1 默认应覆盖

生产恢复基线至少考虑：

- PostgreSQL 业务数据；
- Blob / Attachment 所需内容；
- Storage Provider 非 Secret 配置；
- Plugin Registry 和需要保留的 Plugin Data；
- 系统关键配置；
- Secure Domain 密文数据；
- Secret / Key Material 的独立受保护备份路径；
- 当前 Schema / Contract / Format Version 元信息。

### 5.2 默认可重建

以下数据默认不要求进入核心 Restore Point：

- Search Index；
- Analytics Aggregate；
- 普通 Cache；
- Derived Thumbnail / Preview（当已证明可稳定重建）；
- 临时转码工作区；
- 可重建的 AI Embedding / Projection。

如果为了缩短 RTO 选择备份这些派生数据，Manifest 必须标记它们为 `DERIVED / OPTIONAL`，恢复时不得把其状态优先于业务真相。

### 5.3 外部依赖

某些部署可能使用外部对象存储，Backup 不一定复制所有 Blob 字节。

若 Restore Point 依赖外部不可变 Bucket / Snapshot / Versioned Object Set，Manifest 必须显式记录：

- Provider Reference；
- Snapshot / Version Boundary；
- 所需权限；
- 校验方式；
- 外部依赖是否属于 Restore Point 生命周期管理范围。

不能用“对象还在原 Bucket 里”隐式冒充完整备份。

---

## 6. 一致恢复点

### 6.1 一致性目标

Backup 不要求把整个 Ikaros 长时间停机，但必须建立明确的一致性边界。

恢复点需要回答：

> 数据库中所有有效 Attachment / Blob 引用，在该恢复点声明的内容集合中是否能够解析并验证？

### 6.2 推荐捕获流程

```text
Prepare Backup
    ↓
记录 Source Instance / Schema / Backup Generation
    ↓
建立数据库一致快照或等价逻辑边界
    ↓
生成业务引用 / Blob Inventory
    ↓
捕获数据库 Artifact
    ↓
复制 / 固定所需 Blob Artifact
    ↓
捕获 Config / Plugin / Secure Descriptor
    ↓
写 Manifest
    ↓
Verify
    ↓
Publish Restore Point
```

具体可以使用 PostgreSQL 物理快照、逻辑备份或 Provider Snapshot，但必须保持同一恢复语义。

### 6.3 写入并发

备份期间系统可以继续接受业务写入，只要实现能够证明这些写入被明确归入：

- 本 Restore Point；或
- 下一个 Restore Point。

不得产生“数据库引用已进入本次备份，但对应新 Blob 既不在本次也不在下一次可追踪范围”的悬空窗口。

需要时可以使用短暂 Barrier / Generation Freeze / Object Version Pin，而不是默认全程停机。

---

## 7. Full 与 Incremental Backup

### 7.1 Full

Full Restore Point 必须在不依赖更早 Backup Artifact 的情况下完成恢复，外部基础设施依赖除外且必须在 Manifest 声明。

### 7.2 Incremental

Incremental Restore Point 可以只保存相对 Base 的变化，但必须记录完整 Chain：

```text
Full R1
  ↓
Incremental R2
  ↓
Incremental R3
```

恢复 R3 时必须可以解析 R1 + R2 + R3 的全部依赖。

### 7.3 Chain Compaction

为了控制恢复时间和保留复杂度，可以执行 Consolidation / Synthetic Full：

```text
R1 + R2 + R3
      ↓
New Full R4
```

只有 R4 已验证并不再依赖旧链后，Retention 才允许删除旧 Artifact。

---

## 8. Backup Run 状态与失败语义

Backup 使用 `Background-Task-Scheduler-Design.md` 的统一执行语义。

建议业务阶段：

```text
PREPARING
CAPTURING_DATABASE
CAPTURING_CONTENT
CAPTURING_CONFIGURATION
WRITING_MANIFEST
VERIFYING
PUBLISHING
```

执行终态遵循：

```text
Succeeded
Failed
Cancelled
TimedOut
```

只有 `PUBLISHING` 成功后才产生可选择恢复的 Restore Point。

取消时：

- 未发布的中间 Artifact 标记为 orphan candidate；
- 后台 Cleanup 可以回收；
- 不得留下一个看起来成功的半成品 Restore Point。

---

## 9. Verification

### 9.1 校验等级

建议至少提供：

- `MANIFEST_ONLY`：验证 Manifest 和 Artifact 元信息；
- `STRUCTURAL`：验证数据库备份可解析、Schema 版本、依赖完整；
- `CONTENT_SAMPLE`：抽样验证 Blob Digest / Object 可读性；
- `CONTENT_FULL`：完整验证纳入范围的内容；
- `RESTORE_DRILL`：在隔离环境实际执行恢复并通过验收。

### 9.2 Verification Run

Verification 应是独立可追踪执行：

- restore_point_id；
- level；
- started / finished；
- checked objects；
- failed objects；
- failure category；
- result；
- tool / verifier version。

旧 Restore Point 可以定期重复校验，以发现长期存储腐化。

### 9.3 Restore Verified

只有执行了 `RESTORE_DRILL` 并满足定义验收项时，才可以在 UI 中显示“已完成恢复验证”。

普通 Hash 校验不能冒充完整恢复演练。

---

## 10. Restore 设计

### 10.1 Preflight

Restore 开始前必须检查：

- Backup Format Version；
- Database Schema Version；
- 当前 Server 是否支持该版本；
- Manifest Digest；
- Base Chain 是否完整；
- 必要 Artifact 是否存在且可读；
- Secure Key / Recovery Material 是否可用；
- Storage Provider 是否可访问；
- Plugin Compatibility；
- 可用磁盘 / 存储空间；
- 目标实例状态。

Preflight 失败时不得开始破坏性覆盖。

### 10.2 默认恢复目标

优先推荐：

> **恢复到新的空实例 / 新数据库 / 新存储 Namespace，再完成验证和切换。**

直接覆盖当前生产实例属于高风险模式，需要：

- Maintenance Mode；
- Step-up Verification；
- 明确影响确认；
- 当前实例自身的保护性备份；
- Audit。

### 10.3 推荐恢复顺序

```text
Preflight
  ↓
进入隔离 / Maintenance 环境
  ↓
恢复基础非 Secret 配置
  ↓
恢复 PostgreSQL
  ↓
恢复 / 绑定 Blob Storage
  ↓
恢复 Secure Key / Secret Material
  ↓
恢复 Plugin Registry / Data
  ↓
运行 Schema / Domain Integrity Check
  ↓
重建 Search Index
  ↓
重建 Analytics / Derived Projection
  ↓
抽样 / 完整内容校验
  ↓
Health Check
  ↓
Activate Instance
```

### 10.4 Restore 不做静默 Merge

如果目标实例已经存在业务数据，Restore API 不得自动逐表合并。

用户需要“把旧备份的一部分数据导入当前实例”时，应创建 Export / Import 或专项 Migration Plan。

---

## 11. Restore Activation 与回退

恢复完成后至少通过：

- PostgreSQL Schema 兼容；
- 关键领域约束；
- Blob 引用完整性；
- Secure Domain 基础解锁测试；
- Plugin 启动兼容性；
- Search Rebuild 基础检查；
- Server Health；
- 必要的登录 / 权限冒烟测试。

未通过时保持 Maintenance / Isolated，不对普通用户开放。

如果采用新实例切换，可通过反向代理 / Deployment Routing 激活新实例；旧实例应保留到回退窗口结束，而不是切换后立即销毁。

---

## 12. Retention 与 Prune

### 12.1 Policy

Retention 可以表达：

- 保留最近 N 个；
- 保留 N 天；
- 日 / 周 / 月分层保留；
- Pin 某个 Restore Point；
- 最小 Verified Restore Point 数量；
- Full Chain 最小保留规则。

### 12.2 Prune 前置条件

删除 Restore Point 前必须检查：

- 是否被 Pin；
- 是否被其他 Incremental Point 依赖；
- 是否是策略要求的最后一个 Full；
- 是否正在 Verification / Restore；
- 对应 Artifact 是否被其他 Restore Point 共享。

### 12.3 与 Blob GC 的关系

Backup 使用的 Blob Artifact / Snapshot Reference 是独立保留引用。

业务 Resource 被永久删除，只能释放业务引用；只要有效 Restore Point 仍依赖该内容，备份侧就不能因业务 GC 一并删除。

---

## 13. Export / Import 数据可迁移性

### 13.1 Export Package

Export 应使用独立 `Export Format Version`，至少包含：

- package manifest；
- source instance / export time；
- selected scope；
- logical entities；
- relationship records；
- External Identity；
- optional Attachment / Blob content；
- data classification；
- optional ACL / ownership metadata；
- checksums。

Export 不能只是 PostgreSQL Dump 的别名。

### 13.2 Portable Identity

导出包中保留源内部 ID 以维护引用关系，但 Import 到非空实例时不能假定这些 ID 永远可直接占用。

Import Plan 应能够记录：

```text
source_id -> target_id
```

并保证包内 Relation、Attachment Reference、Collection Membership 等关系在映射后仍然一致。

### 13.3 冲突

Import 至少识别：

- Internal ID collision；
- External Identity collision；
- 同内容 Blob；
- 同名 Collection / Drive Node；
- 已存在 Resource 候选；
- ACL / Owner 不存在；
- Plugin-defined type unsupported；
- Export Format unsupported。

具体业务对象是否 Merge 由目标领域提供 Conflict Resolver；Import 不自行猜测。

### 13.4 Secure Domain Export

Secure Domain 导出默认保持密文，并携带对应格式和 Key Version 元数据。

若用户显式请求明文导出，必须由对应 Secure Domain 自己定义高风险流程、Step-up Verification 和用户确认；通用 Export 不能偷偷解密。

---

## 14. 幂等、重试与断点恢复

### 14.1 Backup

重复触发同一 Scheduled Run 不应产生多个不受控备份。

可使用：

```text
policy_id + scheduled_fire_time
```

或等价幂等键收敛重复调度。

### 14.2 Artifact Copy

大 Blob / Object Copy 必须支持：

- 内容摘要校验；
- 已完成对象跳过；
- 分段 / Multipart Resume（Provider 支持时）；
- Retry；
- Checkpoint。

### 14.3 Restore

Restore Run 必须记录阶段 Checkpoint。

已经验证完成的不可变 Artifact 不应在进程重启后无条件重新复制；但任何“跳过”都必须以 Digest / Version 验证为依据，而不是仅看目标路径存在。

---

## 15. 权限、安全与审计

以下操作属于高风险管理能力：

- 创建 / 修改 Backup Policy；
- 查看 Backup Target Credential；
- 手动执行 Restore；
- 覆盖当前实例；
- 删除 Restore Point；
- 导出 Sensitive / Secure Domain；
- 恢复 Secret / Key Material。

要求：

1. 使用 Platform RBAC；
2. Restore / Secret Restore / destructive prune 使用 Step-up Verification；
3. 所有高风险动作写 Audit；
4. 日志不得记录 Secret、Recovery Key、完整 Credential；
5. Backup Target Credential 使用 Secret Reference；
6. 远程备份传输必须使用安全传输；
7. Backup Artifact 可选择应用层加密，密钥管理独立于普通 Artifact；
8. Backup Encryption Key 的丢失必须被明确显示为“备份不可恢复”，不能只显示文件存在。

---

## 16. API 与管理能力

公开 / 管理 API 需要支持以下平台语义：

- list/create/update Backup Policy；
- run backup；
- list Restore Point；
- get Manifest summary；
- verify Restore Point；
- run Restore Preflight；
- start Restore；
- get Restore Progress；
- pin / unpin Restore Point；
- prune eligible Restore Point；
- create Export；
- validate Import；
- preview Import Plan；
- execute Import。

Backup、Restore、Verify、Export、Import 都属于 Background Task，不使用长时间阻塞 HTTP 请求。

高风险 API 必须遵守 `API-Convention-Design.md` 的 Idempotency、Problem Details、并发与 Step-up 约定。

---

## 17. Plugin / Storage Provider 边界

Storage / Backup Provider 可以扩展：

- Artifact Store；
- Snapshot Capability；
- Object Versioning；
- Immutable Retention；
- Remote Copy；
- Integrity Verification。

Provider 只执行存储能力，不拥有 Restore Point 业务状态。

插件不能自行把“上传成功”标记成系统 Restore Point；必须经过平台 Manifest / Verification / Publish 流程。

---

## 18. 可观测性

至少提供：

- last successful backup；
- last verified restore point；
- restore point age；
- backup duration；
- bytes / objects processed；
- backup throughput；
- failed backup count；
- verification failure count；
- corrupted / missing artifact count；
- incremental chain length；
- restore drill age；
- retention prune result；
- target provider health；
- RPO / RTO 目标与实际值（配置后）。

管理端必须区分：

```text
Backup Run Succeeded
Restore Point Published
Verification Passed
Restore Drill Passed
```

不能用一个绿色“成功”覆盖所有语义。

---

## 19. 测试与验收基线

P0 至少覆盖：

1. 数据库备份成功但 Blob Artifact 缺失时不得发布可信 Restore Point。
2. Manifest 被篡改或损坏时 Verification 失败。
3. Incremental Chain 缺少任一 Base 时 Restore Preflight 失败。
4. 被后继 Restore Point 依赖的 Artifact 不会被 Retention 误删。
5. Secure Domain 备份始终保持密文边界。
6. Key Material 缺失时明确判定对应 Secure 数据不可恢复。
7. Search Index 全部丢失不影响核心 Restore，可在恢复后重建。
8. Restore 到新实例后保留原内部实体 ID 和关系。
9. 非空实例不会被 Restore 静默 Merge。
10. Restore Preflight 不通过时不会开始破坏性覆盖。
11. Backup 中断后可以基于校验过的 Checkpoint 恢复。
12. 同一计划触发重复投递不会生成多个逻辑 Backup Run。
13. Export Format 可被版本识别，未知版本被安全拒绝或进入兼容流程。
14. Import 的 External Identity 冲突不会静默合并两个 Resource。
15. Backup Success 和 Restore Drill Success 在状态模型中可以明确区分。

---

## 20. P0 实现优先级

P0 建议按以下顺序落地：

1. Backup Policy / Backup Run / Restore Point / Manifest；
2. PostgreSQL + Blob 一致恢复点语义；
3. Full Backup；
4. Manifest Digest 与 Structural / Content Verification；
5. Restore Preflight；
6. 新实例恢复流程；
7. Search / Analytics 恢复后重建编排；
8. Retention / Pin / Safe Prune；
9. Scheduled Backup 与 Background Task 集成；
10. Audit、Step-up Verification 与基础 CMS 运维入口；
11. Export Format / Import Plan；
12. Incremental Backup 与 Restore Drill 自动化。

生产环境在宣称“备份功能完成”前，至少需要一次真实 Restore Drill 通过。
