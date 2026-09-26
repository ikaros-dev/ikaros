# 系统 / 运维 — CMS Console 交互规格

> 系统健康和高级诊断统一归“系统 / 运维”。后台长期工作不再在 System 下创建独立任务中心，而统一由“资源 / 活动中心”（`/resources/activity`）承载。

## 1. 系统健康

**Route：** `/system/operations/health`

用于查看：

- 核心服务健康；
- 数据库 / Queue / Search 等依赖；
- Storage Provider Health 摘要；
- 最近健康检查时间；
- 当前 Degraded / Failed / Unknown 项。

Unknown、未探测和请求失败不得显示为 Healthy。

### 1.1 Attention

关键 Health Failure 可以进入 `/dashboard` Attention。Dashboard 跳转到 `/system/operations/health` 或更接近业务对象的页面。

### 1.2 Auto Refresh

自动刷新必须保留筛选、展开状态和已有数据。单个探针失败只影响对应区域。

## 2. 系统诊断

**Route：** `/system/operations/diagnostics`

Advanced Diagnostics 可以包含：

- Event / Outbox；
- Queue backlog；
- Search index；
- Scheduler state；
- Runtime configuration snapshot；
- 其他平台内部诊断。

内部 ID、Payload 和实现状态可以在这里展示，但必须遵守脱敏和权限规则。

`/system/operations` 只是二级目录根，可以 redirect 到第一个可访问页面，不渲染独立运维页面。

## 3. Background Work

System 不再定义 `/system/tasks`、Operations Jobs、Background Tasks 等产品级任务页面。

所有用户可观察的长期工作进入 `/resources/activity`。

系统诊断可以从 Advanced 视角链接到同一 Activity，显示 Worker、Attempt、Lease 等实现信息，但不能形成第二套状态模型。

## 4. Scheduled Jobs

如果需要管理 Cron / Scheduled Job 配置：

- 运行诊断信息属于 `/system/operations/diagnostics` 的页面内区域或适用子路由；
- 产品配置属于 `/system/settings/parameters`。

实际执行实例仍进入 `/resources/activity`。

## 5. 运维与业务工作区边界

- Storage Provider 具体配置在 `/storage/providers`；
- Resource Availability 在 `/resources/library/:resourceId`；
- 应用管理在 `/system/integrations/apps`；
- 外部集成在 `/system/integrations/external`；
- 权限问题在 `/system/access/users`、`/system/access/roles-permissions` 或 `/system/access/authentication`；
- 系统健康只提供跨系统健康摘要和技术诊断。

## 6. 验收

- 系统健康只在 `/system/operations/health`；
- 系统诊断只在 `/system/operations/diagnostics` 及其必要 deep link；
- `/system/operations` 只作为目录根，不渲染业务页面；
- Unknown 与 Healthy 分离；
- 不再存在独立 Operations Task / Background Task Center；
- 所有长期执行实例统一进入 `/resources/activity`；
- 不再使用旧 `/system/health`、`/system/diagnostics` 或历史 `/operations-center/*` 作为 canonical 设计路由。
