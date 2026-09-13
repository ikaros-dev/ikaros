# Planning App — CMS Console 交互规格

> Planning 是 Apps 下的可选业务产品，不是全局一级工作区。

## 1. App Entry

**Base Route：** `/apps/planning`

Canonical routes：

```text
/apps/planning
/apps/planning/today
/apps/planning/projects
/apps/planning/calendar
/apps/planning/goals
/apps/planning/focus
```

只在当前部署启用 Planning 且用户拥有至少一个 `planning.*` 读取能力时出现在 Apps。

## 2. Home / Today

`/apps/planning` 默认进入用户最有价值的 Planning Home；可以直接呈现 Today，而不是再增加“生产力与计划中心”。

Today 展示：收集箱、今日任务、逾期、优先级、计划时间块和完成进度。

主操作：新建任务、快速收集。任务完成采用乐观更新，失败时回滚。

## 3. Projects

**Route：** `/apps/planning/projects`

项目列表展示名称、状态、Owner、任务进度、最近更新时间。项目详情承载任务、里程碑、相关 Resource / Document 和 Activity。

长期导入、批量重建等后台工作进入全局 `/activity`，普通任务本身属于 Planning 业务对象，不进入全局 Activity。

## 4. Calendar

**Route：** `/apps/planning/calendar`

按日/周/月查看 Time Block、Task Deadline 和明确支持的外部日历投影。

拖拽改期前后保持可撤销或明确保存反馈；跨时区展示遵循当前用户地区/时间偏好。

## 5. Goals

**Route：** `/apps/planning/goals`

展示 Goal / OKR、进度、Owner、周期和关联 Project。进度来源必须可解释，不能用虚构 KPI 填充。

## 6. Focus

**Route：** `/apps/planning/focus`

用于 Focus Session、习惯或复盘等已实现能力。没有后端契约的模块不因为规划存在就显示空页面。

## 7. Resource / Document 关联

Planning 可以关联 Library Resource 和 Documents App，但不得复制 Resource 身份。点击关联内容进入 `/library/:resourceId` 或对应 App 页面，并重新执行目标权限。

## 8. Notifications / Activity

普通 Planning 事件留在 App 内。只有真实异步长任务进入全局 `/activity`。

系统通知策略属于 `/system/notifications`；当前用户 Planning 提醒偏好属于 App 或 `/account/notifications`。

## 9. 验收

- Planning 只从 Apps 进入；
- 未启用或无权限时不显示 App；
- 不再存在全局一级 Planning Center；
- 普通 Task 与系统后台 Activity 语义分离；
- 所有路由使用 `/apps/planning/**`。
