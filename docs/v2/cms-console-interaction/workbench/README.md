# Workbench — CMS Console Interaction Specification

## Scope

Workbench is the daily entry point. It surfaces cross-system status without becoming a second copy of every subsystem.

## 1. Dashboard

**Route:** `/console/dashboard`

### Header
- Title `Dashboard`.
- Subtitle `Your Ikaros workspace at a glance.`
- Right actions: `Customize`, `Refresh` icon.
- `Customize` opens a right-side sheet where widgets can be enabled/disabled and reordered by drag handle; save order per user.

### Region A — Welcome and system context
Use an M3 elevated card spanning the page width.

Fields/components:
- Greeting using display name.
- Current server/environment chip.
- Last successful refresh timestamp.
- Optional warning chips for degraded system health, pending security action, failed sync, or backup warning.

Interaction:
- Warning chip navigates to the owning subsystem filtered to the relevant incident.
- Refresh keeps current widgets visible and shows linear progress at top; do not blank the page.

### Region B — KPI card row
Default cards:
1. `Resources` — total resource count; secondary text `+N in last 7 days`.
2. `Storage` — used / provisioned persistent capacity; progress bar.
3. `Today` — planned tasks / completed tasks.
4. `Background work` — running / failed jobs.
5. `Notifications` — unread count.

Each card is clickable only when a useful destination exists. Card hover/focus exposes the destination through tooltip/accessibility label.

### Region C — Continue / recent activity
Two-column desktop grid.

**Continue card** shows up to 6 resumable items:
- thumbnail/icon;
- resource title;
- resource type chip;
- progress percentage / current episode or chapter;
- last activity time;
- `Resume` action.

**Recent activity card** shows timestamped rows:
- activity icon;
- short action text;
- linked entity;
- timestamp.

`View all` opens My Activity & Favorites.

### Region D — Operational attention
Cards for items needing action, hidden when zero:
- metadata conflicts;
- failed automation/sync executions;
- expiring share links;
- storage/archive restore pending;
- security/session warnings.

Each row shows severity, title, owning subsystem, age, and `Review`.

### Empty/loading/error
- First-run empty dashboard keeps KPI skeletons and shows onboarding card with `Import content`, `Configure storage`, `Connect provider` shortcuts.
- A failed widget does not fail the whole dashboard; render widget-local error with `Retry`.

## 2. Global Search

**Route:** `/console/search`

### Header and search surface
- Large M3 Search Bar under title.
- Query field supports plain keyword search without AI dependency.
- Leading search icon; trailing clear icon; optional keyboard shortcut hint `Ctrl/Cmd + K`.

### Filter row
Filter Chips:
- Resource type.
- Subsystem/domain.
- Lifecycle status.
- Owner.
- Updated date.
- Favorites only.

`More filters` opens side sheet for source/provider, tags, collection, attachment type, storage state and advanced metadata fields.

### Results layout
Top summary line: `N results in X ms` and sort selector (`Relevance`, `Updated`, `Created`, `Title`).

Results grouped by domain only when `Group by subsystem` toggle is on. Default is a unified ranked list.

Resource result fields:
- type icon/thumbnail;
- primary title;
- alternate title if any;
- short highlighted metadata snippet;
- collection/tag chips;
- lifecycle/status chip;
- last updated;
- favorite toggle.

Non-resource result fields vary by domain but always show domain label and safe identifying fields.

### Search interaction rules
- Enter submits query and updates URL `q=` except decrypted private queries.
- Search history is local/user-scoped and can be disabled in settings.
- Arrow keys move through suggestions; Enter opens selected suggestion.
- Clicking a result opens detail in same tab; Ctrl/Cmd-click follows browser new-tab behavior.
- Favorites icon updates optimistically and rolls back on server failure.

### Private-domain behavior
Private Notes and Password Manager never return decrypted content into global search while vault is locked. Instead show a secure placeholder row such as `Private Notes — unlock to search`, with `Unlock` action. Search terms are not copied into telemetry.

## 3. My Activity & Favorites

**Route:** `/console/activity`

### Header
Actions: `Export my activity` when permitted, `Clear local history` for local-only UI history.

### Tabs
- `Activity` default.
- `Favorites`.
- `Progress`.

### Activity tab
Toolbar filters: action type, resource type, date range, subsystem.

Timeline rows contain:
- action icon;
- action verb and linked entity;
- contextual metadata such as collection/project;
- device/source when useful;
- timestamp.

Activity is business/user activity and must not be mixed with administrative audit events.

### Favorites tab
Toggle between compact table and card grid.

Table columns:
- resource/title;
- type;
- collection;
- progress;
- last opened;
- favorite date;
- actions.

Bulk actions: remove favorite, add to collection, export selection where supported.

### Progress tab
Sections by media/reading/task semantics. Each row shows item, current position, percentage, last consumed time, and `Resume`/`Reset progress` menu action.

Reset progress requires confirmation describing the affected progress only, not the Resource itself.

### States
- Empty favorites: explanation + `Browse Resource Library`.
- Empty filtered activity: `Clear filters`.
- Pagination uses cursor-based `Load more` or numbered pages consistently with backend capability; never mix within one tab.
