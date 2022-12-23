create table if not exists task
(
    dtype varchar(31) NOT NULL,
    id int8 NOT NULL,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    internal_task_name varchar(255) NULL,
    "name" varchar(255) NULL,
    "type" int2 NULL,
    cron varchar(255) NULL,
    datd_time timestamp(6) NULL,
    start_time timestamp(6) NULL,
    CONSTRAINT task_pkey PRIMARY KEY (id)
);