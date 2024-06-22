create table if not exists tag
(
    id          int8         not null auto_increment,
    type        varchar(255) not null,
    master_id   int8         not null,
    name        varchar(255) not null,
    user_id     int8         not null,
    create_time timestamp(6) null,
    constraint tag_pkey primary key (id)
);