alter table blob add column hash_algorithm varchar(32) not null default 'SHA-256';

alter table blob drop constraint if exists blob_sha256_key;
alter table blob add constraint blob_identity_unique unique (hash_algorithm, sha256, size_bytes);
alter table blob add constraint blob_size_nonnegative_ck check (size_bytes >= 0);
