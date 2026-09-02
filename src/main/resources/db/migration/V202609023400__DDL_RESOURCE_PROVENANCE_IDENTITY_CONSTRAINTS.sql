alter table resource_metadata add constraint resource_metadata_source_ck
    check (source in ('USER', 'FILE_SCAN', 'IMPORT', 'PROVIDER', 'PLUGIN', 'SYSTEM'));

alter table resource_metadata add constraint resource_metadata_field_key_ck
    check (length(trim(field_key)) > 0);

alter table external_identity add constraint external_identity_provider_ck
    check (length(trim(provider)) > 0);

alter table external_identity add constraint external_identity_type_ck
    check (length(trim(external_type)) > 0);

alter table external_identity add constraint external_identity_id_ck
    check (length(trim(external_id)) > 0);
