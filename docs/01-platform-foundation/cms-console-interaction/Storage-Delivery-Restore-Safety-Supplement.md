# Storage Restore Safety Supplement

> 本文定义 Restore / Repair 的 Console 安全交互。页面归属遵循 `/library`、`/storage` 和 `/activity`，不建立独立 Restore Center。

## 1. 发起入口

Restore 可以从：

- `/library/:resourceId` 的内容 Availability；
- `/storage/archive` 的归档内容列表；
- `/storage/maintenance` 的高级诊断。

普通用户优先从业务内容发起；技术维护人员才从 Blob / Replica 视角发起。

## 2. Restore Confirmation

确认必须展示：目标业务内容、当前状态、恢复来源层级、目标层级、预计数据量和异步语义。

如果已有可访问副本，应解释为什么仍需要恢复或禁止重复操作。

## 3. 状态

Resource / 内容项使用 Restoring 表达业务状态。执行细节进入 `/activity/:activityId`。

Activity Detail 展示：当前阶段、进度、关联 Resource、开始时间、错误摘要和允许的取消/重试动作。

Worker、Lease、Attempt、内部 Object Key 等只进入 Advanced。

## 4. Cancel / Retry

只有后端明确支持安全取消的阶段才显示 Cancel。

Retry 必须遵守幂等语义，不能重复创建已完成副本或破坏已有可用内容。

## 5. Failure

Restore Failure 应：

- 更新 Activity 为失败；
- 在对应 Resource / Storage 业务视图产生可理解状态；
- 必要时产生 Dashboard Attention；
- 提供下一步，例如 Retry、选择其他 Provider、进入 Maintenance。

不得只显示内部 Task ID。

## 6. Repair

Replica / Placement Repair 属于 `/storage/maintenance`。Repair 完成后业务 Resource Availability 必须重新计算或刷新。

## 7. 权限

- Resource 查看与 Storage Maintenance 权限分离；
- 发起 Restore 使用业务/存储策略定义的专用能力；
- 高成本或大范围恢复可以按策略要求额外确认；
- Drive 私有内容仍执行 Drive 内容读取边界。

## 8. 验收

- Restore 从业务对象可理解地发起；
- 执行统一进入 `/activity`；
- 失败能回到 Resource / Storage 解释影响；
- Cancel / Retry 只在安全阶段出现；
- 不再设计历史 Restore Center、Delivery Restore 页面或独立任务入口。
