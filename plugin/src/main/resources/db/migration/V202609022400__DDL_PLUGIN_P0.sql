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
