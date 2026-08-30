# Communications & Audit — CMS Console Interaction Specification

## 1. Announcements

**Route:** `/console/communications/announcements`

### Header
- Title `Announcements`.
- Primary `New announcement`.
- Filters: status, audience, priority, scheduled/published date, author.

### Table
Columns:
- title;
- status (`Draft`, `Scheduled`, `Published`, `Expired`, `Archived`);
- priority;
- audience summary;
- publish/start time;
- expiry/end time;
- author;
- updated;
- actions.

### Announcement editor
Fields:
- title required;
- short summary;
- body/content editor;
- priority (`Normal`, `Important`, `Critical` where policy allows);
- audience selector (all users, roles, specific users/groups);
- start/publish datetime;
- expiry datetime optional but recommended;
- dismissible switch;
- require acknowledgement switch for critical notices;
- optional action button label + safe internal/external destination.

Preview mode renders desktop/mobile announcement presentation and audience summary.

Interactions:
- `Save draft` never publishes.
- `Publish now` opens review dialog with audience count, visibility period and acknowledgement behavior.
- `Schedule` validates start < expiry.
- Editing a published announcement creates an updated version and shows `Updated` state to recipients when material changes.
- Unpublish/archive confirmation states whether already acknowledged/read history remains.

## 2. Notification Center & Delivery

**Route:** `/console/communications/notifications`

Tabs: Notification Center, Delivery Rules, Delivery Log.

### Notification Center
This admin view may support both current-user notifications and authorized system-wide operational notifications. Scope selector is explicit (`Mine` / `All authorized`).

Filters: unread/read, source subsystem, priority, date, delivery state.

Notification row fields:
- source icon/subsystem;
- title;
- body preview according to sensitivity policy;
- priority;
- created time;
- read state;
- delivery-channel indicators;
- actions.

Actions: Mark read/unread, Archive, Open target. Bulk mark/archive supported.

Notification detail side sheet shows full safe body, target link, source event ID, created/delivered/read timestamps and delivery attempts when authorized.

Private Notes, Password Manager and sensitive Finance/Security notifications use generic redacted body text outside their protected domain.

### Delivery Rules
Rule table: event/category, audience/user scope, in-app enabled, email/webhook/other channels as configured, quiet-hours behavior, priority override, updated.

Rule editor:
- notification category/source;
- delivery channels;
- severity threshold;
- digest vs immediate;
- quiet hours/timezone;
- destination/account selector;
- sensitive-content policy.

`Send test` creates a clearly marked test notification and never includes real secret/private content.

### Delivery Log
Columns: notification ID, recipient, channel, destination safe label, status, attempts, last attempt, provider response category, actions.

Detail shows attempt timeline. Retry is available only for retry-safe deliveries. `Retry` does not recreate the underlying business event; it retries delivery of the existing notification.

## 3. Audit & Security Events

**Route:** `/console/communications/audit`

Tabs: Audit Log, Security Events.

### Audit Log
Filters:
- actor;
- subsystem;
- action/category;
- entity type;
- entity ID;
- result;
- date/time range;
- correlation/request ID.

Table columns:
1. timestamp;
2. actor (user/service/system);
3. action;
4. subsystem;
5. target safe label/type;
6. result;
7. source/device/network summary when policy allows;
8. correlation ID;
9. inspect action.

Audit detail side sheet:
- immutable audit ID;
- timestamp/timezone;
- actor identity and authentication context;
- action/capability;
- target references;
- before/after summary or changed field names when policy allows;
- request/correlation IDs;
- source metadata;
- result/error category;
- linked execution/job/security event.

Sensitive field values are redacted. Audit must record that a protected field changed without recording the secret itself.

Export audit action requires date range and filters, displays estimated row count, and may require elevated permission/re-authentication. Export metadata includes filter and generation timestamp.

### Security Events
Security event cards/table prioritize severity.

Columns:
- severity;
- event type;
- affected user/service;
- summary;
- detected time;
- state (`Open`, `Investigating`, `Resolved`, `Ignored/False positive`);
- actions.

Detail page sections:
- event summary;
- timeline;
- related sessions/devices/IP/network metadata according to policy;
- related audit events;
- recommended response actions;
- resolution notes.

Response actions may include revoke session, disable account, rotate credential, review plugin/connector. Each delegates to the owning subsystem workflow rather than duplicating security mutation logic inside Audit.

Changing event state requires optional/required resolution note depending on severity. Closing a critical event asks for confirmation that remediation was reviewed.

## Shared rules
- Audit records are immutable through the normal CMS UI.
- Notification read/archive state is mutable and is not equivalent to deleting source events.
- Security severity uses consistent labels: Info, Low, Medium, High, Critical (or backend-defined canonical set) with text/icon/color.
- External delivery destinations are shown in safe/masked form.
- Links from audit/security events respect target permissions; unavailable targets show `No longer available` rather than leaking content.
