create sequence if not exists subject_sync_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists subject_sync
(
    id
    int8
    not
    null
    default
    nextval
(
    'subject_sync_seq'
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
    subject_id int8 not null,
    platform varchar
(
    255
) not null,
    platform_id varchar
(
    255
) null,
    sync_time timestamp
(
    6
) null,
    constraint platform_pid_ukey unique
(
    platform,
    platform_id
),
    constraint subject_sync_pkey primary key
(
    id
)
    );