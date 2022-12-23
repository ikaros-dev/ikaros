create table if not exists anime
(
    id int8 NOT NULL,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    air_time timestamp(6) NULL,
    bgmtv_id int8 NULL,
    cover_url varchar(255) NULL,
    overview varchar(50000) NULL,
    platform varchar(255) NULL,
    producer varchar(255) NULL,
    staffs oid NULL,
    title varchar(255) NOT NULL,
    title_cn varchar(255) NOT NULL,
    CONSTRAINT anime_pkey PRIMARY KEY (id)
);