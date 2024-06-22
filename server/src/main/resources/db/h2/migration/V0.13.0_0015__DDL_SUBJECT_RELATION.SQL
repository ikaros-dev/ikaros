create table if not exists subject_relation
(
    id                  int8         not null auto_increment,
    create_time         timestamp(6) null,
    create_uid          int8         null,
    delete_status       bool         null,
    update_time         timestamp(6) null,
    update_uid          int8         null,
    ol_version          int8         null,
    subject_id          int8         not null,
    relation_type       varchar(255) not null,
    relation_subject_id int8         not null,
    constraint subject_relation_pkey primary key (id)
);