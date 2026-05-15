create table if not exists "ikuser_role"
(
    "id"      uuid primary key DEFAULT uuidv7(),
    "user_id" uuid not null,
    "role_id" uuid not null,
    CONSTRAINT "user_role_id_uk" unique (user_id, role_id)
)
;