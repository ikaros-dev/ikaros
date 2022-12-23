create sequence if not exists album_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists album
(
    id int8 NOT NULL default nextval('album_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    description varchar(255) NULL,
    metadata varchar(255) NULL,
    "name" varchar(255) NOT NULL,
    CONSTRAINT album_pkey PRIMARY KEY (id)
);