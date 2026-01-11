create table if not exists "episode_collection"
(
    "id"          uuid NOT NULL,
    "user_id"     uuid,
    "subject_id"  uuid,
    "episode_id"  uuid,
    "finish"      boolean,
    "progress"    bigint,
    "duration"    bigint,
    "update_time" timestamp(6),
    CONSTRAINT "episode_collection_pkey" PRIMARY KEY ("id")
)
;