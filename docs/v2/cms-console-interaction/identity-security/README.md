# Identity & Security — CMS Console Interaction Specification

## 1. Users & Roles

**Route:** `/console/security/users`

### Header
- Title `Users & Roles`.
- Primary `Invite/Create user` according to deployment policy.
- Filters: status, role, MFA state, last active, account type.

### User table
Columns:
- avatar/display name;
- username/login identity;
- email/contact only when authorized;
- account status (`Active`, `Disabled`, `Locked`, `Pending`);
- role summary;
- MFA status;
- last active;
- created;
- actions.

Row opens user detail.

### User detail
Header: avatar, display name, username, status chips. Actions: Edit, Disable/Enable, Reset/require credential action, overflow.

Tabs:
- Overview.
- Roles & Permissions.
- Sessions & Devices.
- Security Events.
- Activity/Audit references.

Overview fields: user ID, account type, created, last login, locale/timezone, account status, profile fields allowed by policy.

Roles tab lists direct roles and inherited/effective permissions. `Assign role` uses searchable multi-select and shows role description/risk level. Removing the last administrator-equivalent role from the current/only admin account is blocked by backend policy and explained inline.

Disable account dialog states effects on sessions, API tokens, automations and owned data. Data is not deleted.

### Role list/editor
Role table: name, description, user count, permission count, system/custom, updated.

Create/edit role fields: name required, description, permission set, optional inheritance. System roles may be read-only.

Deleting custom role requires reassignment/removal preview for affected users.

## 2. Permission Matrix

**Route:** `/console/security/permissions`

### Layout
Top selector: inspect by Role / User. Search field for capability/resource.

Main matrix rows group capabilities by subsystem. Columns may represent actions (`View`, `Create`, `Edit`, `Delete`, `Admin`) or explicit capability keys depending on authorization model.

Every group is collapsible. High-risk capabilities display warning icon and tooltip explaining scope.

Interactions:
- Checkbox changes are staged, not immediately persisted.
- Header checkbox selects all editable permissions in group; indeterminate state when partial.
- Read-only inherited permissions show lock/inheritance icon and source role.
- Footer sticky action bar appears after changes with `Discard` and `Review changes`.

Review dialog displays added/removed permissions grouped by risk. Privilege escalation to security/key/plugin/private-data administration may require re-authentication.

Effective-permission inspector explains source: direct, role, inherited, owner-specific, denied by policy.

## 3. Active Sessions

**Route:** `/console/security/sessions`

### Summary cards
- active sessions;
- current session;
- active users;
- suspicious/recent security alerts.

### Session table
Columns:
- user;
- device/client;
- platform/browser/app;
- approximate network/location only when policy allows;
- issued time;
- last active;
- expires;
- authentication strength/MFA;
- state;
- actions.

Current session has `Current` chip.

Session detail side sheet:
- session ID shortened + copy full ID action;
- user;
- device/client;
- issued/last active/expires;
- auth method/MFA;
- IP/network metadata according to privacy policy;
- token/session scopes;
- related security events.

Actions:
- `Revoke session`.
- `Revoke all other sessions for user`.
- `Revoke all sessions` only for authorized administrators.

Revocation dialogs state whether refresh/access tokens and device authorization are affected. Current-session revocation signs the user out after server confirmation.

## 4. Authentication, Keys & Recovery

**Route:** `/console/security/authentication`

Tabs: Authentication Policy, Keys & Secrets, Recovery, OAuth/API Access when enabled.

### Authentication Policy
Cards/forms for:
- password/login policy;
- MFA requirement;
- session lifetime/idle timeout;
- login rate limits/lockout;
- trusted device policy;
- external identity providers.

Policy fields include current effective value and short impact text. `Save policy` opens diff review and may require re-authentication.

### Keys & Secrets
Tables separated by purpose: signing/encryption keys, API credentials, integration secrets references. Never mix secret material with ordinary settings.

Key row fields: key name/ID, purpose, algorithm/type, status (`Active`, `Retiring`, `Revoked`), created, expires/rotation due, last used, actions.

`Rotate key` wizard:
1. Select key/purpose.
2. Show affected services/data.
3. Choose overlap/grace period when supported.
4. Generate/import new key.
5. Verify readiness.
6. Activate.
7. Track re-encryption/re-signing background work where applicable.

Private key/secret value is never displayed after initial generation except a deliberate one-time export flow.

### Recovery
Shows recovery readiness for administrator/system and protected vault domains without exposing recovery material. Actions: generate/rotate recovery codes/material, verify recovery, invalidate old recovery set. One-time code display includes explicit saved acknowledgement.

### OAuth/API access
Client/token table: client name, owner, scopes, created, last used, expiry, status. Create token flow shows scopes and resulting secret exactly once. Revocation is immediate and cannot be undone.

## Shared security behavior
- Security-sensitive mutation produces audit event.
- Re-authentication modal explains why it is requested and returns user to pending action on success.
- Permission denial never exposes hidden secret fields.
- IDs may be copied explicitly but sensitive identifiers are shortened in tables by default.
- Security event severity uses text + icon + color.
