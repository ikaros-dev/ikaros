create table if not exists attachment_reference
(
    id            int8         not null auto_increment,
    type          varchar(255) not null,
    attachment_id int8         not null,
    reference_id  int8         not null,
    constraint type_attachment_reference_uk unique (type, attachment_id, reference_id),
    constraint attachment_reference_pkey primary key (id)
);