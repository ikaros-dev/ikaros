create sequence if not exists tag_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists tag
(
    id
    int8
    not
    null
    default
    nextval
(
    'tag_seq'
),
    type varchar
(
    255
) not null,
    master_id int8 not null,
    name varchar
(
    255
) not null,
    user_id int8 not null,
    create_time timestamp
(
    6
) null,
    constraint tag_pkey primary key
(
    id
)
    );