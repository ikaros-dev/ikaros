alter table attachment add column idempotency_key varchar(128);

create unique index attachment_resource_idempotency_uq
    on attachment (resource_id, idempotency_key)
    where idempotency_key is not null and deleted_at is null;
