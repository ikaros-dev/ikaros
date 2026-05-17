create table if not exists "tag"
(
    "id"          uuid primary key default uuidv7(),
    "type"        varchar(255),
    "master_id"   uuid,
    "name"        varchar(255),
    "user_id"     uuid,
    "create_time" timestamp(6),
    "color"       varchar(200)
)
;