# System / Health 与 Diagnostics — CMS Console 交互规格

> 系统健康和高级诊断统一归 System。后台长期工作不再在 System 下创建独立任务中心，而统一由 `/activity` 承载。

## 1. System Health

**Route：** `/system/health`

用于查看：

- 核心服务健康；
- 数据库 / Queue / Search 等依赖；
- Storage Provider Health 摘要；
- 最近健康检查时间；
- 当前 Degraded / Failed / Unknown 项。

Unknown、未探测和请求失败不得显示为 Healthy。

### 1.1 Attention

关键 Health Failure 可以进入 `/dashboard` Attention。Dashboard 跳转到 `/system/health` 或更接近业务对象的页面。

### 1.2 Auto Refresh

自动刷新必须保留筛选、展开状态和已有数据。单个探针失败只影响对应区域。

## 2. Diagnostics

**Base Route：** `/system/diagnostics`

Advanced Diagnostics 可以包含：

- Event / Outbox；
- Queue backlog；
- Search index；
- Scheduler state；
- Runtime configuration snapshot；
- 其他平台内部诊断。

内部 ID、Payload 和实现状态可以在这里展示，但必须遵守脱敏和权限规则。

## 3. Background Work

System 不再定义 `/system/tasks`、Operations Jobs、Background Tasks 等产品级任务页面。

所有用户可观察的长期工作进入 `/activity`。

System Diagnostics 可以从 Advanced 视角链接到同一 Activity，显示 Worker、Attempt、Lease 等实现信息，但不能形成第二套状态模型。

## 4. Scheduled Jobs

如果需要管理 Cron / Scheduled Job 配置，它属于 `/system/diagnostics/schedules` 或 `/system/settings`，取决于它是运行诊断还是产品配置。

实际执行实例仍进入 `/activity`。

## 5. Health 与业务工作区边界

- Storage Provider 具体配置在 `/storage/providers`；
- Resource Availability 在 `/library/:resourceId`；
- Integration / Plugin 配置在 `/system/integrations`；
- 权限问题在 `/system/access`；
- System Health 只提供跨系统健康摘要和技术诊断。

## 6. 验收

- Health 只在 `/system/health`；
- Diagnostics 只在 `/system/diagnostics/**`；
- Unknown 与 Healthy 分离；
- 不再存在独立 Operations Task / Background Task Center；
- 所有长期执行实例统一进入 `/activity`；
- 不再使用历史 `/operations-center/*` 作为设计路由。
