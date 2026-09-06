# Ikaros V2 Media Delivery / CDN / Archive Restore 设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Media Delivery / CDN / Archive Restore Design |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.2 |
| 编写日期 | 2026-09-06 |
| 状态 | Implementation-aligned Draft |
| 系统级上位约束 | `System-Overview-Design.md` |
| Storage 上位约束 | `Attachment-Blob-Storage-Subsystem-Design.md` |
| Media 上位约束 | `Media-Video-Anime-Playback-Subsystem-Design.md` |

> 本文档补充 Ikaros V2 在“大容量媒体库、对象存储、低带宽自托管 Server、CDN 交付、低频与归档存储”场景下的专项设计。
>
> **工程实施说明（2026-09-06）**：Delivery Provider / Delivery Binding 已进入实现阶段。涉及 Provider 类型、Binding 字段、选择顺序、健康过滤、预览 Provider 选择和 URL 生成的描述，以当前 `run.ikaros.storage` 实现与 `src/main/resources/db/migration/` 为工程事实基线；设计中尚未实现的能力必须明确标记为后续演进，不得反向要求当前代码保留已删除字段或伪造交付 URL。

---

## 1. 背景与目标

典型自托管实例可能具有以下特征：

- Ikaros Server 部署在家庭网络、NAS 或低带宽云服务器；
- 视频主体位于对象存储；
- 媒体库容量远大于月访问量；
- 客户端需要稳定的 HTTP Range 播放能力；
- Server 上行带宽不足以承担视频 Proxy 或 Server Cache 回源；
- 用户可以接受长期未观看内容进入归档层，并在再次观看前等待 Restore；
- CDN 可以承担面向客户端的大流量数据面。

设计目标：

1. **控制面与媒体数据面分离**：Ikaros Server 负责身份、授权、策略、状态和短期授权，不默认承载视频字节。
2. **Delivery 与 Storage 分离**：Storage Provider 负责持久化字节；Delivery Provider 负责选择“如何交付”。
3. **低带宽部署可用**：Server Proxy 是一种 Provider 类型，不是所有读取的必经路径。
4. **归档可感知**：Archive Restore 是可观察的媒体可用性状态。
5. **成本可演进**：允许 HOT / WARM / COLD / ARCHIVE 迁移而不改变 Attachment 身份。
6. **厂商无关**：核心模型表达 Endpoint / Capability / Policy，不把云厂商产品名写成领域常量。

---

## 2. 控制面与数据面

### 2.1 默认媒体读取链路

```text
Client
  │ request playback / preview / download authorization
  ▼
Ikaros Server
  │ permission + availability + delivery resolution
  │ issue short-lived grant / lease
  ▼
Client
  │ HTTP / Range traffic
  ▼
Selected Delivery Path
  │
  ├─ DIRECT       -> Storage Provider signed/read URL
  ├─ CDN          -> configured CDN endpoint + Storage object path/signing
  └─ SERVER_PROXY -> Ikaros attachment content endpoint
```

统一原则：

- Ikaros Server 不应成为大文件数据面的默认中转节点；
- Storage Provider 仍然拥有 Blob Placement 真相；
- Delivery Provider 不拥有 Blob；
- CDN 缓存不计入 Durable Blob Replica；
- 客户端不得得到长期 Storage Credential；
- URL 是一次交付合同的结果，不是 Attachment 的永久属性。

### 2.2 Server Proxy 的定位

`SERVER_PROXY` 用于客户端无法直接访问 Storage、需要 Server 侧处理或管理员明确选择代理路径的场景。当前实现会生成：

```text
/api/attachments/{attachmentId}/content?delivery_grant={token}
```

若 Delivery Provider 的 `config.endpoint` 是有效的绝对 HTTP(S) Endpoint，则以该 Endpoint 作为上述路径的外部基址；未配置时保留相对地址。

### 2.3 Server Cache 的定位

Server Cache 是可选加速层，不是 Delivery Provider 的前置依赖。大容量 Server Cache 不得成为 CDN / DIRECT 的必要路径。

---

## 3. Delivery Provider 与 Binding 当前工程模型

### 3.1 Delivery Provider 定位

`Delivery Provider` 表达：

> 已经通过 Storage Availability Resolution 的 Blob，采用哪一种交付策略向客户端提供读取合同。

当前持久化实体为 `media_delivery_provider`，核心属性包括：

```text
provider_key
provider_type
 display_name
credential_ref
config
capabilities
grant_revocation_mode
signing_key_version
health_status
enabled
idempotency_key
version / timestamps
```

其中 Provider 自己拥有 **类型、Endpoint/配置、凭据引用、Capabilities、撤销/签名版本、健康状态和启停状态**。

### 3.2 当前 Provider 类型

```text
DeliveryProviderType
├── DIRECT
├── CDN
└── SERVER_PROXY
```

当前语义：

- `DIRECT`：由绑定的 Storage Provider 生成读取意图；若 Storage Provider 返回签名 URL，保留其真实签名 Host，不由 Delivery 层重写成虚构域名。
- `CDN`：Delivery Provider 必须通过 `config.endpoint` 提供有效 CDN 对外 Endpoint；Storage Object Provider 使用真实 object key 与该 Endpoint 生成读取合同。
- `SERVER_PROXY`：生成 Ikaros Attachment Content URL，可由 Provider `config.endpoint` 提供外部 Server 基址。

**禁止**根据 Binding 或 Provider Key 猜测 CDN 域名、Bucket 域名或厂商 URL。

### 3.3 Delivery Binding 职责

当前 `media_delivery_binding` 只描述 Storage Provider 与 Delivery Provider 的路由关系。工程字段为：

```text
storage_provider_id
delivery_provider_key
priority
enabled
cache_key_policy
range_policy
fallback_participation
version / timestamps
```

约束与语义：

- `delivery_provider_key` 通过稳定 Provider Key 关联 Delivery Provider；当前不是 `delivery_provider_id` 字段。
- 同一 `storage_provider_id + delivery_provider_key` 不能重复绑定。
- `priority` 越小优先级越高。
- `cache_key_policy` 当前值：`CONTENT_IDENTITY / FULL_REQUEST / NO_CACHE`。
- `range_policy` 当前值：`PASSTHROUGH / FIXED_CHUNK / UNSUPPORTED`。
- `fallback_participation` 决定该 Binding 是否参与后续 Placement 的 fallback 选择。

历史设计中的 `origin_type`、`auth_mode` 已从 Binding migration 中删除；Provider 类型和交付策略由 Delivery Provider 自己负责。没有新的 ADR / 设计变更时不得重新把这两个字段加回 Binding。

### 3.4 Provider 与 Binding 的边界

| 能力 | Delivery Provider | Delivery Binding |
|---|---:|---:|
| `DIRECT / CDN / SERVER_PROXY` 类型 | ✅ | ❌ |
| Endpoint / CDN 外部域名 | ✅ `config` | ❌ |
| Credential Reference | ✅ | ❌ |
| Health / Enabled | ✅ | Binding 仅有自己的 Enabled |
| Storage 来源 | ❌ | ✅ `storage_provider_id` |
| Provider 关联 | Provider 自身 `provider_key` | ✅ `delivery_provider_key` |
| 路由优先级 | ❌ | ✅ |
| Range Policy | Provider 可声明 capability | ✅ 路由合同的实际 policy |
| Cache Key Policy | Provider 可声明 capability | ✅ 路由合同的实际 policy |
| Fallback Participation | ❌ | ✅ |
| URL 生成策略 | ✅ 由 Provider Type 驱动 | ❌ 不自行生成域名 |

### 3.5 可用候选过滤

附件预览与 Delivery Lease 选择都必须先得到可读 Placement，并排除：

- Storage Provider 为 `DISABLED` 或 `FAILED`；
- Binding 为 disabled；
- Delivery Provider 为 disabled；
- Delivery Provider `health_status = UNHEALTHY`。

`UNKNOWN` 表示尚未得到有效探测结论，不等同于 `HEALTHY`，但当前实现不会像 `UNHEALTHY` 一样直接排除。

### 3.6 默认选择、显式选择与 Fallback

附件预览接口：

```text
GET /api/attachments/{attachmentId}/preview-url?delivery_provider={providerKey}
```

当前行为：

- 未传、空值或指定 Key 不在可用候选中时，按候选 Binding 的最小 `priority` 选择默认 Provider；
- 指定的 Provider Key 存在于可用候选时，优先选择该 Provider；
- 响应可以返回所有可选 Provider，但只为当前 selected Provider 生成 URL；
- 可选 Provider 信息本身不生成访问 URL。

Delivery Lease 的持久选择会记录 `binding_id`、`selection_epoch`、`selected_at`、`selection_reason`、`fallback_index` 与健康快照信息。非首个 readable Placement 的 Binding 只有在 `fallback_participation = true` 时才参与 Lease fallback。

### 3.7 URL 生成合同

当前 URL 生成必须遵循：

**DIRECT**

```text
Binding.storage_provider_id
 -> StorageProviderRegistry
 -> readable BlobPlacement
 -> StorageObjectProvider.createReadIntent(objectKey)
 -> use returned URL / method / expiry
```

Delivery Provider 不改写 Storage Provider 已签名 URL 的 Host。

**CDN**

```text
DeliveryProvider.config.endpoint
 + Binding.storage_provider_id
 + BlobPlacement.object_key
 -> StorageObjectProvider.createReadIntent(objectKey, cdnEndpoint)
```

`config.endpoint` 缺失或不是有效绝对 URI 时合同失败；不得退回“拼一个看起来像 CDN 的 URL”。

**SERVER_PROXY**

```text
provider endpoint (optional)
 + /api/attachments/{id}/content?delivery_grant={token}
```

### 3.8 Health Probe 与生命周期

Delivery Provider 支持创建、启停、删除、健康探测等生命周期管理。探测按 Provider Type 执行；创建或手动探测可通过 Background Task 异步执行并把结果回写 `health_status`。

Provider 被 Binding 引用时，其删除/禁用必须遵循引用安全规则；控制台不能通过直接删除配置制造悬空 Binding。

### 3.9 Capability

Delivery Provider 的 `capabilities` 是声明/探测得到的能力描述，不是授权真相。设计允许逐步表达：

```text
signed_url
range
private_origin
cache
purge
origin_auth
origin_shield
custom_domain
https
max_ttl
```

业务代码不得仅凭 Provider Type 假定所有能力均存在。

### 3.10 Delivery Grant

短期 Delivery Grant / Contract 应至少能关联：

```text
grant_id
attachment_id
lease_id
delivery_provider_id
method
url
expires_at
range_supported
content_type
content_length
revocation_level
```

约束：

- Grant 必须在权限通过后生成；
- URL 短时有效；
- 不暴露 Storage Credential；
- 不把签名 URL 保存为 Attachment 永久地址；
- 生成合同失败时不得留下错误的长期 Active Lease；
- Range 能力必须与 Binding `range_policy` 一致。

---

## 4. CDN 作为一等交付能力

### 4.1 CDN 不等于 Storage Cache

CDN Cache：

- 属于 Delivery 层临时副本；
- 可以淘汰和重建；
- 不参与 Blob Replica Durable Count；
- 不改变 Placement Tier；
- 不作为完整性真相源。

### 4.2 Range

视频交付必须 Range-aware：

- Client → Delivery 支持 Range 时才能对外声明 Range；
- Binding `range_policy = UNSUPPORTED` 时不得宣称支持；
- Storage Provider / CDN 实际能力不足时应失败或降级，而不是返回虚假能力。

### 4.3 Private Origin

推荐拓扑仍为：

```text
Private Object Storage
        ↑
 authenticated origin
        ↑
      CDN
        ↑
 short-lived client contract
        ↑
      Client
```

当前工程已经具备 CDN Endpoint + Storage Object Path 的基础交付合同；更复杂的 Origin Auth、Purge、Origin Shield 等属于能力扩展，不应通过重新增加 Binding `auth_mode` 来实现。

---

## 5. Archive Restore 作为媒体可用性状态

Storage 继续拥有 Placement / Restore 真相；Media 只消费稳定 Availability View。推荐状态：

```text
READY
RESTORE_REQUIRED
RESTORING
READY_TEMPORARILY
UNAVAILABLE
MISSING
CORRUPTED
```

其中 `READY_TEMPORARILY` 必须同时满足对应 Restore Operation 仍处于可用状态且 `restore_expires_at` 未过期，才能参与 Delivery Lease 选择。

业务侧可按 `ATTACHMENT / EPISODE / SEASON / RESOURCE_SET` 聚合 Restore 意图，Storage 最终按 Blob / Placement 执行。Restore 外部调用应由 Background Task 承担，不在数据库长事务中调用 Provider API。

---

## 6. Storage Heat、Tier 与 Working Set

Heat Score、Promotion / Demotion、Working Set Budget 仍是后续策略层设计：

```text
ARCHIVE
  ↓ restore
READY_TEMPORARILY
  ↓ sustained access / explicit promote
WARM / HOT
  ↓ long inactivity
COLD / ARCHIVE
```

当前播放、Active Delivery Lease、Manual Hold 等必须在不可逆清理或 Demotion 前重新检查，避免正在交付的 Blob 被错误迁移/清理。

---

## 7. 生命周期与成本安全

Storage Provider 可以逐步声明：

```text
minimum_storage_duration
restore_modes
restore_latency_range
restore_temporary_copy
restore_copy_ttl_range
retrieval_billing_model
```

这些字段用于策略和风险提示，不把云厂商公开价格表硬编码为领域常量。Promotion / Demotion 应支持 minimum residency、cooldown、manual hold、active restore protection 与 current playback protection。

---

## 8. Provider Contract Test

Delivery Provider / Binding 至少覆盖：

- Provider Key 唯一；
- 相同 Storage Provider 不可重复绑定同一 `delivery_provider_key`；
- disabled / `UNHEALTHY` Provider 不参与选择；
- Binding priority 默认选择；
- 显式 Provider 选择；
- fallback participation；
- DIRECT 保留 Storage Provider 签名 URL Host；
- CDN 缺少有效 Endpoint 时失败；
- CDN URL 基于配置 Endpoint 与真实 object path，不凭空生成；
- SERVER_PROXY 相对/绝对 Endpoint 行为；
- Range Policy 与响应合同一致；
- Secret / Credential 不进入普通响应或日志。

---

## 9. 事件与可观测性

建议稳定观察：

```text
storage.delivery-lease.created
storage.delivery-lease.released
storage.restore.requested
storage.restore.started
storage.restore.succeeded
storage.restore.failed
delivery.provider.degraded
```

高频媒体请求不得逐请求制造无界 Durable Event。Analytics 可以聚合 CDN Delivered Bytes、Origin Bytes、Cache Hit Ratio、Restore Bytes、Tier Physical Bytes 和 Server Proxy Bytes。

---

## 10. 推荐部署 Profile

低带宽 Server + 对象存储 + CDN 可采用：

```text
media_delivery:
  preferred: CDN
  server_proxy: disabled_by_default

storage:
  durable_provider: object_storage
  archive_restore: enabled_when_supported

client:
  range_required: true
```

Profile 只是默认策略集合，不改变 Provider / Binding 数据模型。

---

## 11. 与其他 V2 文档的边界

### Attachment / Blob / Storage

继续拥有 Blob / Placement、Storage Provider、Restore 真相、Delivery Provider / Binding、Delivery Grant / Lease、Tier 与 Storage Policy。

### Media / Playback

继续拥有 Episode / Season / Playback Session、播放 UX、当前观看状态以及 Restore 业务入口。

### Background Task

继续拥有健康探测、Restore / Promotion / Demotion 等异步执行的 Attempt、Retry、Progress、Lease 与 Crash Recovery。

### Platform Administration

提供 Provider / Binding 配置入口、健康结果、审计和危险操作确认，但不绕过 Storage Application Contract 直接改表。

---

## 12. P0 / 后续演进

### 当前 P0 已有工程基线

- Delivery Provider：`DIRECT / CDN / SERVER_PROXY`；
- Delivery Provider 生命周期与健康探测；
- Storage Provider ↔ Delivery Provider Binding；
- Binding priority / Range / Cache Key / Fallback Participation；
- Attachment Preview 可选 Delivery Provider；
- DIRECT / CDN / SERVER_PROXY URL 合同；
- Delivery Grant / Lease 与 Binding 选择记录；
- Archive Restore / Temporary Ready 基础状态。

### 后续演进

- 更完整的 Private Origin Auth / Purge / Origin Shield；
- Season Restore 与顺序预热；
- Storage Heat Score；
- Working Set Budget；
- 自动 Promotion / Demotion；
- Delivery / Restore Analytics；
- 更细粒度 Provider Capability Contract Test。

---

## 13. 验收原则

至少验证：

1. 有可读 Placement、可用 Binding 和可用 Delivery Provider 时能够生成预览/读取合同。
2. 无可用 Delivery Binding 时明确返回 Storage Unavailable，而不是伪造 URL。
3. `priority` 数值更小的 Binding 默认优先。
4. 显式选择一个可用 `delivery_provider` 时使用对应 Provider；无效 Key 回落到可用默认候选。
5. disabled 或 `UNHEALTHY` Delivery Provider 不参与选择。
6. DIRECT 不改写 Storage Provider 返回的签名 Host。
7. CDN 必须依赖 Delivery Provider 的有效 `config.endpoint`，不能从 Binding 猜测域名。
8. SERVER_PROXY 未配置 Endpoint 时可以返回相对 Attachment Content URL。
9. Binding 不再拥有 `origin_type / auth_mode`；Provider Type 是交付策略来源。
10. Range Support 与 Binding `range_policy` 一致。
11. Delivery Lease 记录实际 `binding_id` 与选择信息，Active Lease 可保护正在交付的 Blob。
12. CDN Cache 不计为 Durable Blob Replica，Restore 完成也不改变 Attachment Identity。
