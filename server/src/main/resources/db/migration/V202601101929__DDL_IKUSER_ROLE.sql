create table if not exists "ikuser_role"
(
    "id"      bigint primary key,
    "user_id" bigint,
    "role_id" bigint,
    CONSTRAINT "user_role_id_uk" unique (user_id, role_id)
)
;