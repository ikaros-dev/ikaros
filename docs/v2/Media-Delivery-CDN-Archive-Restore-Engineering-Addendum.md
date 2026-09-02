# Ikaros V2 Media Delivery / CDN / Archive Restore 工程补充契约

| 项目 | 内容 |
|---|---|
| 文档名称 | Media Delivery / CDN / Archive Restore Engineering Addendum |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-09-02 |
| 状态 | Draft / Normative P0 Extension |
| 上位设计 | `Media-Delivery-CDN-Archive-Restore-Design.md` |
| Storage 上位约束 | `Attachment-Blob-Storage-Subsystem-Design.md` |
| P0 Schema 补充 | `database/P0-Media-Delivery-Restore-Schema-Addendum.md` |
| P0 Contract 补充 | `contracts/P0-Media-Delivery-Restore-Contract-Addendum.md` |
| P0 OpenAPI 补充 | `api/openapi-v2-p0-media-delivery-restore.yaml` |
| P0 Acceptance 补充 | `testing/P0-Media-Delivery-Restore-Acceptance-Matrix.md` |

> 本文档把 `Media-Delivery-CDN-Archive-Restore-Design.md` 中尚未压实的工程约束补充为可实现、可测试的 P0 契约。
>
> 在这些条目被后续直接合并进总 P0 Schema / Command-Query-Event / OpenAPI / Acceptance 文件之前，本 Addendum 与其配套文件视为对应 P0 基线的规范性扩展；若与更上位领域设计冲突，以上位领域设计为准。

---

## 1. 本补充解决的缺口

本次补充必须同时解决以下问题：

1. `Delivery Provider` 已被设计为 P0 能力，但原始 P0 Schema / API 尚无对应持久化与契约。
2. Restore 不能直接等价于 Background Task，需要表达用户业务请求、底层 Provider 操作与执行 Attempt 的不同身份。
3. 同一归档对象的多个 Restore 请求必须去重、合并，不得重复向 Provider 提交相同恢复操作。
4. CDN URL 的鉴权参数不能天然成为 Cache Identity，否则短期 Token 轮换会把同一 Blob 切成大量缓存对象。
5. 当前播放中的媒体必须具有短期保护租约，避免自动 Demotion、Restore 临时副本过期清理或 Provider Drain 破坏正在进行的播放。
6. 自动预热和“恢复本季”必须受容量、字节数、并发和每日预算约束。
7. 已签发 Delivery Grant 的撤销、过期与 Key Rotation 语义必须明确。
8. Delivery Provider 故障时的 fallback 必须可配置，低带宽部署不得自动把大流量回退到 Server Proxy。

---

## 2. 三层 Restore 身份模型

Restore 必须区分：

```text
Restore Request
    │
    │ 业务意图：谁想恢复什么范围
    ▼
Restore Request Item
    │
    │ 解析到具体 Placement
    ▼
Restore Operation
    │
    │ Provider 级去重后的实际恢复操作
    ▼
Background Task
    │
    └── Attempt 1 / Attempt 2 / ...
```

### 2.1 Restore Request

回答：

> “哪个 Actor 请求恢复哪个 Attachment / Episode / Season / Resource Set？”

它具有稳定业务 ID，并保存：

- actor；
- scope；
- 请求策略；
- 总字节 / 项目数量摘要；
- 当前聚合状态；
- 用户可见的错误摘要；
- correlation / idempotency context。

### 2.2 Restore Operation

回答：

> “某个归档 Placement 当前是否已经存在一次有效的 Provider Restore 操作？”

一个 Restore Operation 只针对一个需要恢复的 Placement。

多个 Restore Request 可以共享同一个正在执行或已经有效的 Restore Operation。

### 2.3 Background Task

Background Task 只回答：

> “这个实际 Restore Operation 现在如何执行、重试、取消、超时？”

禁止把 `operations.background_task.id` 当成 Season Restore 的唯一业务 ID。

---

## 3. Restore 去重与并发合并

### 3.1 Active Operation 唯一性

对同一 `placement_id`，在同一有效 restore generation 内最多存在一个 Active Restore Operation。

推荐幂等身份：

```text
restore_operation_key =
  placement_id
  + provider_restore_class
  + restore_generation
```

`provider_restore_class` 表示 Provider 真正影响恢复结果的模式类别，例如 Standard / Bulk / Expedited；具体枚举来自 Provider Capability，不在核心领域写死厂商名。

### 3.2 请求合并

当新的 Restore Request Item 解析到 Placement 时：

```text
if Placement already immediately readable:
    item -> READY

else if active Restore Operation exists:
    attach item to existing operation

else if valid temporary restored copy exists:
    item -> READY_TEMPORARILY

else:
    create Restore Operation
    create/reuse Background Task
```

必须在事务或等价的并发控制中保证不会并发创建两个 Active Operation。

### 3.3 幂等

`RequestRestore` API 必须支持 `Idempotency-Key`。

相同 Actor + 相同 Scope + 相同业务请求语义在 Idempotency Window 内重复提交时返回同一个 Restore Request，而不是创建新的业务请求。

即使不同 Restore Request 未命中同一 Idempotency Key，底层 Operation 仍必须按 Placement 去重。

### 3.4 取消

取消 Restore Request 不等于一定取消 Provider Restore Operation。

规则：

1. 如果该 Operation 仍被其他 Request Item 引用，不得取消底层 Operation。
2. 如果 Provider 已接受且不可取消，只将当前 Request 标记为“不再等待 / 已取消订阅结果”，Operation 可自然完成。
3. 只有没有其他有效请求依赖且 Provider 支持安全取消时，才请求取消底层 Operation。
4. `CANCEL_REQUESTED`、`CANCELLED`、`FAILED` 语义不得混用。

---

## 4. CDN Cache Identity 与 Authorization 解耦

### 4.1 核心规则

必须满足：

```text
Authorization Input != Cache Identity
```

例如：

```text
/media/blob/019...?expires=...&token=A
/media/blob/019...?expires=...&token=B
```

两条 URL 代表同一个不可变 Representation 时，默认应该映射到同一个 CDN Cache Identity。

### 4.2 Cache Identity

推荐：

```text
cache_identity =
  immutable_blob_identity
  + representation_identity
  + content_encoding_variant
```

可以使用稳定的：

- Blob ID + immutable version；
- Blob content digest 的安全派生键；
- Representation ID。

不得依赖：

- 用户 ID；
- Authorization Token；
- expires；
- signature；
- session ID；
- trace ID。

除非某 Delivery Provider 无法安全排除这些字段，此时必须显式声明 `cache_key_auth_coupled=true`，并将其视为性能降级能力。

### 4.3 Query / Header 策略

Delivery Provider 必须声明或配置：

```text
cache_key:
  include_path: true
  include_query_allowlist: [...]
  exclude_auth_query: true
  vary_headers_allowlist: [...]
```

Authorization 可以通过 Query Token、Cookie、Header、Edge Function 等方式完成，但授权输入不得未经审查直接进入 Cache Key。

### 4.4 安全约束

Cache Key 解耦不能绕过授权。

正确顺序：

```text
Request
  ↓
validate delivery authorization
  ↓
authorized?
  ├── no -> 401/403
  └── yes
        ↓
resolve normalized cache identity
        ↓
serve cache / fetch origin
```

禁止“先命中公共缓存，再跳过授权验证”。

---

## 5. Delivery Grant 生命周期与撤销

### 5.1 Grant 是短期能力，不是 Session

Delivery Grant 必须：

- 短 TTL；
- 最小必要 Method；
- 最小必要资源范围；
- 不携带 Storage Credential；
- 不进入普通访问日志全文；
- 不持久化完整签名 URL。

### 5.2 Revocation Contract

系统必须显式声明每个 Delivery Provider 的 Grant 撤销等级：

```text
IMMEDIATE
KEY_VERSION_BOUND
TTL_BOUNDED
NOT_REVOCABLE_BEFORE_EXPIRY
```

语义：

- `IMMEDIATE`：可以按 grant/token ID 立即撤销；
- `KEY_VERSION_BOUND`：轮换或禁用签名 Key 后旧 Grant 失效；
- `TTL_BOUNDED`：无法单条撤销，但最长只存活到短 TTL；
- `NOT_REVOCABLE_BEFORE_EXPIRY`：只允许用于明确接受该风险的场景。

### 5.3 Permission Revocation

当 Resource ACL / Share / User Session 被撤销时：

1. 新 Grant 必须立即拒绝签发；
2. 若 Provider 支持立即撤销，应撤销当前 Active Grant；
3. 否则安全边界等于 Grant 剩余 TTL；
4. 高敏感数据不得使用无法满足自身安全策略的 Delivery Provider。

### 5.4 Key Rotation

Delivery Provider 签名配置必须支持 `key_version`。

轮换要求：

- 新签名使用新版本；
- 可配置短暂双 Key 验证窗口；
- Emergency Rotation 可以直接废弃旧 Key；
- Secret 只以 `secret://` Reference 保存。

---

## 6. Playback / Delivery Lease

### 6.1 目的

为了避免用户正在播放时被自动分层动作破坏，引入短期 `Delivery Lease`。

Lease 不是永久 Lock，也不是客户端播放历史。

### 6.2 Lease 生命周期

```text
issue playback Delivery Grant
        ↓
create / renew Delivery Lease
        ↓
client heartbeat / grant renewal
        ↓
lease_expires_at sliding forward
        ↓
playback ended / timeout
        ↓
lease released / expired
```

### 6.3 Lease 保护范围

只要某 Blob / Placement 存在有效 Playback Lease：

- 不允许自动 Demote 到不可即时读取 Tier；
- 不允许删除唯一可读 Placement；
- 不允许回收当前唯一的临时 Restore Copy；
- Provider Drain 必须等待或迁移可读来源；
- GC 必须视其为临时保护引用。

### 6.4 Lease 过期

客户端崩溃时不能留下永久 Hold。

因此：

- Lease 必须有有限 TTL；
- Heartbeat 丢失后自动过期；
- 不需要每个视频 Range 请求续租；
- 推荐随 Delivery Grant 续签、播放状态心跳或固定低频心跳续租。

---

## 7. Restore Budget 与 Cost Guard

### 7.1 为什么是强约束

“恢复本季”和自动预热可能一次触发几十 GB、数百 GB，甚至更大范围的归档恢复。

因此预算必须在服务端执行，不能只做 UI 提示。

### 7.2 实例级预算

建议支持：

```text
restore_budget:
  max_bytes_per_request
  max_items_per_request
  max_concurrent_operations
  max_concurrent_bytes
  daily_requested_bytes
  daily_provider_restore_bytes
  auto_prefetch_max_bytes_per_trigger
  auto_prefetch_max_items_per_trigger
```

### 7.3 Policy 层级

预算可以按：

```text
Instance
  ↓ override
Storage Provider
  ↓ override
Storage Policy / Media Type
```

用户级配额属于后续能力，不要求 P0 必须实现。

### 7.4 超预算行为

明确区分：

- `REJECT`：硬拒绝；
- `REQUIRE_CONFIRMATION`：需要高风险确认；
- `QUEUE_AFTER_BUDGET_RESET`：允许排队到预算窗口恢复；
- `PARTIAL_ACCEPT`：只接收预算内项目。

自动预热默认不得使用 `REQUIRE_CONFIRMATION`，超过预算应停止预热而不是打扰用户。

### 7.5 预算统计

预算使用逻辑请求字节与实际 Provider Restore Bytes 分开统计。

多个 Request 共享一个 Operation 时：

> Provider Restore Bytes 只能计一次。

否则会因为业务引用数量重复扣减预算。

---

## 8. Delivery Fallback

### 8.1 Fallback 是策略，不是硬编码链

允许：

```text
CDN -> DIRECT -> SERVER_PROXY
CDN -> DIRECT -> FAIL
DIRECT -> CDN -> FAIL
SERVER_PROXY only
```

### 8.2 低带宽 Profile

`LOW_BANDWIDTH_OBJECT_STORAGE_CDN` 推荐默认：

```text
CDN
  ↓ unavailable
DIRECT
  ↓ unavailable
FAIL
```

明确禁止默认：

```text
CDN -> SERVER_PROXY
```

否则 CDN 故障可能瞬间把高码率视频流量灌入低带宽 Ikaros Server。

### 8.3 Fallback 条件

Fallback 只针对可分类故障：

- Delivery Provider Unhealthy；
- Origin Reachability；
- Token Provider Failure；
- Region / Network Policy；
- Capability mismatch。

以下错误不得 fallback 绕过：

- 401 / 403 Permission Denied；
- Data Classification 禁止；
- Resource / Attachment 不存在；
- Blob Corrupted；
- explicit deny policy。

### 8.4 Loop Protection

每次 Delivery Resolution 必须携带已尝试 Provider 集合，禁止 CDN ↔ DIRECT ↔ CDN 无限循环。

---

## 9. Delivery Provider 与 Storage Provider 的正式关系

Delivery Provider 不应塞进 `storage.storage_provider`。

正式模型：

```text
StorageProvider
      │
      │ origin
      ▼
DeliveryBinding
      │
      ▼
DeliveryProvider
```

一个 Storage Provider 可以绑定：

- 0 个 Delivery Provider：仅 DIRECT / Server controlled；
- 1 个 CDN；
- 多个 CDN / Edge Provider；
- 不同优先级或区域策略的 Delivery Provider。

`DeliveryBinding` 负责：

- Origin 类型与授权模式；
- Priority；
- Enabled；
- Cache Key Policy；
- Range Policy；
- fallback participation。

---

## 10. P0 边界

本补充将以下能力正式列为 P0 Engineering Contract：

- Delivery Provider / Delivery Binding；
- Get Media Availability；
- Issue Delivery Grant；
- Delivery Grant TTL / Revocation Capability；
- Request Restore；
- Restore Request / Item / Operation；
- Restore Operation 去重；
- Delivery Lease；
- Restore Budget 基础配置与强制检查；
- Delivery Fallback Policy；
- Cache Identity 与 Authorization 解耦；
- 对应 Permission / Event / OpenAPI / Acceptance Test。

自动 Heat Score、复杂 Working Set 自动淘汰算法仍可保留为 P1，但 P0 必须保证：

- 当前 Playback Lease 不被 Demote；
- 自动预热受硬预算保护；
- 手动 Restore 有明确状态和幂等。

---

## 11. 实现不变量

1. 一个业务 Restore Request 可以引用多个 Restore Operation。
2. 多个 Restore Request 可以共享同一个 Restore Operation。
3. 同一 Placement 不得存在语义等价的并发 Active Restore Operation。
4. Background Task Retry 不创建新的业务 Restore Request。
5. Delivery Token 变化不得默认改变不可变媒体的 Cache Identity。
6. Cache 命中不能绕过授权验证。
7. Active Playback Lease 存在时，不得自动使媒体变为不可读取。
8. 自动预热不得越过 Restore Budget。
9. Permission Denied 不得通过 fallback 获得另一条数据路径。
10. 低带宽 Profile 不得静默 fallback 到 Server Proxy。
11. 完整 Signed URL / Token / Secret 不得进入 Durable Event、普通日志或 Analytics。
12. CDN Cache 永远不是 Durable Blob Replica。
