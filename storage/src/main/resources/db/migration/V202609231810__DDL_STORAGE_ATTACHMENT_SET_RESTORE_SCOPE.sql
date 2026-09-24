alter table storage_restore_request
    drop constraint if exists storage_restore_request_check;

alter table storage_restore_request
    drop constraint if exists storage_restore_request_scope_check;

alter table storage_restore_request
    add constraint storage_restore_request_scope_check
        check (scope in ('ATTACHMENT', 'ATTACHMENT_SET', 'EPISODE', 'SEASON', 'RESOURCE_SET'));

alter table storage_restore_request
    alter column scope_id drop not null,
    add column request_fingerprint varchar(64);

alter table storage_restore_request
    add constraint storage_restore_request_scope_id_ck
        check ((scope = 'ATTACHMENT_SET' and scope_id is null)
            or (scope <> 'ATTACHMENT_SET' and scope_id is not null)),
    add constraint storage_restore_request_attachment_set_fingerprint_ck
        check (scope <> 'ATTACHMENT_SET' or request_fingerprint is not null);

create unique index storage_restore_request_attachment_set_idempotency_uq
    on storage_restore_request (actor_id, idempotency_key)
    where scope = 'ATTACHMENT_SET' and idempotency_key is not null;
