create table if not exists song
(
    id int8 NOT NULL,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    album_id int8 NULL,
    menu_id int8 NULL,
    metadata varchar(255) NULL,
    "name" varchar(255) NULL,
    url varchar(255) NOT NULL,
    CONSTRAINT song_pkey PRIMARY KEY (id)
);