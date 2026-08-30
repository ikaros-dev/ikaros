# Password Manager — CMS Console Interaction Specification

> Password Manager is a high-sensitivity protected domain. Secret values must never appear in URLs, logs, analytics, notification previews, audit descriptions, or generic search results.

## 1. Password Vault

**Route:** `/console/passwords`

### Locked state
Render a security card with `Unlock password vault`. Do not render item counts, titles, domains, folders, usernames, or breach-health information before successful unlock unless product policy explicitly classifies a statistic as safe.

### Unlocked header
- H1 `Password Vault`.
- `Lock now` icon.
- Primary `New item`.
- Search field scoped to unlocked vault.
- Folder/type Filter Chips.

### Main layout
Desktop two/three-pane layout:
- left: folders/favorites/types;
- center: item list;
- right: item detail when selected.

Item list fields:
- type icon;
- title;
- username/display identity masked or partially shown according to policy;
- favorite;
- updated;
- health indicator only if enabled.

Supported item types may include Login, Secure Note, Card, Identity, API Key, SSH/secret-like custom records according to backend design. The UI must render fields from explicit schemas rather than arbitrary untrusted HTML.

### Item detail
Header: icon, title, type, favorite. Actions: Edit, Copy username, Copy password/secret, Open website, overflow.

Field rows:
- label;
- value masked by default for secrets;
- reveal eye icon;
- copy icon;
- optional open-link action for URL fields.

Reveal interaction:
- may require re-authentication according to policy;
- revealed value automatically remasks after timeout or page blur if configured;
- revealing one secret does not reveal every secret on the item.

Copy interaction:
- Snackbar confirms generic `Copied` without echoing secret;
- optional clipboard-clear countdown where platform support exists.

### Item editor
Fields depend on type; Login baseline:
- name/title required;
- username;
- password;
- website URLs;
- TOTP secret/setup if supported;
- notes;
- folder;
- tags;
- custom fields;
- favorite.

Password field includes `Generate` action which opens generator side panel. Save validates URLs/schema but never rejects unusual credentials merely due to character choice.

Delete item moves to protected trash when supported; permanent deletion is a separate high-risk action.

## 2. Generator & Item Creation

**Route:** `/console/passwords/generator`

### Generator card
Controls:
- mode segmented control: Random Password / Passphrase / optional PIN.
- length slider + numeric field.
- include uppercase/lowercase/numbers/symbols switches.
- exclude ambiguous characters.
- minimum digits/symbols advanced fields.
- passphrase word count, separator and capitalization options.

Generated value is masked initially only if policy requires; otherwise it may be visible because generation is an intentional secret-view surface.

Actions:
- `Regenerate`.
- `Copy`.
- `Use in new item`.

Strength meter explains entropy/quality using text labels, not only color. Generator operates locally when architecture supports it so generated secrets are not sent to server before save.

## 3. Health & Secure Send

**Route:** `/console/passwords/health`

Tabs: Health, Secure Send.

### Health
Summary cards:
- weak passwords;
- reused passwords;
- old passwords;
- compromised passwords when breach checking is configured.

Health table shows item title, issue types, last changed and action `Review`. It must not display the actual password.

Issue detail explains why an item is flagged and provides `Edit item`, `Generate replacement`, `Dismiss exception` if policy supports exceptions.

Compromise checking UI states clearly whether hashes/k-anonymity/local data are used; never imply privacy guarantees beyond implementation.

### Secure Send
List columns: safe label, secret/content type, created, expires, access count/limit, password-protected status, state, actions.

Create wizard:
- content type/value or linked vault item field selection;
- expiry required;
- max accesses;
- optional recipient hint that is non-sensitive;
- optional access password;
- allow download where relevant.

After creation, show share URL once in a dedicated result card. `Copy link` does not copy underlying secret. Revocation is immediate. Expired/revoked sends cannot be re-enabled.

## 4. Devices & Access

**Route:** `/console/passwords/devices`

### Trusted/authorized devices table
Fields:
- device name;
- platform/browser/app;
- first authorized;
- last active;
- key/device identity fingerprint shortened;
- trust state;
- actions.

Click opens device detail with sessions, authorization scope, key metadata, recent vault access/security events.

Actions:
- Rename device.
- Revoke vault access.
- Revoke all sessions on device.

Revocation requires confirmation and explains whether the device can re-authenticate later.

### Vault access policy card
Controls may include:
- auto-lock timeout;
- require re-authentication for reveal/export;
- biometric/platform-auth preference where client supports it;
- clipboard clearing policy;
- offline access policy.

Changing security policy shows impact summary and may require current master/recovery authentication.

## Shared security rules
- Locking clears decrypted item state, open editors, generator output and revealed values.
- Secret fields never participate in browser autofill unless explicitly intended and safe; use appropriate autocomplete semantics.
- Generic notifications say `Password vault security action required`, not item names or domains.
- Export/import and recovery actions are high-risk and require re-authentication plus explicit scope review.
- 403/locked/404 states avoid confirming existence of a specific vault item to unauthorized callers.
