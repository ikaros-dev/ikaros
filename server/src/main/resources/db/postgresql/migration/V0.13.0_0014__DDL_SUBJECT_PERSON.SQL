create sequence if not exists subject_person_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists subject_person
(
    id
    int8
    not
    null
    default
    nextval
(
    'subject_person_seq'
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
    person_id int8 not null,
    constraint subject_person_pkey primary key
(
    id
)
    );