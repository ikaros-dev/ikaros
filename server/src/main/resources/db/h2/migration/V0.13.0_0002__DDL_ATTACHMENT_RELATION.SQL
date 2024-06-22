create table if not exists attachment_relation
(
    id                     int8         not null auto_increment,
    attachment_id          int8         not null,
    type                   varchar(255) not null,
    relation_attachment_id int8         not null,
    constraint attachment_relation_pkey primary key (id)
);