create sequence if not exists file_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists file
(
    id int8 NOT NULL default nextval('file_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    folder_id int8 NOT NULL,
    md5 varchar(255) NULL,
    name varchar(255) NOT NULL,
    original_name varchar(255) NULL,
    original_path varchar(255) NULL,
    place varchar(255) NULL,
    "size" int8 NULL,
    "type" varchar(255) NULL,
    url varchar(255) NOT NULL,
    CONSTRAINT file_pkey PRIMARY KEY (id)
);