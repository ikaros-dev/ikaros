drop index if exists idx_attachment_resource;
drop index if exists idx_attachment_active_resource;
drop index if exists attachment_resource_idempotency_uq;

alter table attachment
    drop constraint if exists attachment_resource_fk,
    drop constraint if exists attachment_blob_fk,
    drop column resource_id,
    drop column blob_id,
    drop column file_name,
    drop column deleted_at,
    drop column archived_at;
