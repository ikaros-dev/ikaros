# Media Delivery Reliability / Budget Console Supplement

> Media Delivery 的深度可靠性、预算和故障切换属于“存储 / 存储维护”；媒体消费体验属于“应用 / 媒体”；后台执行属于“资源 / 活动中心”。

## 1. 页面归属

- Media 消费：`/apps/media`；
- Resource Availability：`/resources/library/:resourceId`；
- Storage Delivery 运维：`/storage/maintenance`；
- 系统健康摘要：`/system/operations/health`；
- 长期 Repair / Purge / Rebuild：`/resources/activity`。

不建立独立 Edge Acceleration 一级工作区。

## 2. Reliability

Maintenance 可以展示：

- Delivery Provider Health；
- Origin / Edge 路径；
- Failover 状态；
- Error Rate / Latency；
- 最近故障和恢复时间。

只有真实探测结果可以显示 Healthy；Unknown 和过期探测必须单独表达。

## 3. Budget

Budget 视图展示真实流量、预算阈值、当前消耗和预测（仅在有可靠数据时）。

没有配置预算时显示 Not configured，不用绿色正常状态代替。

达到阈值后可以产生 Dashboard Attention，并链接到 `/storage/maintenance` 对应视图。

## 4. Purge

CDN / Edge Purge 属于 Maintenance 高级动作。

确认必须说明目标范围、缓存影响和是否影响持久化原始数据。Purge 不得被描述成删除 Resource / Blob。

大范围 Purge 属于长期执行时进入 `/resources/activity`。

## 5. Failover / Repair

自动或人工 Failover 必须能够解释当前 active path 和切换原因。

需要后台协调的 Repair / Rebuild 进入 `/resources/activity`。完成后 Resource Availability 和 Delivery Health 刷新。

## 6. Media App

Media App 只需要展示用户可理解的播放可用性和必要错误，不暴露 Provider Route、Origin ID 等实现细节。

需要技术排障时提供 `Open Storage Diagnostics`，进入 `/storage/maintenance` 并重新检查权限。

## 7. 验收

- Delivery 深度配置和诊断全部归 `/storage/maintenance`；
- Media App 不承担运维配置；
- Budget / Reliability 使用真实数据；
- 长期执行统一进入 `/resources/activity`；
- Resource deep link 使用 `/resources/library/:resourceId`；
- 系统健康使用 `/system/operations/health`；
- 不再使用旧 `/library`、`/activity`、`/system/health` 作为 canonical Console 路由；
- 不再设计 `/edge-acceleration/*` 或独立 Delivery Center 路由。
