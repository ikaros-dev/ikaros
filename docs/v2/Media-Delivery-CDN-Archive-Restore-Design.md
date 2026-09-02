# Ikaros V2 Media Delivery / CDN / Archive Restore 设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Media Delivery / CDN / Archive Restore Design |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-09-02 |
| 状态 | 草案（Draft） |
| 系统级上位约束 | `System-Overview-Design.md` |
| Storage 上位约束 | `Attachment-Blob-Storage-Subsystem-Design.md` |
| Media 上位约束 | `Media-Video-Anime-Playback-Subsystem-Design.md` |

> 本文档补充 Ikaros V2 在“大容量媒体库、对象存储、低带宽自托管 Server、CDN 交付、低频与归档存储”场景下的专项设计。
>
> 本文档不改变 Attachment / Blob / Placement 的身份模型，不把 CDN 变成持久化 Storage Provider，也不把播放业务状态下沉为云厂商专有字段。若与上位设计冲突，以上位设计为准。

---

## 1. 背景与目标

典型自托管实例可能具有以下特征：

- Ikaros Server 部署在家庭网络、NAS 或中国大陆低带宽云服务器；
- 视频主体位于对象存储；
- 媒体库容量远大于月访问量，例如 10 TB 级媒体库；
- 客户端需要稳定的 HTTP Range 播放能力；
- Server 上行带宽不足以承担视频 Proxy 或 Server Cache 回源；
- 用户可以接受长期未观看内容进入归档层，并在再次观看前等待 Restore；
- CDN 可以承担面向客户端的大流量数据面。

因此设计目标是：

1. **控制面与媒体数据面分离**：Ikaros Server 负责身份、授权、策略、状态和签名，不默认承载视频字节。
2. **Delivery 与 Storage 分离**：Storage Provider 负责持久化字节；Delivery Provider 负责把可读取字节高效、安全地交付给客户端。
3. **低带宽部署可用**：Server Cache / Server Proxy 必须是能力选项，而不是媒体播放的必要路径。
4. **归档可感知**：Archive Restore 不只是后台运维动作，媒体业务与客户端必须能感知“已归档、恢复中、已就绪”。
5. **成本可演进**：允许根据访问热度在 HOT / WARM / COLD / ARCHIVE 之间迁移，而不改变 Attachment 身份。
6. **厂商无关**：设计表达能力和状态，不绑定阿里云 OSS、腾讯 COS 或其他具体厂商的产品名称与计费模型。

---

## 2. 控制面与数据面

### 2.1 默认媒体读取链路

在低带宽自托管场景，推荐链路为：

```text
Client
  │
  │ 1. request playback / download authorization
  ▼
Ikaros Server
  │
  │ permission + policy + availability resolution
  │ issue short-lived delivery grant
  ▼
Client
  │
  │ 2. HTTP Range media traffic
  ▼
Delivery Provider / CDN
  │
  │ cache miss / origin fetch
  ▼
Storage Provider / Object Storage
```

统一原则：

- Ikaros Server 不应成为大文件媒体数据面的默认中转节点；
- Server 不因“可以 Proxy”就默认 Proxy；
- CDN / Delivery 可以直接从私有对象存储回源；
- 客户端拿到的是短期 Delivery Grant，而不是 Storage Credential；
- 对象存储仍然是 Blob Placement 的持久化真相；
- CDN 缓存对象不是 Blob Replica，也不进入持久化 Replica 计数。

### 2.2 Server Proxy 的定位

Server Proxy 仅作为以下场景的兼容或受控路径：

- Storage / Delivery Provider 无法被客户端访问；
- 必须进行 Server 侧内容处理；
- 特定安全等级禁止外部 Delivery；
- Provider 不支持所需 Range / Auth 能力；
- 管理员显式选择 Server Proxy 部署模式。

对于低带宽 Server，媒体 Proxy 应允许全局关闭或按策略禁用。

### 2.3 Server Cache 的定位

Server Cache 仍保留在 Storage 设计中，但部署策略必须允许：

```text
server_media_cache = OFF
```

或只保留极小的：

- 封面 / 缩略图；
- 字幕；
- Manifest；
- 小型派生资源；
- Metadata Cache。

禁止把“大容量 Server Cache”当作 CDN 的前置依赖。

---

## 3. Delivery Provider

### 3.1 定位

新增逻辑概念 `Delivery Provider`：

> Delivery Provider 表达“已经通过 Storage Availability Resolution 的 Blob，如何安全、高效地交付给客户端”。

Delivery Provider 不拥有 Blob，不负责持久化数据真相，不替代 Storage Provider。

### 3.2 推荐类型

```text
DeliveryProviderType
├── DIRECT
├── CDN
└── SERVER_PROXY
```

语义：

- `DIRECT`：客户端通过 Storage Provider 的临时授权直接读取；
- `CDN`：客户端通过 CDN / Edge Delivery 读取，CDN 回源 Storage Provider；
- `SERVER_PROXY`：字节经过 Ikaros Server 或专用 Delivery Worker。

### 3.3 Capability

Delivery Provider 至少需要声明：

```text
DeliveryCapabilities
├── signed_url
├── range
├── private_origin
├── cache
├── purge
├── origin_auth
├── origin_shield
├── custom_domain
├── https
└── max_ttl
```

Capability 必须由 Provider Contract 探测 / 校验，不能由业务代码假定。

### 3.4 Delivery Grant

短期交付授权建议表达：

```text
DeliveryGrant
├── attachment_id
├── blob_id
├── delivery_provider_id
├── url
├── method
├── expires_at
├── range_supported
├── content_type
├── content_length
├── etag / checksum?
└── trace_id
```

约束：

- Grant 必须在 Permission 通过后生成；
- URL 必须短时有效；
- 不得暴露 Storage Credential；
- 不得把 URL 保存为 Attachment 的永久地址；
- CDN 场景优先使用 CDN 自身 URL 鉴权 / Token，而不是把 Storage Presigned URL 当成最终播放 URL；
- 私有 Origin 的访问授权由 Delivery Provider 与 Storage Provider 的集成完成。

---

## 4. CDN 作为一等交付能力

### 4.1 CDN 不等于 Storage Cache

CDN Cache：

- 是 Delivery 层的临时副本；
- 可以随时失效、淘汰、重建；
- 不参与 Blob Replica Durable Count；
- 不改变 Placement Tier；
- 不作为完整性真相源。

### 4.2 Range

视频交付必须保证 Range-aware：

- Client → CDN 支持 Range；
- CDN → Origin 支持 Range 或可安全降级；
- 不得向播放器声明虚假的 Range 能力；
- 对按请求范围计费的归档直读能力，必须避免无限制的超大 Range 预取。

推荐 Delivery Provider 支持可配置：

```text
range_prefetch_window
origin_range_window
max_single_range
```

具体默认值由 Provider / 客户端实现根据媒体和厂商能力决定，设计层不固定 8 MB、16 MB 等厂商无关参数。

### 4.3 私有回源

推荐拓扑：

```text
Private Object Storage
        ↑
  authenticated origin
        ↑
      CDN
        ↑
 CDN short-lived grant
        ↑
      Client
```

不推荐把公开 Bucket 作为简化 CDN 接入的默认方案。

---

## 5. Archive Restore 作为媒体可用性状态

### 5.1 Storage 状态与媒体状态分离

Storage 继续拥有真实 Placement / Restore 状态；Media 只消费稳定的 Availability View。

推荐媒体侧暴露：

```text
MediaAvailability
├── READY
├── RESTORE_REQUIRED
├── RESTORING
├── READY_TEMPORARILY
├── UNAVAILABLE
├── MISSING
└── CORRUPTED
```

其中：

- `READY`：存在可立即读取的 Placement；
- `RESTORE_REQUIRED`：仅有需要 Restore 的 Placement；
- `RESTORING`：Restore Background Task 正在执行；
- `READY_TEMPORARILY`：归档对象已产生具有过期时间的可读临时副本；
- `UNAVAILABLE`：Provider 暂不可用或策略禁止读取；
- `MISSING` / `CORRUPTED`：沿用 Storage 的严重故障语义。

媒体子系统不得直接修改 Placement 状态。

### 5.2 Restore Request

业务侧可以请求：

```text
RestoreScope
├── ATTACHMENT
├── EPISODE
├── SEASON
└── RESOURCE_SET
```

Storage 最终仍按 Blob / Placement 执行 Restore；Scope 只是业务聚合请求。

Restore Request 应返回或关联 Background Task ID，并允许读取：

- 当前状态；
- 已完成数量 / 总数量；
- Provider 可提供时的预计可用时间范围；
- 临时恢复副本过期时间；
- 部分失败列表。

不得承诺云厂商无法保证的精确完成时刻。

### 5.3 恢复本季与顺序预热

对于剧集媒体，推荐支持两类策略：

**显式恢复本季**

用户可以主动恢复整个 Season / Resource Set。

**顺序预热**

用户恢复或播放 Episode N 后，可以按配置后台恢复：

```text
N + 1
N + 2
N + 3
...
```

约束：

- 预热数量必须受策略和成本预算限制；
- 不得因为播放一集无上限恢复整个媒体库；
- 对大 Season 可分批创建子任务；
- 用户显式“恢复本季”的意图优先于自动预热策略。

---

## 6. Storage Heat 与 Tier Promotion

### 6.1 不使用单一 last_accessed_at 决策

大文件 Range 请求不得逐请求热写数据库；同时，仅凭单一 `last_accessed_at` 也不足以表达业务热度。

推荐派生 `Storage Heat Score`，输入可包括：

```text
recent_playback
play_count_window
currently_watching
favorite / pinned
recently_added
restore_frequency
manual_keep_hot
collection_context
```

Heat Score 是派生值，不是 Resource 或 Blob 的核心真相。

### 6.2 Promotion / Demotion

允许策略表达：

```text
ARCHIVE
  ↓ restore
READY_TEMPORARILY
  ↓ sustained access / explicit promote
WARM / HOT
  ↓ long-term inactivity
COLD / ARCHIVE
```

典型媒体策略可为：

```text
currently watching       -> WARM/HOT
recently accessed        -> WARM
long inactive            -> ARCHIVE
manual pinned            -> never auto-demote below configured tier
```

具体天数、容量阈值和 Tier 映射必须是实例策略，不写死在领域模型中。

### 6.3 Working Set Budget

推荐 Storage Policy 支持可选工作集预算：

```text
working_set.max_bytes
working_set.target_tier
working_set.eviction_policy
```

当热数据超过预算时，根据 Heat Score、Pinned、当前播放状态等选择 Demotion 候选。

---

## 7. 生命周期与成本安全

### 7.1 Minimum Storage Duration

不同 Provider 的低频 / 归档 Tier 可能有最短存储周期和提前删除费用。

Storage Provider Capability / Cost Metadata 可以表达：

```text
minimum_storage_duration
restore_modes
restore_latency_range
restore_temporary_copy
restore_copy_ttl_range
retrieval_billing_model
```

这些字段用于策略决策与 UI 风险提示，不把公开价格表硬编码为领域常量。

### 7.2 防止 Tier 抖动

Promotion / Demotion 必须支持：

- minimum residency；
- cooldown；
- manual hold；
- active restore protection；
- current playback protection。

禁止同一 Blob 因短期访问波动在 Hot / Archive 间频繁搬迁。

---

## 8. Provider Contract 扩展

Storage Provider 在已有 Put / Head / Read / Range / Restore 能力基础上，归档相关能力建议显式声明：

```text
ArchiveCapabilities
├── supports_restore
├── restore_modes[]
├── restore_latency_range
├── supports_direct_archive_read
├── temporary_restore_copy
├── restore_ttl_configurable
└── restore_status_query
```

Delivery Provider Contract Test 至少覆盖：

- Signed URL / Token；
- Expiration；
- Range；
- Private Origin；
- Origin Auth failure；
- CDN cache miss；
- 401 / 403 不泄露 Origin Credential；
- URL 不可作为永久 Attachment 地址使用。

---

## 9. 事件与可观测性

建议稳定事件：

```text
storage.restore.requested
storage.restore.started
storage.restore.succeeded
storage.restore.failed
storage.placement.promoted
storage.placement.demoted
delivery.grant.issued
delivery.provider.degraded
```

高频 CDN 请求不得逐请求写 Durable Event。

Analytics 可以聚合：

- CDN Delivered Bytes；
- Origin Bytes；
- Cache Hit Ratio；
- Restore Bytes；
- Restore Frequency；
- Tier Physical Bytes；
- Promotion / Demotion Bytes；
- Server Proxy Bytes。

对于低带宽部署，应能明确观察到：

> `Server Proxy Bytes ≈ 0` 是否成立。

---

## 10. 推荐部署 Profile：低带宽 Server + 对象存储 + CDN

建议提供一个非强制的部署 Profile：

```text
profile: LOW_BANDWIDTH_OBJECT_STORAGE_CDN

media_delivery:
  preferred: CDN
  server_proxy: disabled_by_default
  server_media_cache: disabled_by_default

storage:
  durable_provider: object_storage
  tiering: enabled
  archive_restore: enabled_when_supported

client:
  range_required: true
  direct_origin_url: false_by_default
```

该 Profile 只是默认配置集合，不改变系统能力模型。

---

## 11. 与现有 V2 文档的边界

### Attachment / Blob / Storage

继续拥有：

- Blob / Placement；
- Storage Provider / Tier；
- Restore 真相状态；
- Migration / Promotion / Demotion；
- Storage Policy；
- Server Cache。

本文补充 Delivery 与媒体归档使用方式，不改变其所有权。

### Media / Video / Anime Playback

继续拥有：

- Episode / Season / Playback Session；
- 播放 UI 语义；
- 当前观看状态；
- “恢复本集 / 本季”的业务入口；
- MediaAvailability 到播放器 UX 的映射。

### Background Task

继续拥有 Restore / Promotion / Demotion 的统一异步执行状态、重试、超时、取消和 Attempt。

### Platform Administration

继续拥有 Provider 配置、健康检查、策略管理、审计和危险操作确认。

---

## 12. P0 / P1 建议

### P0

- Delivery Provider 抽象；
- DIRECT / SERVER_PROXY 基础实现；
- MediaAvailability；
- Restore Request + Background Task；
- Range-aware Delivery Grant；
- Server Media Cache 可关闭。

### P1

- CDN Delivery Provider；
- Private Origin Auth；
- Season Restore；
- 顺序预热；
- Storage Heat Score；
- Working Set Budget；
- 自动 Promotion / Demotion；
- Delivery / Restore Analytics。

---

## 13. 验收原则

至少需要验证：

1. 关闭 Server Media Cache 后，远程媒体仍可通过 Delivery Provider 正常 Range 播放。
2. CDN 模式下，媒体字节不经过 Ikaros Server 数据面。
3. 私有 Object Storage 不向客户端泄露长期 Credential。
4. Archive-only Episode 返回 `RESTORE_REQUIRED`，而不是模糊 404 / 500。
5. Restore 中返回 `RESTORING` 并关联可查询任务。
6. Restore 完成后无需替换 Attachment ID 即可播放。
7. “恢复本季”可聚合多个 Blob Restore，并允许部分失败重试。
8. 自动预热受数量 / 容量预算限制。
9. 当前播放或 Pinned 媒体不会被自动 Demote。
10. CDN 缓存不会被误计为 Durable Blob Replica。
