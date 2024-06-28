create sequence if not exists attachment_reference_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists attachment_reference
(
    id            int8         not null default nextval('attachment_reference_seq'),
    type          varchar(255) not null,
    attachment_id int8         not null,
    reference_id  int8         not null,
    constraint type_attachment_reference_uk unique (type, attachment_id, reference_id),
    constraint attachment_reference_pkey primary key (id)
);