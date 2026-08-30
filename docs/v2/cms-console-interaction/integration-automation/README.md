# Integration & Automation — CMS Console Interaction Specification

## 1. Automation Rules

**Route:** `/console/integration/automation`

### Header
- Title `Automation Rules`.
- Primary `New rule`.
- Secondary `Import rules` when supported.
- Filter Chips: enabled state, trigger type, action type, owner, last result.

### Rule table
Columns:
- enabled switch;
- name;
- trigger summary;
- condition summary;
- action summary;
- owner;
- last run status/time;
- next run when scheduled;
- actions.

Enabled switch saves immediately only when the rule is valid. Enabling an invalid rule opens editor with validation summary.

### Rule builder
Full-page editor with vertical stages:
1. Name and description.
2. Trigger.
3. Conditions.
4. Actions.
5. Error/retry policy.
6. Test and review.

Trigger cards support event, schedule, manual/API and connector-specific triggers. Selecting a trigger reveals only its schema-defined fields.

Condition builder uses nested AND/OR groups with Field / Operator / Value rows. Every field identifies source domain and type. Invalid comparisons are blocked.

Action list is ordered; drag handles reorder. Each action card shows connector/capability, configuration summary, retry behavior and delete action. Sensitive actions show warning banner describing data destination or mutation scope.

`Test rule` runs against a selected/sample event in dry-run where possible. Test result displays matched conditions, rendered action inputs and side-effect mode. A real side-effect test requires explicit opt-in.

Saving creates a version. Publishing/enabling a changed rule may show diff summary from previous active version.

## 2. Executions & Traces

**Route:** `/console/integration/executions`

### Table
Filters: rule, connector, status, date, event type, correlation/trace ID.

Columns: execution ID, rule, trigger/event, status, started, duration, attempts, action count, initiator/source, actions.

Statuses: Queued, Running, Succeeded, Partially Succeeded, Failed, Cancelled.

### Execution detail
Header shows execution ID, rule link, status, started/duration, retry action when eligible.

Timeline/tree stages:
- trigger received;
- condition evaluation;
- each action;
- retries/backoff;
- completion.

Each stage expands to safe inputs/outputs, duration, connector response metadata and error category. Credentials, authorization headers and protected content are redacted.

`Retry failed actions` clearly states whether successful actions will be repeated. Default should retry only safe/failed portions when execution model supports it; otherwise require review.

## 3. Events & Failure Queue

**Route:** `/console/integration/events`

Tabs: Event Stream, Failure/Dead-letter Queue.

### Event Stream
Filters: event type, source subsystem, entity type, correlation ID, date, delivery state.

Columns: timestamp, event type, source, entity safe identifier, correlation ID, subscriber/delivery summary, status.

Event detail shows headers/metadata and payload according to permission/redaction policy. `Replay` is available only for replay-safe event types and always opens a review dialog listing subscribers/actions that may run.

### Failure queue
Columns: failure ID, event/action, destination, reason category, attempts, first/last failure, next retry, state, actions.

Actions: Retry now, Reschedule, Inspect, Discard. Discard requires confirmation and records reason. Bulk retry supports a maximum count preview and rate warning.

## 4. Import & Sync

**Route:** `/console/integration/sync`

Tabs: Sources, Sync Jobs, Import Jobs, Conflicts.

### Sources
Cards/table: source name, connector/provider, data domain, account/endpoint safe label, enabled, last sync, next sync, health, actions.

Source editor fields are connector-schema driven: endpoint, identity, secret credentials, scopes, schedule, conflict policy and mapped collections/tags. `Test connection` precedes enable where practical.

### Sync jobs
Columns: job ID, source, mode (incremental/full), state, discovered, created, updated, skipped, conflicts, failures, started/duration.

Job detail presents progress stages and per-entity result table. `Cancel` only appears while backend can safely stop.

### Conflicts
Rows identify target Resource/metadata field, local/manual value provenance, incoming source value, policy, detected time. Resolution actions: Keep local, Accept source, Merge/Edit, Change rule. Manual/user-owned metadata is visually prioritized and never silently overwritten.

### Import jobs
Wizard pattern follows domain-specific imports but always includes source selection, preview, mapping, validation, duplicate policy, review and asynchronous commit.

## 5. Plugins & Connectors

**Route:** `/console/integration/plugins`

Tabs: Installed, Connectors, Marketplace/Available when supported.

### Installed plugins
Cards/table fields: name, icon, version, publisher/source, enabled, capabilities, permissions, health, update state, actions.

Plugin detail tabs: Overview, Configuration, Permissions, Routes/Capabilities, Logs, Updates.

Configuration renders plugin-declared schema with standard M3 fields. Secret fields are masked. Unknown/untrusted HTML is not rendered as configuration UI.

Permission page lists requested capabilities/data domains with risk descriptions. Expanding permissions or enabling a plugin may require admin confirmation/re-authentication.

Disable dialog explains which routes, jobs, automations or metadata sources stop working. Uninstall previews dependent rules/config/data and offers export when supported.

### Connectors
Connector instances table: name, plugin/provider, account/endpoint, scope, enabled, health, last activity, actions. Multiple instances per provider are supported when backend permits.

## Shared rules
- Every external data transfer shows destination identity and sensitivity consequence where relevant.
- Secret values are never shown after save.
- Execution/event pages distinguish technical retry from business replay.
- Rule, plugin and connector changes create audit events.
- A plugin cannot make a hidden navigation item appear unless current user also has the required permission.
