# Ikaros V2 P0 API / Contract Coverage Map

> This small registry exists to make the P0 contract set discoverable and machine-review friendly.

## Contract files

- `contracts/P0-Command-Query-Event-Catalog.md` — application command/query/event registry, permissions, idempotency, producer/consumer mapping and HTTP operation mapping.
- `contracts/P0-Event-Payload-Schema-Registry.md` — payload shapes and compatibility rules for initial P0 events.
- `api/openapi-v2-p0.yaml` — machine-readable OpenAPI 3.1 baseline.

## P0 contract rule

A new public P0 endpoint is not complete until all applicable layers exist:

```text
Subsystem capability
  -> Command or Query ID
  -> Permission key
  -> HTTP operationId (when public HTTP exists)
  -> OpenAPI request/response schema
  -> Event type/version (when a durable fact is produced)
  -> Producer/consumer mapping
  -> Acceptance/invariant test
```

Controller-first changes are prohibited.
