create table if not exists "role_authority"
(
    "id"           bigint primary key,
    "role_id"      bigint,
    "authority_id" bigint,
    CONSTRAINT "role_authority_id_uk" UNIQUE (role_id, authority_id)
)
;