# Ikaros V2 Media Delivery / Restore Reliability 工程补充契约

| 项目 | 内容 |
|---|---|
| 文档名称 | Media Delivery / Restore Reliability Addendum |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-09-02 |
| 状态 | Draft / Normative P0 Extension |
| 上位设计 | `Media-Delivery-CDN-Archive-Restore-Design.md` |
| 工程补充 | `Media-Delivery-CDN-Archive-Restore-Engineering-Addendum.md` |
| P0 Acceptance | `testing/P0-Media-Delivery-Restore-Reliability-Acceptance.md` |

> 本文档补齐生产环境中最容易在故障时暴露的两类 P0 可靠性语义：Delivery Provider 选择粘性与防抖 Failover，以及 Archive Restore 的 Provider 状态对账与崩溃恢复。

---

## 1. P0 可靠性目标

必须保证：

1. 同一个活跃 Playback Session / Delivery Lease 在健康状态下保持稳定 Delivery Binding，不因每个 Range 请求重新决策而抖动。
2. Delivery Provider 的单次瞬时错误不能立即导致反复切换；Failover 必须有阈值、冷却和恢复滞后。
3. 切换 Delivery Binding 后必须形成新的 Selection Epoch，旧 Grant 不应被误认为当前首选路径。
4. Provider Restore API 已接受但 Ikaros 在落库或完成事件前崩溃时，系统能够通过 Reconciliation 收敛到 Provider 真实状态。
5. `BackgroundTask.SUCCEEDED/FAILED` 不是 Restore 真相的唯一来源；Placement / Restore Operation 必须可以根据 Provider Head / Restore Status Query 重新判断。
6. 外部人工 Restore、临时恢复副本过期、Provider eventual consistency 都必须有明确对账规则。

---

## 2. Delivery Binding 粘性

### 2.1 Selection Scope

Delivery 选择至少按以下上下文稳定：

```text
principal
+ attachment / representation
+ playback_session_id or delivery_lease_id
+ delivery_policy_version
```

一次选择产生：

```text
DeliverySelection
├── selected_binding_id
├── selection_epoch
├── selected_at
├── reason
├── health_snapshot_version
└── fallback_index
```

P0 不要求 `DeliverySelection` 必须单独持久化为长期表；可以保存到 Playback Session / Delivery Lease / 短期状态中，但必须拥有稳定的 `selection_epoch`。

### 2.2 粘性规则

当当前 Binding 仍满足以下条件时，不得只因为下一次 Range / Grant Renewal 就重新负载均衡：

- Binding enabled；
- Delivery Provider 未进入 Open Circuit；
- Storage Origin 仍可访问；
- 当前 Principal 仍有权限；
- 数据分类策略仍允许该 Delivery Type；
- 当前 Representation 未变化。

以下情况可以触发重选：

- 当前 Binding 被禁用；
- Provider 连续达到 Failover Threshold；
- Origin 不可达；
- Signing / Token Service 故障；
- Policy / Permission 改变导致当前路径不再允许；
- Representation Version 改变；
- 管理员 Emergency Drain。

---

## 3. Selection Epoch

每次真正切换 Delivery Binding 时递增或重建 `selection_epoch`。

Grant 应关联：

```text
delivery_lease_id
selection_epoch
binding_id
signing_key_version
```

规则：

1. 同一 Epoch 内可以续签多个短期 Grant。
2. 新 Epoch 创建后，新 Grant 必须使用新 Binding。
3. 旧 Epoch Grant 是否立即失效由对应 Provider Revocation Contract 决定。
4. 客户端收到新 Epoch 后应停止主动申请旧 Binding 的新 Grant。
5. Epoch 只表达路径选择代际，不替代 Playback Progress Sequence。

---

## 4. Circuit Breaker 与 Failover Hysteresis

### 4.1 Provider Health 不能单次翻转

推荐逻辑状态：

```text
CLOSED
  ↓ failure threshold reached
OPEN
  ↓ cooldown elapsed
HALF_OPEN
  ├── probe success -> CLOSED
  └── probe failure -> OPEN
```

Delivery Provider 全局 Health 与某个 Binding 的局部 Origin Health 可以分别维护。

### 4.2 Failure Classification

允许触发 Failover 的故障包括：

- connect timeout；
- DNS / TLS failure；
- Provider 5xx 超阈值；
- Token / Signing Service unavailable；
- Origin unreachable；
- Range capability probe failure；
- 明确 Provider rate limit，且策略允许 fallback。

不得因为以下响应切换到更宽松路径：

- ACL denied；
- Share revoked；
- classification denied；
- object missing；
- corrupted；
- invalid client request。

特别是 `403 permission denied` 不能通过 fallback 变成可访问。

### 4.3 防抖参数

Provider / Binding Policy 至少允许表达：

```text
failure_window
failure_threshold
open_circuit_cooldown
half_open_probe_count
recovery_success_threshold
minimum_sticky_duration
```

这些是策略值，不写死在领域模型常量中。

### 4.4 Playback Continuity

发生 Failover 时：

1. 服务端解析新的 Binding；
2. 建立新 Selection Epoch；
3. 签发新 Delivery Grant；
4. 客户端从当前播放 Offset 使用 Range 续播；
5. 新源应返回同一不可变 Representation 的一致 Content Length / ETag 或等价内容身份；
6. 若内容身份不一致，禁止静默续播，必须重新执行 Playback Source Resolution。

---

## 5. Restore Truth Model

Restore 的本地状态不是 Provider 的绝对真相。

```text
Restore Request        业务意图
Restore Operation      Ikaros 对实际 Provider 操作的记录
Background Task        执行过程
Blob Placement         当前可读性真相投影
Provider Object State  外部物理事实
```

当这些状态冲突时，必须通过受控 Reconciliation 收敛。

---

## 6. Restore Reconciliation

### 6.1 触发条件

Reconciliation 至少在以下情况运行：

- Ikaros 启动 / Worker 恢复；
- `RESTORING` 超过 Provider 预期时间范围；
- Background Task 超时或 Worker Lease 丢失；
- Provider 回调 / 查询结果与本地状态冲突；
- Delivery Read 发现临时恢复副本已经不可读；
- 管理员手动触发 Reconcile；
- 周期性低频扫描 Active Restore Operation。

### 6.2 Provider 查询能力

Storage Provider Capability 应尽量支持：

```text
head_object
query_restore_status
is_immediately_readable
restore_expiration
storage_class
provider_operation_id?
```

如果 Provider 不支持显式 Restore Status Query，可以通过 `Head + readability probe + metadata` 组合推断，但必须标记结果置信度。

### 6.3 Reconcile 状态机

示例：

```text
local RESTORING
    │
    ├── provider says restored/readable
    │      -> Placement AVAILABLE / restored temporary readable
    │      -> Operation SUCCEEDED
    │
    ├── provider says still restoring
    │      -> keep RESTORING
    │      -> refresh observed_at / expected range
    │
    ├── provider says not restoring and not readable
    │      -> classify UNKNOWN / FAILED according to evidence
    │      -> retry only by policy
    │
    └── provider unknown / temporary unavailable
           -> RECONCILE_PENDING
           -> do not fabricate FAILED
```

### 6.4 外部人工 Restore

如果管理员直接在云控制台恢复对象：

- Ikaros 下一次 Reconcile 可以把 Placement 收敛为可读；
- 不要求伪造一个历史上不存在的用户 Restore Request；
- 可以产生 `storage.restore.externally-observed` 审计 /事件事实；
- 预算统计中必须区分 Ikaros 发起与外部观察到的 Restore Bytes。

### 6.5 Restore 临时副本过期

如果 `restore_expires_at < now`，不能只根据时间戳直接宣告不可读。

正确逻辑：

```text
expiry reached
  ↓
provider readability check
  ├── still readable -> refresh provider truth
  └── not readable  -> Placement returns RESTORE_REQUIRED
```

原因是 Provider 可能延迟删除临时副本，或对象已经被 Promotion 到可即时读取 Tier。

---

## 7. 崩溃窗口

必须覆盖：

### Window A

```text
Provider accepted Restore
        ↓
Ikaros crashes before Operation marked accepted
```

恢复后必须先 Query Provider，再决定是否重发，不能无条件重复 Restore。

### Window B

```text
Provider restore completed
        ↓
Ikaros crashes before local success commit
```

Reconcile 后应收敛成功，不重复取回。

### Window C

```text
Local task says FAILED
        ↓
Provider actually completed asynchronously
```

Provider Truth 可以把 Placement 修正为可读，但必须保留失败 Attempt 历史。

---

## 8. Reconciliation 幂等与并发

1. 同一 Restore Operation 同时最多一个 Reconcile Owner。
2. Reconcile 必须允许重复执行。
3. Reconcile 不得删除 Background Task / Attempt 历史。
4. 状态修正应使用 optimistic version / row lock / equivalent CAS。
5. Reconcile 发现新的物理事实后，Outbox Event 与本地状态修正同事务提交。
6. Reconcile 不因为一次 Provider Query Timeout 把 `RESTORING` 直接改为 `FAILED`。

---

## 9. 建议事件

```text
delivery.selection.changed
delivery.provider.circuit-opened
delivery.provider.circuit-closed
storage.restore.reconcile-requested
storage.restore.reconciled
storage.restore.externally-observed
```

高频 Range Failover Probe 不需要逐次进入 Durable Outbox；只记录重要状态变化。

---

## 10. 可观测性

建议指标：

```text
delivery.binding.switch.count
delivery.binding.switch.reason
delivery.circuit.open.duration
delivery.failover.success_rate
restore.reconcile.count
restore.reconcile.corrected_state.count
restore.reconcile.provider_unknown.count
restore.operation.age
restore.operation.stuck.count
```

低带宽 Profile 下需要额外验证 Failover 后：

```text
server_proxy_bytes == 0
```

除非管理员显式允许 Server Proxy。

---

## 11. P0 验收结论

以下属于 P0 Required：

- Sticky Binding；
- Selection Epoch；
- Circuit Breaker / Hysteresis；
- Failover 不绕过 Permission；
- Failover 后从当前 Offset Range 续播；
- Restore Reconciliation；
- Server Crash Window A/B/C；
- 外部人工 Restore 收敛；
- Restore Expiry 对账；
- Reconcile 幂等与并发控制。
