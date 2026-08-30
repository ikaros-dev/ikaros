# AI Intelligence — CMS Console Interaction Specification

## 1. Assistant

**Route:** `/console/ai/assistant`

### Header
- Title `AI Assistant`.
- Persona selector.
- Model/provider selector showing effective routing policy rather than secret credentials.
- `New conversation`.

### Layout
Desktop uses conversation list left, conversation canvas center, context inspector right (collapsible).

Conversation list fields: title, persona, last model/provider, updated, pinned state. Search is local/domain-scoped.

Conversation canvas:
- message bubbles with role, timestamp, model/persona provenance on assistant messages;
- Markdown/code rendering;
- attachment/resource citation chips;
- per-response actions: Copy, Regenerate, Branch, Feedback, View trace if authorized.

Composer:
- multiline field;
- attach Resource/Attachment action;
- context scope button;
- model override when allowed;
- send button;
- stop-generation button while streaming.

Interactions:
- Streaming response grows in place with stop control.
- Stop preserves partial output and marks it `Stopped`.
- Regenerate creates a sibling response/version rather than silently replacing history.
- Branch creates new conversation from selected message.
- Resource chips open referenced resource detail.

Context inspector sections:
- selected resources;
- explicit user context;
- enabled memories;
- tools/capabilities;
- privacy classification.

Users can remove context before sending. The final context summary must be inspectable before high-sensitivity requests.

## 2. Models & Providers

**Route:** `/console/ai/models`

Tabs: Providers, Models, Routing.

### Providers table
Columns: provider name, type, endpoint/region, enabled, health, credential state (`Configured`, `Missing`, `Invalid`), supported capabilities, last check, actions.

Provider editor fields:
- display name;
- provider type;
- base endpoint when supported;
- secret API credential field;
- organization/project identifiers where applicable;
- timeout/retry limits;
- proxy/network options if supported;
- enabled switch.

`Test connection` uses entered values without exposing secret in response/log UI. Saved credential displays masked configured state only.

### Models table
Columns: model alias, provider, upstream model ID, modalities, context limit, tool support, enabled, cost metadata, default role, actions.

Model editor contains explicit capability switches only when they override/disambiguate provider discovery; otherwise discovered capabilities are read-only.

### Routing
Rule list ordered by priority. Rule fields: request class/persona/capability/privacy class → preferred model(s) → fallback. Drag reorders priority. `Simulate routing` accepts a synthetic request profile and displays selected path without sending content to provider.

## 3. Personas

**Route:** `/console/ai/personas`

### List
Card/table fields: avatar/icon, name, description, enabled, default model policy, memory policy, tool policy, updated.

Primary `New persona`.

### Persona editor
Full-page with tabs:
- Identity.
- Instructions.
- Models.
- Tools.
- Memory & Context.
- Safety/Privacy.
- Test.

Identity: name required, avatar/icon, short description, tone tags.

Instructions: structured fields for system behavior/instructions; version indicator; unsaved-change protection.

Models: default routing policy, allowed model classes, fallback behavior.

Tools: capability list with enabled/disabled, risk class, permission reason. Enabling a sensitive tool may require admin permission and displays its data-access consequence.

Memory & Context: memory scope, retention, allowed domains, exclusions. Private/password/finance context defaults to denied unless explicitly designed and authorized.

Test tab offers sandbox conversation with visible assembled persona configuration and no production memory mutation unless `Save test as memory` is explicitly chosen.

## 4. Context, Privacy & Memory

**Route:** `/console/ai/privacy`

Tabs: Context Policy, Memory, Data Sharing.

### Context Policy
Policy cards/rules define which data domains may be included for which personas/models/providers. Each rule row: data domain, sensitivity, allowed destinations, explicit-consent requirement, retention, enabled.

Rule editor shows a consequence summary such as `Private Notes content may be sent to Provider X` in warning/error emphasis. Sensitive changes require re-authentication or elevated role when policy demands.

### Memory
Table: memory label/summary, scope, owner, source conversation, created, last used, expiry, state. Content display follows sensitivity rules.

Actions: inspect, edit summary when supported, disable, delete. Deletion confirmation explains whether historical conversations remain.

`Clear memories` is scoped by persona/domain/date and requires typed confirmation for bulk deletion.

### Data Sharing
Shows provider-by-provider disclosure matrix: data categories that may leave the server, retention/config notes, telemetry state, and links to routing policy. This page is explanatory and policy-backed, not marketing copy.

## 5. Jobs, Trace & Usage

**Route:** `/console/ai/jobs`

Tabs: Jobs, Traces, Usage.

### Jobs
Columns: job ID, type, input entity safe label, persona/model, state, progress, created, duration, initiator, actions. Failed row exposes error category and retry eligibility.

Job detail timeline: queued → context build → provider request/tool calls → postprocess → persisted. Each stage shows duration and status. Sensitive prompt/content is hidden unless user has explicit trace-content permission.

### Traces
Filter by trace ID, conversation, model/provider, tool, status, date. Table columns: trace, request class, model, tool count, latency, token/input-output units, status, created.

Trace detail is an expandable event tree with request routing, tool calls, retries and timings. Secret headers/credentials are always redacted. Private content redaction follows domain policy.

### Usage
Period selector + cards: requests, input/output tokens or provider units, estimated cost, failures, latency. Charts by model/provider/persona. Table allows export of aggregated usage metadata; exporting prompt content is a separate privileged action.

## Shared rules
- AI features must clearly distinguish deterministic system actions from model-generated suggestions.
- Any `Apply suggestion` action previews the concrete mutation before executing it.
- Provider/model outages expose fallback path and never silently switch to a destination disallowed by privacy policy.
- Prompt, response, tool and trace content follow sensitivity classification; operational metadata can remain visible when content is redacted.
