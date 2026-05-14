create table if not exists "tag"
(
    "id"          bigint primary key,
    "type"        varchar(255),
    "master_id"   bigint,
    "name"        varchar(255),
    "user_id"     bigint,
    "create_time" timestamp(6),
    "color"       varchar(200)
)
;