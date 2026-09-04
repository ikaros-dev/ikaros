-- Repair migration for databases that recorded the archive model migration
-- without applying its blob_placement alterations.
alter table blob_placement
    add column if not exists durability_role varchar(32) not null default 'PRIMARY',
    add column if not exists evictable boolean not null default false,
    add column if not exists gc_protected boolean not null default false,
    add column if not exists retention_until timestamptz,
    add column if not exists minimum_retention_until timestamptz,
    add column if not exists last_accessed_at timestamptz,
    add column if not exists source_placement_id uuid;
