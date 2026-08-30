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

产品体验可参考 ezBookkeeping 的轻量、自托管、账户/交易/分类/标签/定期交易/对账/统计思路，并与 Ikaros 的 Analytics、Task、Automation、Notification、Attachment 和 Secret Reference 能力联动。

---

## 2. 安全边界

Accounting 包含隐私数据，但 **默认不是 Secure Data Domain**。

其普通业务数据使用平台正常持久化能力：

```text
Accounting
   ↓
PostgreSQL
Attachment / Blob
Analytics
```

包括：

- Account
- Transaction
- Category
- Tag
- Budget
- Scheduled Transaction
- Balance Snapshot
- Reconciliation
- Exchange Rate
- Report Aggregate

这些数据需要受到正常的：

- Identity
- Permission / ACL
- Audit
- Backup
- TLS
- Database / Storage 基础安全措施

保护，但不要求全部通过 Secure Data Foundation 做应用层密文落盘。

### 2.1 真正 Secret 不直接进入 Accounting

如果 Accounting 需要保存：

- 银行连接 Token
- Open Banking Credential
- 网银密码
- 卡片 PIN
- API Key
- OAuth Refresh Token

应使用：

```text
Secret Reference
      ↓
Password Manager / Credential Store
```

Accounting 仅保存安全引用：

```text
credential_ref = secret://vault/...
```

而不是复制 Secret 明文。

### 2.2 卡号与账户号

银行卡号、银行账号等字段属于高敏感标识，但不等于整个 Ledger 必须进入 Secure Data Foundation。

可根据详细设计采用：

- Masked Display Value
- Field-level Encryption
- Tokenized Reference
- Secret Reference

例如：

```text
account_name = 招商银行信用卡
masked_number = **** 1234
secret_ref = secret://vault/.../card-number
```

如果某场景只需要展示尾号，则业务数据库无需保存完整卡号明文。

### 2.3 不默认提供全账本 E2EE

V2 核心 Accounting 不以“全账本 USER_LOCKED_E2EE”为默认设计，因为这会直接限制：

- 服务器端统计
- 自动化
- 多端查询
- 预算计算
- 定期账生成
- Analytics
- Dashboard

如果未来确实需要“私密账本”模式，可作为独立扩展能力评估，但不能反向约束普通 Accounting 的核心领域模型。

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
- Project

Ledger 至少包含：

- Name
- Base Currency
- Owner
- Member / Permission
- Default Category Set
- Budget Configuration
- Archived Status

不同 Ledger 之间的数据默认隔离。

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
- LIABILITY
- OTHER

Account 典型字段：

```text
Account
├── id
├── ledger_id
├── name
├── type
├── currency
├── opening_balance
├── current_balance
├── institution
├── masked_identifier
├── credential_ref?
├── archived
└── metadata
```

完整银行凭据不应直接进入 Account 普通字段。

### 5.1 Credit Card

信用卡可扩展：

- Credit Limit
- Billing Day
- Due Day
- Statement Balance
- Available Credit

可通过 Notification / Automation 创建还款提醒。

### 5.2 Liability / Loan

贷款或负债账户可记录：

- Principal
- Outstanding Balance
- Interest Metadata
- Payment Schedule

V2 初期不要求实现完整金融计算引擎。

---

## 6. Transaction

Transaction 是账本中的核心业务事实。

类型：

```text
INCOME
EXPENSE
TRANSFER
ADJUSTMENT
```

典型字段：

```text
Transaction
├── id
├── ledger_id
├── type
├── account_id
├── target_account_id?
├── amount
├── currency
├── exchange_rate?
├── category_id?
├── payee_id?
├── occurred_at
├── note
├── status
├── source
└── attachment_refs
```

### 6.1 Transfer 是一个业务事实

转账应保持来源账户和目标账户之间的明确关系：

```text
Account A
   ↓
Transfer
   ↓
Account B
```

不要把 Transfer 简化成完全无关联的“一条支出 + 一条收入”。

这样才能正确支持：

- 转账修改
- 删除
- 对账
- 多币种
- 手续费
- 统计排除内部资金流动

### 6.2 Split Transaction

支持将一笔交易拆分到多个 Category：

```text
超市消费 300
├── 食品 180
├── 日用品 80
└── 其他 40
```

---

## 7. Category / Tag / Payee

### 7.1 Category

支持：

- Income Category
- Expense Category
- Hierarchical Category

例如：

```text
餐饮
├── 早餐
├── 午餐
├── 晚餐
└── 咖啡
```

### 7.2 Tag

Tag 用于跨分类分析，例如：

- 旅行
- 装修
- Ikaros
- 医疗
- 报销

### 7.3 Payee / Counterparty

Payee 表示交易对象，例如：

- 商户
- 公司
- 个人
- 平台

Payee 不应保存支付平台密码或登录 Credential。

---

## 8. Scheduled Transaction

支持周期账：

- 每日
- 每周
- 每月
- 每年
- 自定义 RRULE / 规则

典型场景：

- 房租
- 工资
- 订阅
- 贷款还款
- 保险

需要区分：

```text
Scheduled Transaction
= 计划规则

Transaction
= 已经发生的账务事实
```

定时规则到期后可以：

- 自动创建 Draft Transaction
- 提醒用户确认
- 在明确允许的规则下自动入账

---

## 9. Transaction Template

用户可建立常用模板，例如：

```text
咖啡
Account = 支付宝
Category = 餐饮/咖啡
Amount = 可留空
```

模板减少重复录入。

---

## 10. Reconciliation

对账用于确认 Ikaros 账面余额与外部实际余额一致。

```text
Reconciliation
├── account
├── statement_date
├── statement_balance
├── calculated_balance
├── difference
└── status
```

状态：

```text
OPEN
MATCHED
DIFFERENCE
CLOSED
```

不得通过静默修改历史交易来“自动让余额对上”。

必要时使用 Adjustment Transaction 并保留审计记录。

---

## 11. Budget

支持按：

- Ledger
- Category
- Tag
- Project
- 时间周期

建立预算。

例如：

```text
餐饮
Monthly Budget = 2000
```

需要展示：

- Budget
- Actual
- Remaining
- Usage Rate
- Forecast

预算本身不是账户余额。

---

## 12. 多币种

Ledger 有 Base Currency，但 Account / Transaction 可以使用不同币种。

```text
Ledger Base = CNY

USD Account
JPY Cash
EUR Transaction
```

Exchange Rate 需要记录 Provenance：

```text
ExchangeRate
├── from_currency
├── to_currency
├── rate
├── effective_at
├── provider
└── source_type
```

来源：

```text
MANUAL
EXTERNAL_PROVIDER
IMPORTED
```

历史报表不能因为今天汇率变化就无声改变过去账目的口径。

---

## 13. 导入

可逐步支持：

- CSV
- OFX
- QFX
- QIF
- 自定义映射
- 第三方记账软件导入

导入流程：

```text
Source
  ↓
Parse
  ↓
Normalize
  ↓
Deduplicate
  ↓
Preview
  ↓
Confirm
  ↓
Transaction
```

大批量导入必须支持预览与回滚策略。

### 13.1 Dedup

可使用：

- External Transaction ID
- Account
- Amount
- Time
- Counterparty
- Import Batch

辅助去重。

不能仅凭金额相同就判定重复交易。

---

## 14. Receipt / Attachment

交易可以关联：

- 收据
- 发票
- 账单 PDF
- 截图
- 合同

普通财务附件使用平台 Attachment / Blob 存储。

如果附件中包含真正需要 Secure Domain 保护的内容，可由用户显式存入 Private Notes / Secure Data，并通过 Relation 或安全引用关联，而不是让整个 Accounting Attachment 体系自动转为 Secure Blob。

---

## 15. 统计与 Analytics

Accounting 应深度接入 Data Analytics / Statistics 子系统。

典型指标：

### Cash Flow

```text
Income
Expense
Net Cash Flow
```

### Spending

- Category Distribution
- Tag Distribution
- Account Distribution
- Payee Distribution
- Daily / Weekly / Monthly Trend

### Asset / Liability

```text
Assets
Liabilities
Net Worth
```

### Budget

```text
Budget Usage
Remaining
Overspent
Forecast
```

### Comparison

- Month over Month
- Year over Year
- Rolling Average

统计事实应来源于 Transaction / Account 等业务事实，而不是把 Dashboard 结果回写成账本状态。

---

## 16. Dashboard

Accounting 首页可包含：

```text
Net Worth
Current Month Income
Current Month Expense
Cash Flow
Budget Progress
Upcoming Bills
Recent Transactions
Account Balances
```

敏感账号默认只显示 Masked Identifier。

---

## 17. Calendar

财务事件可以投射到 Calendar：

- 信用卡还款日
- 房租
- 订阅
- 工资
- 贷款还款
- 预算周期

Calendar Event 不直接取代 Transaction。

---

## 18. 与 Productivity / Planning 联动

例如：

```text
Credit Card Due Date
       ↓
Automation
       ↓
Create Task
“偿还信用卡”
       ↓
Notification
```

但：

> Task Completed ≠ Transaction Occurred

用户完成还款待办后，仍需要实际 Transaction 或外部同步事实确认账务变化。

---

## 19. 与 Automation 联动

事件可以包括：

```text
finance.transaction.created
finance.transaction.updated
finance.budget.threshold_reached
finance.bill.upcoming
finance.account.reconciled
finance.import.completed
```

Automation 可以：

- 发送通知
- 创建 Task
- 标记预算风险
- 创建 Draft Transaction
- 触发报表

但不能绕过 Accounting Command 直接修改账务表。

---

## 20. 与 Notification 联动

通知场景：

- 账单即将到期
- 预算达到 80% / 100%
- 导入完成
- 对账异常
- 周期账生成
- 外部同步失败

Notification 默认避免直接包含完整卡号、银行 Credential 等 Secret。

---

## 21. 与 Password Manager / Secret Reference 联动

Accounting 可以引用 Password Manager 中的 Secret：

```text
Accounting Account
       ↓
credential_ref
       ↓
Password Manager
```

典型用途：

- Bank Connector Credential
- API Token
- OAuth Refresh Token
- Complete Card Number
- PIN

优先支持：

```text
Use Secret
```

而不是：

```text
Reveal Secret
```

如果一个 Bank Connector 只需要 Token 完成 API 请求，则 Accounting 不需要得到 Token 的可展示明文。

---

## 22. AI 边界

AI 可以辅助：

- 交易分类建议
- 账单 OCR 后字段识别
- 月度财务摘要
- 支出趋势解释
- 预算建议
- 异常消费提示

但必须遵循：

```text
AI Suggestion
≠
Financial Fact
```

AI 不得：

- 自动修改已确认交易而不留下痕迹
- 获取 Password Manager 中的 Secret
- 将完整银行卡号、PIN、Credential 发送给通用模型

财务 AI 是否允许访问交易明细，应由用户和 AI Data Policy 控制。

---

## 23. Permission

至少区分：

```text
finance.read
finance.transaction.create
finance.transaction.update
finance.transaction.delete
finance.account.manage
finance.budget.manage
finance.import
finance.export
finance.admin
```

家庭共享 Ledger 可以给予不同成员不同权限。

---

## 24. Audit

关键操作进入 Audit：

- 创建 / 删除 Account
- 修改历史 Transaction
- 大批量导入
- 批量删除
- Reconciliation Adjustment
- Export
- Credential Reference 修改

Audit 不记录 Secret 明文。

---

## 25. Offline / Sync

客户端应支持离线记账：

```text
Offline Transaction
      ↓
Local Queue
      ↓
Reconnect
      ↓
Sync
```

冲突不能简单使用“最后写入覆盖一切”。

尤其：

- Transaction Update
- Reconciliation
- Balance Adjustment

需要明确冲突策略。

---

## 26. 非目标

V2 初期不定位为：

- 企业 ERP
- 企业总账系统
- 税务申报系统
- 银行核心系统
- 证券交易平台
- 全自动投资顾问

也不要求 Accounting 整体使用 Secure Data Foundation 或 Zero-knowledge E2EE。

---

## 27. 实施优先级

### P0

- Ledger
- Account
- Income / Expense / Transfer
- Category
- Tag
- Basic Search
- Monthly Statistics
- CSV Import / Export
- Audit
- Secret Reference 边界

### P1

- Budget
- Scheduled Transaction
- Transaction Template
- Reconciliation
- Multi-currency
- Receipt Attachment
- Calendar / Reminder
- Advanced Analytics

### P2

- External Bank Connector
- OCR
- AI Classification
- Subscription Detection
- Debt / Loan Enhancement
- Optional Advanced Privacy Mode

---

## 28. 结论

Accounting 的核心目标是：

> 保持账务模型清晰、可查询、可统计、可自动化，同时把真正的 Secret 隔离到专门的安全域中。

因此安全模型应是：

```text
普通财务业务事实
→ Normal Platform Storage

真正凭据 / Secret
→ Secret Reference
→ Password Manager / Secure Data Foundation
```

而不是：

```text
所有 Accounting 数据
→ 全量密文落盘
```

这使 Accounting 可以自然接入 Analytics、Automation、Task、Notification 和多端体验，同时不牺牲 Password、Token、PIN 等真正高敏感数据的安全边界。