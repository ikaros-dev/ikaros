create table if not exists person_character
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    person_id     int8         not null,
    character_id  int8         not null,
    constraint person_character_pkey primary key (id)
);