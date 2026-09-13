# Analytics App — CMS Console 交互规格

> Analytics 是 Apps 下的可选产品能力。它只能聚合调用者原本有权访问的数据，不能通过统计权限扩大 Finance、Drive、Private Notes、Passwords 等敏感域读取范围。

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

展示真实可解释的个人/内容/系统指标。没有数据、计算未完成或依赖异常时显示 Unknown / No data，不生成占位指标。

## 3. Content Analytics

`/apps/analytics/content` 可以聚合 Library 中有权访问的 Resource 类型、消费进度、Collection 等数据。

点击图表进入 `/library` 对应筛选或 Resource Detail，而不是打开另一套 Resource 列表。

## 4. Storage Analytics

`/apps/analytics/storage` 展示容量趋势、Tier 分布、Provider 健康趋势等分析视图。

实时管理动作仍回到 `/storage`；Analytics 不直接承担 Provider Credential、Policy 或 Maintenance 操作。

## 5. System Analytics

`/apps/analytics/system` 可以展示调用者有权查看的系统历史统计。实时 Health / Diagnostics 属于 `/system/health` 和 `/system/diagnostics`。

## 6. Reports

`/apps/analytics/reports` 用于已实现的报表、导出和重建结果。

报表生成或指标重建属于长期工作时统一进入 `/activity`。

## 7. 敏感域规则

- Analytics 权限不自动授予 Finance 数据读取；
- 不自动授予 Drive File READ；
- 不返回 Private Notes / Passwords 解密内容；
- 图表和报告的 Deep Link 重新检查目标权限；
- 聚合结果仍需避免小样本或其他方式泄露无权读取数据。

## 8. 验收

- Analytics 只从 Apps 进入；
- 无真实数据时不展示虚构 KPI；
- 管理动作回到 Library、Storage 或 System 的 canonical 页面；
- 长期报表/重建工作进入 `/activity`；
- 不再存在全局一级 Analytics Center 或历史 `/analytics-center/*` 设计路由。
