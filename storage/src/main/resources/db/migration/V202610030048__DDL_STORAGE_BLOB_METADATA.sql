create table blob_metadata
(
    id          uuid primary key default uuid_v7(),
    blob_id     uuid not null,
    field_key   varchar(128) not null,
    field_value jsonb not null,
    updated_at  timestamptz not null default current_timestamp,
    version     bigint not null default 0,
    constraint blob_metadata_blob_fk foreign key (blob_id) references blob(id) on delete cascade,
    constraint blob_metadata_field_unique unique (blob_id, field_key),
    constraint blob_metadata_field_key_ck check (field_key = btrim(field_key) and field_key <> ''),
    constraint blob_metadata_version_ck check (version >= 0)
);
