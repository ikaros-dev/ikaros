# Platform Configuration — CMS Console Interaction Specification

## 1. Parameters

**Route:** `/console/platform/parameters`

### Header
- Title `Parameters`.
- Subtitle explains these are platform/runtime configuration values, not arbitrary database editing.
- Search field.
- Filter Chips: subsystem, scope, restart requirement, source (`Default`, `Config`, `Database`, `Environment`, `Effective override`).
- Primary `Add custom parameter` only when backend supports extensible parameters.

### Parameter table
Columns:
- key;
- display name;
- subsystem/group;
- effective value;
- source;
- data type;
- scope (`Global`, `User-default`, etc.);
- restart-required indicator;
- last changed;
- actions.

Sensitive values render `Configured`/masked state, never raw content.

### Parameter detail/edit sheet
Fields:
- key read-only for system parameters;
- description;
- current effective value;
- configured override;
- default value;
- type and validation constraints;
- source precedence explanation;
- restart requirement;
- affected subsystem/services.

Interactions:
- Editing uses type-appropriate M3 control: Switch for boolean, numeric field for numbers, select for enums, textarea for structured text, secret field for credentials.
- `Reset to default` removes override rather than writing a copy of the default value.
- Save validates format and displays before/after diff.
- If restart is required, success Snackbar/banner says `Saved — restart required` and links to relevant operation documentation/status; UI must not imply live application.
- Environment-sourced immutable parameters show read-only explanation and where the value must be changed operationally without exposing secret environment content.

### Bulk configuration import/export
If supported, use explicit actions in overflow. Import flow previews unknown keys, invalid values, restart-required changes and secret omissions before apply. Secrets are excluded from export by default.

## 2. Dictionaries

**Route:** `/console/platform/dictionaries`

This page manages controlled platform dictionaries/enumerations that are explicitly designed to be runtime-configurable; it is not a generic SQL table editor.

### Master/detail layout
Left panel/table: dictionary key, name, entry count, subsystem, system/custom, updated.

Selecting a dictionary shows detail header and entries table.

Entry columns:
- value/key;
- display label;
- optional localized labels indicator;
- description;
- sort order;
- enabled;
- system/custom;
- usage count where available;
- actions.

### Entry editor
Fields: value/key required, default label required, localized labels, description, order, enabled, optional metadata/schema-defined fields.

Interactions:
- Drag handles reorder when ordering is meaningful; server persists explicit sort index after drop.
- Disabling keeps existing references valid but removes entry from new-selection controls unless subsystem states otherwise.
- Deleting an entry with references is blocked or requires a migration destination. Dialog shows reference count and `Replace with` selector.
- System-owned entries may be read-only except label localization if explicitly supported.

## 3. Menus

**Route:** `/console/platform/menus`

This page manages configurable navigation entries only when V2 supports runtime/plugin menus. Core subsystem grouping rules from the root interaction specification remain authoritative.

### Layout
Left: menu tree grouped by subsystem. Right: selected item inspector/editor.

Every subsystem group is shown as a tree root. Core groups cannot be removed. The editor clearly differentiates `Core`, `Plugin`, and `Custom` entries.

Tree row fields:
- drag handle when movable;
- icon;
- label;
- route/link summary;
- visibility state;
- source chip;
- permission requirement indicator;
- overflow actions.

### Menu item editor
Fields:
- label required;
- optional localization key/labels;
- icon token;
- parent subsystem/group;
- order;
- destination type (`Internal route`, `External URL`, `Plugin route`);
- destination;
- required permission/capability;
- open-in-new-tab for external links;
- visibility/enabled.

Validation:
- Internal route must match registered/allowed route pattern.
- External URL requires valid safe scheme (`https` by default; other schemes only by explicit policy).
- Permission requirement cannot be removed from a plugin/core item when backend declares a mandatory capability.

Interactions:
- Drag reorder only within allowed parent boundaries.
- Moving a custom/plugin item between subsystem groups requires confirmation because it changes information architecture.
- Preview panel shows how the navigation drawer will render for a selected example role, but preview never bypasses permissions.
- `Restore defaults` is scoped to selected menu/customization and previews which custom order/labels are lost.

## Shared configuration rules
- Every changed setting displays provenance and effective value after save.
- Secrets remain masked; copy/reveal is unavailable unless the setting is explicitly designed for retrieval.
- Changes that affect security, plugins, storage, authentication, or external connectivity link to audit records.
- Configuration edits should be versioned/auditable where backend supports rollback; `Rollback` previews the exact target version.
- Invalid platform configuration must fail closed with actionable field errors rather than saving partially without explanation.
