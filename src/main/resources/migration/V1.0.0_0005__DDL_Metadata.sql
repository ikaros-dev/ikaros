create sequence if not exists metadata_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists metadata
(
    id int8 NOT NULL default nextval('metadata_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    host_id int8 NOT NULL,
    meta_key varchar(255) NOT NULL,
    "type" varchar(255) NULL,
    meta_value varchar(50000) NULL,
    CONSTRAINT metadata_pkey PRIMARY KEY (id)
);