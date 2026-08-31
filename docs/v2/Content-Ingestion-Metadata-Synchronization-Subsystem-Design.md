# Ikaros V2 Content Ingestion / Metadata Synchronization 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Content Ingestion / Metadata Synchronization 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Core-Resource-Library-Subsystem-Design.md`、`Attachment-Blob-Storage-Subsystem-Design.md`、`Background-Task-Scheduler-Design.md`、`Platform-Integration-Automation-Design.md` |

> 本文档定义 Ikaros V2 中“内容如何进入系统、如何被识别、如何与已有 Resource 匹配、如何持续同步外部元数据”的完整服务端流程。
>
> Ingestion 不拥有 Resource、Attachment、Blob 或 Provider 的最终业务真相。它负责把 Source 中发现的候选内容转换为可审计、可预览、可重试的 Import Plan，并通过目标子系统的公开 Command 落地。

---

## 1. 设计目标

本子系统需要解决：

1. 本地文件系统、NAS、对象存储、上传、远程 Provider、插件等来源如何统一进入导入流程。
2. 如何区分 Source、Scan、Candidate、Match、Import Plan、Import Run，而不是“扫描到文件就直接写 Resource”。
3. 文件移动、改名、重复扫描时如何避免重复创建 Resource / Attachment / Blob。
4. 如何通过 Hash、路径、外部身份、领域特征等多种信号识别同一内容。
5. 自动匹配不确定时如何进入人工确认，而不是做不可逆错误合并。
6. Metadata Provider 如何提交候选值，并遵守 Metadata Provenance 与用户修改优先级。
7. Provider 数据变化后如何增量同步，同时避免覆盖用户锁定字段。
8. 导入大量内容时如何支持 Dry Run、Checkpoint、重试、取消、部分失败与恢复。
9. Source 中内容被删除、移动、不可访问时，Ikaros 内部 Resource / Attachment 应如何变化。
10. Scanner、Provider、Plugin 和 AI 如何参与识别而不绕过权限、领域规则和审计。

核心原则：

> **发现内容不等于创建 Resource；识别候选不等于自动合并；外部元数据不等于 Ikaros 内部事实。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Ingestion Source 定义；
- Source Scan；
- Candidate 生成；
- 文件 / 对象 Fingerprint；
- Resource Match Proposal；
- External Identity Match；
- Import Plan；
- Dry Run / Preview；
- Import Run / Checkpoint；
- Metadata Fetch / Refresh 编排；
- Metadata Candidate 与 Provenance 提交；
- 冲突和人工确认队列；
- Source 变化检测；
- 与 Background Task、Plugin、Automation 的集成；
- 导入过程的可观测性和审计。

### 2.2 本子系统不负责

- Resource 的最终领域状态与生命周期；
- Blob 去重和物理存储策略；
- Attachment 的最终引用管理；
- 视频 / 音乐 / 图片等专业媒体模型；
- Metadata Provider 的抓取协议细节；
- Search Index；
- AI 模型调用本身；
- 文件系统 watcher / S3 API 等具体技术实现的唯一选型。

---

## 3. 核心不变量

1. **扫描是只读发现行为**：仅扫描 Source 不应直接创建或删除业务 Resource。
2. **Import Plan 可解释**：真正写入前必须知道将创建、绑定、更新、跳过或冲突哪些对象。
3. **导入可重试**：同一逻辑导入重复执行不得无控制地产生重复 Resource、Attachment 或 External Identity。
4. **外部身份唯一性由核心 Resource 规则保证**：Ingestion 不能绕过 External Identity 唯一约束。
5. **内容身份与路径分离**：路径变化不能自动被当作新内容；路径也不能成为 Blob 或 Resource 的内部身份。
6. **不确定匹配不得自动危险合并**：低置信度或冲突匹配必须人工确认或保留为独立 Candidate。
7. **用户修改优先**：Metadata Refresh 不得静默覆盖用户锁定或确认的字段。
8. **Source 删除不等于 Resource 删除**：外部文件消失只影响来源可用性 / Attachment 状态，不能直接永久删除逻辑 Resource。
9. **Checkpoint 是执行进度，不是业务真相**：任务可以重建或重跑，但已提交的目标领域事实仍由目标子系统拥有。
10. **Scanner / Provider 只能提交事实或候选**：不得获得绕过目标 Command 的数据库写权限。
11. **导入过程必须有 Actor / Principal**：人工、Scheduled Job、Automation、Plugin 触发都必须可追踪。
12. **删除与清理保守处理**：任何可能导致数据丢失的清理动作都必须与普通扫描分离，并具有显式策略。

---

## 4. 总体流程

```text
Ingestion Source
      ↓
Scan Run
      ↓
Discovered Item
      ↓
Candidate
      ↓
Fingerprint / Probe / Metadata Hint
      ↓
Match Engine
      ├── Existing Resource
      ├── Existing Attachment / Blob
      ├── External Identity Match
      └── No Match / Conflict
      ↓
Import Plan
      ↓
Dry Run / Review / Policy
      ↓
Import Run
      ↓
Resource / Attachment / External Identity Commands
      ↓
Metadata Candidate / Provenance
      ↓
Integration Event
      ↓
Search / Analytics / Automation
```

该流程允许实现优化，但不应跳过“发现”和“提交业务事实”的语义分层。

---

## 5. Ingestion Source

Source 表示一个可被导入或同步的内容来源。

典型类型：

- LOCAL_FILESYSTEM；
- NAS / MOUNTED_PATH；
- OBJECT_STORAGE；
- MANUAL_UPLOAD；
- REMOTE_URL；
- PROVIDER_COLLECTION；
- PLUGIN_SOURCE；
- EXPORT_PACKAGE；
- 未来其他受控来源。

Source 至少表达：

- UUIDv7；
- type；
- display name；
- root / endpoint reference；
- credential reference（不能明文保存 Secret）；
- scan policy；
- include / exclude rule；
- ownership / creator；
- enabled；
- last successful scan；
- health status。

一个 Source 可以产生多个 Scan Run。

---

## 6. Source 安全边界

### 6.1 文件系统

本地 / NAS Source 只允许访问明确配置的 Root。

必须防止：

- `../` Path Traversal；
- 符号链接逃逸 Root；
- 扫描系统敏感目录；
- 插件通过 Source 读取任意宿主文件。

是否跟随 symlink 必须由 Source Policy 明确决定。

### 6.2 对象存储

Object Storage 凭据使用 Secret / Credential Reference，不在普通 Source 配置中明文返回。

### 6.3 Remote URL

如果允许服务端抓取用户提供 URL，必须考虑 SSRF 防护：

- 私网地址策略；
- metadata endpoint；
- redirect；
- DNS rebinding；
- 最大响应大小；
- timeout。

---

## 7. Scan Run

Scan Run 表示一次 Source 枚举和变化发现。

状态建议：

- PENDING；
- RUNNING；
- PAUSED（可选）；
- SUCCEEDED；
- PARTIALLY_SUCCEEDED；
- FAILED；
- CANCELLED。

Scan Run 至少记录：

- Source ID；
- Trigger；
- Actor / Principal；
- Start / End；
- Cursor / Checkpoint；
- discovered count；
- changed count；
- skipped count；
- error summary；
- Background Task ID。

大型 Scan 必须作为 Background Task 执行。

---

## 8. Discovered Item

Discovered Item 是 Source 层观察到的对象。

可以是：

- 文件；
- 目录；
- Object Storage Object；
- Provider Record；
- Upload Session Result；
- Export Package Entry。

典型字段：

- source relative key；
- size；
- modified time；
- etag / version id；
- media type；
- optional weak fingerprint；
- availability；
- scan generation。

原则：

- 不长期把绝对宿主路径暴露给普通客户端；
- Source Key 只是来源定位，不等于内部实体 ID；
- modified time / etag 只用于变化检测，不能单独当作内容身份。

---

## 9. Candidate

Candidate 是“可能成为 Ikaros 业务对象”的导入候选。

Scanner 可以把一个文件解析成多个 Candidate，例如：

- 一个媒体文件 Candidate；
- 内嵌字幕 Candidate；
- 海报 Candidate；
- NFO / metadata Candidate。

多个文件也可以组成一个逻辑 Candidate，例如：

```text
Series Folder
├── S01E01.mkv
├── S01E01.ass
├── poster.jpg
└── tvshow.nfo
```

Candidate 可以表达：

- suggested resource type；
- title hint；
- episode / track / chapter hint；
- file group；
- external id hint；
- scanner confidence；
- technical probe result；
- candidate fingerprint。

Candidate 本身不是 Resource。

---

## 10. Fingerprint 策略

匹配可使用多层 Fingerprint。

### 10.1 Weak Fingerprint

用于快速判断是否需要重新处理：

- relative key；
- size；
- modified time；
- object etag。

Weak Fingerprint 不保证内容身份。

### 10.2 Strong Content Fingerprint

例如：

- SHA-256 / 平台统一内容 Hash；
- Blob 已存在的 Cryptographic Hash。

Strong Hash 可以用于 Blob 内容去重，但不能直接证明两个逻辑 Resource 是同一个作品。

### 10.3 Domain Fingerprint

专业领域可增加：

- media duration；
- audio fingerprint；
- ISBN；
- embedded provider id；
- episode naming pattern；
- archive manifest。

Domain Fingerprint 只能提供 Match Signal，不应由 Ingestion 私自定义目标领域最终唯一性。

---

## 11. Match Engine

Match Engine 的目标是产生 Match Proposal，不是绕过人工判断做不可解释合并。

可能结果：

- EXACT_MATCH；
- HIGH_CONFIDENCE_MATCH；
- MULTIPLE_MATCHES；
- LOW_CONFIDENCE_MATCH；
- NO_MATCH；
- CONFLICT。

匹配信号优先级可包括：

1. 已绑定 Source Item Identity；
2. External Identity 精确匹配；
3. Strong Content Hash 匹配现有 Blob / Attachment；
4. 专业领域稳定身份；
5. 标题 + 类型 + 年份 + Episode 等组合；
6. AI / 模糊匹配建议。

AI 或 fuzzy match 默认不能单独触发不可逆 Resource Merge。

---

## 12. Source Item Identity 与文件移动

系统需要保存 Source 内对象的持续识别信息，以区分：

- 内容没变，仅路径改名；
- 文件移动；
- 同路径内容被替换；
- 同内容在 Source 中复制；
- Source 暂时不可用。

如果文件系统能提供稳定 inode / file key，可作为辅助信号，但不得假定跨文件系统、备份恢复后仍稳定。

推荐决策：

```text
old item missing + new item appears
        ↓
strong hash equal
        ↓
优先判断为 move / rename
而不是 delete + new content
```

但最终 Attachment 来源定位仍由 Storage / Attachment 规则维护。

---

## 13. Import Plan

Import Plan 是执行前的明确决策集合。

每项 Action 可为：

- CREATE_RESOURCE；
- LINK_EXISTING_RESOURCE；
- CREATE_ATTACHMENT；
- LINK_EXISTING_BLOB；
- BIND_EXTERNAL_IDENTITY；
- UPDATE_METADATA_CANDIDATE；
- ADD_RELATION；
- ADD_COLLECTION_MEMBER；
- MARK_SOURCE_UNAVAILABLE；
- SKIP；
- REQUIRE_REVIEW；
- CONFLICT。

Import Plan 应记录：

- 输入 Candidate；
- Match Evidence；
- Action；
- Target ID（如有）；
- reason；
- confidence；
- policy snapshot；
- generated_at。

---

## 14. Dry Run / Preview

任何批量导入都应支持 Dry Run 或等价预览。

Dry Run：

- 可以读取 Source；
- 可以 Probe；
- 可以查询已有 Resource / Blob；
- 可以生成 Match / Import Plan；
- 不得提交业务写入；
- 不产生真实 Resource / External Identity / Attachment 关系。

Preview 应至少展示：

- 将创建多少 Resource；
- 将匹配多少现有 Resource；
- 将复用多少 Blob；
- 将更新多少 Metadata；
- 有多少冲突；
- 有多少项需要人工确认；
- 预计处理字节 / 文件数量。

---

## 15. Import Run

Import Run 是对一个确认后的 Import Plan 的执行。

状态建议：

- PENDING；
- RUNNING；
- PARTIALLY_SUCCEEDED；
- SUCCEEDED；
- FAILED；
- CANCELLED。

Import Run 不追求“几万文件全部一个数据库事务”。

正确模型是：

- 每个领域 Command 保持本域事务完整；
- Import Run 保存 Checkpoint；
- 已成功项可幂等跳过；
- 失败项可重试；
- 最终给出逐项结果与汇总。

---

## 16. Checkpoint 与恢复

Checkpoint 至少可以记录：

- current source cursor；
- current plan item；
- successful item ids；
- failed item ids；
- retry attempt；
- last heartbeat。

恢复原则：

1. Worker 崩溃后可从最近 Checkpoint 继续。
2. Checkpoint 丢失时允许重新枚举，但依赖目标领域幂等避免重复副作用。
3. Checkpoint 不保存必须保密的 Provider Secret。
4. 长任务升级前应能安全暂停 / 取消或等待 Checkpoint。

---

## 17. 幂等设计

重点幂等键：

### 17.1 Source Item

可使用：

`source_id + source_object_stable_key/version`

用于避免同一次 Source 对象被重复处理。

### 17.2 External Identity

最终由核心 Resource 唯一约束保证。

### 17.3 Attachment / Blob

Blob 内容身份由 Storage Hash 规则保证；Ingestion 不自行实现第二套去重。

### 17.4 Import Plan Item

每个 Plan Item 应具有稳定 ID / logical idempotency key。

同一 Plan 重试时，已完成项不产生重复写入。

---

## 18. Metadata Provider

Metadata Provider 可以来自：

- 核心 Provider；
- Plugin；
- 外部 API；
- NFO / embedded metadata；
- AI suggestion。

Provider 返回的是 Metadata Candidate，不直接成为最终 Resource 字段。

Candidate 至少包含：

- provider；
- external identity；
- field path / semantic key；
- value；
- fetched_at；
- confidence（适用时）；
- source version / etag；
- locale；
- optional evidence。

---

## 19. Metadata Refresh

Refresh 触发来源：

- 用户手动；
- Scheduled Job；
- Provider Webhook；
- Import 后首次 enrich；
- Automation；
- Provider mapping 变化。

流程：

```text
Fetch Provider Data
      ↓
Normalize Candidate
      ↓
Compare with current Provenance
      ↓
Policy Decision
      ├── Auto Apply
      ├── Keep Current
      ├── Store Candidate
      └── Conflict / Review
      ↓
Core Resource Command
```

Metadata Refresh 不能直接执行 SQL 更新 Resource。

---

## 20. Provenance 与优先级

完全复用 `Core-Resource-Library-Subsystem-Design.md` 的原则：

```text
用户锁定
  > 用户确认
  > 导入 / 管理策略
  > 主 Provider
  > 其他 Provider / Scanner
  > System / AI Suggestion
```

Provider 发现值变化时：

- current field 未被人工修改：可按策略自动更新；
- current field 已锁定：保存新 Candidate，不覆盖；
- 多 Provider 冲突：按明确 Provider Policy 决策；
- 没有 Policy：进入 Review Queue。

---

## 21. Metadata 删除语义

Provider “字段消失”与“字段值为空”必须区分。

例如 Provider API 不再返回 `summary`：

- 不能默认清空用户当前 Summary；
- 需要判断 Provider 是明确删除，还是本次响应不包含；
- 用户锁定字段永远不自动清空；
- 外部来源被解绑时，是否回退到其他 Candidate 由 Provenance Policy 决定。

---

## 22. External Identity 冲突

如果一个 Provider ID 已绑定 Resource A，但新导入逻辑认为它应该属于 Resource B：

禁止：

- 自动改绑；
- 自动合并 Resource A/B；
- 覆盖唯一约束。

应创建 Conflict：

- existing mapping；
- proposed mapping；
- evidence；
- actor；
- source；
- recommended actions。

人工可选择：

- keep existing；
- rebind；
- merge via dedicated Resource Merge Flow；
- mark candidate invalid。

Resource Merge 若未来支持，应由 Core Resource 专项 Command 处理。

---

## 23. Source 内容删除 / 不可用

扫描发现 Source Item 消失时，不直接删除 Resource。

建议状态变化：

```text
AVAILABLE
   ↓ missing / inaccessible
SOURCE_UNAVAILABLE
   ↓ grace period / confirmation
STALE / DETACHED
```

根据 Source Policy 可以：

- 保留 Attachment 但标记来源不可用；
- 如果 Blob 已完整复制进 Ikaros 管理存储，仍保持内容可用；
- 如果只有 external reference，内容可能进入 unavailable；
- 经过显式 cleanup policy 后释放无用来源引用。

永远不能仅因为一次网络故障 / NAS 未挂载就批量永久删除 Resource。

---

## 24. Mirror / Managed Copy 策略

Source 可以区分：

- REFERENCE_ONLY：只记录外部来源；
- IMPORT_COPY：复制到 Ikaros 管理 Storage；
- MOVE_INTO_MANAGED_STORAGE（高风险，可选）；
- MIRROR：维持 Source 到 Storage 的副本策略。

“移动源文件”必须与普通扫描分离，具有明确危险提示、权限、审计和失败恢复策略。

---

## 25. 文件分组与专业 Scanner

通用 Ingestion 只定义 Scanner Contract。

专业 Scanner 可以识别：

- Anime / TV：Season / Episode / Subtitle / Poster；
- Music：Artist / Album / Disc / Track / Cover；
- Comic：Volume / Chapter / Page archive；
- Photo：EXIF / Burst / Live Photo；
- Ebook：ISBN / TOC / Cover；
- Game：package / metadata / save reference。

Scanner 输出规范化 Candidate，不直接拥有 Resource Schema。

---

## 26. Plugin 扩展

Plugin 可以注册：

- Source Adapter；
- Scanner；
- Metadata Provider；
- Matcher Signal；
- Import Post-processor；
- Validator。

Plugin 必须声明 Capability / Permission。

禁止 Plugin：

- 直接获取所有文件系统权限；
- 直接写核心数据库；
- 未经授权读取其他 Source；
- 绕过 Metadata Provenance；
- 把 Provider 外部 ID 当内部 Resource ID。

---

## 27. Automation 与 Scheduled Job

典型自动化：

```text
每天 03:00
    ↓
Scan Source
    ↓
Generate Import Plan
    ↓
按 Source Policy 自动应用高置信度项
    ↓
冲突进入 Review Queue
```

或：

```text
resource.created
    ↓
Fetch Metadata
    ↓
Generate Candidate
    ↓
Apply Provenance Policy
```

Automation 只调用公开 Command，并保留 correlation / causation。

---

## 28. Command 契约

典型 Command：

- CreateIngestionSource
- UpdateIngestionSource
- DisableIngestionSource
- StartScan
- CancelScan
- GenerateImportPlan
- ApproveImportPlan
- ExecuteImportPlan
- RetryImportItems
- IgnoreCandidate
- ResolveImportConflict
- RefreshResourceMetadata
- RefreshProviderCollection
- ReevaluateMatch
- MarkSourceItemDetached

高风险 Source 变更和 Move / Cleanup 必须单独 Command，不放在普通 Scan 中隐式执行。

---

## 29. Integration Event

建议发布：

- `ingestion.source.created`
- `ingestion.scan.completed`
- `ingestion.plan.generated`
- `ingestion.import.completed`
- `ingestion.import.partially-failed`
- `ingestion.conflict.detected`
- `metadata.refresh.completed`
- `metadata.conflict.detected`
- `source.item.unavailable`

不要为每一个扫描目录项产生全局 Integration Event；大型扫描应汇总，只对有跨域价值的事实发布事件。

---

## 30. 数据库约束

至少要求：

1. Source、Scan Run、Candidate / Plan（若持久化）、Import Run、Conflict 使用 UUIDv7。
2. 时间点使用 `timestamptz`。
3. Source 内稳定对象标识建立适当唯一约束。
4. Import Plan Item 的幂等键防止重复执行。
5. Conflict 记录保留 resolution status。
6. Credential 只保存 Secret Reference。
7. 大量 Probe 原始结果避免无控制长期 JSONB 膨胀，应定义保留 / 摘要策略。
8. Scanner 结果和 Import Plan 可以按生命周期清理，但不能删除目标 Resource 的业务事实。

---

## 31. API 约定

API 遵守 `API-Convention-Design.md`。

特别要求：

- Scan / Import / Refresh 默认返回 Background Task；
- 列表分页；
- Dry Run 有明确标志；
- 大批量结果支持分页读取；
- Import Plan 通过 version / ETag 防止用户审批旧计划；
- Source 更新使用乐观并发；
- Conflict Resolve 需要 expected version；
- 高风险 cleanup 使用 Step-up Verification。

---

## 32. 并发场景

### 32.1 两个 Worker 同时导入同一 Provider ID

External Identity 数据库唯一约束负责最终仲裁；失败 Worker 重读现有 Resource 后转为 Link，而不是创建第二个身份。

### 32.2 Scan 与用户手动导入同时发生

Import Plan 执行时必须重新验证目标状态，不能假定生成计划时的快照仍然有效。

### 32.3 Metadata Refresh 与用户编辑同时发生

使用 Resource version / provenance 重新决策；不能用 Refresh 的旧快照覆盖新用户值。

---

## 33. Error / Failure 语义

需要区分：

- Source unreachable；
- credential invalid；
- scan partial failure；
- probe failure；
- unsupported format；
- ambiguous match；
- external identity conflict；
- provider rate limited；
- provider unavailable；
- import command conflict；
- storage capacity insufficient；
- source changed during import；
- permission denied。

可恢复错误进入 retry / review；不可恢复错误应保留明确原因，不能只显示“导入失败”。

---

## 34. Retry 与 Backoff

Provider 调用遵守：

- exponential backoff；
- rate limit header；
- jitter；
- max attempt；
- circuit breaker（需要时）。

本地文件 probe 错误不应无限重试占用 Worker。

重试必须以幂等 Command 为前提。

---

## 35. 取消语义

取消 Scan / Import：

- 停止未开始的新工作；
- 当前安全单元执行到一致性边界；
- 已提交的 Resource / Attachment 不回滚；
- 任务进入 CANCELLED 或 PARTIALLY_SUCCEEDED；
- 可从 Checkpoint 重新开始。

禁止为了“取消”跨大量已完成实体做自动补偿删除。

---

## 36. 可观测性

至少监控：

- Source health；
- scan duration；
- items/sec；
- bytes scanned；
- candidate count；
- exact / ambiguous / no match 分布；
- import success / failed / skipped；
- dedup hit；
- provider latency / rate limit；
- metadata conflict；
- review queue size；
- task backlog；
- unavailable source items；
- checkpoint age。

日志携带 source_id、scan_id、candidate_id、plan_id、task_id、correlation_id。

默认不记录完整文件内容、Secret、Provider Token。

---

## 37. 典型流程

### 37.1 NAS 媒体目录首次导入

```text
Admin 创建 NAS Source
      ↓
Start Scan
      ↓
枚举文件
      ↓
Scanner 识别 Episode + Subtitle + Poster Candidates
      ↓
计算必要 Fingerprint / Blob Hash
      ↓
External Identity / Existing Resource Match
      ↓
Generate Import Plan
      ↓
用户 Preview
      ↓
Approve
      ↓
Background Import Run
      ↓
Create / Link Resource
      ↓
Create Attachment / reuse Blob
      ↓
Bind External Identity
      ↓
Metadata Enrichment
```

### 37.2 文件重命名

```text
旧路径 missing
新路径 appears
      ↓
Strong Hash / Source Evidence 相同
      ↓
识别为 rename / move
      ↓
更新来源定位
      ↓
不创建新 Resource / Blob
```

### 37.3 Provider 元数据更新

```text
Scheduled Metadata Refresh
      ↓
Provider 返回新标题 / 封面 / 简介
      ↓
Normalize Candidate
      ↓
检查 Provenance
      ├── 未人工修改 → 自动更新
      └── 用户锁定 → 保存候选，不覆盖
      ↓
metadata.refresh.completed
```

### 37.4 NAS 暂时掉线

```text
Scan Source unreachable
      ↓
Source health = DEGRADED
      ↓
本次 Scan FAILED / PARTIAL
      ↓
不批量标记 Resource deleted
      ↓
Notification / Alert（按平台规则）
```

---

## 38. 测试与验收基线

至少覆盖：

1. 重复扫描同一 Source 不产生重复 Resource。
2. 文件改名但内容 Hash 相同不会重新创建 Blob。
3. 同一 External Identity 并发导入最终只对应一个 Resource。
4. Dry Run 不产生任何业务写入。
5. Import Run 崩溃后可从 Checkpoint 恢复。
6. 重试同一 Plan Item 保持幂等。
7. Ambiguous Match 不自动合并 Resource。
8. 用户锁定 Metadata 后 Refresh 不覆盖。
9. Provider 字段缺失不会错误清空用户数据。
10. Source 暂时不可访问不会批量永久删除 Resource。
11. Source Item 删除只影响来源可用性，Resource 生命周期由核心子系统控制。
12. Scanner Plugin 无法越权访问未授权路径 / Source。
13. Object Storage Secret 不通过普通 API 返回。
14. Remote URL 导入具备 SSRF 与大小限制防护。
15. 大规模 Scan / Import 可以取消且不破坏已提交业务事实。
16. Scan / Import / Refresh 的日志和事件可通过 correlation id 串联。
17. Metadata Provider 限流 / 失败时使用合理 backoff。
18. Import Plan 在目标状态变化后执行会重新校验而不是盲目应用旧决策。

---

## 39. 后续专项设计边界

以下内容由其他专项文档继续展开：

- Media Scanner 的 Season / Episode / Track / Subtitle 规则；
- Music Fingerprint；
- Ebook / Comic 内容结构识别；
- Photo EXIF / 相册聚类；
- Resource Merge / Split；
- 专业 Metadata Provider Mapping；
- 大规模分布式 Scan Worker。

本子系统只固定所有导入来源共同需要的 Source、Candidate、Match、Plan、Run、Provenance、幂等和失败恢复基线。