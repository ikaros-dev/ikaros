# Personal Finance — CMS Console Interaction Specification

> Financial data is private user data. The UI must avoid leaking amounts, payees, notes, account identifiers, or imported raw rows into URLs, analytics events, notifications, or logs beyond the authorized finance subsystem.

## 1. Ledger Overview

**Route:** `/console/finance`

### Header
- Title `Ledger Overview`.
- Ledger selector when multiple ledgers exist.
- Date period selector defaults to current month.
- Primary `Add transaction`.
- Secondary `Import`.

### KPI cards
- Net worth / selected-ledger balance.
- Income in period.
- Expenses in period.
- Net cash flow.
- Budget usage when budgets exist.

Amount cards provide `Hide amounts` eye toggle. Hidden state persists per user/device and masks all finance amounts on this page until restored.

### Main regions
1. Cash-flow chart with Income / Expense / Net toggle.
2. Spending by category card with top categories and `View details`.
3. Account balances card.
4. Recent transactions table.
5. Upcoming recurring transactions / budget warnings.

Chart interactions: hover/focus shows period + values; selecting a segment applies that date/category filter to Transactions. Essential chart data has table alternative.

## 2. Accounts

**Route:** `/console/finance/accounts`

### Table columns
- account name;
- type;
- masked account identifier;
- currency;
- current balance;
- included in net worth switch/status;
- reconciliation state;
- updated;
- actions.

Primary `Add account` opens side sheet.

Account fields:
- name required;
- account type (cash, bank, e-wallet, credit, investment/manual other);
- currency required;
- opening balance/date;
- institution optional;
- masked identifier optional;
- include in net worth;
- notes;
- archive state.

Account detail tabs: Overview, Transactions, Reconciliation, Settings.

Overview cards: current balance, cleared balance, period inflow/outflow, last reconciled. Balance history chart below.

Archive account prevents new normal postings unless explicitly restored, but preserves transactions. Delete is unavailable while transactions exist unless a dedicated migration/delete workflow resolves them.

## 3. Transactions

**Route:** `/console/finance/transactions`

### Toolbar
Search is limited to authorized finance fields. Filters: date range, account, transaction type, category, amount range, payee, tag, source/import batch, reconciliation state. Sort defaults to date descending.

### Table columns
1. selection;
2. date/time;
3. transaction type (`Expense`, `Income`, `Transfer`);
4. account;
5. destination account for transfers;
6. category;
7. payee;
8. memo indicator;
9. amount + currency;
10. source (`Manual`, `CSV Import`, connector);
11. reconciliation/cleared state;
12. actions.

Amount sign/color is accompanied by transaction type text/icon; color alone is insufficient.

Bulk actions: set category, add tag, mark cleared, exclude from reports where supported, delete with confirmation.

### Add/edit transaction sheet
Common fields:
- type required;
- date/time required;
- account required;
- amount > 0 required;
- currency inherited from account unless conversion is supported;
- category required by policy but may allow `Uncategorized`;
- payee;
- memo;
- tags;
- receipt attachment.

For Transfer:
- source account;
- destination account required and different from source;
- source amount;
- destination amount/exchange rate when currencies differ;
- transfer creates linked ledger entries but presents as one logical transfer in UI.

Validation prevents invalid negative/zero amount representation; sign is derived from transaction type.

Deleting a transfer explains that both linked sides are affected. Editing one side edits the logical transfer.

## 4. Budgets & Recurring Items

**Route:** `/console/finance/budgets`

Tabs: Budgets, Recurring.

### Budgets
Period selector and budget cards/table. Fields: category/group, budgeted amount, actual amount, remaining, utilization progress, rollover state, forecast.

Create budget dialog:
- category/group;
- amount;
- period (monthly/custom);
- start date;
- rollover switch;
- warning thresholds.

Clicking actual amount navigates to Transactions filtered by budget category and period.

Over-budget uses warning/error semantics but never blocks transaction entry.

### Recurring items
Columns: name, type, account, category, amount/rule, cadence, next occurrence, auto-create mode, status, actions.

Recurring editor fields: template transaction fields, recurrence rule, start/end, next date, auto-create vs reminder, tolerance/variable amount notes. Recurrence preview lists next five occurrences.

Pausing preserves history and stops future generation. Deleting the recurring rule never deletes generated transactions.

## 5. Reconciliation & Import

**Route:** `/console/finance/reconcile`

Tabs: Reconciliation, Import Batches.

### Reconciliation
Account selector + statement ending date + statement ending balance.

Main split layout:
- left: unreconciled transactions with checkboxes;
- right: reconciliation summary showing opening balance, selected cleared total, calculated ending balance, statement balance, difference.

`Complete reconciliation` enabled only when difference is zero or policy explicitly permits adjustment. If adjustment is needed, create an explicit adjustment transaction with review step; never silently alter balances.

Completed reconciliation is immutable by normal edit. Reopen requires elevated confirmation and audit entry.

### Import wizard
Step 1 Upload/source: CSV/file/connector selection.
Step 2 Mapping: preview rows and map source columns to date, amount, type, account, payee, category, memo, external ID.
Step 3 Parsing rules: date format, decimal separator, currency, debit/credit interpretation.
Step 4 Deduplication: show candidate duplicate count and matching rule.
Step 5 Review: valid, warning, rejected rows in tabs.
Step 6 Commit import.

Every preview row shows source row number and normalized transaction. Mapping changes recompute preview. Rejected rows show actionable reason.

Import batch detail after commit: imported count, skipped duplicates, rejected, created transactions, mapping profile, source checksum, rollback availability.

Rollback, when supported, only removes transactions created by that import and checks whether they were edited/reconciled afterward; conflicts require manual review.

## Shared interaction and security rules
- Amount masking is available on overview/list/detail surfaces.
- Currency always accompanies ambiguous amounts.
- Export requires explicit scope/date and may require re-authentication for large/private exports.
- Finance data is excluded from generic global notification body text unless the user explicitly enables detailed finance notifications.
- Destructive operations describe accounting impact, not just record deletion.
