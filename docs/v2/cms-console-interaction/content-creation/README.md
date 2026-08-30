# Content & Creation — CMS Console Interaction Specification

## 1. Unified Resource Library

**Route:** `/console/resources`

### Header
- H1 `Unified Resource Library`.
- Subtitle explains that Resource represents logical content, not physical storage.
- Primary `Create resource` button opens type picker.
- Secondary `Import` button opens import workflow.

### KPI row
Cards: total resources, available, archived, in trash, metadata conflicts. Clicking a card applies the matching filter.

### Filter/search toolbar
- Search text field.
- Type Filter Chip with multi-select: anime, episode, film, video, manga, chapter, novel, music album, track, image, image collection, article, note, document, game, archive/other.
- Lifecycle chip: Available, Archived, Trash.
- Collection chip.
- Favorite chip.
- `More filters`: source/provider, external identity platform, tag, owner, created/updated ranges, progress state, attachment presence, metadata conflict state.
- Sort selector: Updated desc default, Created, Title, Type, Progress.

### Main data table
Columns:
1. selection checkbox;
2. thumbnail/type icon;
3. title + alternate title below;
4. type chip;
5. lifecycle/status chip;
6. collection summary;
7. primary external identity/provider;
8. progress where relevant;
9. metadata source/conflict indicator;
10. updated time;
11. favorite icon;
12. overflow actions.

Row interactions:
- Clicking title/row opens `/console/resources/{id}`.
- Favorite icon mutates without navigating.
- Conflict icon opens Metadata tab and scrolls to conflict panel.
- Overflow: Edit, Add to collection, Manage relations, Archive/Restore, Move to trash, Copy internal ID.

Bulk actions:
- Add to collection.
- Add/remove tags.
- Archive.
- Move to trash.
- Export metadata.
- Re-run metadata resolution when permitted.

### Resource detail page
**Route:** `/console/resources/{id}`

Header shows title, type chip, lifecycle chip, favorite toggle. Actions: Edit, Open/Play/Read when applicable, Share, overflow.

Tabs:
- `Overview` default.
- `Metadata`.
- `Attachments`.
- `Relations`.
- `Collections & Tags`.
- `Activity`.
- Type-specific tab such as Episodes/Chapters/Tracks/Pages only when meaningful.

Overview regions:
- cover/poster card;
- primary facts grid: internal ID, type, created, updated, owner, lifecycle;
- description/summary;
- progress/consumption card when applicable;
- external identities list with platform, external ID, URL, sync status;
- quick related-resources card.

Metadata tab:
- field-by-field rows with field name, effective value, source badge (`Manual`, `Provider`, `Scanner`, `Import`), lock/manual ownership indicator, last update.
- conflicts show side-by-side candidate values with `Keep mine`, `Accept source`, `Edit manually`.
- automated sync must never silently replace locked/manual fields.

Attachments tab embeds the attachment relationship list but deep-links full management to Attachment & Storage.

Relations tab renders typed graph/list. Add relation dialog requires relation type, direction, target resource search, optional notes. Prevent self-relation and invalid relation types.

### Create/edit resource workflow
Full-page or large side sheet depending on type complexity.

Common fields:
- Resource type (required on create, immutable unless explicit migration exists).
- Title (required).
- Alternate title.
- Summary/description.
- Cover attachment selector.
- Collections.
- Tags.
- External identities.
- Type-specific fields.

Save validates required/type-specific fields. `Save and add attachments` may be offered after create.

## 2. Collections, Tags & Relations

**Route:** `/console/collections`

Tabs: Collections, Tags, Relation Types/Explorer.

### Collections tab
Table columns: name, kind (`Manual`, `Dynamic`), resource count, owner, visibility, updated, actions.

Create collection dialog fields:
- name required;
- description;
- kind;
- parent collection when hierarchy is supported;
- visibility;
- for Dynamic: rule builder.

Dynamic rule builder uses rows of Field / Operator / Value with AND/OR groups, live preview count, `Test rules` action. Invalid rules block save.

Collection detail shows header + tabs Resources, Rules/Settings, Activity. Resources support drag ordering only for explicitly ordered manual collections.

### Tags tab
Columns: tag name, namespace/type, usage count, color/visual token, updated. Actions: rename, merge, delete. Merge dialog chooses destination and previews affected resources. Delete warns how many assignments are removed.

### Relations explorer
Source resource selector + relation type chips + optional target type. Main area shows either table or graph. Graph node click opens inspector; table remains available for accessibility and bulk editing.

## 3. Articles & Documents

**Route:** `/console/documents`

List toolbar: search, content kind, status Draft/Published/Archived, author, tags, updated range. Table columns: title, kind, status, author, version, updated, published, actions.

Primary `New` menu: Article, Document, Note-like public document types supported by the core model.

### Editor
Full-page editor with:
- top title field;
- autosave state (`Saved`, `Saving…`, `Offline changes`, `Conflict`);
- formatting toolbar / Markdown mode depending on chosen editor implementation;
- center editing canvas;
- right properties panel for status, slug/public identity if supported, tags, collection, cover, permissions, publication schedule;
- bottom/version indicator.

Interactions:
- Ctrl/Cmd+S forces save.
- Publish opens confirmation/review dialog showing visibility and scheduled time.
- Version history opens side sheet; selecting a version shows read-only diff/preview; restore creates a new version instead of deleting history.
- Attachment insertion opens attachment picker/upload and creates explicit attachment relationship.

## 4. Media Consumption

**Route:** `/console/media`

This is management-oriented consumption state, not a dedicated living-room player UI.

Tabs: Continue, History, Playlists/Queues, Playback Settings.

Continue cards show poster, title, episode/chapter/track context, progress bar, last activity, Resume.

History table: resource, content position, device/client, started, completed state, last position, actions. `Remove from history` does not delete resource.

Playlist/queue detail: ordered rows with drag handles, resource, duration, availability, actions. Reordering persists after drop with snackbar confirmation.

Playback settings expose server-supported defaults only; unsupported client-specific settings are not shown.

## 5. Sharing & Collaboration

**Route:** `/console/sharing`

Tabs: Share Links, Rooms, Collaboration.

### Share Links
Columns: target resource/collection, permission, expiry, access count, created by, status, actions.

Create share dialog:
- target search;
- permission: view, view+download, comment where supported;
- expiry datetime required by default policy;
- optional password;
- access limit;
- download switch;
- optional note.

After create, show one-time copyable URL/token surface. Revocation is immediate and requires confirmation. Expired/revoked links cannot be reactivated; create a new link.

### Rooms
Columns: room name, media/queue, owner, member count, state, created/last activity. Detail shows members, roles, synchronized playback state, chat/event history where supported. Ending a room asks confirmation but does not alter underlying resources.

### Collaboration
Shows documents/resources with active collaborators, pending comments, presence status and permission. Permission changes use explicit role selector and create audit events.

## Shared states
All list pages implement root-spec loading/empty/error states. Resource lifecycle operations must distinguish Archive, Trash and Permanent Delete. Permanent deletion is never placed directly in the primary row action; it is available only from trash/detail flows with high-risk confirmation.
