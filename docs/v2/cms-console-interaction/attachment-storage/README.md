# Attachment & Storage — CMS Console Interaction Specification

## 1. Attachments & Blobs

**Route:** `/console/attachments`

### Header
- Title `Attachments & Blobs`.
- Subtitle explains `Resource → Attachment → Blob → Placement`.
- Primary `Upload attachment`.
- Secondary `Scan/import`.

### KPI cards
- Attachments.
- Unique Blobs.
- Deduplication saved bytes.
- Missing/unavailable Blobs.
- Integrity warnings.

### Filters
Search by attachment name, checksum, blob ID or linked Resource. Chips: attachment role/type, MIME family, availability, original/derived, integrity state, storage tier. Advanced filters: size range, created range, checksum algorithm, reference count, orphan state.

### Table
Columns:
1. selection;
2. type icon/preview;
3. attachment display name;
4. role (`VIDEO`, `AUDIO`, `SUBTITLE`, `COVER`, `PAGE`, `DOCUMENT`, etc.);
5. linked resource count;
6. blob short ID + checksum status;
7. logical size;
8. placement summary (`2 replicas · Hot/Warm`);
9. origin (`Original`, `Derived`);
10. availability/integrity chip;
11. updated;
12. actions.

Click opens attachment detail.

### Attachment detail
Header: display name, role, MIME, availability. Actions: Preview/Open, Download, Replace relationship-safe content if allowed, Create derivative, overflow.

Tabs:
- Overview.
- Resource Links.
- Blob.
- Derivatives.
- Activity.

Overview fields: attachment ID, filename/display name, MIME, size, role, created, creator/source, original/derived, metadata, accessibility state.

Resource Links table: Resource, relationship role, primary flag, created. `Link resource` searches resources and selects attachment relation role. Removing a link must state whether the attachment remains referenced elsewhere.

Blob tab: blob ID, checksum algorithm/value, byte size, verification time, reference count, placement list. `Verify integrity` starts background job.

Derivatives shows parent/child graph (e.g. source video → thumbnail/transcode/subtitle extraction) with generation status and recipe/tool identity when available.

## 2. Persistent Storage Tiers

**Route:** `/console/storage/tiers`

### Overview
Cards for Hot, Warm, Cold/Archive and total persistent bytes. Each card shows used/capacity, object count, healthy/degraded status and provider/backend.

Important language: cache is not a persistent storage tier.

### Storage backend table
Columns: name, backend type, tier, endpoint/location (safe display), capacity/used, health, read/write state, replica count, last check, actions.

`Add storage backend` side sheet fields:
- name required;
- backend/provider type;
- tier;
- endpoint/bucket/path as applicable;
- credentials secret fields;
- encryption/config switches;
- read/write mode;
- health-check options.

`Test connection` validates before save without persisting secret values in UI logs. Saved secrets return masked placeholders only.

### Backend detail
Tabs: Overview, Placements, Policies, Health, Activity.

Overview shows configuration-safe fields, capacity chart, latency/error stats. Placements table lists blobs on this backend with size, replica state, last verified.

Policies page section defines placement/migration rules with priority ordering and dry-run preview. Rule fields: matching condition, target tier/backend, minimum replicas, age/access conditions, action. `Simulate` reports matching objects and estimated bytes before enabling.

## 3. Cache & My Downloads

**Route:** `/console/storage/cache`

Tabs: Server Cache, Client Downloads.

### Server Cache
Cards: cache bytes, object count, hit ratio, eviction activity. Table: blob/resource, cache type, size, last access, expires/eviction eligibility, origin persistent placement.

Actions: Evict selected, Warm cache / Prefetch. Eviction must explicitly say persistent data is unaffected.

### Client Downloads
Columns: user/device, resource, attachment, size, state, downloaded at, last sync. Admins see only metadata required for management; no client-local private path unless explicitly reported and authorized.

Remote removal request, if supported, is labeled `Request client removal` rather than implying immediate deletion.

## 4. Archive, Restore & Trash

**Route:** `/console/storage/archive`

Tabs: Archived Resources, Restore Queue, Trash.

### Archived Resources
Columns: Resource, type, archive date, archive storage summary, logical size, retrieval estimate/status, retention policy, actions.

`Restore` dialog:
- target restore tier/backend;
- estimated bytes;
- affected attachments;
- expected asynchronous behavior;
- optional `Open when ready` notification.

Submission creates background task and immediately returns to page with progress row.

### Restore Queue
Columns: task, target resource/blob count, stage, bytes restored/total, destination, started, ETA when trustworthy, initiator, actions. `Cancel` appears only during cancellable stages and explains cleanup semantics.

### Trash
Columns: Resource/Attachment, type, deleted by, moved to trash, scheduled permanent deletion, dependency/reference summary, actions.

Actions: Restore, Delete permanently. Permanent delete uses error-colored high-risk dialog, lists referenced data impact, requires typing entity name or `DELETE` for bulk deletion.

## 5. Backup & Restore

**Route:** `/console/storage/backup`

### Summary cards
- Last successful backup.
- Next scheduled backup.
- Backup destination health.
- Restore verification status.

### Backup sets table
Columns: backup set ID/time, scope, destination, logical size, incremental/full, encryption state, verification, retention expiry, status, actions.

Primary actions: `Run backup`, `Configure backup`.

### Configure backup
Sections:
- Scope: database/config/metadata/blob selection policy.
- Destination backend.
- Schedule.
- Retention policy.
- Encryption/key reference.
- Verification policy.

Every section shows a concise consequence summary. Credentials/keys are referenced, not displayed.

### Restore wizard
Full-page stepper:
1. Select backup set.
2. Verify compatibility and integrity.
3. Choose restore scope.
4. Conflict/overwrite policy.
5. Review impact.
6. Re-authenticate for destructive overwrite.
7. Start restore.

Once started, show a durable background-operation page with stage progress, logs/events safe for UI, and explicit server restart requirement if applicable.

## Shared interaction rules
- Byte counts use binary units consistently and may expose exact bytes in tooltip.
- Integrity failures use error severity and must never be reduced to a neutral status chip.
- Background migration, archive, restore, verify, backup and cleanup operations remain navigable after leaving the page.
- No UI action may imply deleting a Blob when only an Attachment relation is being removed; dialogs must name the actual layer being changed.
