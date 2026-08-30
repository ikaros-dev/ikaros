# Ikaros V2 CMS Console Interaction Specification

> Status: Draft
>
> This directory is the implementation source of truth for the Ikaros V2 CMS Console page structure and interaction behavior. The HTML/JS prototype under `docs/v2/prototypes/draft` is exploratory historical material only.

## 1. Purpose

This specification describes the CMS Console without relying on visual mockups. Every implementation should be derivable from the Markdown documents in this directory: what appears in each page region, which fields are visible, which Material Design 3 component is used, what happens when the user interacts with it, and how loading, empty, error, permission, validation, destructive, and background-task states behave.

The Console follows the V2 PRD principles: Resource-centric information architecture, Attachment/Blob separation, HTTP-first capabilities, user-owned metadata, explicit lifecycle semantics, composable permissions, and subsystem boundaries.

## 2. Global visual language

The Console uses **Material Design 3 (M3)** consistently.

- Desktop-first administration experience with responsive tablet/mobile fallback.
- Use M3 Navigation Drawer, Top App Bar, Cards, Data Tables, Tabs, Chips, Buttons, Icon Buttons, Menus, Dialogs, Bottom Sheets, Snackbars, Tooltips, Text Fields, Selects, Switches, Checkboxes, Radio Buttons, Progress Indicators, Banners and Empty States.
- Do not invent subsystem-specific component styles when an M3 component already expresses the state.
- Dense information pages may use compact density, but touch targets remain at least 44×44 CSS px.
- Primary action: Filled Button. Secondary action: Filled Tonal or Outlined Button. Low-emphasis action: Text Button/Icon Button.
- Destructive actions use error color and always require confirmation when data loss, permission revocation, key rotation, session termination, restore overwrite, or permanent deletion is possible.

## 3. Application shell

### 3.1 Permanent left navigation drawer

Desktop width: 280 px expanded. On smaller desktop widths it may collapse to a rail; tablet/mobile uses modal navigation drawer.

Top-to-bottom regions:

1. **Product identity row**
   - Ikaros mark.
   - Text `Ikaros Console`.
   - Optional environment chip such as `Production`, `Staging`, `Local`.
   - Clicking product identity navigates to `/console/dashboard`.
2. **Global quick actions**
   - Search icon/button opens global search.
   - Create button opens the global create menu when the current permission set has creatable resource types.
3. **Subsystem navigation groups**
   - Every menu group corresponds to exactly one subsystem directory in this specification.
   - **All subsystem groups are collapsed by default on a new browser/session state.**
   - Clicking a group header toggles only that group.
   - Group expansion state is persisted locally per browser/user and restored on later visits.
   - Entering a page from a deep link automatically expands its parent group so the active page is visible.
   - Expanding one group does not automatically collapse another; users may keep multiple groups open.
   - Group header shows icon, label and chevron. It is not itself a page unless explicitly documented.
   - Current page uses the M3 navigation active indicator.
   - Menu entries hidden by permission are not rendered. A direct URL without permission shows the standard 403 page.
4. **Bottom utilities**
   - Documentation link.
   - Theme selector (System / Light / Dark).
   - User avatar/menu.

### 3.2 Top app bar

Height follows M3 medium/compact desktop guidance depending on page density.

Left region:
- Optional drawer/rail toggle.
- Breadcrumb: `Subsystem / Page / Detail` where applicable.

Center/flexible region:
- Page title is normally rendered in page body, not duplicated in the bar.

Right region:
- Background-task indicator with running count.
- Notification icon with unread badge.
- Help icon when the page has subsystem documentation.
- User avatar.

Interactions:
- Background-task indicator opens a right-side drawer containing running/recent tasks; clicking a task navigates to its owning subsystem detail when available.
- Notification icon opens notification center preview; `View all` navigates to the full notification page.
- Avatar menu contains profile, security/session shortcut, appearance, language, and sign out.

## 4. Standard page anatomy

Unless a page explicitly overrides it, render regions in this order:

1. **Breadcrumb row**.
2. **Page header**
   - H1 title.
   - One-sentence scope description.
   - Optional status chips.
   - Primary and secondary actions aligned right on desktop, stacked under title on mobile.
3. **Context / KPI cards** when the page needs at-a-glance operational information.
4. **Filter/search toolbar**.
5. **Primary content** such as data table, cards, editor, chart, calendar, kanban, tree, or detail grid.
6. **Pagination / infinite loading controls** where required.
7. **Context drawer or dialog** for create/edit/inspect workflows that should not lose table context.

## 5. Common component interaction rules

### 5.1 Search fields

- Leading search icon; clear button appears when non-empty.
- Enter executes immediately.
- For server-backed lists, typing debounces for 300 ms if live search is enabled; otherwise explicit Enter/Search button is used.
- Query is reflected in URL parameters for shareable list state unless the query contains private decrypted content.
- Clearing restores unfiltered list without full page reload.

### 5.2 Filter chips and advanced filters

- Common high-frequency filters use Filter Chips.
- `More filters` opens a side sheet containing selects, date/time ranges, numeric ranges and switches.
- Applied advanced-filter count appears as a badge.
- `Reset` restores documented defaults.
- Filters that materially change URL-addressable list state persist in query parameters.

### 5.3 Data tables

- Header row remains sticky when the table scrolls vertically.
- Sortable column header toggles ascending → descending → none.
- Selection checkboxes appear only when bulk actions are available.
- Bulk action bar replaces/appears above the normal toolbar after at least one row is selected.
- Row click opens detail only when it does not conflict with text selection; explicit overflow menu remains available.
- Overflow menu contains row-specific actions and is permission-aware.
- Horizontal overflow must scroll; columns must not silently disappear without a responsive alternative.
- User-customizable column visibility may be stored locally when documented by that page.

### 5.4 Forms

- Required fields show required semantics, not only color.
- Validate on blur for field-level validation; validate all on submit.
- Server validation errors map back to fields when possible and also appear in an error summary banner.
- `Save` is disabled only when submission is impossible (e.g. required field empty) or request is in flight; do not disable merely because no changes are present unless a separate `No changes` state is obvious.
- Unsaved changes trigger navigation confirmation when leaving the page/drawer/dialog.
- Secrets are masked by default and never echoed back after save unless the backend explicitly supports retrieval.

### 5.5 Dialogs and side sheets

Use Dialog for focused confirmation or short forms. Use right-side sheet/drawer for medium-complexity create/edit/inspect tasks that benefit from retaining list context. Full-page editor is used for complex resource editors, document editors, permission matrices, automation builders, and security recovery flows.

### 5.6 Feedback

- Successful lightweight mutation: Snackbar, optionally with `Undo` for reversible actions.
- Validation failure: inline field errors + error summary.
- Background operation: progress indicator and background-task entry; the user may leave the page.
- Blocking operation: modal progress only when leaving would be unsafe.
- Error Snackbar includes `Retry` only when retry is idempotent/safe.

### 5.7 Loading, empty and error states

Every primary page must implement:

- Initial loading skeleton that preserves approximate layout.
- Refresh/loading indicator for subsequent fetches without blanking existing data.
- Empty state with explanation and a relevant primary action when the user can create/import data.
- Filtered-empty state with `Clear filters` action.
- Recoverable error state with retry.
- 403 permission state with requested capability and navigation back.
- 404 detail state when the entity no longer exists.

### 5.8 Destructive confirmations

Confirmation dialog contains:
- exact entity name/count;
- consequence in plain language;
- whether operation is reversible;
- affected dependent objects when relevant;
- destructive button label that names the action (`Delete permanently`, `Revoke session`, `Rotate key`).

For irreversible high-risk actions, require typed confirmation or re-authentication as specified by the subsystem.

## 6. Responsive behavior

- ≥ 1280 px: permanent expanded navigation drawer, multi-column page layouts.
- 960–1279 px: navigation rail or compact drawer; two-column layouts collapse where needed.
- 600–959 px: modal drawer; tables may use horizontal scrolling; detail side panels become full-width sheets.
- < 600 px: single-column layout; header actions move into overflow/stack; complex tables offer card/list representation only when documented.
- Console remains functional on mobile but desktop is the optimization target.

## 7. Accessibility and keyboard behavior

- Logical tab order follows visual reading order.
- Every icon-only action has tooltip and accessible name.
- Dialogs trap focus and restore focus to trigger on close.
- Escape closes non-destructive transient surfaces unless a save/critical operation is active.
- Enter submits simple forms; Ctrl/Cmd+Enter may submit editors when documented.
- Use semantic status text in addition to color.
- Charts provide textual summaries/table alternatives for essential values.

## 8. Navigation information architecture

| Subsystem | Directory | Pages |
|---|---|---|
| Workbench | `workbench/` | Dashboard, Global Search, My Activity & Favorites |
| Content & Creation | `content-creation/` | Unified Resource Library, Collections/Tags/Relations, Articles & Documents, Media Consumption, Sharing & Collaboration |
| Attachment & Storage | `attachment-storage/` | Attachments & Blobs, Persistent Storage Tiers, Cache & Downloads, Archive/Restore/Trash, Backup & Restore |
| Productivity & Planning | `productivity-planning/` | Inbox & Today, Projects & Tasks, Calendar & Time Blocks, Goals & OKR, Habits/Focus/Review |
| Personal Finance | `personal-finance/` | Ledger Overview, Accounts, Transactions, Budgets & Recurring Items, Reconciliation & Import |
| Private Notes | `private-notes/` | Vault, Versions & Sync Conflicts, Recovery & Export |
| Password Manager | `password-manager/` | Password Vault, Generator & Item Editor, Health & Secure Send, Devices & Access |
| AI Intelligence | `ai-intelligence/` | Assistant, Models & Providers, Personas, Context/Privacy/Memory, Jobs/Trace/Usage |
| Data Analytics | `data-analytics/` | Personal Overview, Content, Storage, Productivity, System History, Metrics Catalog, Reports & Rebuild |
| Integration & Automation | `integration-automation/` | Automation Rules, Executions & Traces, Events & Failure Queue, Import & Sync, Plugins & Connectors |
| Identity & Security | `identity-security/` | Users & Roles, Permission Matrix, Active Sessions, Authentication/Keys/Recovery |
| Platform Configuration | `platform-configuration/` | Parameters, Dictionaries, Menus |
| Communications & Audit | `communications-audit/` | Announcements, Notification Center & Delivery, Audit & Security Events |
| System Operations | `system-operations/` | System Health & Alerts, Scheduled Jobs, Background Tasks |

## 9. Cross-subsystem deep-link rules

- Resource references open Content & Creation resource detail.
- Attachment/Blob references open Attachment & Storage detail.
- User/session references open Identity & Security detail if authorized.
- Task/execution/background-job references open the subsystem that owns the job, not a generic opaque status page.
- Audit entries may link to referenced business entities but never expose fields the current user cannot read.
- Private Notes and Password Manager links never reveal decrypted titles/content in URL query strings, analytics events, browser history labels, or notification previews.

## 10. Document conventions for subsystem specs

Each subsystem document defines every page using these dimensions:

- Route and access capability.
- Page goal and default state.
- Header actions.
- Exact page regions and component types.
- Visible fields/columns/cards.
- Component-level interaction logic.
- Create/edit/detail workflows.
- Validation and destructive behavior.
- Loading/empty/error/permission states.
- Cross-subsystem navigation.

When this root document and a subsystem document conflict, the subsystem document wins only for that page-specific behavior; global safety, accessibility, and permission rules still apply.
