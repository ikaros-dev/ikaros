create table if not exists "role_authority"
(
    "id"           uuid NOT NULL,
    "role_id"      uuid,
    "authority_id" uuid,
    CONSTRAINT "role_authority_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "role_authority_id_uk" UNIQUE (role_id, authority_id)
)
;