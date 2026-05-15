create table if not exists "attachment_relation"
(
    "id"                     uuid primary key DEFAULT uuid_generate_v7(),
    "attachment_id"          uuid,
    "type"                   varchar(255),
    "relation_attachment_id" uuid
)
;