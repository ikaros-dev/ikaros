create table media_delivery_provider
(
    id                    uuid primary key default uuid_v7(),
    provider_key          varchar(128) not null unique,
    provider_type         varchar(32) not null,
    display_name          varchar(256) not null,
    credential_ref        varchar(512),
    config                jsonb not null default '{}'::jsonb,
    capabilities          jsonb not null default '{}'::jsonb,
    grant_revocation_mode varchar(48) not null default 'IMMEDIATE',
    signing_key_version   bigint not null default 1,
    health_status         varchar(32) not null default 'UNKNOWN',
    enabled               boolean not null default true,
    created_at            timestamptz not null default current_timestamp,
    updated_at            timestamptz not null default current_timestamp,
    version               bigint not null default 0,
    check (provider_type in ('DIRECT', 'CDN', 'SERVER_PROXY')),
    check (grant_revocation_mode in ('IMMEDIATE', 'KEY_VERSION_BOUND', 'TTL_BOUNDED', 'NOT_REVOCABLE_BEFORE_EXPIRY')),
    check (signing_key_version >= 1),
    check (health_status in ('UNKNOWN', 'HEALTHY', 'DEGRADED', 'UNHEALTHY'))
);
