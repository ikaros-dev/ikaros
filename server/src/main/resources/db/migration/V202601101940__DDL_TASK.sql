create table if not exists "task"
(
    "id"           uuid NOT NULL,
    "name"         varchar(255),
    "status"       varchar(255),
    "create_time"  timestamp(6),
    "start_time"   timestamp(6),
    "end_time"     timestamp(6),
    "total"        bigint,
    "index"        bigint,
    "fail_message" varchar(2000),
    CONSTRAINT "task_pkey" PRIMARY KEY ("id")
)
;