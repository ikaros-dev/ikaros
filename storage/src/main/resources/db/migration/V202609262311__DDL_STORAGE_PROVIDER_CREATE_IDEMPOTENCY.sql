alter table storage_provider
    add column idempotency_key varchar(256),
    add column request_fingerprint char(64);

create unique index storage_provider_idempotency_key_uq
    on storage_provider (idempotency_key)
    where idempotency_key is not null;
