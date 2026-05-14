create table if not exists "attachment_relation"
(
    "id"                     bigint primary key,
    "attachment_id"          bigint,
    "type"                   varchar(255),
    "relation_attachment_id" bigint
)
;