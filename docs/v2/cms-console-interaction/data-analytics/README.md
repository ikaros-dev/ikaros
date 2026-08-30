# Data Analytics — CMS Console Interaction Specification

## 1. Personal Overview

**Route:** `/console/analytics`

### Header
- Title `Personal Analytics`.
- Period selector with presets: 7 days, 30 days, 90 days, year, custom.
- Compare toggle: previous period / previous year when data supports it.
- `Export report` secondary action.

### KPI cards
Default cards:
- resources added;
- consumption time/progress events;
- tasks completed;
- focus time;
- storage growth;
- automation/sync activity.

Each card shows current value, comparison delta and accessible textual interpretation. Clicking a card navigates to the owning analytics page filtered to the same period.

### Charts
- Activity over time.
- Content type distribution.
- Completion/progress trend.
- Storage growth.

Charts provide legend toggles, tooltip/focus values, and data-table alternative. Hidden series remain hidden only for current view unless user saves dashboard preferences.

## 2. Content Analytics

**Route:** `/console/analytics/content`

Filters: period, Resource type, collection, provider/source, tags, lifecycle, owner.

Cards: resources created, resources completed, favorites added, average progress, most active collection.

Sections:
- resource creation trend;
- consumption by type;
- top resources by activity;
- metadata source/conflict trend;
- collection growth.

Top-resources table columns: resource, type, views/opens or domain activity count, progress/completion, last activity, favorite state. Row opens Content & Creation detail.

## 3. Storage Analytics

**Route:** `/console/analytics/storage`

Cards: persistent bytes, logical attachment bytes, dedup savings, cache bytes (visually and semantically separated), archive bytes, integrity failures.

Charts:
- persistent bytes over time by tier;
- bytes by backend;
- attachment MIME/type distribution;
- archive/restore throughput;
- cache hit/eviction trend.

Tables:
- largest Resources by logical attachment size;
- largest Blobs;
- backends by utilization;
- integrity incidents.

Clicking backend/blob navigates to Attachment & Storage details.

## 4. Productivity Analytics

**Route:** `/console/analytics/planning`

Cards: planned tasks, completion rate, overdue rate, planned time, actual/focus time, habit completion.

Charts:
- completed tasks over time;
- planned vs actual time;
- projects by effort;
- deadline performance;
- focus session distribution.

Filter by project, task tag, goal and period. `View source tasks` navigates to Planning with matching filters.

## 5. System History

**Route:** `/console/analytics/system`

This page contains operational historical metrics, not security audit events.

Cards: uptime percentage, request volume if collected, error rate, background job throughput, sync success rate, storage health incidents.

Charts: CPU/memory where available, request latency, job duration, queue depth, DB/storage latency, connector availability.

Metric collection gaps render as `No data` intervals, not zero values.

## 6. Metrics Catalog

**Route:** `/console/analytics/metrics`

### Table columns
- metric key;
- display name;
- domain;
- type (counter/gauge/histogram/derived);
- unit;
- dimensions;
- retention;
- status;
- last sample;
- actions.

Metric detail displays definition, formula for derived metrics, source events/tables, aggregation window, dimensions/cardinality warning, retention policy and example visualization.

`Preview` opens time-series preview for selected period. Metric definitions that are system-owned are read-only; custom derived metrics, when supported, use formula editor with validation and sample evaluation.

## 7. Reports & Rebuild

**Route:** `/console/analytics/reports`

Tabs: Saved Reports, Scheduled Reports, Rebuild Jobs.

### Saved Reports
Columns: name, owner, scope, period rule, widgets/metrics count, updated, actions.

Report editor:
- name;
- description;
- default period;
- filters;
- widget list with metric, visualization, grouping, ordering;
- preview.

Drag reorders widgets. Invalid metric/filter combinations show inline errors before save.

### Scheduled Reports
Columns: report, cadence, recipients/destination, next run, last run, status. Editor supports schedule, timezone, output format and delivery target. Private domain data cannot be added unless report policy explicitly permits it.

### Rebuild Jobs
Used for rebuilding aggregates/materialized statistics after configuration/schema changes.

Columns: job ID, scope, time range, state, progress, rows/events processed, started, duration, initiator. `Start rebuild` dialog requires scope and date range and estimates impact where possible. Rebuild is asynchronous and does not blank existing analytics; affected dashboards may show `Rebuilding — data may be stale` banner.

## Shared analytics rules
- Always show timezone and aggregation granularity when interpretation depends on them.
- Comparison deltas distinguish percentage points from percent change.
- Missing data, suppressed private data and true zero are visually/textually distinct.
- Aggregation must respect current user's authorization; cross-user/admin analytics pages must explicitly state scope.
- Exports include applied filters, period, timezone and generation timestamp in metadata.
