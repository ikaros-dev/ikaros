# Storage Delivery / Archive Console Operations Design

> 本文补充 Storage Workspace 中归档、恢复和 Delivery 运维交互。所有页面必须遵循新的菜单/路由层级：Storage 页面位于 `/storage/**`，长期执行位于“资源 / 活动中心” `/resources/activity/**`。

## 1. 页面归属

- Storage Overview：`/storage/overview`；
- Archive & Restore：`/storage/archive`；
- Provider：`/storage/providers`；
- Policy：`/storage/policy`；
- Delivery / Replica / Blob 高级诊断：`/storage/maintenance`；
- 长期执行状态：`/resources/activity`；
- 执行详情：`/resources/activity/:activityId`。

`/storage` 只作为一级目录根 redirect，不渲染 Storage Overview。不得建立独立 Edge Acceleration Center、Restore Center 或 Storage Task Center。

## 2. Archive

Archive 页面以 Resource 为主语，展示：标题、Resource 类型、逻辑大小、当前 Tier、归档时间、Availability 和恢复动作。

归档策略本身在 `/storage/policy` 配置。归档执行实例进入 `/resources/activity`。

## 3. Restore

恢复动作从 `/resources/library/:resourceId` 或 `/storage/archive` 发起。

确认信息至少包含：

- 目标 Resource；
- 当前归档层级；
- 目标可用层级；
- 预计数据量；
- 当前是否已有可访问副本；
- 操作为异步过程的说明。

提交后进入活动中心。业务页面显示 Restoring 状态，并能跳到 `/resources/activity/:activityId`。

## 4. Delivery

常规用户只关心“能否访问 Resource”和必要的 Delivery 状态。

CDN、Purge、Failover、Origin、Budget、Provider routing 等高级分发运维进入 `/storage/maintenance`，不能形成新的一级导航。

## 5. Failure / Failover

Delivery Failure 优先反映到业务对象 Availability 和 Dashboard Attention。

Maintenance 页面可以展示 Provider、Route、Failover、Request ID 等技术字段。业务 Resource 页面只展示可理解的影响和下一步。

## 6. Reliability / Budget

Reliability 和 Budget 是 Storage Maintenance 的高级视图。

预算阈值、流量消耗和故障切换策略必须使用真实数据。Unknown / 未配置不渲染成正常。

## 7. Activity

Archive、Restore、Purge、Failover repair、Replica repair 等长期操作全部统一注册到 `/resources/activity`。

Storage 页面可以显示局部业务队列，但不得建立第二套执行状态模型。

## 8. 安全边界

- Resource 读取权限不自动授予 Storage Maintenance；
- Storage 运维权限不自动授予 Drive 私有文件内容读取；
- Purge / destructive repair 等高影响操作执行独立权限和必要确认；
- 内部 Blob / Placement / Object Key 信息只在 Maintenance 中展示。

## 9. 验收

- `/storage` 只作为目录根，不渲染页面；
- Storage Overview 使用 `/storage/overview`；
- Archive / Restore 使用 `/storage/archive`；
- Delivery 深度运维使用 `/storage/maintenance`；
- 长期执行统一进入 `/resources/activity`；
- Resource deep link 使用 `/resources/library/:resourceId`；
- 不再使用旧 `/activity` 或把 `/storage` 本身当 Overview 页面；
- 不再定义独立 edge/storage delivery 一级中心。
