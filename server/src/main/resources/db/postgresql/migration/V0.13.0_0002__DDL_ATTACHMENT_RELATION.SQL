create sequence if not exists attachment_relation_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists attachment_relation
(
    id                     int8         not null default nextval('attachment_relation_seq'),
    attachment_id          int8         not null,
    type                   varchar(255) not null,
    relation_attachment_id int8         not null,
    constraint attachment_relation_pkey primary key (id)
);