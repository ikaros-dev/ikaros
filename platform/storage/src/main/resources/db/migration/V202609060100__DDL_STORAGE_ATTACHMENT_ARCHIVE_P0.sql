alter table attachment add column archived_at timestamptz;

create index idx_attachment_active_resource
    on attachment (resource_id, created_at)
    where archived_at is null and deleted_at is null;
