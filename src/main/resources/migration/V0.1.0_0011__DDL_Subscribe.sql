create sequence if not exists subscribe_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists subscribe
(
    id int8 NOT NULL default nextval('subscribe_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    notes varchar(255) NULL,
    target_id int8 NOT NULL,
    "type" varchar(255) NOT NULL,
    user_id int8 NOT NULL,
    additional varchar(255) NULL,
    progress varchar(255) NULL,
    CONSTRAINT subscribe_pkey PRIMARY KEY (id)
);