# Ikaros V2 数据统计与分析子系统设计

| 项目 | 内容 |
|---|---|
| 子系统 | Data Analytics / Statistics |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-30 |
| 状态 | 草案（Draft） |

> 本文档定义 Ikaros V2 的统一数据统计、指标、分析、聚合与报表能力。
>
> 本子系统不承担业务数据的主存储职责，也不替代 System Operations 中的实时运行监控。它通过消费业务 Event、Activity、Audit、Task Run、Health 等事实数据，形成统一、可解释、可追溯的数据分析能力。

---

## 1. 设计目标

Ikaros V2 的子系统数量较多，如果每个模块各自实现统计逻辑，会快速出现以下问题：

- 同一个指标在不同页面出现不同数值。
- 每个模块重复实现按日、周、月聚合。
- 为了一个 Dashboard 在大量业务表之间进行复杂 JOIN。
- 指标口径无法追踪，后续调整时不知道哪些页面会受影响。
- 历史状态无法还原，只能查询当前结果。
- 插件和自动化产生的数据难以统一统计。
- 业务表被大量报表查询拖慢。
- 操作日志、Activity、播放历史、任务、存储、系统健康分别形成孤岛。

因此 V2 需要建立统一的数据统计与分析体系。

核心目标：

1. **统一指标口径**：一个指标只定义一次。
2. **统一时间维度**：日、周、月、季度、年等时间窗口采用一致语义。
3. **统一维度体系**：User、Resource、Type、Collection、Project、Goal、Storage Tier 等维度可组合分析。
4. **历史可追溯**：支持趋势和历史状态，而不只查询当前值。
5. **数据血缘清晰**：能够知道一个指标由哪些事实、哪些规则计算得到。
6. **统计与业务解耦**：统计结果不是业务真相源。
7. **可增量计算**：避免每次打开 Dashboard 都全量扫描历史数据。
8. **可重建**：聚合结果损坏时能够从原始事实或更低层级聚合重新生成。
9. **隐私可控**：个人行为统计必须有明确权限和数据可删除能力。
10. **可扩展**：插件可以声明统计事件、维度和指标，而不是直接修改核心统计表。

---

## 2. 核心原则

### 2.1 Business State ≠ Analytics State

业务子系统拥有自己的当前状态。

例如：

```text
Productivity
Task.status = COMPLETED
```

这是业务事实。

统计子系统可以派生：

```text
completed_task_count(day=2026-08-30) = 12
```

但统计结果不能反过来决定 Task 是否已经完成。

因此：

```text
Business Subsystem
      │
      │ Event / Activity / Snapshot
      ▼
Analytics
      │
      ├── Fact
      ├── Aggregate
      ├── Metric
      └── Dashboard
```

而禁止：

```text
Dashboard Aggregate
       ↓
直接修改业务状态
```

如确需根据统计结果触发业务动作，必须通过 Automation → Command 回到目标子系统。

---

### 2.2 Metric Definition First

所有正式指标必须先有定义，再有查询。

一个指标至少需要描述：

```text
Metric
├── key
├── name
├── description
├── unit
├── value_type
├── aggregation
├── dimensions
├── source_facts
├── time_semantics
├── null_semantics
├── owner
└── version
```

例如：

```text
Metric Key:
productivity.task.completed.count

Definition:
统计指定时间窗口内完成的用户任务数量。

Time Semantics:
以 task.completed_at 所在用户时区日期归属。

Aggregation:
COUNT

Dimensions:
user / project / goal / priority / tag
```

以后任何 Dashboard、API 或插件引用该指标，都应使用同一个 Metric Definition。

---

### 2.3 Raw → Fact → Aggregate → Metric

统计数据分层：

```text
Raw Event / Source State
          ↓
        Fact
          ↓
       Aggregate
          ↓
        Metric
          ↓
Dashboard / Report / Automation
```

不同层职责：

#### Raw

原始 Event、Activity、Audit、运行记录或业务快照。

#### Fact

经过标准化的统计事实。

例如：

```text
PlaybackCompletedFact
TaskCompletedFact
ResourceCreatedFact
BlobAccessFact
StorageCostFact
FocusSessionFact
LoginFact
```

#### Aggregate

按照固定粒度预计算的数据，例如：

```text
user_daily_playback
resource_daily_views
storage_daily_bytes
project_weekly_focus
```

#### Metric

在 Aggregate 或 Fact 之上定义的稳定业务指标。

---

### 2.4 统计不等于埋点

Ikaros 不应为了“以后可能有用”无限收集客户端行为。

优先数据来源顺序：

1. 已经存在的业务 Event。
2. Activity。
3. Audit / Login / Security Event。
4. Background Task / Scheduled Job Run。
5. Storage / Health 等系统事实。
6. 必要时增加明确目的的 Analytics Event。

只有当现有业务事实不足以支持明确指标时，才增加专门 Analytics Event。

禁止无明确用途的大规模 UI 点击追踪。

---

## 3. Analytics 与其他子系统的关系

```text
Resource / Media / Document / Productivity
                │
                │ Domain Event
                ▼
        Platform Integration
                │
                ├───────────────┐
                ▼               ▼
            Activity          Audit
                │               │
                └──────┬────────┘
                       ▼
                  Analytics
                       │
           ┌───────────┼────────────┐
           ▼           ▼            ▼
        Metric      Dashboard     Report
           │                        │
           └──────────┬─────────────┘
                      ▼
                  Automation
                      │
                      ▼
                    Command
```

Analytics 是 Event Consumer。

它不要求业务子系统在完成一次操作后同步调用：

```text
AnalyticsService.increment(...)
```

而是消费：

```text
task.completed
playback.completed
resource.created
blob.accessed
article.published
```

这样业务逻辑不依赖统计服务可用性。

---

## 4. 数据域分类

统一分析平台至少覆盖以下数据域。

### 4.1 Content Analytics

内容与资源统计。

典型指标：

- Resource 总数
- 按 Resource Type 数量
- 新增 Resource 数量
- Archived / Trash 数量
- Attachment 数量
- Blob 数量
- 内容去重率
- Collection 数量
- 标签使用情况
- 元数据完整率
- 外部身份映射覆盖率

---

### 4.2 Media Consumption Analytics

视频、音乐、漫画、小说等消费行为。

典型指标：

- 观看时长
- 阅读时长
- 音乐收听时长
- 完成剧集数
- 完成电影数
- 漫画章节完成数
- 小说章节完成数
- 播放次数
- Continue Watching 数量
- 完成率
- 每日/每周消费趋势
- 最常访问 Resource
- 按 Resource Type 的时间投入

需要避免简单将“打开播放器”视为观看完成。

统计应尽量使用业务明确事件，例如：

```text
playback.started
playback.progressed
playback.completed
reading.progressed
reading.completed
```

---

### 4.3 Productivity Analytics

与 Productivity / Planning 子系统联动。

典型指标：

- 新建 Task 数
- 完成 Task 数
- 完成率
- Overdue 数量
- Planned Duration
- Actual Focus Duration
- Estimate Accuracy
- 按 Project 的时间投入
- 按 Goal 的时间投入
- 按 Tag 的时间投入
- Habit Check-in
- Habit Streak
- Goal Progress 趋势
- OKR Check-in 趋势
- At Risk Goal 数
- Time Block 利用率

明确禁止将这些指标组合成未经用户定义的“个人效率评分”。

Analytics 的职责是帮助用户理解事实，而不是评价人格或价值。

---

### 4.4 Creation Analytics

文章、博客、笔记、文档等创作行为。

典型指标：

- 新建文档数
- 发布文章数
- Revision 数
- 创作时间
- Focus Time
- 草稿数量
- 发布频率
- 字数变化趋势（如果内容类型支持）
- 协作次数
- 评论/批注数量

如果未来存在公开内容访问，可增加阅读量等指标，但 Self-hosted 默认不假设公网访问存在。

---

### 4.5 Storage Analytics

围绕 Attachment / Blob / Placement / Cache。

典型指标：

- Blob 总字节数
- 唯一 Blob 字节数
- Attachment 逻辑字节数
- 去重节省空间
- 按 Storage Tier 占用
- 按 Storage Provider 占用
- Hot / Warm / Cold / Archive 分布
- Replica 数量
- 缺失副本数量
- Corrupted Blob 数量
- Archive Restore 次数
- Blob Migration 数量
- Cache Hit Rate
- Server Disk Cache Hit Rate
- Client Cache Hit Rate（可用时）
- Cache Eviction
- 对象存储读取字节数
- 对象存储写入字节数
- 数据迁移流量

需要同时区分：

```text
Logical Size
Physical Unique Size
Replica Physical Size
Cache Size
```

否则“存储占用”会产生严重歧义。

---

### 4.6 Sharing & Collaboration Analytics

典型指标：

- Share 创建数量
- Active Share 数量
- Share 访问次数
- Share 下载次数
- Room 创建数量
- Room Session 数量
- Watch Together 时长
- Listen Together 时长
- Collaborative Editing Session 数量

涉及不同用户的统计必须遵循权限和隐私要求。

---

### 4.7 Plugin & Integration Analytics

典型指标：

- Plugin 安装数量
- Enabled Plugin 数量
- Plugin 调用成功率
- Metadata Sync 次数
- Import 数量
- Sync 成功/失败数量
- External Provider 调用量
- External Provider Error Rate
- Automation Rule 执行次数
- Automation 成功率
- Automation Loop Protection 次数

---

### 4.8 Notification Analytics

典型指标：

- Notification 生成数量
- Read / Unread
- Delivery Success
- Delivery Failure
- Provider Error Rate
- 按类型通知分布

需要区分：

```text
Created
Delivered
Read
Actioned
```

---

### 4.9 Security Analytics

来自 Login Log、Security Event、Audit。

典型指标：

- Login Success
- Login Failure
- Failed Login by IP
- Active Session
- Revoked Session
- Permission Denied
- Share Token Failure
- Sensitive Operation Count
- Security Alert Count

安全统计权限应高于普通业务 Dashboard。

---

### 4.10 Operations Analytics

与 Platform Administration / System Operations 联动。

这里主要提供历史分析，而不是替代实时监控。

典型指标：

- Subsystem Availability
- Background Task Success Rate
- Scheduled Job Success Rate
- Task Duration
- Queue Delay
- Storage Provider Availability
- Search Index Lag
- Notification Provider Availability
- API Error Trend
- Database Connection Trend

实时告警仍由 System Operations 负责。

Analytics 用于回答：

> 过去 30 天对象存储稳定性怎么样？

而 Operations Monitoring 用于回答：

> 对象存储现在是否正常？

---

## 5. Metric Catalog

V2 应建立统一 Metric Catalog。

Metric Catalog 是统计体系最重要的基础设施之一。

每个 Metric Definition 包含：

```text
MetricDefinition
├── key
├── version
├── name
├── description
├── domain
├── unit
├── value_type
├── aggregation_type
├── source
├── time_field
├── supported_dimensions
├── default_time_grain
├── privacy_level
├── retention_policy
├── owner_subsystem
└── deprecated
```

例如：

```text
key:
media.playback.duration

unit:
seconds

aggregation:
SUM

source:
PlaybackSessionFact

time_field:
ended_at

dimensions:
user
resource
resource_type
collection
client_type
```

---

## 6. Metric 类型

### 6.1 Counter

累计事件数量。

例如：

```text
resource.created.count
login.failed.count
task.completed.count
```

---

### 6.2 Sum

数值累加。

例如：

```text
playback.duration
focus.duration
storage.bytes.read
```

---

### 6.3 Gauge

某个时间点状态。

例如：

```text
resource.total
active.sessions
storage.bytes.used
```

Gauge 需要明确是当前查询还是 Snapshot。

---

### 6.4 Ratio

比例指标。

例如：

```text
cache.hit.rate
background_task.success.rate
task.completion.rate
```

Ratio 必须定义 numerator 与 denominator。

禁止平均已经计算好的百分比。

例如错误：

```text
AVG(daily_cache_hit_rate)
```

正确：

```text
SUM(hit) / SUM(request)
```

---

### 6.5 Distribution

分布指标。

例如：

```text
background_task.duration
api.latency
blob.size
focus.session.duration
```

可用于：

- min
- max
- avg
- median
- percentile
- histogram

具体实现由后续设计确定。

---

### 6.6 Snapshot Metric

表示某时刻状态。

例如每日 00:00：

```text
resource.total = 120034
blob.bytes = 8.4 TB
active_share.total = 23
```

用于长期趋势而无需回放所有历史状态变化。

---

## 7. Dimension Model

分析维度应统一，而不是每张报表自己定义。

核心 Dimension 候选：

```text
Time
User
Resource
Resource Type
Collection
Tag
Project
Goal
OKR Cycle
Storage Provider
Storage Tier
Plugin
Client
Device
Room
Share
Notification Type
Background Task Type
Scheduled Job
Subsystem
```

---

## 8. Time Dimension

时间是统计系统中最容易产生错误的维度之一。

### 8.1 时间语义

每个 Fact 都必须区分：

```text
occurred_at
recorded_at
processed_at
```

例如客户端离线时：

```text
用户 10:00 完成 Task
18:00 联网同步
18:01 Analytics 消费
```

那么：

```text
occurred_at  = 10:00
recorded_at  = 18:00
processed_at = 18:01
```

业务统计默认使用 occurred_at。

---

### 8.2 用户时区

个人统计需要尊重用户时区。

例如用户当地时间：

```text
2026-08-30 23:30
```

即使数据库 UTC 已经进入第二天，Daily Review 仍应归入用户的 8 月 30 日。

必须保存足够的信息以重新进行时间归属。

---

### 8.3 时间粒度

标准 Grain：

```text
Hour
Day
Week
Month
Quarter
Year
All Time
```

其中 Week Start 必须可配置或至少明确系统默认值。

---

## 9. Fact Model

Analytics 不应直接把所有 Domain Event 原样长期用于查询。

需要将它们标准化为 Fact。

例如：

```text
Domain Event
playback.completed.v2
       ↓
PlaybackSessionFact
```

Fact 应具有较稳定的数据结构。

通用字段可包含：

```text
fact_id
event_id
fact_type
occurred_at
recorded_at
user_id
resource_id
context
source_subsystem
source_version
```

再加具体 Fact 字段。

---

## 10. Fact 示例

### PlaybackSessionFact

```text
user
resource
started_at
ended_at
watched_seconds
completed
client
room
```

### FocusSessionFact

```text
user
project
goal
task
started_at
ended_at
focus_seconds
interrupted
```

### BlobAccessFact

```text
blob
user
source_tier
cache_level
bytes
operation
latency
```

### BackgroundTaskRunFact

```text
task_type
subsystem
scheduled_job
queued_at
started_at
ended_at
status
retry_count
```

### LoginFact

```text
user
occurred_at
result
client
ip_classification
auth_method
```

对于敏感字段，应避免为了统计保存完整原始值。

例如统计通常不需要长期保存完整 IP，可根据产品需求使用脱敏或分类后的信息。

---

## 11. Aggregation Architecture

不应每次 Dashboard 请求都扫描 Fact 全历史。

推荐采用多层聚合：

```text
Fact
 ↓
Hourly Aggregate
 ↓
Daily Aggregate
 ↓
Monthly / Long-term Aggregate
```

并根据数据域决定是否需要 Hourly。

例如个人阅读统计通常 Daily 即可。

而系统运行统计可能需要 Hourly 或更小粒度。

---

## 12. PostgreSQL-first Analytics

V2 核心 Analytics 初期仍以 PostgreSQL 为默认实现。

优先使用标准 SQL 与 PostgreSQL 原生能力，例如：

- 分区表
- Window Function
- Recursive CTE
- FILTER
- GROUPING SETS
- ROLLUP / CUBE（适合时）
- Materialized View
- Partial Index
- Expression Index
- Generated Column（适合时）
- JSONB（仅动态维度/插件扩展）

不为了 Analytics 强制引入外部 OLAP 系统。

---

## 13. PostgreSQL 数据分层原则

概念上可以分为：

```text
Core OLTP Tables
       ↓
Analytics Fact
       ↓
Analytics Aggregate
       ↓
Materialized / Cached Result
```

注意：

核心业务表仍归业务子系统。

Analytics 不直接拥有：

```text
resource
attachment
task
user
```

等业务主状态。

---

## 14. 分区策略

大规模 Fact 表应考虑按时间分区。

例如：

```text
analytics_fact
├── 2026_08
├── 2026_09
└── 2026_10
```

具体按月、季度还是其他粒度，由数据量决定。

PRD/HLD 阶段只确定原则：

> 高增长、Append-heavy 的统计事实数据应设计为可时间分区。

---

## 15. Materialized View

适合以下场景：

- Dashboard 高频查询
- 计算复杂
- 实时性要求不高
- 可重建

例如：

```text
user_monthly_media_summary
storage_provider_daily_summary
productivity_weekly_summary
```

但不能把所有 Metric 都实现为 Materialized View。

---

## 16. Incremental Aggregation

聚合任务应尽量增量执行。

例如：

```text
新 Playback Fact
      ↓
更新对应 User + Day Aggregate
```

或定时批处理：

```text
每小时
聚合上一小时新增 Fact
```

需要支持 Late Arriving Fact。

例如移动端三天后才同步旧 Activity，系统必须能够重新计算对应历史日期，而不是错误计入同步当天。

---

## 17. Rebuild

所有派生统计数据必须具有重建策略。

```text
Aggregate
Materialized View
Metric Cache
```

都应视为可重建数据。

重建可以：

- 指定 Metric
- 指定时间范围
- 指定 User
- 指定 Domain

并以 Background Task 执行。

---

## 18. Data Lineage

用户或管理员应能够在必要时知道：

```text
Dashboard Card
      ↓
Metric
      ↓
Aggregate
      ↓
Fact
      ↓
Source Event / Subsystem
```

至少系统内部必须保存这套血缘信息。

这样当某指标异常时，可以回答：

> 这个数字到底怎么算出来的？

---

## 19. Metric Versioning

指标定义可能变化。

例如：

```text
Watch Completion
v1: progress >= 90%
v2: playback.completed event
```

这两个口径并不完全相同。

因此 Metric Definition 需要 Version。

不能静默修改历史指标语义。

允许：

```text
media.watch.completed.count@v1
media.watch.completed.count@v2
```

最终旧版本可以 Deprecated。

---

## 20. Dashboard Framework

V2 不应该为每一个页面手写统计组件和查询。

需要统一 Dashboard Model。

```text
Dashboard
├── Sections
│   └── Cards
│       ├── Metric Card
│       ├── Trend
│       ├── Breakdown
│       ├── Table
│       ├── Distribution
│       └── Heatmap
└── Filters
```

---

## 21. Dashboard 类型

### Personal Overview

例如：

```text
本周
观看      8h 20m
阅读      4h 10m
音乐      12h
专注      16h 40m
完成任务  31
```

重点是帮助用户理解自己的数字生活，而不是排名或打分。

---

### Content Dashboard

```text
Resource 总数
新增趋势
类型分布
Metadata 覆盖率
最近导入
```

---

### Storage Dashboard

```text
Logical Data
Unique Blob Data
Replica Data
Cache Data

Tier Distribution
Provider Distribution
Dedup Savings
Cache Hit Rate
Restore Trend
```

---

### Productivity Dashboard

```text
Task Completion
Planned vs Actual
Focus Trend
Project Time Allocation
Goal Progress
Habit Trend
```

---

### System Analytics Dashboard

```text
Availability Trend
Task Failure Trend
Provider Error Trend
Job Duration Trend
```

实时状态则跳转 System Operations。

---

## 22. Filters

统一统计查询应支持常见 Filter：

```text
Time Range
User
Resource Type
Collection
Tag
Project
Goal
Storage Tier
Provider
Plugin
Subsystem
Client Type
```

某 Metric 只暴露自己支持的维度。

不能因为 Dashboard 通用 Filter 存在，就强迫所有指标支持所有维度。

---

## 23. Drill-down

统计结果应尽量可追溯。

例如：

```text
本周完成任务：31
       ↓
按 Project Breakdown
       ↓
Ikaros V2：18
个人：8
其他：5
       ↓
查看具体 Task
```

但 Drill-down 必须经过原业务子系统权限校验。

Analytics 能告诉用户“有 18 个”，不意味着用户自动拥有这 18 个 Task 的读取权限。

---

## 24. Statistics API

需要提供统一统计 API，而不是各模块散落：

```text
GET /analytics/metrics/...
GET /analytics/dashboards/...
GET /analytics/reports/...
```

具体 URL 由 API Design 决定。

能力语义至少需要：

```text
Metric Query
Time Series Query
Breakdown Query
Comparison Query
Dashboard Query
Report Export
```

---

## 25. Comparison

支持常见同比/环比分析：

```text
Today vs Yesterday
This Week vs Last Week
This Month vs Last Month
This Quarter vs Previous Quarter
Custom Range vs Previous Range
```

需要明确周期长度和用户时区。

---

## 26. Report

Dashboard 是交互式查看。

Report 是可持久化、可导出的分析结果。

典型 Report：

- Weekly Review
- Monthly Personal Summary
- Quarterly OKR Review
- Storage Monthly Report
- System Reliability Report
- Security Report

Report 可以由用户手动生成，也可以由 Scheduled Job 触发。

---

## 27. Scheduled Report

例如：

```text
每周日 20:00
      ↓
Generate Weekly Review
      ↓
Report Resource
      ↓
Notification
```

Report 本身可以作为 Resource 或与 Resource 体系关联，具体模型在后续 HLD 中确定。

---

## 28. Export

统计数据需要支持用户可导出。

候选格式：

- CSV
- JSON
- Markdown Report

大规模原始分析数据是否开放导出由权限与实现阶段决定。

---

## 29. Object Storage 与历史统计归档

PostgreSQL 是核心统计数据库，但非常长期的大规模 Raw Analytics 数据可考虑归档到对象存储。

例如：

```text
Recent Facts
PostgreSQL

Old Raw Facts
Object Storage Archive

Aggregates
PostgreSQL
```

这不是 V2 初期强制实现，但 Analytics 数据模型必须允许未来进行冷热归档。

归档后 Dashboard 仍优先使用 Aggregates，不应为了普通月度统计恢复所有 Raw Fact。

---

## 30. Retention Policy

不同统计数据具有不同保留策略。

例如：

```text
Fine-grained Operational Fact
30 / 90 days

Daily Aggregate
长期

Personal Activity Fact
用户可配置/删除

Security Fact
按安全策略保留
```

具体默认天数在产品设置阶段确定。

---

## 31. Privacy

个人 Analytics 默认属于用户私有数据。

例如：

- 阅读历史
- 观看历史
- Focus Time
- Task Completion
- Goal Progress

管理员不能仅因为拥有 Dashboard 管理权限就默认读取所有用户的私人统计明细。

必须结合平台 Permission 与 Resource / User Scope。

---

## 32. Analytics Permission

权限可考虑：

```text
analytics:read:self
analytics:read:system
analytics:read:security
analytics:manage
analytics:export
analytics:rebuild
```

最终命名由 Permission Design 确定。

---

## 33. Delete / Forget

如果用户删除历史 Activity 或要求清除相关个人数据，Analytics 必须能够处理派生统计。

不能出现：

```text
原始 Activity 已删除
但 Dashboard 永远保留原来的统计
```

需要支持：

```text
Delete Source
      ↓
Invalidate Aggregate
      ↓
Rebuild Affected Range
```

---

## 34. Auditability

以下操作应记录 Audit：

- 创建/修改 Metric Definition
- 修改 Retention Policy
- 导出敏感 Analytics
- 重建统计
- 删除统计事实
- 查看安全统计（必要时）

---

## 35. Analytics Event

专用 Analytics Event 仅在必要时使用。

统一格式应与 Platform Event 体系兼容：

```text
event_id
event_type
schema_version
occurred_at
actor
subject
context
correlation_id
causation_id
payload
```

不能再创建另一套完全独立的事件基础设施。

---

## 36. Event Idempotency

由于 Platform Event 采用至少一次投递语义，Analytics Consumer 必须幂等。

同一个 Event：

```text
event_id = E1
```

即使收到两次，也只能形成一次 Fact。

---

## 37. Correction Event

某些业务事实会被修正。

例如：

```text
Focus Session 误记录 4h
用户修正为 40m
```

不应简单直接 UPDATE 所有聚合。

可以通过：

- Fact Revision
- Correction Event
- Aggregate Invalidation

等方式实现。

具体方案后续设计。

---

## 38. Snapshot

对于无法通过 Event 完整推导的当前状态，可周期性生成 Snapshot。

例如：

```text
StorageDailySnapshot
ResourceDailySnapshot
ActiveUserDailySnapshot
```

Snapshot 也是统计 Fact 的一种。

---

## 39. Search Analytics

可统计：

- 搜索次数
- 无结果查询数量
- Search Latency
- Index Lag

但默认不应该无限期保存用户完整搜索关键词。

需要考虑隐私。

如果需要“无结果搜索分析”，可以采用可配置保留或脱敏策略。

---

## 40. Cache Analytics

由于 V2 有多级访问缓存，统计应明确每次 Blob Access 的来源层级。

例如：

```text
CLIENT_CACHE
SERVER_DISK_CACHE
OBJECT_HOT
OBJECT_WARM
OBJECT_COLD
ARCHIVE_RESTORE
```

从而计算：

```text
Client Cache Hit Rate
Server Cache Hit Rate
Origin Object Storage Rate
Archive Restore Rate
```

注意：

客户端 Cache 和用户 Download 是不同语义，Analytics 也必须分开统计。

```text
client.cache.bytes
client.download.bytes
```

不能合并成一个指标。

---

## 41. Storage Cost Analytics

如果 Storage Provider 可以提供价格配置或成本信息，未来可以计算：

```text
Storage Cost
Egress Cost
Restore Cost
Request Cost
```

成本数据属于可选能力。

Ikaros 不强依赖某一家云厂商计费 API。

可以支持：

```text
Manual Cost Profile
Provider Cost Plugin
```

---

## 42. Content Dedup Analytics

重要指标：

```text
Logical Attachment Bytes
Unique Blob Bytes
Replica Bytes
Dedup Saved Bytes
Dedup Ratio
```

例如：

```text
Logical Attachment Data: 10 TB
Unique Blob Data:       7 TB
Dedup Saved:            3 TB
```

Replica Size 不计入 Dedup Saved。

否则多副本会扭曲结果。

---

## 43. Resource Lifecycle Analytics

可统计：

```text
Active
Archived
Trash
Purged
```

以及：

```text
Archive Rate
Restore Rate
Trash Growth
GC Reclaimed Bytes
```

GC 统计来自真实 GC Run，而不是通过 Attachment 删除数量估算。

---

## 44. Provenance Analytics

可以统计 Metadata 来源质量：

```text
User Edited
Bangumi
TMDB
Plugin X
Import
```

例如：

- Metadata Coverage
- Source Distribution
- Conflict Count
- User Override Count

但不得根据“哪个来源覆盖更多”自动决定优先级。

优先级仍由 Metadata / Provenance 业务规则决定。

---

## 45. External Identity Analytics

例如：

```text
Resource with Bangumi ID
Resource with TMDB ID
Resource with MusicBrainz ID
```

可以用于发现元数据缺失。

---

## 46. Notification + Analytics 联动

Analytics 可以产生分析信号：

```text
storage.free.capacity < threshold
```

但不能直接发送通知。

标准流程：

```text
Metric / Analytics Signal
      ↓
Automation / Alert Rule
      ↓
Notification Command
      ↓
Notification Subsystem
```

---

## 47. Analytics + Automation

允许用户或管理员根据 Metric 触发 Automation。

例如：

```text
WHEN
storage.hot.usage > 80%

THEN
Create Admin Task
Run Tier Migration
Send Notification
```

又如：

```text
WHEN
Goal Progress < 50%
AND Quarter Remaining < 20%

THEN
Mark At Risk
```

Automation 必须使用 Metric Definition，而不是复制 SQL。

---

## 48. Analytics Signal

对于需要自动化监测的统计条件，可以形成 Analytics Signal：

```text
MetricThresholdCrossed
TrendDetected
AnomalyDetected
```

其中 Anomaly Detection 属于后续高级能力，V2 初期无需实现复杂机器学习。

最基础的阈值和趋势规则优先。

---

## 49. Trend

支持：

```text
Increasing
Decreasing
Stable
```

趋势算法必须透明，不能使用不可解释方式给出结论。

---

## 50. Forecast

未来可以支持：

- Storage Growth Forecast
- Goal Completion Forecast
- Capacity Forecast

Forecast 必须明确是预测值，而不是事实。

例如 UI：

```text
Current: 8.2 TB
Projected in 90 days: 9.4 TB
```

不能混在同一数值体系中。

---

## 51. Plugin Analytics Extension

Plugin 可以声明：

- Fact Type
- Dimension
- Metric Definition
- Dashboard Card Provider

但必须遵守核心 Analytics Contract。

Plugin 不应该直接：

```text
CREATE TABLE analytics_xxx
并要求 Core 知道其结构
```

具体扩展方式后续 Plugin Design 确定。

动态 Payload 可以使用 JSONB，但核心公共 Metric 不应全部 JSON 化。

---

## 52. Analytics Namespace

Metric Key 应具有 Namespace。

例如：

```text
content.resource.total
media.playback.duration
productivity.task.completed.count
productivity.focus.duration
storage.blob.unique.bytes
storage.cache.hit.rate
security.login.failed.count
operations.task.success.rate
plugin.sync.success.rate
```

Plugin：

```text
plugin.{plugin-id}.xxx
```

---

## 53. Semantic Layer

Analytics API 不应该暴露物理表结构给 Dashboard。

Dashboard 查询：

```text
metric = productivity.focus.duration
range = last_7_days
group_by = day
```

而不是：

```sql
SELECT ... FROM analytics_focus_fact ...
```

这样未来底层实现可以改变而不破坏 Dashboard。

---

## 54. Query Guardrail

统计查询需要保护核心数据库。

包括：

- 最大时间范围
- 最大 Group Cardinality
- Query Timeout
- Pagination
- Export 限制
- 大查询转 Background Task

避免一个自定义 Dashboard 拖垮 PostgreSQL。

---

## 55. High Cardinality Dimension

以下维度可能具有高基数：

- Resource
- User
- Attachment
- Blob
- Search Query

不能默认允许所有 Metric 按这些维度无限 Group By。

需要 Metric Definition 显式声明。

---

## 56. Near Real-time vs Batch

不是所有统计都需要实时。

### Near Real-time

适合：

- Today Dashboard
- 当前 Task 完成数
- 当前 Storage 使用

### Batch

适合：

- Monthly Summary
- Long-term Trends
- 大规模 Rollup

Metric 应声明 Freshness Target。

例如：

```text
REALTIME
NEAR_REALTIME
HOURLY
DAILY
```

这些是产品语义，不意味着具体实现必须是流式计算框架。

---

## 57. Freshness

Dashboard 可以显示：

```text
Updated 2 minutes ago
```

对于批处理指标尤其重要。

不能让用户误认为所有数字都是实时值。

---

## 58. Data Quality

Analytics 需要基本数据质量检查：

- Duplicate Fact
- Missing Dimension
- Invalid Time
- Impossible Duration
- Aggregate Mismatch
- Late Fact Count

例如 Playback Duration 不应该为负数。

Data Quality Failure 可形成系统 Alert。

---

## 59. Consistency

Analytics 默认最终一致。

例如用户刚完成一个 Task：

```text
Task 页面：立即显示完成
Analytics Dashboard：几秒后更新
```

这是允许的。

产品 UI 应避免造成“业务操作失败”的误解。

---

## 60. Reconciliation

需要定期支持：

```text
Analytics Aggregate
        vs
Business Source Snapshot
```

进行一致性校验。

例如：

```text
analytics.resource.total
vs
Resource subsystem current count
```

发现差异后触发 Rebuild 或 Alert。

---

## 61. Background Task 集成

以下 Analytics 操作均通过 Background Task：

- Backfill
- Rebuild
- Large Export
- Long-range Report
- Archive
- Restore Historical Fact
- Reconciliation

不能阻塞普通 HTTP 请求。

---

## 62. Scheduled Job 集成

Scheduled Job 可触发：

- Hourly Rollup
- Daily Snapshot
- Daily Aggregate
- Retention Cleanup
- Weekly Report
- Monthly Report
- Reconciliation

Scheduled Job 仅负责触发，实际执行仍是 Background Task。

---

## 63. Analytics Monitoring

Analytics 自身也需要被 System Operations 监控。

Subsystem：

```text
Analytics Ingestion
Analytics Aggregator
Analytics Query
Analytics Scheduler
```

指标：

```text
Event Lag
Fact Ingest Rate
Aggregation Lag
Failed Aggregation
Query Latency
Rebuild Status
```

---

## 64. Failure Handling

Analytics 消费失败不能影响业务主流程。

```text
Task Completed
    ↓
Domain Commit Success
    ↓
Event
    ↓
Analytics Failure
```

此时 Task 仍然是完成状态。

Analytics 通过 Retry / Dead Letter / Replay 修复。

---

## 65. Replay

平台事件设计应允许 Analytics 在必要范围内 Replay。

但不能假设所有历史事件永远保留。

因此重要长期统计需要 Fact / Snapshot 自己具备保留策略。

---

## 66. Dashboard Personalization

用户可以：

- 调整 Card 顺序
- 隐藏 Card
- 保存 Filter
- 创建自定义 Dashboard

但自定义 Dashboard 只能组合其有权限访问的 Metric。

---

## 67. User-defined Metric

后期可以支持高级用户创建派生 Metric。

例如：

```text
reading_to_focus_ratio
=
reading.duration / focus.duration
```

V2 初期不作为 P0。

用户自定义 Metric 不允许执行任意 SQL。

---

## 68. Personal Review Integration

Productivity 的 Daily / Weekly / Monthly / Quarterly Review 应直接使用 Analytics Metric。

例如 Weekly Review：

```text
完成任务
计划 vs 实际时间
Focus Time
Goal Progress
媒体消费时间
创作时间
```

这样 Review 不需要复制统计逻辑。

---

## 69. Personal Digital Life Timeline

Activity + Analytics 可以形成长期个人数字生活总结：

```text
2026
├── 看完 42 部动画
├── 阅读 18 本书
├── 听音乐 620h
├── 完成 1,280 个 Task
├── Focus 730h
└── 发布 36 篇文章
```

所有数据都必须来自可解释 Metric。

这类总结属于用户私有数据。

---

## 70. No Dark Metrics

Ikaros 不应默认设计以下类型指标：

- 用户价值评分
- 效率人格评分
- 内容沉迷评分
- 基于隐私行为的隐藏画像

如果未来存在 AI 分析，也必须建立在用户明确请求和可解释数据基础上。

---

## 71. 数据统计优先级

### P0

- Metric Catalog
- Fact 基础模型
- Event → Fact Ingestion
- Time Dimension
- Daily Aggregate
- Content Statistics
- Media Consumption Statistics
- Productivity Statistics
- Storage Statistics
- Operations Historical Statistics
- Personal Dashboard
- Admin Dashboard
- Permission
- Rebuild

### P1

- Report
- Scheduled Report
- Drill-down
- Comparison
- Advanced Breakdown
- Storage Cost
- Plugin Analytics
- Data Quality
- Reconciliation
- Historical Raw Fact Archive

### P2

- User-defined Dashboard
- User-defined Derived Metric
- Trend Signal
- Forecast
- Anomaly Detection
- Advanced Analytics Automation

---

## 72. 与其他 V2 文档的关系

```text
Product Requirements Document
        │
        ├── Productivity Planning
        ├── Platform Administration / Operations
        ├── Platform Integration / Automation
        └── Data Analytics / Statistics
```

其中：

- Integration 提供 Event / Activity / Context。
- 各业务子系统提供 Source Fact。
- Analytics 形成 Metric / Aggregate / Report。
- Operations 负责 Analytics 自身健康。
- Automation 可以消费 Analytics Signal。
- Notification 负责最终通知。

---

## 73. 后续详细设计

后续需要继续形成：

- Analytics PostgreSQL Schema
- Metric Definition Schema
- Fact Contract
- Aggregation / Rollup Engine
- Retention & Archive
- Statistics HTTP API
- Dashboard Schema
- Report Engine
- Analytics Permission Model
- Analytics Plugin Extension Contract
- Rebuild / Backfill / Replay Strategy

---

## 74. 最终原则

V2 数据统计体系应始终遵循：

> **业务状态属于业务子系统，统计状态属于 Analytics。**

> **指标必须先定义口径，再实现查询。**

> **事实可追溯，聚合可重建，指标可版本化。**

> **Dashboard 不直接绑定物理数据库表。**

> **实时监控与历史分析分离，但可以联动。**

> **统计帮助用户理解数据，不替用户评价自己。**
