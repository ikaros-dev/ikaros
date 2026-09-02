create table plugin
(
    id                       uuid primary key default uuid_v7(),
    plugin_id                varchar(256) not null unique,
    manifest_json            jsonb not null,
    status                   varchar(32) not null,
    granted_permissions_json jsonb not null default '[]'::jsonb,
    created_at               timestamptz not null default current_timestamp,
    updated_at               timestamptz not null default current_timestamp,
    constraint plugin_status_ck check (status in
        ('DISCOVERED','INSTALLED','ENABLED','DISABLED','FAILED','INCOMPATIBLE','UNINSTALLED'))
);

create table search_document
(
    document_id       uuid primary key,
    source_id         uuid not null,
    source_version    bigint not null,
    projector_version varchar(128) not null,
    rebuild_generation bigint not null,
    fields_json       jsonb not null default '{}'::jsonb,
    projected_at      timestamptz not null default current_timestamp,
    constraint search_document_version_ck check (source_version >= 0),
    constraint search_document_generation_ck check (rebuild_generation >= 0)
);

create index search_document_source_idx on search_document (source_id, source_version);
