create sequence if not exists episode_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists episode
(
    id            int8           not null default nextval('episode_seq'),
    create_time   timestamp(6)   null,
    create_uid    int8           null,
    delete_status bool           null,
    update_time   timestamp(6)   null,
    update_uid    int8           null,
    ol_version    int8           null,
    subject_id    int8           not null,
    name          varchar(255)   not null,
    name_cn       varchar(255)   null,
    description   varchar(50000) null,
    air_time      timestamp(6)   null,
    sequence      int8           null,
    ep_group      varchar(50)    not null,
    constraint subject_group_seq_uk unique (subject_id, ep_group, sequence),
    constraint episode_pkey primary key (id)
);