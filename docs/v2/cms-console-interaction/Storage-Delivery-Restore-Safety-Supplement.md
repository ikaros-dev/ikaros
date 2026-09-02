# Ikaros V2 CMS Storage Delivery / Restore 安全与预算补充

| 项目 | 内容 |
|---|---|
| 文档名称 | Storage Delivery / Restore Safety Supplement |
| 适用端 | CMS Console |
| 状态 | Draft |
| 基线 | `Storage-Delivery-Archive-Operations-Design.md` |
| 工程契约 | `../Media-Delivery-CDN-Archive-Restore-Engineering-Addendum.md` |

> 本文档补充 Delivery Cache Key、Grant Revocation / Key Rotation、Restore Budget、Restore Operation 去重与 Fallback Policy 的后台管理体验。

---

## 1. Delivery Binding 高级配置

Delivery Binding 页面增加明确的 Cache Identity 区域：

```text
Cache Identity
├── Exclude auth query from cache key     ON
├── Auth query names
│   ├── token
│   ├── signature
│   └── expires
├── Query allowlist
├── Vary header allowlist
└── Representation identity policy
```

默认值必须满足：

> Token / Signature / Expires 不参与不可变媒体 Cache Identity。

如果 Provider 无法做到，应显示高可见风险：

> 当前 Delivery Provider 的鉴权参数会参与缓存键。短期 URL 轮换可能导致 CDN 命中率显著下降。

不得为追求缓存命中率关闭授权验证。

---

## 2. Fallback Policy Builder

管理员需要看到实际路径顺序，而不是隐式 fallback：

```text
1. China CDN
2. Direct Object Storage
3. Fail
```

或：

```text
1. CDN
2. Server Proxy
```

每个步骤显示：

- Provider；
- Range Capability；
- 是否允许 Private Origin；
- Health；
- 是否允许在当前 Deployment Profile 使用。

### Low-bandwidth Profile Guard

当 Profile 为低带宽模式时，管理员把 `SERVER_PROXY` 加入自动 fallback 链必须二次确认：

> Server Proxy 会让媒体正文经过 Ikaros Server。低带宽服务器可能无法承载高码率视频流量。

默认链：

```text
CDN -> DIRECT -> FAIL
```

---

## 3. Restore Budget 页面

建议展示：

```text
Per Request
├── Max Bytes
└── Max Items

Concurrency
├── Max Operations
└── Max Bytes In Flight

Daily
├── Requested Bytes
└── Provider Restore Bytes

Auto Prefetch
├── Max Bytes / Trigger
└── Max Items / Trigger

Overflow Action
└── REJECT / REQUIRE_CONFIRMATION / QUEUE / PARTIAL
```

同时展示本日：

- logical requested bytes；
- deduplicated provider restore bytes；
- active operations；
- queued operations；
- budget remaining。

必须明确：

> 多个 Restore Request 共享一个 Restore Operation 时，Provider Restore Bytes 只统计一次。

---

## 4. Restore Request 与 Operation 分层展示

Restore Task 页面不应把 Background Task 当唯一对象。

推荐：

```text
Restore Request
├── Scope: Season
├── Requested By
├── Budget Decision
├── 24 Items
└── Items
    ├── EP01 -> Operation A
    ├── EP02 -> Operation B
    └── EP03 -> Operation C (shared)
```

Operation Detail：

- Placement；
- Provider；
- restore class；
- operation key 的安全摘要；
- Background Task ID；
- Attempts；
- Provider Request Ref（按权限显示安全摘要）；
- temporary copy expiry；
- 被多少 Restore Request 共享。

管理员取消某 Request 时，如果 Operation 被其他 Request 使用，UI 必须提示：

> 底层恢复操作仍被其他请求使用，因此不会取消 Provider Restore。

---

## 5. Delivery Grant Security

Delivery Provider 页面显示：

```text
Grant Revocation Mode
├── IMMEDIATE
├── KEY_VERSION_BOUND
├── TTL_BOUNDED
└── NOT_REVOCABLE_BEFORE_EXPIRY

Grant TTL
Signing Key Version
Last Rotation
```

对于 `NOT_REVOCABLE_BEFORE_EXPIRY`：

- 默认显示风险；
- 高敏感数据 Policy 不允许选择时，要在配置校验阶段直接拒绝。

Console 永远不展示当前用户正在使用的完整 Signed URL。

---

## 6. Signing Key Rotation

提供：

```text
[Rotate Signing Key]
[Emergency Rotate]
```

普通 Rotate：

- 配置新 `secret://` Reference；
- 新 Grant 使用新 Key Version；
- 可设置短 overlap window。

Emergency Rotate：

- Step-up Verification；
- 明确提示旧 Grant 可能立即失效；
- 记录 Audit；
- 不在确认框显示 Raw Secret。

---

## 7. Delivery Lease 可观测性

不需要把每个 Lease 做成主要后台实体列表，但 Storage Diagnostics 应能查询：

- Active Playback Leases；
- Active Download Leases；
- protected bytes；
- oldest / nearest expiry；
- 某 Placement 为什么暂时不能 Demote / Cleanup。

在 Blob / Placement 详情可显示：

```text
Tier action blocked
Reason: 2 active playback leases
```

管理员不应默认“强制删除 Lease”；极端运维操作需要 Step-up + Audit，并提示可能中断播放。

---

## 8. Budget Confirmation Audit

当用户绕过普通 Restore Budget 进行确认恢复：

Audit 保存：

```text
actor
restore_request_id
logical bytes
budget policy id/version
confirmation result
occurred_at
```

不保存支付数据或云厂商账单假设。

---

## 9. Metrics 增量

新增 / 强化：

### Cache / Delivery

- normalized cache hit ratio；
- auth-coupled cache request ratio；
- Grant issue success / failure；
- Grant refresh count；
- fallback count by path；
- Server Proxy fallback bytes。

### Restore

- Restore Requests；
- deduplicated Restore Operations；
- operation sharing ratio；
- logical requested bytes；
- provider restore bytes；
- budget rejected bytes；
- auto-prefetch stopped bytes/items；
- restore latency distribution。

### Lease

- active lease count；
- protected bytes；
- tier actions blocked by lease。

---

## 10. Alerts

建议：

```text
LOW_BANDWIDTH_SERVER_PROXY_BYTES_HIGH
DELIVERY_AUTH_COUPLED_CACHE_RATIO_HIGH
RESTORE_BUDGET_EXHAUSTED
RESTORE_OPERATION_DUPLICATION_DETECTED
DELIVERY_PROVIDER_ALL_PATHS_DOWN
SIGNING_KEY_ROTATION_FAILED
ACTIVE_LEASE_CLEANUP_CONFLICT
```

`RESTORE_OPERATION_DUPLICATION_DETECTED` 属于高优先级一致性 / 成本风险，因为可能意味着同一归档对象被重复提交恢复。

---

## 11. Admin Acceptance

1. Cache Key Policy 明确展示 auth query 是否被排除。
2. 无法解耦 Token/Cache Key 时有显著风险提示。
3. low-bandwidth Profile 加 Server Proxy fallback 需要二次确认。
4. Restore Request 与 Restore Operation 分层展示。
5. 可以识别多个 Request 共享一个 Operation。
6. Budget 页面区分 logical requested bytes 与 provider restore bytes。
7. Grant Revocation Mode / TTL / Key Version 可见。
8. Emergency Key Rotation 需要 Step-up + Audit。
9. Active Lease 能解释为什么 Placement 无法 Demote / Cleanup。
10. Console 不显示完整 Signed URL / Raw Token / Secret。
