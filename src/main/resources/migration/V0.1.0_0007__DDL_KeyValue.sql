create table if not exists key_value
(
    id int8 NOT NULL,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    ik_key varchar(255) NULL,
    "type" varchar(255) NULL,
    ik_value varchar(255) NULL,
    CONSTRAINT key_value_pkey PRIMARY KEY (id)
);