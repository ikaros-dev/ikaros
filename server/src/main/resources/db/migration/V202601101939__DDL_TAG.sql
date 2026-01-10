create table if not exists "tag"
(
    "id"          uuid NOT NULL,
    "type"        varchar(255),
    "master_id"   uuid,
    "name"        varchar(255),
    "user_id"     uuid,
    "create_time" timestamp(6),
    "color"       varchar(200),
    CONSTRAINT "tag_pkey" PRIMARY KEY ("id")
)
;