create table storage_provider
(
    id                uuid primary key default uuid_v7(),
    provider_key      varchar(256) not null unique,
    provider_type     varchar(128) not null,
    tier              varchar(32) not null,
    status            varchar(32) not null default 'ENABLED',
    secret_reference  varchar(512) not null,
    provider_metadata jsonb not null default '{}'::jsonb,
    created_at        timestamptz not null default current_timestamp,
    updated_at        timestamptz not null default current_timestamp,
    constraint storage_provider_status_ck check (status in ('ENABLED','DISABLED','DRAINING','FAILED'))
);
