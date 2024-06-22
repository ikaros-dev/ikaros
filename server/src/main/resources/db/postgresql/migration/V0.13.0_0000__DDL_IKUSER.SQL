create sequence if not exists ikuser_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists ikuser
(
    id
    int8
    not
    null
    default
    nextval
(
    'ikuser_seq'
),
    create_time timestamp
(
    6
) null,
    create_uid int8 null,
    delete_status bool null,
    update_time timestamp
(
    6
) null,
    update_uid int8 null,
    ol_version int8 null,
    role_id int8 null,
    avatar varchar
(
    255
) null,
    email varchar
(
    255
) null,
    "enable" bool null,
    introduce varchar
(
    50000
) null,
    last_login_ip varchar
(
    255
) null,
    last_login_time timestamp
(
    6
) null,
    nickname varchar
(
    255
) null,
    non_locked bool null,
    "password" varchar
(
    255
) null,
    site varchar
(
    255
) null,
    telephone varchar
(
    255
) null,
    username varchar
(
    255
) unique null,
    constraint ikuser_pkey primary key
(
    id
)
    );