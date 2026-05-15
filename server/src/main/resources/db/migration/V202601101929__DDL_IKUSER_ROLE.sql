create table if not exists "ikuser_role"
(
    "id"      uuid NOT NULL,
    "user_id" uuid,
    "role_id" uuid,
    CONSTRAINT "ikuser_role_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "user_role_id_uk" unique (user_id, role_id)
)
;