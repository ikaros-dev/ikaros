# Storage Delivery / Archive Console Operations Design

> 本文补充 Storage Workspace 中归档、恢复和 Delivery 运维交互。所有页面必须遵循 canonical `/storage` 与 `/activity` IA。

## 1. 页面归属

- Storage Overview：`/storage`；
- Archive & Restore：`/storage/archive`；
- Provider：`/storage/providers`；
- Policy：`/storage/policy`；
- Delivery / Replica / Blob 高级诊断：`/storage/maintenance`；
- 长期执行状态：`/activity`。

不建立独立 Edge Acceleration Center、Restore Center 或 Storage Task Center。

## 2. Archive

Archive 页面以 Resource / 内容为主语，展示：标题、内容类型、逻辑大小、当前 Tier、归档时间、Availability 和恢复动作。

归档策略本身在 `/storage/policy` 配置。归档执行实例进入 `/activity`。

## 3. Restore

恢复动作从 Resource Detail 或 `/storage/archive` 发起。

确认信息至少包含：

- 目标内容；
- 当前归档层级；
- 目标可用层级；
- 预计数据量；
- 当前是否已有可访问副本；
- 操作为异步过程的说明。

提交后进入 Activity。业务页面显示 Restoring 状态，并能跳到对应 Activity Detail。

## 4. Delivery

常规用户只关心“能否访问内容”和必要的 Delivery 状态。

CDN、Purge、Failover、Origin、Budget、Provider routing 等高级分发运维进入 `/storage/maintenance`，不能形成新的一级导航。

## 5. Failure / Failover

Delivery Failure 优先反映到业务对象 Availability 和 Dashboard Attention。

Maintenance 页面可以展示 Provider、Route、Failover、Request ID 等技术字段。业务 Resource 页面只展示可理解的影响和下一步。

## 6. Reliability / Budget

Reliability 和 Budget 是 Storage Maintenance 的高级视图。

预算阈值、流量消耗和故障切换策略必须使用真实数据。Unknown / 未配置不渲染成正常。

## 7. Activity

Archive、Restore、Purge、Failover repair、Replica repair 等长期操作全部统一注册到 `/activity`。

Storage 页面可以显示局部业务队列，但不得建立第二套执行状态模型。

## 8. 安全边界

- Resource 读取权限不自动授予 Storage Maintenance；
- Storage 运维权限不自动授予 Drive 私有文件内容读取；
- Purge / destructive repair 等高影响操作执行独立权限和必要确认；
- 内部 Blob / Placement / Object Key 信息只在 Maintenance 中展示。

## 9. 验收

- Archive / Restore 使用 `/storage/archive`；
- Delivery 深度运维使用 `/storage/maintenance`；
- 长期执行统一进入 `/activity`；
- Resource 页面能解释用户可见影响；
- 不再定义独立 edge/storage delivery 一级中心。
