# Private Notes — CMS Console Interaction Specification

> Private Notes is a protected private-data domain. The Console must not expose decrypted titles, snippets, tags, search terms, conflict content, or export contents outside the unlocked vault context.

## 1. Vault

**Route:** `/console/private-notes`

### Locked state
The default state after session start is a security card, not a list skeleton.

Card contains:
- lock icon;
- title `Private Notes is locked`;
- short explanation;
- `Unlock vault` primary button;
- optional supported unlock methods listed without revealing sensitive account detail.

Unlock opens an authentication dialog/full-page security flow. On success, return to the original route. Failed attempts use generic error text and rate-limit feedback.

### Unlocked header
- Title `Private Notes`.
- Lock-now icon button.
- Primary `New private note`.
- Search field scoped only to decrypted vault search.
- Timeout indicator/menu showing auto-lock policy.

### Layout
Desktop three-pane option:
1. folders/collections/tags navigation;
2. note list;
3. editor/preview.

On narrower screens, list and editor become separate routes/sheets.

### Note list fields
- title;
- updated time;
- favorite/pin icon;
- optional safe local status such as unsynced/conflict;
- no plaintext snippet when privacy policy disables it.

Filters: folder, tag, favorites, updated date, conflict state. Filter state containing private names is stored only within encrypted/local vault context and never URL query parameters.

### Editor
Fields/components:
- title required;
- content editor;
- folder/collection selector;
- private tags;
- attachment picker for private attachments only;
- pinned/favorite state;
- autosave indicator;
- version indicator.

Interactions:
- Autosave after idle debounce; explicit Ctrl/Cmd+S supported.
- Leaving with unsynced changes prompts confirmation.
- `Lock now` flushes pending encrypted save, clears decrypted UI state, closes previews and returns to locked card.
- Clipboard actions may show optional `Clear clipboard after N seconds` message if platform supports it.

## 2. Versions & Sync Conflicts

**Route:** `/console/private-notes/conflicts`

The page itself remains locked until vault unlock.

### Summary cards
- notes with conflicts;
- unsynced local changes;
- failed encrypted sync operations.

### Conflict list
Columns/rows show safe identifiers within unlocked context:
- note title;
- local version timestamp/device;
- remote version timestamp/device;
- conflict reason/type;
- status;
- action `Resolve`.

### Conflict resolution page
Three-column desktop comparison:
- Local version.
- Remote version.
- Result/merged version.

Each side shows timestamp, device/source, version ID and decrypted content. For text content, line/block diff highlights additions/removals. Attachments are compared by safe attachment metadata.

Actions:
- `Keep local`.
- `Keep remote`.
- `Use merged result`.
- `Cancel`.

Resolution never deletes historical versions immediately; selected result becomes a new current version. A conflict resolution audit record contains technical metadata but not plaintext note content.

## 3. Recovery & Export

**Route:** `/console/private-notes/recovery`

### Recovery section
Shows:
- vault encryption/recovery status;
- last successful secure backup/recovery-package creation;
- recovery method availability;
- warnings if recovery is not configured.

Actions requiring re-authentication:
- create/rotate recovery material;
- restore from encrypted package;
- verify recovery package.

Recovery secrets are shown only when product design explicitly requires one-time display. One-time screens include `I have saved this securely` acknowledgement before leaving.

### Export section
Export form:
- scope: all notes / selected folder / selected notes;
- format: encrypted Ikaros package default, optional plaintext formats only when explicitly supported;
- include attachments switch;
- password/encryption options;
- destination: download or configured secure storage.

Plaintext export displays high-risk warning and requires re-authentication. Export is a background task for large scopes. Notification text only states that the private export is ready; it does not include note names.

### Import/restore
Wizard:
1. Select encrypted package.
2. Authenticate/decrypt.
3. Validate package and version compatibility.
4. Preview counts only unless vault is unlocked.
5. Select merge policy: keep existing, replace newer/older according to explicit rule, import as duplicates.
6. Confirm and run.

## Shared security behavior
- Auto-lock on configured inactivity, explicit sign-out, session revocation, or security event.
- Browser refresh/deep link may require unlock again depending on key residency policy.
- Decrypted data must not be placed in URL, document title, generic browser notification, generic analytics, audit message, or global search index.
- Error messages avoid revealing whether a specific private note exists when authorization/unlock has failed.
- Screenshots/clipboard restrictions are platform-dependent; the UI may communicate them but must not claim guarantees the browser cannot enforce.
