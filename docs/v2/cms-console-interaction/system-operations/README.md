# System Operations — CMS Console Interaction Specification

## 1. System Health & Alerts

**Route:** `/console/ops/health`

### Header
- Title `System Health & Alerts`.
- Environment/server identity chip.
- Manual `Refresh` icon.
- Auto-refresh selector (`Off`, `30s`, `1m`, `5m`) when permitted.

### Overall status banner
Shows canonical overall health: Healthy, Degraded, Unhealthy, Maintenance. Include last checked timestamp and short reason summary. Status is computed by backend health model; the UI does not infer health from one chart.

### Service/component cards
Default cards/rows for:
- application/API;
- PostgreSQL;
- persistent storage backends;
- cache if enabled;
- search/indexing;
- task scheduler/worker queues;
- plugin/connector runtime;
- external providers that have health checks.

Each card fields: component, status, latency/response time when meaningful, last successful check, current incident count, short message, `Inspect`.

### Resource charts
Separate charts for CPU, memory, heap/runtime memory, disk/filesystem where meaningful, DB pool/connections, queue depth and request/error latency. Every chart labels unit and time range.

Time range chips: 15m, 1h, 6h, 24h, 7d. Missing samples render gaps.

### Alerts panel
Columns/list fields: severity, alert name, component, opened, current value/condition, state, owner/acknowledged by, actions.

Actions: Acknowledge, Silence where supported, Open related logs/metrics, Resolve only when manual resolution is part of alert model. Silence dialog requires duration and reason.

## 2. Scheduled Jobs

**Route:** `/console/ops/jobs`

### Job table
Filters: enabled, status, subsystem, schedule type, last result.

Columns:
- enabled switch;
- job name;
- owning subsystem;
- schedule/cron human-readable summary;
- next run;
- last run;
- last result;
- last duration;
- concurrency policy;
- actions.

System-owned jobs may have read-only schedule/enabled state.

### Job detail
Header actions: Run now, Edit schedule/config when allowed, Enable/Disable.

Tabs:
- Overview.
- Run History.
- Configuration.

Overview fields: job ID, description, owner subsystem, schedule expression + human interpretation, timezone, concurrency/misfire policy, timeout, retry policy, next run, dependencies.

Run History table: run ID, trigger source (`Schedule`, `Manual`, `Retry`), started, duration, result, processed count/progress, initiator, actions.

`Run now` dialog shows whether an instance is currently running and the concurrency consequence: reject, queue, parallel, replace according to backend policy. Do not start a duplicate silently.

Schedule editor fields: enabled, schedule type, cron/interval/calendar rule, timezone, misfire behavior, concurrency policy, timeout. Cron field includes human-readable preview and next five run times. Invalid expressions block save.

## 3. Background Tasks

**Route:** `/console/ops/background`

This page is the cross-system operational task registry for asynchronous work such as imports, backups, restores, migrations, integrity checks, report rebuilds and AI/integration jobs. Domain-specific detail remains owned by each subsystem.

### Header/KPI cards
Cards: Running, Queued, Failed in last 24h, Completed in last 24h, Long-running warnings.

### Task table
Filters: status, subsystem, type, initiator, date range, cancellable, failed only.

Columns:
1. task ID;
2. task type/name;
3. owning subsystem;
4. related entity safe label;
5. state (`Queued`, `Running`, `Waiting`, `Succeeded`, `Failed`, `Cancelled`);
6. progress indicator + percentage when measurable;
7. current stage;
8. started/queued time;
9. elapsed/duration;
10. initiator;
11. actions.

Row click opens task inspector side sheet.

### Task inspector
Sections:
- identity and owner subsystem;
- current state/stage;
- progress values (items/bytes/steps);
- queued/started/finished timestamps;
- parent/child task relationships;
- related entity links;
- safe event/log timeline;
- failure category/message;
- retry/cancel capability.

`Open in subsystem` is the preferred action and navigates to domain-specific job detail with richer information.

### Cancel behavior
Cancel appears only when backend marks task cancellable. Confirmation explains:
- which stage is active;
- whether already-completed work remains;
- whether cleanup/rollback runs;
- whether task can be retried later.

After request, state may become `Cancelling`; UI must not immediately label it Cancelled until server confirms.

### Retry behavior
Retry button appears only for eligible failed/cancelled tasks. Review dialog shows retry scope and whether it creates a new task ID. Original task remains in history.

### Bulk actions
No destructive bulk operation by default. Authorized admins may bulk cancel queued tasks or retry selected failures only after impact count is shown.

## Shared operations rules
- Auto-refresh never resets filters, selection, scroll position, expanded rows or open inspector.
- Live updates use SSE/WebSocket/polling according to implementation but present one consistent state model.
- Operational logs shown in UI are bounded, paginated/streamed and redacted; the page is not a raw arbitrary server-file viewer.
- Timestamps show local display timezone with exact timestamp tooltip and server/UTC context where useful.
- Failed health checks and tasks include error category plus concise human-readable summary; raw stack traces require separate privileged diagnostics view if provided.
- Actions that restart services, shut down server, purge queues, clear persistent state or perform other high-risk operations are not implied by this baseline spec; if added later they require dedicated high-risk workflows and documentation.
