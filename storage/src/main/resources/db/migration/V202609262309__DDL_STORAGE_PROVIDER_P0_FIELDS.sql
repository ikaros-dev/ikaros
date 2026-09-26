alter table storage_provider
    add column display_name varchar(256),
    add column capabilities jsonb not null default '{}'::jsonb,
    add column enabled boolean,
    add column drain_status varchar(32) not null default 'NORMAL',
    add column version bigint not null default 0;

update storage_provider
set display_name = provider_key,
    enabled = status not in ('DISABLED', 'DRAINING'),
    drain_status = case when status = 'DRAINING' then 'DRAINING' else 'NORMAL' end;

alter table storage_provider
    alter column display_name set not null,
    alter column enabled set not null,
    add constraint storage_provider_drain_status_ck
        check (drain_status in ('NORMAL', 'DRAINING', 'DRAINED'));
