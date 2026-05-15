create table if not exists "role_authority"
(
    "id"           uuid primary key DEFAULT uuid_generate_v7(),
    "role_id"      uuid,
    "authority_id" uuid,
    CONSTRAINT "role_authority_id_uk" UNIQUE (role_id, authority_id)
)
;