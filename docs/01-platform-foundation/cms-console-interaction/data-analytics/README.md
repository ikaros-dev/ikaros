# Analytics App — CMS Console 交互规格

> Analytics 是“应用”下的可选产品能力。它只能聚合调用者原本有权访问的数据，不能通过统计权限扩大 Finance、Drive、Private Notes、Passwords 等敏感域读取范围。

## 1. App Entry

Base Route：`/apps/analytics`

建议二级视图：

```text
/apps/analytics
/apps/analytics/content
/apps/analytics/storage
/apps/analytics/system
/apps/analytics/reports
```

只有 Analytics 已启用且用户拥有对应能力时显示 App。

## 2. Overview

展示真实可解释的个人 / Resource / 系统指标。没有数据、计算未完成或依赖异常时显示 Unknown / No data，不生成占位指标。

## 3. Resource Analytics

`/apps/analytics/content` 可以聚合资源库中有权访问的 Resource 类型、消费进度、Collection 等数据。

点击图表进入 `/resources/library` 对应筛选或 `/resources/library/:resourceId`，而不是打开另一套 Resource 列表。

## 4. Storage Analytics

`/apps/analytics/storage` 展示容量趋势、Tier 分布、Provider 健康趋势等分析视图。

实时管理动作仍回到对应存储页面，例如 `/storage/overview`、`/storage/providers`、`/storage/policy` 或 `/storage/maintenance`；Analytics 不直接承担 Provider Credential、Policy 或 Maintenance 操作。

## 5. System Analytics

`/apps/analytics/system` 可以展示调用者有权查看的系统历史统计。实时系统健康 / 诊断属于 `/system/operations/health` 和 `/system/operations/diagnostics`。

## 6. Reports

`/apps/analytics/reports` 用于已实现的报表、导出和重建结果。

报表生成或指标重建属于长期工作时统一进入“资源 / 活动中心”（`/resources/activity`）。

## 7. 敏感域规则

- Analytics 权限不自动授予 Finance 数据读取；
- 不自动授予 Drive File READ；
- 不返回 Private Notes / Passwords 解密内容；
- 图表和报告的 Deep Link 重新检查目标权限；
- 聚合结果仍需避免小样本或其他方式泄露无权读取数据。

## 8. 验收

- Analytics 只从“应用”进入；
- 无真实数据时不展示虚构 KPI；
- Resource 管理动作回到 `/resources/library/**`；
- Storage 管理动作回到 `/storage/**` 对应二级页面；
- System 运维动作回到 `/system/operations/**`；
- 长期报表 / 重建工作进入 `/resources/activity`；
- 不再使用旧 `/library`、`/activity`、`/system/health`、`/system/diagnostics` 作为 canonical 设计路由；
- 不再存在全局一级 Analytics Center 或历史 `/analytics-center/*` 设计路由。
