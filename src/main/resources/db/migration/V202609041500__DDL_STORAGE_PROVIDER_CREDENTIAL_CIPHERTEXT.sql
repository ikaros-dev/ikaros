alter table storage_provider add column if not exists access_key_id_ciphertext text;
alter table storage_provider add column if not exists secret_access_key_ciphertext text;
alter table storage_provider add column if not exists session_token_ciphertext text;
