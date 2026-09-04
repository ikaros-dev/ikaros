# Ikaros V2 Media Archive Working Set Policy

| 项目 | 内容 |
|---|---|
| 文档名称 | Media Archive Working Set Policy |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.3 |
| 编写日期 | 2026-09-04 |
| 状态 | Draft / Media Delivery Addendum |
| Storage 上位约束 | `../01-platform-foundation/Archive-Base-Restore-Promotion-Policy.md` |
| P0 Restore 契约 | `../00-product-baseline/contracts/P0-Media-Delivery-Restore-Contract-Addendum.md` |
| 关联设计 | `Media-Delivery-CDN-Archive-Restore-Design.md` |

> 本文档定义媒体域如何使用 Archive Base / Restore / Promotion 语义维护低成本全量存档与按需 Working Set。

---

## 0. 契约边界与实现状态

Restore Request / Operation / Budget / Cancellation 的基础状态机、`operation_key` 去重和共享 Operation 规则，以既有 P0 Restore Contract / Schema / Acceptance 为事实源。

本文只补媒体域特有行为：

- 播放请求如何 Join Existing Restore；
- Restore Window 如何覆盖 Playback / Download Lease；
- 多用户共享 Restore 时取消一个 Request 不影响其他用户；
- Promotion 如何按媒体热度触发并去重；
- Working Set 如何做 Cost-aware Eviction；
- Provider Lifecycle Drift 如何影响媒体可用性。

本文描述目标策略，不代表当前 P0 已经实现全部 `DEEP_ARCHIVE`、`ARCHIVE_BASE`、`PROMOTED_COPY`、Cost-aware Eviction 或 Provider Lifecycle Introspection 能力。

---

## 1. 适用场景

本策略主要用于：

- VCB 等体量大、极低频访问、通常可重新获取的压制资源；
- 长期媒体收藏库；
- 用户不要求所有内容始终在线，但希望访问时可恢复的内容；
- 需要把昂贵在线存储限制在近期 Working Set 内的部署。

不可重新获取的原创照片、家庭视频、重要文档不能仅依赖单一 Archive Base；还需要独立 Replica / Backup。

---

## 2. 推荐数据布局

```text
Media Resource / Release
        │
        ▼
      Blob
        │
        ├── DEEP_ARCHIVE / ARCHIVE_BASE
        │       全量、长期、禁止自动删除
        │
        ├── HOT / PROMOTED_COPY
        │       高频播放
        │
        ├── WARM / PROMOTED_COPY
        │       中低频播放
        │
        └── COLD / PROMOTED_COPY
                低频但希望较快恢复
```

Archive Base 是存档库底座；其余 Placement 是工作副本。

---

## 3. 首次访问与共享 Restore

当媒体只有 Archive Base 时：

```text
Playback Requested
      │
      ▼
Archive Base frozen?
      │ yes
      ▼
Create or Join Restore Request
      │
      ▼
RESTORING
      │
      ▼
TEMPORARILY_READABLE
      │
      ├── one-off playback -> Delivery from restored object
      │
      └── promotion policy matched -> PROMOTED_COPY
```

客户端必须明确展示“正在恢复归档”的状态，不得把小时级恢复伪装成普通加载。

### 3.1 Restore 去重与多用户复用

以下请求不能重复提交等价 Provider Restore：

- 第二个用户播放；
- 播放器重试；
- Grant Renewal；
- 新 Range Request；
- Prefetch 和用户主动恢复同时发生。

```text
User A Playback ─┐
User B Playback ─┼──> same Active Restore Operation
Prefetch Task ───┘
```

Restore Ready 后，兼容请求共享同一个 Restore Window；媒体域只维护各自 Playback / Delivery 状态，不复制底层 Operation。

### 3.2 共享 Restore 的取消

取消一个媒体请求不能破坏其他用户依赖的共享 Restore：

```text
User A cancel playback
      ↓
A Restore Request -> CANCELLED
Shared Operation  -> continues if B/C still depend on it
```

要求：

1. A 取消只结束 A 的等待意图；
2. 仍有 B/C 等 Active Request 时，不得取消共享 Provider Operation；
3. Provider Restore 不可取消时，允许 Request 已取消但 Operation 继续运行；
4. Operation 后续成功不能把已取消 Request 重新标记成功；
5. Prefetch 被取消不能影响用户主动播放请求。

---

## 4. Restore 与 Promotion 的边界

媒体域必须遵守：

- Restore 只请求现有归档 Placement 临时可读；
- Restore 不删除 Archive Base；
- Restore 不自动创建新的 Ikaros Placement；
- Promotion 才创建新的 HOT / WARM / COLD Placement；
- Promotion 成功后 Archive Base 继续保留；
- Restore Window 到期后，只允许临时读取能力结束，不允许删除 Archive Base。

对于阿里云 OSS Provider，`RestoreObject` 属于 Restore；恢复完成后的 `CopyObject` 才能承载 Promotion。

### 4.1 Restore Unit 不等于 Playback Range

媒体播放大量使用 HTTP Range，但 Range 读取粒度与归档恢复粒度是两个概念。

```text
Playback:
Range 0-8MiB
Range 8-16MiB
seek -> Range 6GiB-6.01GiB

Archive Restore:
Provider Object / Placement
```

因此：

1. 播放器只请求 10 MiB 不代表 Provider 只恢复 10 MiB；
2. Restore Budget 按 Provider 实际 `restore_unit` 和 Object Size 评估；
3. 对 20 GiB MKV 的小 Range 访问，可能仍意味着恢复整个 20 GiB Object；
4. 真正细粒度恢复必须通过 Representation / Segment / Object 切分实现。

### 4.2 Restore Window 与 Playback / Download Lease

媒体域不能签发一个明显长于当前 Restore Window 的稳定播放或下载 Lease。

推荐条件：

```text
restore_expires_at
>= expected_delivery_lease_until + safety_margin
```

如果当前 Window 不足以覆盖预计播放或下载时间，必须选择：

```text
extend restore window
or
promote to HOT/WARM
or
shorten/refuse long-lived lease
or
return restore-required state
```

禁止让长视频在播放中途因为 Restore Window 到期突然失去源，然后静默触发另一轮 Restore。

对于电影、长 OVA、整包下载等持续时间明显较长的行为，Promotion 往往比反复延长 Restore Window 更稳定。

---

## 5. 按访问热度选择目标 Tier

Promotion Target 由 Heat / Working Set Policy 决定，不应每次恢复都复制到 Standard。

示例：

```text
首次偶发访问
    -> 仅 Restore，直接读取临时可读对象

近期偶尔访问
    -> 可选 WARM / COLD PROMOTED_COPY

活跃 Playback Session / 连续访问
    -> HOT PROMOTED_COPY

长期无访问
    -> Cost-aware Eviction
    -> 最终只保留 ARCHIVE_BASE
```

具体阈值可配置，不在 P0 固定。

### 5.1 Promotion 完整性门禁

媒体域不能在 Copy 返回成功后立即把新副本作为首选播放源。

```text
COPYING
   ↓
VERIFYING
   ↓
HEALTHY
   ↓
eligible for normal Delivery Binding
```

至少验证：

- Object 存在；
- Size 与 Blob 一致；
- Provider checksum / Blob digest 在能力允许时匹配；
- Range Read 能力满足 Delivery Requirement。

校验失败：

```text
Promoted Copy -> FAILED / CORRUPT
Archive Base   -> unchanged
Playback       -> continue using other valid source
```

### 5.2 Promotion 去重

多个媒体请求不能并发 Copy 同一个大对象到同一目标层。

```text
Playback A ─┐
Playback B ─┼──> one Promotion Operation
Prefetch  ──┘
```

等价 Promotion 应按 Storage 上位策略的 Promotion Identity Join Existing Operation；如果已经存在 Healthy 等价 Placement，直接复用，不再 Copy。

---

## 6. 连续剧集预恢复

Ikaros 可以利用媒体领域语义，而不仅是 Blob 访问次数。

```text
用户开始播放 EP01
        │
        ├── EP01: Restore / Promote HOT
        ├── EP02: background Restore
        └── EP03: optional background Restore
```

预恢复应受到：

- Restore Budget；
- 用户近期观看行为；
- Provider 成本；
- 当前 Playback Session；
- 用户明确下载/收藏意图；
- Provider Restore Unit；
- 已存在的 Active Restore Operation / Window；

共同约束。

禁止因为浏览详情页、搜索命中或元数据扫描就恢复大批媒体字节。

当 EP02 已经因为用户主动播放进入 `RESTORING`，EP02 Prefetch 只能 Join Existing Restore；如果已在有效 Window 内，则直接视为命中。

---

## 7. Working Set Eviction

自动淘汰只能删除 `PROMOTED_COPY`。

必须满足：

1. Placement 没有 Active Playback / Download / Delivery Lease；
2. Archive Base 或其他 durability policy 要求的副本仍 Healthy；
3. 删除幂等；
4. 删除后重新计算 Delivery Availability；
5. Archive Base 永远不是预算压力下的自动淘汰候选；
6. 遵守 Provider minimum-retention / early-delete 约束。

推荐：

```text
idle HOT  -> keep / demote / evict
idle WARM -> keep / demote / evict
idle COLD -> keep / evict
ARCHIVE_BASE -> never automatic eviction
```

### 7.1 Cost-aware Eviction

热度下降不等于立即删除最省钱。

策略至少比较：

```text
remaining minimum billed duration
vs immediate deletion / early-delete cost
vs keep cost
vs demotion cost
vs future playback probability
```

结果可以是：

```text
KEEP
DEMOTE
EVICT
DEFER_UNTIL_BILLING_BOUNDARY
```

媒体侧只提供 Heat / Playback Intent；存储计费边界由 Storage Policy / Provider Cost Model 提供。

---

## 8. VCB 推荐默认策略

```yaml
archive_base:
  logical_tier: DEEP_ARCHIVE
  durability_role: ARCHIVE_BASE
  retain: indefinite
  automatic_delete: false

working_set:
  enabled: true
  promotion_by_heat: true
  eviction_by_idle_time: true
  cost_aware_eviction: true

restore:
  deduplicate_active_restore: true
  reuse_restore_window: true
  allow_direct_read_during_restore_window: true
  promote_only_when_policy_matches: true
  ensure_lease_covered_by_restore_window: true
```

核心目标：

> 全量低成本保存，访问时恢复，真正的热数据才占用在线存储成本。

如果一个 VCB 文件是 15~30 GiB 单 Object，应明确接受“首次访问可能恢复整个 Object”的成本，不能把播放器 Range 当成部分解冻机制。

---

## 9. 删除保护与 Lifecycle Drift

媒体删除、资源下架或库清理不得隐式删除 Archive Base。

真正删除存档底仓时，至少展示：

- Blob / Resource 范围；
- Archive Base 数量与物理大小；
- 是否仍存在 Replica / Backup；
- 删除后的可恢复性；
- Provider 最终删除影响；
- Object Lock / Retention / Versioning；
- minimum-retention / early-delete 费用边界。

### 9.1 Provider Lifecycle Drift

Bucket 原生 Lifecycle 不能偷偷删除 Archive Base。

```text
Provider native transition
    -> allowed only when observable/reconcilable

Provider native expiration/delete
    -> forbidden by default for ARCHIVE_BASE
```

媒体域看到 `ARCHIVE_BASE_MISSING` 或等价 Critical 状态时：

- 不得当成普通 Cache Miss；
- 不得只显示“需要 Restore”；
- 有 Replica 时可进入受控 Repair；
- 无 Replica 时必须暴露高优先级数据耐久性告警。

对于 VCB 等可重新获取内容，运维人员可以选择后续重新获取；但这仍属于 Archive Base 丢失，不应静默处理。

---

## 10. 验收不变量

至少覆盖：

1. Restore 后 Archive Base Object 仍存在；
2. Restore Window 过期后 Archive Base 仍存在；
3. Promotion 后同时存在 Archive Base 与新 Placement；
4. Working Set Eviction 只删除 PROMOTED_COPY；
5. Active Playback 时不能淘汰绑定 Placement；
6. Archive Base 删除被普通 GC / Tiering / Heat Job 阻止；
7. Promotion 失败不改变 Archive Base 健康状态；
8. 仅浏览元数据不会触发媒体字节 Restore；
9. 两个并发 Playback Request 对同一归档 Placement 只共享一个兼容 Provider Restore；
10. Restore Window 有效期内的新请求不重复触发 Restore；
11. 取消 User A 的 Restore Request 不影响仍依赖共享 Operation 的 User B；
12. Provider Restore 不可取消时，允许 Request Cancelled 但 Operation 继续；
13. 当前 Restore Window 不足以覆盖长时 Playback Lease 时，必须延长、Promotion 或拒绝长期 Lease；
14. Promotion Copy 完成但校验未完成时不能成为 Healthy Delivery Source；
15. 两个并发 Playback 对同一目标 Promotion 只产生一个 Promotion Operation；
16. 小 Range Request 不得被计为“小 Range Restore”；Provider Restore Unit 为 Object 时按整个 Object 处理；
17. Idle 达阈值但仍处于 minimum-retention 边界时不得机械立即删除；
18. Provider Retention / Object Lock 拒绝删除时安全阻断；
19. Provider Lifecycle 静默删除 Archive Base 时必须进入 Critical Durability Fault，而不是普通 Cache Miss。
