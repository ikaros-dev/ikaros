create sequence if not exists resource_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists resource
(
    id int8 NOT NULL default nextval('resource_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    box_id int8 NULL,
    fid int8 NULL,
    "type" varchar(255) NULL,
    CONSTRAINT resource_pkey PRIMARY KEY (id)
);