drop index if exists idx_resource_owner_lifecycle_updated;

alter table resource
    drop constraint if exists resource_lifecycle_ck,
    drop constraint if exists resource_purged_deleted_at_ck,
    drop column owner_id,
    drop column lifecycle,
    drop column deleted_at;
