alter table storage_provider add constraint storage_provider_secret_reference_ck
    check (secret_reference like 'secret://%');

alter table storage_provider add constraint storage_provider_tier_ck
    check (tier in ('HOT', 'WARM', 'COLD', 'ARCHIVE'));
