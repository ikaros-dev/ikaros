create table if not exists "person_character"
(
    "id"            uuid NOT NULL,
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
    "ol_version"    bigint,
    "person_id"     uuid,
    "character_id"  uuid,
    CONSTRAINT "person_character_pkey" PRIMARY KEY ("id")
)
;
