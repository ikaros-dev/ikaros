# Ikaros V2 CMS Storage Delivery / Archive 运维交互设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Storage Delivery / Archive Operations Design |
| 适用端 | CMS Console |
| 适用版本 | Ikaros V2 |
| 编写日期 | 2026-09-02 |
| 状态 | Draft |
| 依赖设计 | `../Media-Delivery-CDN-Archive-Restore-Design.md` |

> 本文档定义管理员如何配置 Delivery Provider、低带宽媒体模式、归档策略、恢复任务、工作集预算与相关可观测性。

---

## 1. 管理页面范围

建议在 Storage 管理域增加：

```text
Storage
├── Providers
├── Delivery Providers
├── Storage Policies
├── Restore Tasks
├── Tiering / Working Set
├── Cache
└── Metrics
```

不要求这些必须是独立路由，也可以是同一 Storage 页面下的 Tab。

---

## 2. Delivery Provider 配置

配置项按 Capability 动态展示，典型字段：

```text
name
provider_type: DIRECT | CDN | SERVER_PROXY
custom_domain
https_required
signed_url_enabled
signed_url_ttl
private_origin_enabled
origin_auth_mode
range_required
cache_enabled
purge_supported
```

Secret / Credential 必须使用安全 Secret Reference，不允许在普通配置详情页完整回显。

### CDN 配置校验

保存前建议执行 Probe：

- 域名解析；
- HTTPS；
- Signed URL；
- Range；
- Private Origin；
- Origin Auth；
- 401 / 403 行为；
- 到一个测试 Blob 的读取。

Probe 失败时不得把 Provider 标记为 Healthy。

---

## 3. 低带宽媒体部署 Profile

Console 可以提供快捷 Profile：

> 低带宽 Server + 对象存储 + CDN

启用后建议默认：

```text
preferred_delivery = CDN
server_media_proxy = OFF
server_media_cache = OFF
thumbnail_cache = ON
subtitle_cache = ON
range_required = ON
private_origin = ON when supported
```

这是配置模板，不是不可修改的模式。

页面必须明确提示：

> 关闭 Server Media Proxy / Cache 后，媒体可用性依赖 Delivery Provider 与远程 Storage Provider。

---

## 4. Storage Policy / Archive Policy

管理员可以按规则配置：

```text
match:
  media_type
  resource_type
  size
  age
  heat_score
  pinned
  currently_watching

action:
  target_tier
  minimum_residency
  cooldown
  restore_policy
```

不建议直接暴露复杂脚本作为 P0 唯一配置方式；优先结构化 Policy Builder。

### 防误操作

以下规则需要阻止或高亮：

- 当前正在播放的 Blob 自动 Demote；
- Pinned Blob 自动进入不可即时读取 Tier；
- Provider 不支持 Restore 却配置到需要 Restore 的 Tier；
- 冷层最短存储周期尚未满足时频繁迁移；
- Working Set Budget 小于当前受保护热数据总量。

---

## 5. Working Set

建议展示：

```text
Target Tier
Current Bytes
Budget Bytes
Protected Bytes
Evictable Bytes
```

管理员可以配置：

- 最大工作集容量；
- 目标 Tier；
- Heat Score 阈值；
- 自动 Demotion 是否开启；
- minimum residency；
- cooldown。

页面需要允许查看“为什么这个 Blob 被保留在热层 / 为什么成为 Demotion 候选”。

---

## 6. Restore Tasks

Restore Task 列表建议字段：

```text
Task ID
Scope
Resource / Season / Episode
Provider
Blob Count
Restored / Total
State
Requested By
Created At
Estimated Range
Temporary Copy Expires At
```

详情页支持：

- 查看子任务；
- 查看失败 Blob；
- 重试失败项；
- 取消仍可取消的部分；
- 查看审计记录。

不得把云厂商 Object Key 当成主要业务标题；需要通过 Attachment / Episode / Resource 展示可理解对象。

---

## 7. 手动 Promotion / Demotion

管理员可以：

- Promote 到更热 Tier；
- Demote 到更冷 Tier；
- 设置 Keep Hot / Hold；
- 清除 Hold；
- 请求 Restore。

高风险操作需要二次确认：

- 批量 Demote 当前可播放媒体；
- Demote 大量 Pinned 媒体；
- 可能触发显著取回 / 迁移费用的批量动作；
- 删除唯一 Durable Placement。

最后一项仍遵循 Storage / GC 的更严格规则，不能由本页面绕过。

---

## 8. Metrics

建议 Storage / Delivery Dashboard 展示：

### Capacity

- Logical Bytes；
- Unique Physical Bytes；
- Replica Physical Bytes；
- Bytes by Tier；
- Temporary Restore Bytes；
- Server Cache Bytes。

### Delivery

- CDN Delivered Bytes；
- Origin Bytes；
- Cache Hit Ratio；
- DIRECT Delivered Bytes；
- Server Proxy Bytes；
- Delivery Error Rate。

### Archive

- Restore Requests；
- Restore Bytes；
- Restore Success Rate；
- Restore Latency Distribution；
- Promotion / Demotion Bytes。

低带宽 Profile 下应重点显示：

```text
Server Proxy Bytes
```

并在该值异常升高时告警，因为这通常意味着媒体流量意外绕过 CDN / DIRECT 路径。

---

## 9. Health

Delivery Provider 独立 Health：

```text
HEALTHY
DEGRADED
UNHEALTHY
UNKNOWN
```

Storage Provider Healthy 不代表 Delivery Provider Healthy；反之亦然。

典型检测：

- DNS / TLS；
- Token / Signed URL；
- Origin Auth；
- Range；
- 读取测试；
- CDN / Edge Provider API（可选）。

---

## 10. Cost Metadata

Console 可以允许 Provider 插件上报成本相关元数据：

```text
minimum_storage_duration
restore_modes
restore_latency_range
retrieval_billing_model
```

设计约束：

- 这些信息是策略提示，不是账单真相；
- 公开单价变化快，不应硬编码进核心领域；
- 没有可靠实时价格源时，只显示定性风险提示；
- 用户手工配置成本参数时，要标记其来源和更新时间。

---

## 11. Audit

以下动作必须审计：

- 新增 / 修改 / 删除 Delivery Provider；
- 修改 Private Origin / Auth；
- 启停 Server Proxy；
- 修改 Archive Policy；
- 批量 Restore；
- 手动 Promotion / Demotion；
- 修改 Working Set Budget；
- 修改 Keep Hot / Hold。

审计日志不得记录完整 Signed URL、Secret、Origin Credential。

---

## 12. 验收场景

1. 可以配置 CDN Delivery Provider 并完成 Range Probe。
2. 可以启用低带宽 Profile，Server Media Proxy / Cache 默认关闭。
3. Storage 与 Delivery Health 分开显示。
4. 可以创建 Archive Policy，并阻止明显不兼容的 Provider/Tier 组合。
5. Restore Task 可以追踪到 Resource / Season / Episode。
6. 可以对失败 Restore 项单独重试。
7. 可以看到 Tier 容量、Restore Bytes、Origin Bytes、Server Proxy Bytes。
8. 批量 Demotion / Restore 有审计记录。
9. Console 不泄露完整 Storage / CDN Credential。
10. Cost Metadata 不被冒充为云厂商实时账单。
