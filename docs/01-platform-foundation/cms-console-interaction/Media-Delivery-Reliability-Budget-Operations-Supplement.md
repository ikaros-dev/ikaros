# Ikaros V2 CMS Media Delivery Reliability / Budget 运维补充

| 项目 | 内容 |
|---|---|
| 适用端 | CMS Console |
| 适用版本 | Ikaros V2 |
| 状态 | Draft |
| 依赖 | `../Media-Delivery-Restore-Reliability-Addendum.md`、`../Media-Delivery-Representation-Sharing-Budget-P1-Design.md` |

---

## 1. Delivery Reliability 页面

建议在 Storage / Delivery 管理域增加 Reliability 区域：

```text
Delivery Reliability
├── Binding Health
├── Circuit State
├── Active Selection Epochs
├── Failover Events
└── Restore Reconciliation
```

---

## 2. Circuit Breaker 配置

管理员可以按 Delivery Provider / Binding 配置：

```text
failure_window
failure_threshold
open_circuit_cooldown
half_open_probe_count
recovery_success_threshold
minimum_sticky_duration
```

必须提供合理默认值和能力说明，避免管理员把 threshold 设置为 1 后造成抖动。

危险配置需警告：

- cooldown = 0；
- failure_threshold 过低；
- low-bandwidth profile 把 SERVER_PROXY 放在自动 fallback 链；
- 多个 Binding 均指向同一实际故障 Origin，却被误认为冗余路径。

---

## 3. Failover 观测

列表建议展示：

```text
Session / Lease
Previous Binding
New Binding
Selection Epoch
Reason
Provider Health Snapshot
Occurred At
Resume Result
```

默认不显示完整 Signed URL。

指标：

- binding switch count；
- switch reason；
- failover success rate；
- circuit open duration；
- session interruption count。

---

## 4. Restore Reconciliation

Restore 运维页必须区分：

```text
Restore Request
Restore Operation
Background Task / Attempt
Provider Observed State
```

管理员可以：

- 手动触发 Reconcile；
- 查看上次 Provider Observation；
- 查看 Operation Age；
- 查看 Provider Operation ID（若可安全显示）；
- 查看本地状态与 Provider 状态是否冲突；
- 对长期卡住 Operation 执行受控 Retry / Reconcile。

不得提供“强制把本地状态改成成功”而不查询 Provider 的普通按钮。

---

## 5. Stuck Restore Alert

建议告警：

```text
restore_operation_age > expected_latency_upper_bound + grace
provider_status_unknown_too_long
reconcile_corrected_state
restore_retry_loop_detected
```

`reconcile_corrected_state` 不是必然错误，但应记录审计，因为说明本地执行状态曾与 Provider Truth 偏离。

---

## 6. Representation 生命周期

Console 可查看：

```text
Source Attachment
Source Tier
Representation ID / Version
Derived Bytes
Derived Tier
Active Leases
Lifecycle
GC Eligibility
```

允许管理员：

- Promote / Demote Derived Representation；
- 重新生成 Representation；
- Retire 旧 Version；
- 在无 Lease / Retention 阻塞时 GC；
- 查看为什么 Source 已归档但 Derived 仍保留在热层。

不得把删除 Derived Representation 等价为删除 Source。

---

## 7. Delivery Traffic Budget

建议页面：

```text
Traffic Budget
├── CDN Delivered
├── Origin Bytes
├── Direct Egress
├── Server Proxy Bytes
└── Cross-region Origin
```

配置：

```text
daily_delivered_bytes_soft
monthly_delivered_bytes_soft
origin_bytes_daily_soft
origin_bytes_monthly_soft
direct_egress_bytes_soft
server_proxy_bytes_hard_limit
server_proxy_bandwidth_limit
server_proxy_concurrent_streams_limit
```

Soft Budget：

- 预警；
- Dashboard 标记；
- 可暂停非必要 Prefetch / 后台工作；
- 默认不打断活跃播放。

Hard Guard：

- 明确阻止新的隐式 Server Proxy media traffic；
- 必须审计触发原因；
- 提供解除 / 调整策略入口。

---

## 8. Share Delivery Diagnostics

管理员排查分享媒体时可以查看：

```text
Share ID
Guest Principal / Session summary
Effective capabilities
Grant provider type
Grant expiration bound
Revocation mode
```

禁止显示：

- Share Raw Token；
- Delivery Raw Token；
- 完整 Signed URL；
- Origin Credential。

页面需要明确提示 `stream != download`。

---

## 9. CDN Purge

Purge 放在高级 / 危险运维区域，而不是 Representation 普通“保存”流程。

执行 Purge 时：

- 选择 Provider / Binding；
- 选择不可变 Representation / path scope；
- 输入原因；
- Step-up / 二次确认（按策略）；
- 记录审计；
- 不在日志中写完整授权 URL。

正常发布新 Representation 时 UI 不应自动提示“先覆盖再 Purge”。

---

## 10. 验收

1. 能看到 Binding Circuit 状态和 Failover 原因。
2. 管理员不能用普通按钮伪造 Restore Success。
3. 能手动触发 Reconcile 并看到 Provider Observation。
4. 能区分 Source 与 Derived Representation 的 Tier / GC。
5. Soft Traffic Budget 与 Server Proxy Hard Guard 分开配置。
6. low-bandwidth profile 中 Server Proxy fallback 配置有高风险提示。
7. Share Diagnostics 不泄露 Raw Token。
8. CDN Purge 是显式高风险运维动作并有审计。
