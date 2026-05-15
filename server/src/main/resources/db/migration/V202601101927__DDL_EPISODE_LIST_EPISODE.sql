create table if not exists "episode_list_episode"
(
    "id"              uuid NOT NULL,
    "episode_list_id" uuid,
    "episode_id"      uuid,
    CONSTRAINT "episode_list_episode_pkey" PRIMARY KEY ("id")
)
;
