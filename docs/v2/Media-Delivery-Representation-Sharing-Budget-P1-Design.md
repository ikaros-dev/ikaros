# Ikaros V2 Media Delivery Representation / Sharing / Traffic Budget P1 设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Media Delivery Representation / Sharing / Traffic Budget P1 Design |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-09-02 |
| 状态 | Draft / P1 Design |
| 上位设计 | `Media-Delivery-CDN-Archive-Restore-Design.md` |
| Media 上位约束 | `Media-Video-Anime-Playback-Subsystem-Design.md` |
| Sharing 上位约束 | `Sharing-Collaboration-Room-Subsystem-Design.md` |

> 本文档补充 P1 的四类能力：Adaptive Representation 与 Storage/Delivery 的桥接、Share/Guest 到 Delivery Grant 的授权链、Delivery Traffic Budget，以及不可变 Representation 与 CDN Purge 原则。

---

## 1. Adaptive Representation 与 Storage / Delivery

### 1.1 Source 与 Representation 分离

沿用 Media 设计：

```text
Media Release
  └── Source Attachment
        ↓ Transcode / Package
     Playback Representation
        ├── Manifest
        ├── Segment / Chunk Set
        ├── Subtitle / Audio variant
        └── Derived Attachments / Blobs
```

规则：

1. Source Attachment 与 Derived Representation 必须可追溯。
2. Derived Representation 可以删除并重建，不影响 Source。
3. Source 与 Derived Representation 的 Storage Tier 独立。
4. Source 可以进入 Archive，而高频播放 Representation 保持 WARM / HOT。
5. Representation 是否物化为多个 Attachment / Blob 由封装策略决定，但不能把 Provider 内部 Object Key 当作媒体业务身份。

### 1.2 Representation Identity

必须有不可变身份：

```text
representation_id
representation_version
source_blob_identity
packaging_profile_version
codec / resolution / track selection
```

Cache Identity 必须至少包含 `representation_id + representation_version`。

### 1.3 Manifest / Segment

如果使用 HLS / DASH / CMAF：

- Manifest 是 Derived Artifact；
- Segment 不允许原地覆盖旧版本内容；
- 新 Packaging Profile 或 Transcode 输出产生新 Representation Version；
- Manifest 只能引用同一 Representation Version 的 Segment；
- 旧 Representation 可以在无 Active Delivery Lease 后 GC。

P1 不强制所有 Segment 都成为独立 Attachment；允许 Provider 内部批量物化，但必须有可重建边界、完整性验证和删除边界。

### 1.4 Archive Source + Hot Derived

允许：

```text
VCB Source MKV          -> ARCHIVE / COLD
Frequently Played 1080p -> WARM / HOT
Manifest / small assets  -> HOT
```

这样 Source 的长期保存成本与播放交付成本可以独立优化。

如果 Derived Representation 丢失且 Source 已归档：

1. Playback Source Resolution 可以选择其他现存 Variant；
2. 若必须重建，则创建 Source Restore + Transcode/Package Background Task；
3. 客户端显示 `PREPARING` / `RESTORING_SOURCE` 等可解释状态，而不是普通 500。

---

## 2. Share / Guest → Delivery Grant

### 2.1 授权链

公开分享媒体必须遵循：

```text
Share Token / Invite
        ↓ validate / exchange
Guest or Share Principal
        ↓ effective capability resolution
Media / Attachment authorization
        ↓
Delivery Grant
        ↓
CDN / DIRECT
```

禁止：

```text
Share URL -> permanent CDN URL
```

Share 只提供进入授权上下文的方式，不直接成为长期媒体地址。

### 2.2 Capability 映射

必须区分：

```text
read / stream
download
join_room
control_playback
```

`read` 或 `stream` 不自动等于 `download`。

Delivery Grant 的 Method / Content-Disposition / Range 能力必须遵守 Share Effective Capability。

### 2.3 TTL 收敛

Guest / Share Delivery Grant 必须满足：

```text
grant.expires_at <= share.expires_at
grant.expires_at <= guest_session.expires_at
```

如果 Share 被撤销：

- 新 Grant 立即拒绝；
- Active Grant 按 Delivery Provider Revocation Contract 收敛；
- Active Room Member / Guest connection 同样进入权限收敛流程。

### 2.4 Watch Party

Room 一起看不拥有媒体访问权。

每个参与者仍需要独立解析：

```text
Room Membership
+ Target Resource ACL / Share Capability
= Effective Media Access
```

主持人能控制播放位置不代表其他成员自动获得视频字节读取权限。

---

## 3. Delivery Traffic Budget

### 3.1 与 Restore Budget 分离

Restore Budget 控制“从归档层取回多少数据”；Delivery Traffic Budget 控制“向客户端和 Origin 传输多少数据”。

建议指标：

```text
cdn_delivered_bytes
direct_delivered_bytes
origin_bytes
server_proxy_bytes
cross_region_origin_bytes
```

### 3.2 Budget 类型

建议：

```text
delivery_budget:
  daily_delivered_bytes_soft
  monthly_delivered_bytes_soft
  origin_bytes_daily_soft
  origin_bytes_monthly_soft
  direct_egress_bytes_soft
  server_proxy_bytes_hard_limit
  server_proxy_concurrent_streams_hard_limit
```

原则：

- CDN Delivered Bytes 主要使用 Soft Budget / Alert；
- Origin Bytes 主要使用 Soft Budget / Alert；
- Server Proxy 在 LOW_BANDWIDTH Profile 下可以有 Hard Limit；
- 不默认因为月 CDN 流量预算超限而中断正在播放的视频；
- 自动化 / Prefetch / 非必要后台任务可以在 Soft Budget 超限后降级。

### 3.3 预算窗口

Budget Window 必须明确：

- timezone；
- daily reset；
- monthly reset；
- Provider billing unit conversion；
- projected usage 是否仅供提示。

账单预测不是 Cloud Provider Invoice Truth。

### 3.4 Server Proxy Hard Guard

低带宽实例建议：

```text
server_proxy_bytes_hard_limit
server_proxy_bandwidth_limit
server_proxy_concurrent_streams_limit
```

如果 Hard Limit 达到：

- 不允许新的隐式 media proxy fallback；
- 已允许的管理下载 / 特殊任务按独立策略处理；
- 返回可解释错误而不是把 Server 打满。

---

## 4. Immutable Representation / CDN Purge Contract

### 4.1 正常一致性不依赖 Purge

核心原则：

> CDN Purge 是运维逃生能力，不是正常内容更新协议。

禁止：

```text
/media/foo/segment001
   ↓ overwrite bytes
purge CDN
```

推荐：

```text
/media/foo/representation-v1/segment001
/media/foo/representation-v2/segment001
```

或者使用不可变内容身份路径。

### 4.2 为什么

不可变 URL 可以简化：

- CDN cache correctness；
- ETag；
- HTTP Range；
- Browser / Client cache；
- 多 CDN；
- 回滚；
- 并发播放；
- Eventual consistency。

### 4.3 Purge 允许场景

Purge 主要用于：

- 安全泄露；
- 错误发布；
- 法律 / 删除要求；
- Provider cache corruption；
- Emergency policy change；
- 管理员显式操作。

Purge 必须审计。

### 4.4 Representation GC

新 Representation 发布后：

```text
v1 ACTIVE
v2 PREPARING
   ↓ validate
v2 ACTIVE
v1 RETIRING
   ↓ no active lease / retention satisfied
v1 GC eligible
```

不能在仍有 Active Delivery Lease 时删除旧 Representation 唯一字节。

---

## 5. 多 CDN / 多区域扩展

虽然不作为 P1 强制实现，但模型必须兼容：

```text
Storage Provider A
   ├── Delivery Binding -> CDN-A
   ├── Delivery Binding -> CDN-B
   └── Delivery Binding -> DIRECT
```

以及：

```text
Storage Provider CN
Storage Provider Global
```

Delivery Policy 可以按客户端网络区域选择首选 Binding，但不能改变 Blob / Attachment 身份。

跨区域复制仍属于 Storage Placement，不属于 Delivery Cache。

---

## 6. P1 Acceptance 建议

至少验证：

1. Source 进入 Archive 后，现存 Derived Representation 仍可以播放。
2. Derived Representation GC 不影响 Source。
3. Representation Version 变化后 CDN Cache Identity 不冲突。
4. Share Guest 只能获得 Share Capability 允许的 Delivery Grant。
5. `stream` Share 不允许转换为普通下载授权。
6. Share 到期时间早于默认 Grant TTL 时，Grant TTL 被截断。
7. Share 撤销后拒绝新 Grant。
8. Delivery Traffic Soft Budget 只告警，不默认中断活跃播放。
9. Server Proxy Hard Limit 能阻止隐式大流量回退。
10. 正常 Representation 更新不依赖 CDN Purge。
11. Purge 操作被审计且不会暴露完整 Signed URL。
