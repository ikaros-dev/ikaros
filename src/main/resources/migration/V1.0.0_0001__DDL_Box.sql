create sequence if not exists box_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists box
(
    id int8 NOT NULL default nextval('box_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    name varchar(255) NOT NULL,
    parent_id int8 NOT NULL,
    CONSTRAINT box_pkey PRIMARY KEY (id)
);