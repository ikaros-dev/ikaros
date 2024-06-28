create sequence if not exists subject_collection_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists subject_collection
(
    id               int8         not null default nextval('subject_collection_seq'),
    user_id          int8         not null,
    subject_id       int8         not null,
    type             varchar(255) not null,
    main_ep_progress int8         not null,
    is_private       bool         not null,
    constraint user_subject_id_uk unique (user_id, subject_id),
    constraint subject_collection_pkey primary key (id)
);