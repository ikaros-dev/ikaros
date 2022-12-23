create table if not exists episode
(
    id int8 NOT NULL,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    air_time timestamp(6) NULL,
    duration int8 NULL,
    overview varchar(50000) NULL,
    season_id int8 NOT NULL,
    seq int8 NULL,
    title varchar(255) NOT NULL,
    original_cn varchar(255) NULL,
    url varchar(255) NULL,
    CONSTRAINT episode_pkey PRIMARY KEY (id)
);