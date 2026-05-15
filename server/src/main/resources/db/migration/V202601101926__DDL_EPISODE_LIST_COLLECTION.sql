create table if not exists "episode_list_collection"
(
    "id"              uuid NOT NULL,
    "user_id"         uuid,
    "episode_list_id" uuid,
    "update_time"     timestamp(6),
    CONSTRAINT "episode_list_collection_pkey" PRIMARY KEY ("id")
)
;
