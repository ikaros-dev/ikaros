create table search_projection_failure
(
    id                uuid primary key default uuid_v7(),
    source_id         uuid not null,
    source_version    bigint not null,
    rebuild_generation bigint not null,
    reason            varchar(4000) not null,
    failed_at         timestamptz not null default current_timestamp,
    resolved_at       timestamptz,
    constraint search_projection_failure_version_ck check (source_version >= 0),
    constraint search_projection_failure_generation_ck check (rebuild_generation >= 0)
);

create index search_projection_failure_pending_idx
    on search_projection_failure (failed_at)
    where resolved_at is null;
