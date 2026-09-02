# Ikaros V2 Media Delivery / Restore Contract Index

> 本页是 Media Delivery / CDN / Archive Restore 从领域设计到 P0 工程契约与 P1 扩展设计的统一入口索引。

---

## 1. 领域与工程设计

1. [`Media-Delivery-CDN-Archive-Restore-Design.md`](./Media-Delivery-CDN-Archive-Restore-Design.md)
   - 控制面 / 数据面分离；
   - Delivery Provider；
   - CDN / Private Origin；
   - Archive Restore；
   - Heat / Tiering / Working Set。

2. [`Media-Delivery-CDN-Archive-Restore-Engineering-Addendum.md`](./Media-Delivery-CDN-Archive-Restore-Engineering-Addendum.md)
   - Restore Request / Operation 三层模型；
   - 去重与并发合并；
   - CDN Cache Identity 与 Authorization 解耦；
   - Delivery Grant 撤销；
   - Playback Lease；
   - Restore Budget；
   - Delivery Fallback；
   - Storage Provider / Delivery Provider / Binding 正式边界。

3. [`Media-Delivery-Restore-Reliability-Addendum.md`](./Media-Delivery-Restore-Reliability-Addendum.md)
   - Sticky Delivery Binding；
   - Selection Epoch；
   - Circuit Breaker / Hysteresis；
   - Failover continuity；
   - Restore Provider Truth；
   - Restore Reconciliation；
   - Server Crash Window A/B/C；
   - External Restore observation。

4. [`Media-Delivery-Representation-Sharing-Budget-P1-Design.md`](./Media-Delivery-Representation-Sharing-Budget-P1-Design.md)
   - Adaptive Representation 与 Storage / Delivery 桥接；
   - Source / Derived Tier 独立；
   - Share / Guest → Delivery Grant；
   - Delivery Traffic Budget；
   - Server Proxy Hard Guard；
   - Immutable Representation / CDN Purge Contract；
   - 多 CDN / 多区域兼容边界。

---

## 2. P0 Engineering Contracts

### Database

[`database/P0-Media-Delivery-Restore-Schema-Addendum.md`](./database/P0-Media-Delivery-Restore-Schema-Addendum.md)

新增规范性 Schema Contract：

```text
storage.delivery_provider
storage.delivery_binding
storage.restore_request
storage.restore_request_item
storage.restore_operation
storage.delivery_lease
storage.restore_budget_policy
```

### Command / Query / Event / Permission

[`contracts/P0-Media-Delivery-Restore-Contract-Addendum.md`](./contracts/P0-Media-Delivery-Restore-Contract-Addendum.md)

定义：

- Delivery Provider Commands / Queries；
- Restore Commands / Queries；
- Delivery Grant / Lease Commands；
- deterministic Permission Registry 增量；
- Stable Error Codes；
- Durable Event Catalog。

### Event Payload

[`contracts/P0-Media-Delivery-Restore-Event-Payload-Schema-Addendum.md`](./contracts/P0-Media-Delivery-Restore-Event-Payload-Schema-Addendum.md)

定义 Delivery / Restore / Lease Event v1 最小 Payload，并明确禁止 Signed URL / Token / Secret 进入 Durable Event。

### OpenAPI

[`api/openapi-v2-p0-media-delivery-restore.yaml`](./api/openapi-v2-p0-media-delivery-restore.yaml)

覆盖：

- Attachment Availability；
- Delivery Grant；
- Attachment / Season Restore；
- Restore Request 查询 / 取消；
- Delivery Lease Renew / Release；
- Delivery Provider Admin；
- Probe / Key Rotation；
- Restore Budget Policy。

### Acceptance — 基础

[`testing/P0-Media-Delivery-Restore-Acceptance-Matrix.md`](./testing/P0-Media-Delivery-Restore-Acceptance-Matrix.md)

覆盖：

- Schema Constraints；
- Restore Idempotency / Race Dedup；
- Budget Hard Guard；
- Cache Identity / Authorization；
- Grant Revocation；
- Delivery Lease；
- Fallback；
- Availability；
- Permission / OpenAPI；
- Redaction；
- Required E2E Scenarios。

### Acceptance — Reliability

[`testing/P0-Media-Delivery-Restore-Reliability-Acceptance.md`](./testing/P0-Media-Delivery-Restore-Reliability-Acceptance.md)

覆盖：

- Sticky Binding；
- Provider transient failure 不抖动；
- Circuit OPEN / HALF_OPEN / recovery；
- Selection Epoch；
- Failover Range Continuity；
- Restore Crash Window A/B/C；
- Reconciliation；
- External Restore；
- Restore temporary copy expiry；
- Reconcile idempotency / concurrency。

---

## 3. P1 Contracts / Acceptance

### P1 Design

[`Media-Delivery-Representation-Sharing-Budget-P1-Design.md`](./Media-Delivery-Representation-Sharing-Budget-P1-Design.md)

P1 负责：

- Adaptive Streaming Representation 生命周期；
- Source / Derived Storage Tier 独立；
- Guest / Share Delivery Capability；
- Delivery Traffic Budget；
- Immutable URL / Representation Version；
- Purge 高风险运维语义。

### P1 Acceptance

[`testing/P1-Media-Delivery-Representation-Sharing-Budget-Acceptance.md`](./testing/P1-Media-Delivery-Representation-Sharing-Budget-Acceptance.md)

覆盖：

- Archived Source / Hot Derived；
- Derived GC；
- Representation Version Cache Isolation；
- stream-only Share；
- Share TTL / Revocation；
- Cache Hit authorization；
- CDN / Origin soft traffic budget；
- Server Proxy hard guard；
- Immutable Representation；
- CDN Purge audit。

---

## 4. Client / Console Interaction

### App

- [`app-interaction/Media-Archive-Restore-Interaction-Design.md`](./app-interaction/Media-Archive-Restore-Interaction-Design.md)
- [`app-interaction/Media-Delivery-Restore-Safety-Supplement.md`](./app-interaction/Media-Delivery-Restore-Safety-Supplement.md)
- [`app-interaction/Media-Delivery-Reliability-Sharing-Supplement.md`](./app-interaction/Media-Delivery-Reliability-Sharing-Supplement.md)

覆盖：

- Restore；
- Budget Confirmation；
- Grant / Lease；
- Permission Revocation；
- Provider Failover；
- Selection Epoch；
- Restore Reconciliation；
- Archived Source / Hot Derived；
- Share / Guest Streaming；
- Traffic Guard UX。

### CMS Console

- [`cms-console-interaction/Storage-Delivery-Archive-Operations-Design.md`](./cms-console-interaction/Storage-Delivery-Archive-Operations-Design.md)
- [`cms-console-interaction/Storage-Delivery-Restore-Safety-Supplement.md`](./cms-console-interaction/Storage-Delivery-Restore-Safety-Supplement.md)
- [`cms-console-interaction/Media-Delivery-Reliability-Budget-Operations-Supplement.md`](./cms-console-interaction/Media-Delivery-Reliability-Budget-Operations-Supplement.md)

覆盖：

- Provider / Binding；
- Cache Key Policy；
- Restore Budget；
- Signing Key Rotation；
- Circuit Breaker；
- Failover Diagnostics；
- Restore Reconciliation；
- Representation Lifecycle；
- Traffic Budget；
- Share Delivery Diagnostics；
- CDN Purge Audit。

---

## 5. Normative Relationship

在这些 Addendum 后续被折叠进总 P0 文档前，解释顺序为：

```text
Product Requirements
    ↓
System Overview
    ↓
Attachment / Blob / Storage + Media Playback + Sharing Subsystem Design
    ↓
Media Delivery / CDN / Archive Restore Design
    ↓
Media Delivery / Restore Engineering Addendum
    ↓
Media Delivery / Restore Reliability Addendum
    ↓
P0 Schema / CQE / Event Payload / OpenAPI Addendum
    ↓
P0 Acceptance + Reliability Acceptance
    ↓
Implementation

P1:
Media Delivery Representation / Sharing / Budget Design
    ↓
P1 Acceptance
```

Addendum 不降低任何既有上位约束。

---

## 6. P0 必须成立的关键不变量

1. Ikaros Server 在低带宽 Profile 下不默认进入媒体数据面。
2. Delivery Provider 与 Durable Storage Provider 分离。
3. CDN Cache 不计为 Blob Durable Replica。
4. 同一 Placement 的等价 Active Restore Operation 只能有一个。
5. 多个业务 Restore Request 可以共享 Restore Operation。
6. CDN Authorization 参数不得默认改变不可变媒体 Cache Identity。
7. Cache Hit 不能绕过授权验证。
8. Delivery Grant 每次签发重新检查 Permission。
9. Active Playback Lease 保护当前唯一可读来源。
10. Restore / Auto Prefetch 受服务端 Hard Budget Guard。
11. Permission / Classification Denied 不能通过 Fallback 绕过。
12. low-bandwidth Profile 默认 `CDN -> DIRECT -> FAIL`，不静默 Server Proxy。
13. Signed URL / Token / Secret 不进入 Durable Event、普通日志和 Analytics。
14. Restore 完成后 Attachment ID 不变化。
15. 同一 Playback Session 在 Provider 健康时保持 Sticky Binding。
16. 瞬时错误不能造成每 Range Provider 抖动。
17. 真正 Failover 必须产生新的 Selection Epoch。
18. Failover 后只有内容身份一致时才允许从原 byte offset 续播。
19. Restore 状态必须能够通过 Provider Reconciliation 收敛。
20. Background Task 状态不能单独凌驾 Provider Object Truth。
21. Server Crash 后不得无条件重复提交已经被 Provider 接受的 Restore。
22. 外部人工 Restore 可以被观察并收敛，但不能伪造用户请求历史。

---

## 7. P1 关键不变量

1. Source 与 Derived Representation 生命周期、Tier 和 GC 独立。
2. Representation URL / Cache Identity 必须不可变版本化。
3. 正常发布新 Representation 不依赖 CDN Purge。
4. Share Token 先解析为 Principal / Capability，再签发 Delivery Grant。
5. `stream/read != download`。
6. Guest Delivery Grant TTL 不得超过 Share / Guest Session TTL。
7. Delivery Traffic Soft Budget 默认不打断活跃播放。
8. LOW_BANDWIDTH Profile 可以对 Server Proxy 使用 Hard Guard。
9. 旧 Representation 有 Active Lease 时不能破坏性 GC。
