-- Resource 永久删除采用可审计终态，保留 Resource 身份；Blob/Attachment GC 由其 Owner 依据引用和保留规则处理。
alter table resource
    drop constraint if exists resource_lifecycle_ck;

alter table resource
    add constraint resource_lifecycle_ck
    check (lifecycle in ('ACTIVE', 'ARCHIVED', 'TRASHED', 'PURGED'));

alter table resource
    add constraint resource_purged_deleted_at_ck
    check (lifecycle <> 'PURGED' or deleted_at is not null);
