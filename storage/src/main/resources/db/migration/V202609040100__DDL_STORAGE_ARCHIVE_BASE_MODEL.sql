-- Archive Base / Working Set domain model. Existing placements remain PRIMARY and
-- are protected from automatic eviction until an explicit policy assigns a role.
alter table blob_placement
    add column if not exists durability_role varchar(32) not null default 'PRIMARY',
    add column if not exists evictable boolean not null default false,
    add column if not exists gc_protected boolean not null default false,
    add column if not exists retention_until timestamptz,
    add column if not exists minimum_retention_until timestamptz,
    add column if not exists last_accessed_at timestamptz,
    add column if not exists source_placement_id uuid;

alter table blob_placement
    add constraint blob_placement_durability_role_ck
        check (durability_role in ('PRIMARY', 'REPLICA', 'ARCHIVE_BASE', 'PROMOTED_COPY'));

alter table blob_placement
    add constraint blob_placement_archive_base_protection_ck
        check (durability_role <> 'ARCHIVE_BASE' or (evictable = false and gc_protected = true));

alter table blob_placement
    add constraint blob_placement_source_fk
        foreign key (source_placement_id) references blob_placement(id);

create index if not exists idx_blob_placement_role
    on blob_placement (blob_id, durability_role, placement_state);

create unique index if not exists uq_blob_placement_promoted_copy
    on blob_placement (source_placement_id, storage_tier)
    where durability_role = 'PROMOTED_COPY' and placement_state = 'ACTIVE';

alter table storage_restore_operation
    add column if not exists operation_key varchar(512);

update storage_restore_operation
set operation_key = placement_id::text || ':' || provider_restore_class || ':' || restore_generation
where operation_key is null;

alter table storage_restore_operation
    alter column operation_key set not null;

create unique index if not exists uq_storage_restore_operation_key
    on storage_restore_operation (operation_key);

alter table storage_restore_operation
    drop constraint if exists storage_restore_operation_status_check;

alter table storage_restore_operation
    add constraint storage_restore_operation_status_check
        check (status in ('REQUESTED', 'IN_PROGRESS', 'READY_TEMPORARILY', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED'));

alter table storage_provider
    drop constraint if exists storage_provider_tier_ck;

alter table storage_provider
    add constraint storage_provider_tier_ck
        check (tier in ('HOT', 'WARM', 'COLD', 'ARCHIVE', 'DEEP_ARCHIVE'));
