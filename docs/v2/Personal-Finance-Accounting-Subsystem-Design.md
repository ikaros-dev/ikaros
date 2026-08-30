# Ikaros V2 个人记账子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Personal Finance / Accounting Subsystem Design |
| 适用版本 | Ikaros V2 |
| 文档状态 | Draft |
| 设计目标 | 提供面向个人与家庭的账户、交易、预算、对账、多币种、周期账、资产负债与统计分析能力 |

---

## 1. 产品定位

Accounting 是 Ikaros V2 的个人财务记录与分析子系统。

目标不是建设企业 ERP 或完整财务会计系统，而是帮助个人或家庭完成：

- 记录收入、支出和转账
- 管理现金、银行卡、信用卡、电子钱包等账户
- 追踪余额
- 分类与标签
- 周期账单
- 预算
- 对账
- 多币种
- 资产趋势
- 财务附件
- 统计与复盘

产品体验可参考 ezBookkeeping 的轻量、自托管、账户/交易/分类/标签/定期交易/对账/统计思路，并与 Ikaros 的 Resource、Analytics、Task、Automation、Notification 和 Secure Data Foundation 联动。

---

## 2. 安全原则

个人财务数据属于 Sensitive Data。

Accounting 的业务数据必须通过 Secure Data Foundation 加密落盘。

支持两种主要 Profile：

### SERVER_ASSISTED_ENCRYPTED

默认推荐用于希望使用服务器统计、自动化、报表与多端 Web 访问的用户。

服务端在授权运行时可解密必要字段，但所有持久化内容仍为应用层密文。

### USER_LOCKED_E2EE

适合隐私优先用户。

服务端只保存密文，深度统计与搜索主要在解锁客户端完成。

用户创建 Ledger 时必须明确安全模式。

---

## 3. 核心领域模型

```text
Finance Workspace / Ledger
│
├── Account
│   ├── Sub Account
│   └── Balance Snapshot
│
├── Transaction
│   ├── Income
│   ├── Expense
│   └── Transfer
│
├── Category
├── Tag
├── Payee / Counterparty
├── Budget
├── Scheduled Transaction
├── Transaction Template
├── Reconciliation
├── Exchange Rate
└── Attachment / Receipt
```

---

## 4. Finance Workspace / Ledger

Ledger 是一套独立财务账本。

用户可以创建：

- Personal
- Family
- Travel
- Business-like Personal Project

不同 Ledger 可以使用不同：

- Base Currency
- Security Profile
- Category System
- Budget
- Sharing Policy

---

## 5. Account

Account 表示资金账户。

类型至少支持：

- CASH
- CHECKING
- SAVINGS
- CREDIT_CARD
- DEBIT_CARD
- E_WALLET
- PREPAID
- INVESTMENT_TRACKING
- LOAN
- ASSET
- LIABILITY
- OTHER

Account 属性可包括：

- Name
- Account Type
- Institution
- Currency
- Initial Balance
- Current Balance
- Credit Limit
- Billing Day
- Due Day
- Icon
- Color
- Description
- Archived
- Sort Order

敏感账户号必须加密，UI 默认脱敏。

---

## 6. Sub Account

支持账户层级，例如：

```text
Bank A
├── Checking
├── Savings
└── Credit Card
```

但层级不应破坏独立余额和交易语义。

---

## 7. Transaction

交易是记账系统的核心事实。

类型：

```text
INCOME
EXPENSE
TRANSFER
```

逻辑字段：

```text
Transaction
├── id
├── type
├── source_account
├── destination_account
├── amount
├── currency
├── category
├── tags
├── payee
├── occurred_at
├── timezone
├── note
├── location
├── attachments
├── status
└── provenance
```

---

## 8. Transfer

转账不能建模成“支出 + 收入”两个互不关联记录。

应有一个统一 Transfer Identity：

```text
Account A -100
      ↕ same transfer
Account B +100
```

多币种转账可包含：

- Source Amount
- Source Currency
- Destination Amount
- Destination Currency
- Effective Exchange Rate
- Fee

---

## 9. Transaction Status

至少支持：

```text
DRAFT
PENDING
CLEARED
RECONCILED
VOID
```

信用卡未入账、银行 Pending 等业务不能强行当作最终已清算交易。

---

## 10. Category

Category 至少区分：

- Income Category
- Expense Category
- Transfer Category / Purpose

支持两级或多级层级，但 P0 可限制合理深度。

例如：

```text
Food
├── Dining
├── Grocery
└── Coffee
```

---

## 11. Tag

Tag 用于跨 Category 的自由维度：

- Travel
- Work
- Family
- Reimbursable
- Subscription

Category 表达主要财务分类，Tag 表达多维上下文。

---

## 12. Payee / Counterparty

可选管理交易对象：

- Merchant
- Employer
- Person
- Institution

Payee 属于敏感数据，默认加密。

---

## 13. Multi-currency

每个 Account 有自己的 Native Currency。

Ledger 有 Base Currency。

交易记录必须保留交易发生时的原始金额，不应以后汇率变化就重写历史交易。

```text
Original Amount
Original Currency
Recorded Exchange Rate
Base Currency Amount
```

---

## 14. Exchange Rate

汇率来源可以包括：

- Manual
- Provider
- Transaction Actual Rate

必须记录 Provenance 和时间。

历史统计默认使用交易当时记录的有效汇率，而不是查询今天汇率重算。

---

## 15. Credit Card

信用卡需要专门业务语义：

- Billing Day
- Due Day
- Statement Period
- Credit Limit
- Outstanding Balance
- Statement Amount
- Payment

信用卡还款应与 Transfer 体系关联。

---

## 16. Scheduled Transaction

周期交易用于：

- Rent
- Salary
- Subscription
- Loan Payment
- Utility

Scheduled Transaction 是规则，不是实际交易。

```text
Scheduled Transaction
      ↓ due
Draft / Pending Transaction
      ↓ confirm / auto-post
Actual Transaction
```

避免把未来一年的账全部提前写成真实交易。

---

## 17. Recurrence

支持：

- Daily
- Weekly
- Monthly
- Yearly
- Custom RRULE-like
- Last Business Day（后续）

需要处理：

- Month End
- Timezone
- Missed Schedule
- Pause / Resume

---

## 18. Transaction Template

用户可保存模板：

```text
Lunch
account=Wallet
category=Dining
amount=optional
```

用于快速记账。

---

## 19. Draft

移动端输入过程中应支持自动保存 Draft。

Draft 不进入正式财务统计。

---

## 20. Attachments / Receipts

交易可关联：

- Receipt Image
- Invoice PDF
- Statement
- Warranty

Attachment 默认通过 Secure Data Foundation 加密。

OCR 结果同样属于敏感数据。

---

## 21. OCR / AI Assisted Entry

可支持：

```text
Receipt Image
   ↓ OCR / Vision
Draft Transaction
├── amount
├── merchant
├── date
└── suggested category
   ↓ User Confirm
Actual Transaction
```

AI 只能生成 Draft，不应未经确认直接把不确定识别结果写成财务事实。

---

## 22. Import

支持未来导入：

- CSV
- OFX / QFX
- QIF
- Custom Bank CSV
- ezBookkeeping-like export mapping

Import 必须：

- Preview
- Column Mapping
- Dedup Detection
- Dry Run
- Validation
- Rollback / Batch Undo

---

## 23. Transaction Dedup

避免银行账单重复导入。

可使用：

- External ID
- Date
- Amount
- Account
- Counterparty
- Reference

生成 Dedup Candidate。

系统不应仅凭“同日同金额”自动删除。

---

## 24. Reconciliation

支持账户对账。

```text
Statement Ending Balance
        ↓
Compare
        ↓
Recorded Cleared Balance
        ↓
Difference
```

Reconciliation Session：

- Statement Period
- Beginning Balance
- Ending Balance
- Cleared Transactions
- Difference
- Completed At

完成后相关 Transaction 标记为 RECONCILED。

---

## 25. Balance

余额应区分：

- Ledger Balance
- Cleared Balance
- Available Balance（如有）
- Credit Available

不能只有一个模糊 `balance`。

---

## 26. Balance Snapshot

为趋势统计和历史恢复，可生成 Balance Snapshot。

Snapshot 是派生数据，不是交易事实替代品。

交易仍然是真相源。

---

## 27. Budget

预算支持：

- Monthly
- Weekly
- Quarterly
- Yearly
- Custom Period

维度：

- Overall
- Category
- Tag
- Account Group

状态：

```text
Budget
├── limit
├── spent
├── remaining
├── progress
└── forecast
```

---

## 28. Rollover Budget

P1 可支持预算结余滚动：

```text
previous remaining
    ↓
next period budget
```

正负余额是否滚动由用户配置。

---

## 29. Subscription Tracking

基于 Scheduled Transaction 和 Tag 可形成订阅视图：

- Monthly Cost
- Annualized Cost
- Next Charge
- Price Change

不需要另建完全独立的订阅交易系统。

---

## 30. Debt / Loan Tracking

P1 支持简单负债：

- Principal
- Outstanding
- Payment Schedule
- Interest Metadata

复杂摊销模型可延后，不把 V2 P0 做成专业贷款计算器。

---

## 31. Asset Tracking

P1 可支持手工资产估值：

- Cash-like
- Property
- Vehicle
- Investment Snapshot

投资行情和交易撮合不是 P0 目标。

---

## 32. Dashboard

核心 Dashboard：

```text
Net Worth
Income
Expense
Cash Flow
Budget Status
Upcoming Bills
Account Balances
Recent Transactions
```

---

## 33. Analytics Integration

财务统计必须通过 Data Analytics / Statistics Subsystem 的 Metric 体系定义。

主要指标：

```text
finance.income.amount
finance.expense.amount
finance.cashflow.net
finance.asset.total
finance.liability.total
finance.networth
finance.budget.used
finance.category.expense
finance.account.balance
```

---

## 34. Charts

支持：

- Category Pie / Bar
- Income / Expense Trend
- Cash Flow Trend
- Account Balance Trend
- Asset Allocation
- Net Worth Trend
- Tag Analysis
- Sankey-like Flow（P1）
- YoY / Period-over-period

统计口径必须明确币种与汇率。

---

## 35. Calendar View

Transaction 支持日历视图：

```text
August
12  ¥85 Lunch
13  ¥20 Coffee
15  +Salary
```

Scheduled Transaction 可作为未来预测项单独显示，不能与已发生交易混淆。

---

## 36. Search

支持按：

- Date Range
- Account
- Category
- Tag
- Amount Range
- Payee
- Note
- Status
- Attachment

在 USER_LOCKED_E2EE 模式下搜索主要由客户端执行。

---

## 37. Automation

典型联动：

```text
scheduled_transaction.due
→ Notification
→ Create Draft Transaction
```

```text
budget.usage > 80%
→ Notification
```

```text
credit_card.due_in = 3 days
→ Create Productivity Task
```

```text
transaction.created
→ Analytics Fact
```

---

## 38. Productivity Integration

可将财务事项映射为 Task：

- Pay Credit Card
- Reconcile Account
- Submit Reimbursement
- Review Monthly Budget

但 Task 完成不自动代表财务交易已经发生。

二者通过 Context / Relation 关联。

---

## 39. Notification

通知内容默认脱敏。

例如：

```text
A scheduled payment is due tomorrow
```

用户可自行配置显示金额，但锁屏默认尽量减少敏感信息。

---

## 40. Password Manager Integration

银行账号登录凭据、网银密码、银行卡 PIN 不存入 Accounting 字段。

Account 可以引用：

```text
credential_ref = secret://vault/...
```

UI 可提供：

```text
Bank Account
[Open Login Credential]
```

需要再次 Unlock Password Vault。

---

## 41. Private Notes Integration

Account 或 Transaction 可关联私密笔记，例如：

- 开户说明
- 报税备忘
- 合同说明

但反向关系默认遵循 Private Side Only Visibility。

---

## 42. Sharing

家庭账本可支持多用户。

权限至少：

```text
OWNER
EDITOR
VIEWER
```

可进一步控制：

- View Amount
- Edit Transaction
- Manage Account
- Export
- Manage Members

---

## 43. Audit

需要审计：

- Transaction Create / Update / Delete
- Account Edit
- Import
- Export
- Reconciliation
- Ledger Member Change
- Security Profile Change

Audit 不记录银行卡完整号码等 Secret。

---

## 44. Delete / Correction

财务数据不建议默认硬删除。

支持：

- Edit with Audit
- Void
- Trash
- Restore

已完成 Reconciliation 的交易被修改时必须明确提示，并可能使对账状态失效。

---

## 45. Timezone

交易必须保留：

- Local Date
- Local Time
- Timezone

跨时区旅行时不能仅用服务器时区解释交易日期。

---

## 46. Precision

金额不得使用普通浮点数作为财务真相。

详细数据库设计应采用可精确表达金额的小数/最小货币单位模型，并明确 Currency Minor Unit。

---

## 47. Privacy Analytics Boundary

Accounting 数据不得进入跨用户排行榜或所谓“消费健康评分”。

统计用于帮助用户理解自己的数据，不对用户进行价值判断。

---

## 48. Export

支持：

- Encrypted Backup
- CSV
- JSON
- Statement PDF（P1）

明文 Export 属于敏感操作。

---

## 49. Backup / Restore

Accounting 数据备份必须保留：

- Transaction Integrity
- Account Relationship
- Currency Metadata
- Exchange Rate Provenance
- Attachments
- Crypto Metadata

恢复后需要执行一致性检查。

---

## 50. P0

- Ledger
- Account
- Income / Expense / Transfer
- Category
- Tag
- Multi-currency
- Credit Card Basic
- Scheduled Transaction
- Transaction Template
- Receipt Attachment
- Search
- Dashboard
- Core Statistics
- Reconciliation
- CSV Import / Export
- Secure Data Integration

---

## 51. P1

- Budget
- OCR Assisted Entry
- Subscription View
- Advanced Analytics
- Shared Family Ledger
- Loan Tracking
- Asset Tracking
- Bank Format Import
- Smart Automation

---

## 52. P2

- Bank Provider Sync
- Advanced Forecast
- Investment Portfolio Integration
- More Financial Data Providers

第三方金融连接必须单独评估隐私、安全和地区合规，不作为 P0 前置依赖。

---

## 53. 非目标

P0 不目标于：

- 企业复式会计 ERP
- 自动报税
- 证券交易
- 银行支付执行
- 信贷审批
- 自动投资建议

---

## 54. 核心结论

Accounting 应成为 Ikaros 的个人数据体系之一，而不是孤立的账本 App：

```text
Accounting
├── Secure Data Foundation
├── Analytics
├── Automation
├── Productivity
├── Notification
├── Attachment
└── Password Manager Secret Reference
```

其核心是真实、可追溯、精确的交易事实，以及在严格隐私边界下生成的个人财务统计与复盘能力。
