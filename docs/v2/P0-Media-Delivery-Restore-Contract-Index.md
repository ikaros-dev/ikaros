# Ikaros V2 P0 Media Delivery / Restore Contract Index

> 本页是 Media Delivery / CDN / Archive Restore 从领域设计到 P0 工程契约的入口索引。

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

### Acceptance

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

---

## 3. Client / Console Interaction

### App

- [`app-interaction/Media-Archive-Restore-Interaction-Design.md`](./app-interaction/Media-Archive-Restore-Interaction-Design.md)
- [`app-interaction/Media-Delivery-Restore-Safety-Supplement.md`](./app-interaction/Media-Delivery-Restore-Safety-Supplement.md)

### CMS Console

- [`cms-console-interaction/Storage-Delivery-Archive-Operations-Design.md`](./cms-console-interaction/Storage-Delivery-Archive-Operations-Design.md)
- [`cms-console-interaction/Storage-Delivery-Restore-Safety-Supplement.md`](./cms-console-interaction/Storage-Delivery-Restore-Safety-Supplement.md)

---

## 4. Normative Relationship

在这些 Addendum 后续被折叠进总 P0 文档前，解释顺序为：

```text
Product Requirements
    ↓
System Overview
    ↓
Attachment / Blob / Storage + Media Playback Subsystem Design
    ↓
Media Delivery / CDN / Archive Restore Design
    ↓
Media Delivery / Restore Engineering Addendum
    ↓
P0 Schema / CQE / Event Payload / OpenAPI Addendum
    ↓
P0 Acceptance Matrix Addendum
    ↓
Implementation
```

Addendum 不降低任何既有上位约束。

---

## 5. P0 必须成立的关键不变量

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
