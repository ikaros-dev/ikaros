create table if not exists options
(
    id int8 NOT NULL,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    category varchar(255) NOT NULL,
    o_key varchar(255) NOT NULL,
    "type" varchar(255) NOT NULL,
    o_value varchar(50000) NULL,
    CONSTRAINT options_pkey PRIMARY KEY (id)
);