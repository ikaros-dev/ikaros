create sequence if not exists ikuser_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists ikuser
(
    id int8 NOT NULL default nextval('ikuser_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    avatar varchar(255) NULL,
    email varchar(255) NULL,
    "enable" bool NULL,
    introduce varchar(50000) NULL,
    last_login_ip varchar(255) NULL,
    last_login_time int8 NULL,
    nickname varchar(255) NULL,
    non_locked bool NULL,
    "password" varchar(255) NULL,
    site varchar(255) NULL,
    telephone varchar(255) NULL,
    username varchar(255) NULL,
    CONSTRAINT ikuser_pkey PRIMARY KEY (id)
);