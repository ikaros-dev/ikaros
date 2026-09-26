alter table storage_provider
    add column configuration jsonb not null default '{}'::jsonb;

update storage_provider
set configuration = provider_metadata;
