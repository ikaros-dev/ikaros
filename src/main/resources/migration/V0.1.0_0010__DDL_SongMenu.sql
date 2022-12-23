create table if not exists song_menu
(
    id int8 NOT NULL,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    description varchar(255) NULL,
    metadata varchar(255) NULL,
    "name" varchar(255) NOT NULL,
    CONSTRAINT song_menu_pkey PRIMARY KEY (id)
);