# 系统运维 — CMS Console 交互规格

## 1. 系统健康与告警

**路由：** `/console/ops/health`

### 页面标题区
- 标题：`系统健康与告警`。
- 当前环境/服务器标识 Chip。
- 手动 `刷新` 图标按钮。
- 有权限时显示自动刷新选择器：`关闭`、`30 秒`、`1 分钟`、`5 分钟`。

### 整体状态 Banner

展示后端定义的整体健康状态：`健康`、`降级`、`异常`、`维护中`。同时显示最近检查时间和简短原因摘要。

整体健康状态由 Backend Health Model 计算，前端不能因为某一张图表异常就自行推导系统整体状态。

### Service / Component 卡片

默认覆盖：
- Application/API；
- PostgreSQL；
- 持久化 Storage Backend；
- 启用时的 Cache；
- Search/Index；
- Scheduler/Worker Queue；
- Plugin/Connector Runtime；
- 配置了 Health Check 的 External Provider。

每张卡字段：Component、Status、适用时的 Latency/Response Time、最近一次成功检查、当前 Incident Count、简短说明、`查看`。

### 资源图表

分别展示：CPU、Memory、Heap/Runtime Memory、适用时的 Disk/Filesystem、DB Pool/Connection、Queue Depth、Request/Error Latency。

每张图必须明确 Unit 和 Time Range。

时间范围 Chip：15m、1h、6h、24h、7d。缺失 Sample 必须显示 Gap，不能补成 0。

### 告警面板

列/字段：Severity、Alert Name、Component、Opened Time、Current Value/Condition、State、Owner/Acknowledged By、操作。

操作：`确认告警`、支持时的 `静默`、打开关联 Log/Metric，以及告警模型允许人工关闭时的 `解决`。

静默 Dialog 要求填写持续时间和原因。

## 2. 定时任务

**路由：** `/console/ops/jobs`

### Job 表格

筛选：Enabled、Status、Subsystem、Schedule Type、Last Result。

列：
- Enabled Switch；
- Job Name；
- Owning Subsystem；
- Schedule/Cron 的人类可读摘要；
- Next Run；
- Last Run；
- Last Result；
- Last Duration；
- Concurrency Policy；
- 操作。

System-owned Job 的 Schedule/Enabled 状态可以只读。

### Job 详情

标题区操作：`立即运行`、允许时的 `编辑调度/配置`、`启用/禁用`。

Tabs：`概览`、`运行历史`、`配置`。

概览字段：Job ID、描述、所属子系统、Schedule Expression + 人类可读解释、Timezone、Concurrency/Misfire Policy、Timeout、Retry Policy、Next Run、Dependency。

运行历史表格：Run ID、Trigger Source（`定时`、`手工`、`重试`）、开始时间、耗时、结果、处理数量/进度、发起人、操作。

点击 `立即运行` 时，如果已有实例运行中，Dialog 必须根据 Backend Policy 明确说明本次操作会“拒绝、排队、并行还是替换”，不能静默启动重复实例。

Schedule Editor 字段：Enabled、Schedule Type、Cron/Interval/Calendar Rule、Timezone、Misfire Behavior、Concurrency Policy、Timeout。

Cron Field 必须显示人类可读预览和未来 5 次运行时间。Expression 无效时禁止保存。

## 3. 后台任务

**路由：** `/console/ops/background`

该页面是全系统异步任务的统一运维注册表，用于 Import、Backup、Restore、Migration、Integrity Check、Report Rebuild、AI/Integration Job 等。具体业务详情仍由对应子系统负责。

### 标题区 / KPI 卡片

卡片：运行中、排队中、最近 24 小时失败、最近 24 小时完成、长时间运行警告。

### Task 表格

筛选：Status、Subsystem、Type、Initiator、Date Range、Cancellable、仅失败。

列：
1. Task ID；
2. Task Type/Name；
3. Owning Subsystem；
4. Related Entity Safe Label；
5. State：`排队中`、`运行中`、`等待中`、`成功`、`失败`、`已取消`；
6. 可测量时显示 Progress Indicator + Percentage；
7. Current Stage；
8. Started/Queued Time；
9. Elapsed/Duration；
10. Initiator；
11. 操作。

点击行打开 Task Inspector Side Sheet。

### Task Inspector

分区：
- Identity 与 Owner Subsystem；
- Current State/Stage；
- Progress：Item/Byte/Step；
- Queued/Started/Finished Timestamp；
- Parent/Child Task Relationship；
- Related Entity Link；
- 安全处理后的 Event/Log Timeline；
- Failure Category/Message；
- Retry/Cancel Capability。

优先操作为 `在所属子系统中打开`，跳转到业务域提供的更完整 Job Detail。

### 取消行为

只有 Backend 标记 Task 可取消时才显示 `取消`。

确认 Dialog 必须说明：
- 当前处于哪个 Stage；
- 已经完成的工作是否保留；
- 是否会执行 Cleanup/Rollback；
- 之后是否允许重试。

发起取消后状态可以先变为 `正在取消`；只有服务端确认完成后才能显示 `已取消`。

### 重试行为

只有符合条件的 Failed/Cancelled Task 显示 `重试`。Review Dialog 说明 Retry Scope，以及是否会生成新的 Task ID。原 Task 始终保留在 History 中。

### 批量操作

默认不提供危险批量操作。有权限的管理员可以批量取消 Queued Task 或重试选中的 Failure，但必须先展示影响数量。

## 通用运维规则
- Auto-refresh 不能重置 Filter、Selection、Scroll Position、Expanded Row 或已打开 Inspector。
- 实时状态可以由 SSE/WebSocket/Polling 实现，但 UI 对用户只表现一套一致 State Model。
- UI 中的运维日志必须有边界、分页/流式加载并进行 Redaction；本页面不是任意服务器文件浏览器。
- Timestamp 使用本地显示时区，并在 Tooltip 中提供精确时间；有价值时补充 Server/UTC Context。
- Health Check/Task Failure 同时显示 Error Category 和简短可读摘要。Raw Stack Trace 如需提供，应放在独立高权限诊断视图。
- 重启 Service、关闭 Server、Purge Queue、清空持久化状态等高风险运维操作不属于本基线默认能力；未来增加时必须单独设计高风险交互和文档。
