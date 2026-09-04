# Ikaros V2 Archive Base / Restore / Promotion Policy

| 项目 | 内容 |
|---|---|
| 文档名称 | Archive Base / Restore / Promotion Policy |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.3 |
| 编写日期 | 2026-09-04 |
| 状态 | Draft / Normative Storage Addendum |
| 上位设计 | `Attachment-Blob-Storage-Subsystem-Design.md` |
| P0 Restore 契约 | `../00-product-baseline/contracts/P0-Media-Delivery-Restore-Contract-Addendum.md` |
| P0 Restore Schema | `../00-product-baseline/database/P0-Media-Delivery-Restore-Schema-Addendum.md` |
| P0 Restore 验收 | `../00-product-baseline/testing/P0-Media-Delivery-Restore-Acceptance-Matrix.md` |
| 关联设计 | `../02-domain-capabilities/Media-Delivery-CDN-Archive-Restore-Design.md` |

> 本文档补充 Ikaros V2 对长期归档底仓、归档恢复、访问热度晋升副本和自动清理的规范语义。
>
> 核心原则：**归档底仓与热副本是两个独立 Placement；Restore 不是迁移；Promotion 不替换底仓；任何自动分层不得删除 Archive Base。**

---

## 0. 契约所有权与实现状态

### 0.1 P0 Restore Contract 是 Restore 机制事实源

本文档不是第二套 Restore 状态机。

以下内容继续由既有 P0 Restore Contract / Schema / Acceptance 拥有：

- Restore Request / Request Item / Operation 的身份与状态；
- `operation_key`、Operation 去重与并发唯一性；
- 多个 Request Item 共享一个 Restore Operation；
- Restore Budget、并发预算、字节预算与 Budget Decision；
- Restore 的取消、失败、重试、超时与幂等基础语义；
- 对外 API、稳定字段、错误码和公共 Event Contract。

本文档只增加 Archive Base / Working Set 场景特有的不变量：

```text
ARCHIVE_BASE protection
Restore window reuse
shared-operation cancellation safety
Restore window vs Delivery Lease
Promotion integrity / deduplication
cost-aware eviction
Provider lifecycle drift detection
```

若本文档与 P0 Restore Contract 在已有 P0 机制上出现冲突，以 P0 Restore Contract / Schema / Acceptance 为准；若涉及 `ARCHIVE_BASE` 是否允许自动删除、Promotion 是否替换底仓等本文新增语义，以本文档为专项约束，并应同步回契约与验收材料后再进入实现。

### 0.2 当前实现状态

本文档定义的是 V2 的规范目标模型，不代表当前 P0 实现已经具备本文出现的全部字段、状态或 Provider Adapter 能力。

当前主线已经存在 Placement Tiering / Restore 等基础能力，但以下概念仍可能需要后续 Schema、API、Provider Adapter 与 Acceptance 落地：

- `DEEP_ARCHIVE` 逻辑 Tier；
- `durability_role`；
- `ARCHIVE_BASE` / `PROMOTED_COPY`；
- Promotion 完整性校验和 Promotion Operation 去重；
- Restore Window 与 Delivery Lease 的安全约束；
- Provider Object Lock / Retention / Versioning 能力映射；
- Provider Lifecycle Drift Reconcile；
- Cost-aware Eviction。

因此本文中的规范字段不能被解释为“当前数据库已经存在同名列”或“当前公共 API 已经暴露同名字段”。后续实现即使采用不同内部字段名，也必须满足本文定义的不变量。

---

## 1. 设计目标

Ikaros 需要同时支持：

1. 大容量、极低访问频率的数据长期低成本保存；
2. 用户真正访问时按需恢复，并根据访问热度生成更高可用性的 Delivery Placement；
3. 热副本可以自动回收，但长期存档底仓不能因为访问热度、GC、Tiering 或 Restore Window 结束而被误删。

典型场景：

- VCB 等体量大、极低频访问、通常可重新获取的压制资源全集；
- 大型视频、漫画、音乐、游戏安装包归档库；
- 用户主动选择“长期存档”的媒体集合；
- 保留一个低成本基线副本，同时允许 Standard / IA / Archive 等工作副本自动淘汰的部署。

本文档不把任何单一云厂商的具体 Storage Class、恢复时长或价格定义为 Ikaros 领域事实。

---

## 2. 两个正交维度：Tier 与 Durability Role

Storage Tier 回答：

> 这份 Placement 当前的访问成本、恢复延迟和在线读取能力是什么？

Durability Role 回答：

> 这份 Placement 在数据保留策略中承担什么职责，是否允许自动删除？

二者必须分离。

建议逻辑 Tier：

```text
HOT
WARM
COLD
ARCHIVE
DEEP_ARCHIVE
```

建议 Durability Role：

```text
PRIMARY
REPLICA
ARCHIVE_BASE
PROMOTED_COPY
```

规则：

- `ARCHIVE_BASE`：长期存档底仓，默认不可自动删除；
- `PROMOTED_COPY`：为访问性能或 Working Set 生成的额外副本，允许按策略淘汰；
- `PRIMARY`：当前主要持久化副本；
- `REPLICA`：独立容灾或额外持久化副本。

Tier 与 Role 不绑定。例如 `ARCHIVE_BASE` 可以落在 `ARCHIVE` 或 `DEEP_ARCHIVE`；`PROMOTED_COPY` 可以落在 `HOT`、`WARM` 或 `COLD`。

---

## 3. Archive Base Placement

Archive Base Placement 表示：

> 对一个 Blob 长期保留、以归档为主要目的的基线物理副本。

推荐目标模型：

```text
BlobPlacement
├── storage_tier: DEEP_ARCHIVE
├── durability_role: ARCHIVE_BASE
├── evictable: false
├── gc_protected: true
├── auto_delete: false
└── retention_policy: explicit
```

### 3.1 强制约束

对于 `ARCHIVE_BASE`：

1. Heat / Working Set / Tiering 自动任务不得删除该 Placement；
2. Promotion 成功不得删除该 Placement；
3. Restore 完成、失败、取消或 Window 过期不得删除该 Placement；
4. 自动 Demotion 不得将它视为待回收热副本；
5. 通用 Blob GC 默认不得物理删除它；
6. Provider Drain 默认流程不得在没有满足替代保留策略时删除它；
7. 删除必须是显式高风险操作，并进入授权、确认与审计链路。

### 3.2 Archive Base 不是 Backup 的同义词

`ARCHIVE_BASE` 表示长期保留意图，不自动意味着：

- 跨 Region；
- 跨云厂商；
- 多 AZ；
- 可抵御账号级误删除；
- 满足 Backup / Disaster Recovery 的全部要求。

对于不可重新获取的数据，应另外配置独立 `REPLICA` 或正式 Backup。

---

## 4. Restore 的规范语义

归档 Provider 常见的 Restore / Rehydrate 操作表示：

> 请求 Provider 让已有归档对象在一段时间内变为可读。

Ikaros 必须把 Restore 建模为已有 Placement 的临时可读能力，而不是新建持久 Placement。

```text
ARCHIVE_BASE Placement
        │
        │ Restore Request
        ▼
RESTORING
        │
        ▼
TEMPORARILY_READABLE
        │
        │ restore window expires
        ▼
ARCHIVED / FROZEN
```

Restore 必须满足：

- Blob ID 不变；
- Placement ID 不变；
- Object Key 不变；
- Durability Role 不变；
- 原归档对象不因 Restore 自动删除；
- Window 结束只意味着临时读取能力失效，不等于删除 Placement。

### 4.1 Provider 映射示例：Aliyun OSS

对于将 Ikaros `DEEP_ARCHIVE` 映射到阿里云 OSS 深度冷归档的 Provider：

- `RestoreObject` 映射为 Restore Operation；
- 该操作不是 Ikaros Placement Migration；
- 恢复出的临时可读副本不注册为新的持久 Placement；
- 如果需要长期 Standard / IA / Archive 副本，应在恢复可读后执行独立 Copy / Promotion；
- 只有显式 Delete Operation 才允许真正删除 Archive Base Object。

其他 Provider 按同样领域语义映射自己的 Restore / Rehydrate 能力。

### 4.2 Restore 去重与窗口复用

具体 `operation_key`、数据库唯一性和共享 Operation 仍由 P0 Restore Contract / Schema 定义。本文只补充 Archive 场景要求：

- 多个用户、Range 请求、Playback Retry 或 Prefetch 命中同一兼容 Restore 时必须复用已有 Active Operation；
- `RESTORING` 期间的重复请求不能重复触发 Provider Restore 副作用；
- Restore 已 Ready 且当前 Window 满足请求时，后续请求直接复用现有 Window；
- Background Task 重试必须保持 Provider 副作用幂等；
- 只有新的恢复等级、Window 时长或 Provider 约束与当前 Operation 不兼容时，才允许依据 P0 Contract 创建新的 Operation。

### 4.3 Restore Unit 与 Range Read 的边界

必须区分：

```text
Restore Unit != Delivery Read Unit
```

典型情况：

```text
Restore Unit      = Provider Object / Placement
Delivery Read Unit = Object Range / Segment / Full Object
```

因此：

- HTTP `Range` 只描述恢复完成后的读取范围；
- `Range: bytes=...` 不得被解释为“Provider 只恢复该字节范围”；
- Provider 只支持对象级恢复时，恢复预算、延迟和成本必须按整个 Object 计算；
- 更细恢复粒度必须通过对象切分、Representation、Segmented Blob 等显式建模实现，而不能伪造 Range Restore。

### 4.4 共享 Restore 的取消语义

Restore Request 与 Restore Operation 必须保持独立生命周期。

```text
Request A ─┐
Request B ─┼──> Shared Restore Operation
Request C ─┘
```

取消规则：

1. 调用方取消自己的 Restore Request，只取消该 Request / Request Item 的等待意图；
2. 只要仍有其他 Active Request 依赖同一个 Operation，不得取消 Provider Operation；
3. 当最后一个依赖者取消后，只有 Provider 明确支持安全取消、且取消不会破坏已存在 Restore Window 时，才允许尝试取消 Provider Operation；
4. Provider Restore 不可取消时，允许出现：

```text
Request = CANCELLED
Operation = RUNNING / SUCCEEDED
```

5. Operation 后续成功不能把已取消 Request 重新变成成功；
6. 取消一个 Playback / Prefetch 不能影响其他用户共享的恢复任务。

具体状态枚举仍以 P0 Restore Contract 为准。

### 4.5 Restore Window 与 Delivery / Playback Lease

Ikaros 不得发放一个明显超过可读恢复窗口的稳定 Delivery Binding。

推荐安全条件：

```text
restore_expires_at
>= expected_delivery_lease_until + safety_margin
```

当当前 Restore Window 不足以覆盖预期播放或下载时，策略必须显式选择：

```text
EXTEND_RESTORE_WINDOW
PROMOTE_TO_ONLINE_TIER
SHORTEN_OR_REFUSE_LONG_LEASE
RETURN_RESTORE_REQUIRED
```

禁止：

```text
restore window ends
      ↓
active playback suddenly loses source
      ↓
silent re-restore / provider oscillation
```

对于 Provider 不支持延长 Window 的情况，长时播放或下载更适合 Promotion 到在线 Tier。

---

## 5. Promotion：从归档底仓生成工作副本

Promotion 表示：

> 基于现有可读 Placement，创建一个新的、更适合当前访问模式的持久 Placement。

Promotion 是复制，不是替换。

```text
                    ┌── HOT / PROMOTED_COPY
                    │
Blob ───────────────┼── WARM / PROMOTED_COPY
                    │
                    ├── COLD / PROMOTED_COPY
                    │
                    └── DEEP_ARCHIVE / ARCHIVE_BASE
```

Promotion 完成后：

- Archive Base 继续存在；
- 新 Placement 拥有自己的 `placement_id`；
- 新 Placement 可以被 Delivery Binding 选择；
- 新 Placement 可以根据 Heat / Budget / Idle Time 自动淘汰；
- Promotion 失败不得损坏或降级 Archive Base。

### 5.1 推荐流程

```text
Access Requested
      │
      ▼
Only Archive Base Available?
      │ yes
      ▼
Request Restore
      │
      ▼
Restore Ready
      │
      ├── one-off access ──> read restored object
      │
      └── promotion policy matched
              │
              ▼
          Copy Object
              │
              ▼
      Verify Promoted Bytes
              │
              ▼
     Mark PROMOTED_COPY Healthy
              │
              ▼
          Delivery Ready
```

### 5.2 Promotion 完整性校验门禁

Provider 返回 Copy 成功不等于新的 Placement 可以成为 Healthy Delivery Source。

至少需要：

```text
Copy / Upload completed
        ↓
Provider object exists
        ↓
size verification
        ↓
checksum / digest verification when supported
        ↓
Blob identity validation
        ↓
Placement HEALTHY
```

强制规则：

1. 新 Placement 在校验完成前只能处于非 Healthy 状态；
2. Object Size 必须与 Blob 期望大小一致；
3. Provider 支持强校验和时优先使用；
4. Provider 校验和不足以代表完整 Blob Digest 时，按 Storage Integrity Policy 补充校验；
5. 校验失败只能隔离/清理失败副本，不得修改 Archive Base 健康状态；
6. Delivery Selector 不能选择未通过校验的 Promotion 目标；
7. Server-side Copy 也不能跳过完整性门禁。

### 5.3 Promotion Operation 去重

多个播放请求不能把同一个大对象并发 Copy 多次。

推荐 Promotion Identity 至少包含：

```text
blob_id
+ source_placement_id
+ target_provider_id
+ target_tier
+ representation / object-shape identity when applicable
```

同一 Promotion Identity 在同一时刻最多存在一个 Active Promotion Operation：

```text
Playback A ─┐
Playback B ─┼──> one Promotion Operation
Prefetch  ──┘
```

要求：

- 后续请求 Join 已存在的 Promotion；
- Promotion Task Retry 不产生重复 Provider Copy；
- 已存在 Healthy 等价 Placement 时直接复用，不再 Promotion；
- 失败 Operation 可以安全重试，但必须通过 Idempotency / Operation Key 防止重复物理副本。

---

## 6. Working Set 与 Cost-aware Eviction

Working Set 只管理可淘汰工作副本，不管理 Archive Base。

示例：

```yaml
archive_base:
  required: true
  role: ARCHIVE_BASE
  tier: DEEP_ARCHIVE
  evictable: false

promotion:
  hot:
    target_tier: HOT
    when:
      playback_active: true
      or_access_count_7d_gte: 3
  warm:
    target_tier: WARM
    when:
      access_count_30d_gte: 1

eviction:
  HOT:
    idle_after: 14d
  WARM:
    idle_after: 60d
```

阈值只是示例，不是 P0 常量。

自动策略必须遵守：

1. 删除 `PROMOTED_COPY` 前确认仍存在满足 durability policy 的持久 Placement；
2. Active Playback / Download / Delivery Lease 期间不得删除当前绑定 Placement；
3. Promotion / Eviction 必须幂等；
4. 半完成复制不得注册为 Healthy；
5. `ARCHIVE_BASE` 永远不是 Working Set 自动淘汰候选。

Idle Time 只能作为淘汰输入之一。对于存在最低存储时长、提前删除费用、取回费或迁移费的 Tier，应综合：

```text
idle time
+ remaining minimum billed duration
+ early deletion charge
+ retrieval / transition charge
+ current tier storage cost
+ target tier storage cost
+ expected future heat
+ promotion cost already paid
```

策略结果可以是：

```text
KEEP
DEMOTE
EVICT
DEFER_UNTIL_BILLING_BOUNDARY
```

实时价格不是 Storage 领域事实；缺少可靠价格时，至少遵守 Provider 声明的 minimum-retention / early-delete 边界。

---

## 7. Archive Base 删除安全

Archive Base 删除属于高风险、不可逆操作。

至少要求：

```text
explicit delete request
      ↓
authorization
      ↓
step-up verification when configured
      ↓
retention / hold validation
      ↓
impact preview
      ↓
explicit confirmation
      ↓
provider delete
      ↓
verification
      ↓
audit event
```

以下路径不得直接删除 `ARCHIVE_BASE`：

- Heat 下降；
- Working Set Eviction；
- Promotion 成功；
- Restore Window 过期；
- Provider Drain 默认自动流程；
- 普通 Attachment 逻辑归档；
- 未经过保护检查的通用 GC。

### 7.1 Provider 侧防误删

应用层保护不能替代 Provider 自身保护。Provider 应尽可能声明：

```text
supports_object_lock
supports_retention_lock
supports_versioning
supports_soft_delete / recycle_bin
```

建议：

- 可重新获取的大容量媒体 Archive Base：Provider Lock 可选；
- 原创照片、家庭视频、重要文档：推荐结合 Versioning / Retention / Object Lock 与独立 Replica / Backup；
- Provider 返回 Retention Denied 时记录为 Delete Blocked，而不是普通可重试失败。

### 7.2 Provider-native Lifecycle Drift

Ikaros 不能只防自己删除，还必须防 Bucket / Provider 原生生命周期规则在系统外改变 Archive Base。

对于 `ARCHIVE_BASE`：

```text
Provider native transition
    -> 可允许，但必须可观测并 Reconcile

Provider native expiration / delete
    -> 默认禁止
```

典型危险配置：

```text
bucket lifecycle:
  expire_after: 365d
```

如果该规则能命中 Archive Base，Ikaros 的 `gc_protected=true` 没有任何保护作用。

Reconciler 应周期性或事件驱动校验：

```text
object exists?
storage class / tier matches expectation?
retention / object-lock state matches expectation?
version identity still reachable?
provider lifecycle rule conflicts with Archive Base?
```

发生 Drift 时：

- Storage Class 被合法 Provider Lifecycle 改变：更新观测状态并按 Policy 判断是否修正；
- Retention / Lock 比预期更强：安全阻塞删除并告警；
- Archive Base Object 缺失：标记 `ARCHIVE_BASE_MISSING` 或等价 Critical 状态；
- 不得把 Archive Base 缺失当作普通 Cache Miss；
- 如果存在 Healthy Replica，可触发受控 Repair；没有 Replica 时必须产生高优先级告警。

Provider Lifecycle 配置无法读取时，运维界面应提示“Archive Base 依赖外部生命周期配置，Ikaros 无法验证删除保护”。

---

## 8. Provider Capability

Storage Provider 应能够声明或被配置：

```text
supports_archive_restore
supports_temporary_restore_window
supports_restore_window_extension
supports_server_side_copy
supports_tier_transition
supports_deep_archive
restore_unit
restore_profile
supports_object_lock
supports_retention_lock
supports_versioning
supports_lifecycle_introspection
minimum_retention_profile
```

`restore_profile` 至少表达：

- 是否异步恢复；
- 是否存在临时可读 Window；
- 是否可延长 Window；
- 是否允许恢复后 Server-side Copy；
- 是否支持 Range Read；
- 恢复状态如何查询；
- 哪些 Restore Request 可以安全合并。

`restore_unit` 至少表达：

```text
OBJECT
SEGMENT
PROVIDER_DEFINED
```

`minimum_retention_profile` 表达最低存储时长、提前删除限制或类似账单边界，不是固定价格表。

不要在领域层假设所有 Provider 都有 `RestoreObject`、`CopyObject` 或 Lifecycle API 的同名实现。

---

## 9. 状态、事件与告警

Restore Request / Operation 继续复用现有 P0 Media Delivery / Restore Contract。

Promotion / Demotion 应与当前 `storage.placement.tiering` 任务对齐，并能够区分：

```text
PROMOTION
DEMOTION
EVICTION
```

Archive Base 至少需要审计可观测性：

```text
archive-base created
archive-base delete requested
archive-base delete blocked
archive-base deleted
archive-base drift detected
archive-base missing
```

新增稳定 Event Name 必须先进入 Event Catalog / Payload Schema；在此之前可以使用内部 Metric / Audit / Alert 表达，不得私自创造公共稳定事件名。

---

## 10. 典型 VCB 策略

对于 VCB 等“大容量、可重新获取、极低频访问”的媒体数据：

```text
全量文件
  │
  ▼
DEEP_ARCHIVE / ARCHIVE_BASE
  │
  │ 用户访问
  ▼
Shared Restore Operation
  │
  ├── one-off access -> reuse temporary restore window
  │
  └── sustained heat -> deduplicated Promotion
                         │
                         ▼
                 HOT/WARM PROMOTED_COPY
                         │
                         │ idle / budget pressure
                         ▼
                  Cost-aware Eviction
                         │
                         ▼
            Archive Base remains untouched
```

对于大媒体 Object，即使播放器只读取一个 Range，也必须按 Provider `restore_unit` 计算恢复成本。

对于可重新获取媒体，Archive Base 可以采用成本优先的 Provider 冗余配置；照片、原创文档等不可重新获取数据应另行配置正式 Replica / Backup。

---

## 11. 验收不变量

实现和后续文档必须始终满足：

1. **Restore != Copy != Delete**；
2. **Restore 不创建新的持久 Placement**；
3. **Promotion 创建新的 Placement，不替换 Archive Base**；
4. **Archive Base 默认不可自动删除**；
5. **Tier 与 Durability Role 是独立维度**；
6. **Working Set 只能自动管理可淘汰 Placement**；
7. **Restore Window 过期不等于 Archive Object 被删除**；
8. **不可重新获取数据不能把单一 Archive Base 当作完整 Backup**；
9. **Restore Operation 去重、共享和预算的基础契约以 P0 Restore Contract 为事实源**；
10. **取消一个 Restore Request 不能破坏其他依赖同一 Operation 的 Request**；
11. **长时 Delivery Lease 必须被 Restore Window 覆盖，或显式延长/Promotion/拒绝**；
12. **Promotion 在完整性校验通过前不能成为 Healthy Placement**；
13. **等价 Promotion 必须去重，不能并发复制同一大对象**；
14. **Delivery Range 不得被误解释为 Archive Restore Range**；
15. **Eviction 必须尊重 minimum-retention / early-delete 成本边界**；
16. **Provider Retention / Object Lock 拒绝删除时必须安全失败**；
17. **Provider Lifecycle 不得静默 Expire / Delete Archive Base**；
18. **Archive Base 缺失是 Critical Durability Fault，不是普通 Cache Miss**。
