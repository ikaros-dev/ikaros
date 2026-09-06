create table blob
(
    id           uuid primary key default uuid_v7(),
    sha256       varchar(64) not null unique,
    size_bytes   bigint not null,
    media_type   varchar(256),
    availability varchar(32) not null default 'AVAILABLE',
    created_at   timestamptz not null default current_timestamp,
    version      bigint not null default 0
);

create table attachment
(
    id              uuid primary key default uuid_v7(),
    resource_id     uuid not null,
    blob_id         uuid not null,
    file_name       varchar(512) not null,
    attachment_kind varchar(32) not null default 'ORIGINAL',
    created_at      timestamptz not null default current_timestamp,
    deleted_at      timestamptz,
    version         bigint not null default 0
);

create index idx_attachment_resource on attachment (resource_id) where deleted_at is null;

create table blob_placement
(
    id              uuid primary key default uuid_v7(),
    blob_id         uuid not null,
    provider        varchar(128) not null,
    storage_tier    varchar(32) not null,
    object_key      varchar(1024) not null,
    placement_state varchar(32) not null default 'ACTIVE',
    verified_at     timestamptz,
    created_at      timestamptz not null default current_timestamp,
    version         bigint not null default 0,
    unique (provider, object_key)
);

create index idx_blob_placement_blob on blob_placement (blob_id, placement_state);
