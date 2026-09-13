# Finance App — CMS Console 交互规格

> Finance 是 Apps 下的私密业务产品，不是全局一级工作区。

## 1. App Entry

Base Route：`/apps/finance`

```text
/apps/finance
/apps/finance/accounts
/apps/finance/transactions
/apps/finance/budgets
/apps/finance/reconcile
```

只有在 Finance 已启用且当前用户拥有对应 `finance.*` 读取能力时才显示 App。

## 2. Home

`/apps/finance` 展示调用者有权读取的账本摘要、账户、近期收支、预算状态和待对账事项。

所有数字都必须来自真实数据。无权限的账户和交易不能通过总览被间接推断。

## 3. Accounts

`/apps/finance/accounts` 管理账户名称、类型、币种、余额、状态和更新时间。

账户删除必须明确对历史记录和统计的影响，不得静默级联破坏业务数据。

## 4. Transactions

`/apps/finance/transactions` 支持日期、账户、分类、金额范围和状态筛选。

敏感业务字段不得写入普通 Telemetry 或通用通知预览。

## 5. Budgets

`/apps/finance/budgets` 展示预算周期、目标、实际、剩余和状态。没有真实基线时不生成虚假完成率。

## 6. Reconcile / Import

`/apps/finance/reconcile` 使用来源选择、预览、字段映射、重复识别、确认和结果流程。

大批量导入或重算属于长期工作时统一进入 `/activity`，Finance 页面只保留业务上下文摘要。

## 7. Analytics 与权限

Analytics App 只有同时拥有相应 Finance Scope 时才可以聚合 Finance 数据。Analytics 权限本身不能扩大 Finance 读取范围。

## 8. 验收

- Finance 只从 Apps 进入；
- App 按启用状态和权限显示；
- 私密业务字段不进入普通 Telemetry；
- 长期导入/重算工作进入 `/activity`；
- 不再设计全局一级 Finance Center 或历史 `/console/finance/*` 路由。
