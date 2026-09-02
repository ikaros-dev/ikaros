# Ikaros V2 P0 API / Contract Coverage Map

| 项目 | 内容 |
|---|---|
| Baseline | `v2-p0-foundation-0.2` |
| Base OpenAPI | `api/openapi-v2-p0.yaml` |
| Convergence Addendum | `api/openapi-v2-p0-contract-convergence.yaml` |
| Machine Registry | `contracts/P0-HTTP-Operation-Registry.yaml` |
| Event Schema | `contracts/schema/p0-event-v1.schema.json` |

> Application Contract exists ≠ public HTTP endpoint exists. Controller-first changes are prohibited.

## Contract Files

- `P0-Implementation-Baseline.md` — Phase 0 engineering GO / frozen boundary.
- `P0-Requirement-Traceability-Matrix.md` — Requirement → Contract → DB → API → Event → Test traceability.
- `contracts/P0-Command-Query-Event-Catalog.md` — Application Command / Query / Event authority.
- `contracts/P0-Event-Payload-Schema-Registry.md` — human-readable event payload compatibility contract.
- `contracts/schema/p0-event-v1.schema.json` — machine-readable Event Envelope / Type / Version baseline.
- `contracts/P0-HTTP-Operation-Registry.yaml` — complete public P0 HTTP operation mapping.
- `api/openapi-v2-p0.yaml` — original P0 OpenAPI baseline.
- `api/openapi-v2-p0-contract-convergence.yaml` — additive coverage for Catalog-declared HTTP operations missing from the original spec.
- `testing/P0-Acceptance-Invariant-Test-Matrix.md` — REQUIRED engineering gates.

## P0 Public API Rule

```text
Subsystem capability
  -> Command or Query ID
  -> Permission / authorization policy
  -> HTTP operationId
  -> HTTP Operation Registry
  -> OpenAPI request/response schema
  -> Event type/version (when durable fact exists)
  -> Acceptance/invariant test
```

CI should verify operationId uniqueness, method/path uniqueness, Registry → OpenAPI existence, Registry contract IDs → Catalog existence, and OpenAPI reference validity.

## Coverage After Convergence

The original OpenAPI baseline has **16** public operations. The convergence addendum adds **12** missing mappings, for **28** public P0 operations total.

Newly covered contracts:

- `resource.find-by-external-identity`
- `resource.trash-resource`
- `resource.get-user-state`
- `resource.list-tags`
- `resource.list-collections`
- `operations.list-background-tasks`
- `operations.list-task-attempts`
- `storage.list-blob-placements`
- `storage.get-provider`
- `identity.get-current-user`
- `identity.list-sessions`
- `identity.get-user`

The exact machine list is `contracts/P0-HTTP-Operation-Registry.yaml`.

## Intentionally Not Public Yet

The following Application Contracts remain internal or `contract-deferred` until route/request/response, authorization/step-up, idempotency and concurrency semantics are frozen:

- Resource mutation contracts for external identities, tags, collections and user-state;
- Storage attachment lifecycle, blob verify/GC, provider update/enable/disable/drain;
- `operations.retry-background-task`;
- Identity user/role/session mutation commands not already in the base OpenAPI.

Do not invent Controller routes for these capabilities.

## Event Machine Contract

`contracts/schema/p0-event-v1.schema.json` locks the P0 event envelope, UUIDv7 event ID, schema version, producer namespace and the current **43 P0 v1 Event Types**. Per-event payload field constraints remain governed by `P0-Event-Payload-Schema-Registry.md` and must be expanded into machine compatibility checks during Phase 0 implementation (`P0-EVT-004/013/014`).

## Change Checklist

- [ ] Catalog Command / Query exists.
- [ ] Permission / object authorization is explicit.
- [ ] OpenAPI operationId exists.
- [ ] HTTP Operation Registry entry exists.
- [ ] New/changed operation carries `x-ikaros-contract-id`.
- [ ] Idempotency / concurrency semantics are explicit.
- [ ] Event schema / payload registry is synchronized when applicable.
- [ ] Acceptance Test ID exists.
- [ ] Traceability Matrix is synchronized.