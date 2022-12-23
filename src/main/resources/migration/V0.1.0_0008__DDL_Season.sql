create sequence if not exists season_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists season
(
    id int8 NOT NULL default nextval('season_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    anime_id int8 NOT NULL,
    overview varchar(50000) NULL,
    title varchar(255) NULL,
    title_cn varchar(255) NULL,
    "type" varchar(255) NULL,
    CONSTRAINT season_pkey PRIMARY KEY (id)
);