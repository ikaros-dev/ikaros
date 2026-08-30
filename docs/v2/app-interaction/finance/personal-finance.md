# Finance：个人财务与记账

## 1. 页面目录

- Finance Dashboard。
- Ledger 切换与管理。
- Account 列表 / 详情。
- Transaction 列表 / 详情。
- Quick Add Transaction。
- Split Transaction。
- Scheduled Transaction。
- Transaction Template。
- Budget。
- Reconciliation。
- Import Preview。
- Finance Insights。

普通财务数据属于正常业务域；银行密码、PIN、API Token 等真正 Secret 通过 Password Manager / Secret Reference 使用，不复制明文到 Finance 表单。

---

## 2. Finance Dashboard

### 2.1 App Bar

- 当前 Ledger Selector。
- Search。
- Date Range。
- More。

主 FAB：`记一笔`。

### 2.2 Compact 页面顺序

1. Net Worth Card。
2. 本月 Income / Expense / Net Cash Flow 三指标。
3. Account Balance 横向列表。
4. Budget Progress。
5. Upcoming Bills。
6. Cash Flow Mini Chart。
7. Recent Transactions。

### 2.3 Net Worth Card

字段：

- Net Worth。
- Assets。
- Liabilities。
- Base Currency。
- 与上期变化。

金额可通过 App 级 `隐藏金额` 快捷按钮统一遮挡为 `••••`，但不改变数据权限。

### 2.4 Account Balance Card

- Account Icon。
- Name。
- Masked Identifier，例如 `•••• 1234`。
- Current Balance。
- Currency。
- Credit Card 可显示 Available Credit / Statement Balance。

Tap 进入 Account Detail。

---

## 3. Ledger Selector

Bottom Sheet / Menu：

- Ledger Name。
- Base Currency。
- Owner / Shared 标识。
- 当前 Ledger Check。

底部：`管理账本`。

Ledger 管理页：Name、Base Currency、Members / Permission、Archived Status。

切换 Ledger 后所有 Account / Transaction / Budget 查询切换到该账本，不把多个 Ledger 金额无意义混加。

---

## 4. Account 列表

### 4.1 Group

可按类型分组：Cash、Bank、Credit、E-wallet、Investment Tracking、Loan / Liability、Other。

### 4.2 Account Row

- Type Icon。
- Name。
- Masked Identifier。
- Currency。
- Current Balance。
- Institution。
- Archived Chip。
- More。

### 4.3 Create Account 字段

1. Name *。
2. Type *。
3. Currency *。
4. Opening Balance。
5. Institution。
6. Masked Identifier。
7. Optional Secret Reference：`选择已保存凭据`，只展示 Vault Item 名称 / 安全引用，不展示 Secret。
8. Type-specific Fields。

Credit Card：Credit Limit、Billing Day、Due Day。

Loan：Principal、Outstanding Balance、Payment Metadata。

---

## 5. Account Detail

Header：Name、Masked ID、Balance、Currency、Institution、Edit。

Sections：

- Balance Trend。
- Recent Transactions。
- Upcoming Scheduled Transactions。
- Reconciliation Status。
- Related Receipts / Attachments。

Credit Card 额外显示：Credit Limit、Available Credit、Statement Balance、Billing Day、Due Day。

---

## 6. Transaction 列表

### 6.1 顶部

- Search。
- Date Range。
- Filter。
- Sort。

Filter：Type、Account、Category、Tag、Payee、Currency、Source、Status。

### 6.2 Transaction Row

- Category Icon。
- Payee / Title。
- Category。
- Account。
- Occurred At。
- Amount。
- Currency。
- Type Icon。
- Receipt Indicator。
- Imported / Manual Source（详情中优先，异常时可显示）。

金额视觉：Income / Expense 使用正负号与 Icon，不仅靠绿色 / 红色。

Transfer Row 同时显示 `Account A → Account B`，不伪装成一条支出。

---

## 7. Quick Add Transaction

### 7.1 顶部 Type

`SegmentedButton`：

- 支出。
- 收入。
- 转账。
- 调整（仅高级 / 有权限）。

### 7.2 大金额输入

页面顶部大号 Amount Field：

- Currency Selector。
- Decimal Keyboard（Mobile）。
- 支持简单计算表达式可作为增强，不是 P0 必须。

### 7.3 支出 / 收入字段

1. Account *。
2. Category。
3. Payee。
4. Occurred At *。
5. Tags。
6. Note。
7. Receipt / Attachment。
8. `拆分分类`。
9. Source / Import 信息只读（编辑已导入项时）。

### 7.4 Transfer 字段

1. From Account *。
2. To Account *。
3. Amount *。
4. Currency / Exchange Rate（跨币种）。
5. Fee（可选）。
6. Occurred At。
7. Note。

From 与 To 不能相同；选择后即时校验。

### 7.5 保存

- `保存` Filled Button。
- `保存并再记一笔` 放 More。
- Offline：保存本地 Pending Transaction 并明确显示待同步。

---

## 8. Split Transaction

Sheet / 独立页：

顶部显示总金额 `300.00` 与剩余待分配金额。

每行：

- Category。
- Amount。
- Note 可选。
- Delete。

底部：`添加拆分`。

只有 `所有拆分金额之和 = 总金额` 才允许保存；差额实时显示。

---

## 9. Transaction Detail

字段：

- Type。
- Amount / Currency。
- Account / Target Account。
- Category。
- Payee。
- Occurred At。
- Tags。
- Note。
- Receipt Preview。
- Source。
- Import Batch（适用时）。
- Created / Updated。

操作：Edit、Duplicate、Create Template、Share Receipt（仅附件权限允许）、Delete。

修改历史 Transaction 进入 Audit 的事实可在 UI 提醒“此修改会保留审计记录”。

---

## 10. Scheduled Transaction

列表按 Upcoming Date 排序。

Card：

- Name / Template。
- Next Occurrence。
- Frequency。
- Account。
- Amount（可能为空）。
- Mode：Create Draft / Notify / Auto Post（策略允许时）。
- Enabled。

编辑字段：Recurrence、Start / End、Template、Action Mode、Reminder。

Scheduled Transaction 与已经发生的 Transaction 使用不同 Icon 和页面标题。

---

## 11. Transaction Template

Card：Name、Type、Account、Category、Default Payee、Default Amount（可空）。

点击 Template 直接打开预填 Quick Add；用户仍需确认实际发生时间 / 金额。

---

## 12. Budget

### 12.1 Budget 首页

Period Selector：Month / Quarter / Custom。

Summary：Total Budget、Actual、Remaining、Overspent Count。

### 12.2 Budget Card

- Name / Category / Tag。
- Budget Amount。
- Actual。
- Remaining。
- Usage Rate Progress。
- Forecast。
- Period。

达到 80% / 100% 用 Warning / Error Icon + 文本说明，不只改变进度条颜色。

### 12.3 Create Budget

字段：Scope（Ledger / Category / Tag / Project）、Target、Amount、Period、Carry-over Policy（若支持）、Alert Threshold。

---

## 13. Reconciliation

### 13.1 列表

Account、Statement Date、Calculated Balance、Statement Balance、Difference、Status。

### 13.2 New Reconciliation

1. Account。
2. Statement Date。
3. Statement Balance。
4. System Calculated Balance 只读。
5. Difference 自动计算。

若 Difference != 0：

- 显示差额。
- `查看未核对交易`。
- `创建 Adjustment`，必须用户明确确认。

禁止按钮“自动修正历史交易”。

---

## 14. Import

### 14.1 Import Source

- CSV。
- OFX / QFX / QIF（能力支持时）。
- Plugin Provider。

### 14.2 Mapping

字段映射表：Source Column → Ikaros Field，支持日期格式、金额正负规则、Currency。

### 14.3 Preview

表格列：Date、Account、Amount、Type、Payee、Category、Duplicate Status、Error。

顶部统计：Total、Ready、Possible Duplicate、Invalid。

Possible Duplicate 展开显示匹配依据，不只写“重复”。

### 14.4 Confirm

用户可排除行，然后 `导入 N 条`。

大批量导入创建 Background Task；完成后 Notification + Import Batch Detail。

---

## 15. Receipt / Attachment

添加方式：相机拍摄、相册、文件。

Upload / OCR 可后台执行。

AI / OCR 建议字段显示 Candidate：Amount、Date、Payee、Tax 等；用户确认后才写 Transaction。

真正高敏感附件可由用户转存 Secure Domain，并使用安全引用关联。

---

## 16. Finance Insights

入口：Dashboard `查看分析`。

Tabs：Cash Flow、Spending、Net Worth、Budget。

统一 Time Range + Comparison。

图表必须有文本摘要 / 数据表入口。

AI 可解释趋势，但显示 `AI 解释` 标识，不能生成不存在的金额。

---

## 17. Secret 使用

Account 中存在 `credential_ref` 时：

- 显示 `已连接安全凭据：Bank API`。
- 操作优先 `使用凭据重新连接`。
- 不显示 `Reveal Token` 作为默认动作。
- 需要 Reveal 时跳 Password Manager，并遵循 Vault Unlock / Step-up。

---

## 18. 隐私

- Account Identifier 默认 Masked。
- Notification 默认不显示完整卡号 / Secret。
- App Switcher 金额隐藏可由用户设置，但这不是加密边界。
- 导出账本属于敏感动作，可要求 Step-up。

---

## 19. Offline

- Offline 可新增 Transaction。
- 查看已缓存 Ledger / Account / Recent Transactions。
- 编辑未同步的本地 Transaction。
- 重新联网后同步 Queue。

Reconciliation / Balance Adjustment 等高冲突操作离线时可禁用或创建明确 Draft，不静默在重连后覆盖服务端事实。

---

## 20. 响应式

- Compact：Dashboard 单列；Quick Add 全屏 / Bottom Sheet；Transaction 列表卡片化。
- Medium：Dashboard 2 列；账户与交易可 Master Detail。
- Expanded：12 列 Dashboard；Transaction 使用 Data Table + Detail Pane。
- Large：Chart 与 Table 并排，但金额列右对齐、列宽稳定。
