alter table media_delivery_provider add column if not exists idempotency_key varchar(256);

create unique index if not exists uq_media_delivery_provider_idempotency
    on media_delivery_provider (idempotency_key)
    where idempotency_key is not null;
