create sequence if not exists option_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists option
(
    id int8 NOT NULL default nextval('option_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    name varchar(255) NOT NULL,
    CONSTRAINT option_pkey PRIMARY KEY (id)
);