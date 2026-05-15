create table if not exists "ikuser_role"
(
    "id"      uuid primary key DEFAULT uuid_generate_v7(),
    "user_id" uuid,
    "role_id" uuid,
    CONSTRAINT "user_role_id_uk" unique (user_id, role_id)
)
;