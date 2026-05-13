create table if not exists "attachment_relation"
(
    "id"                     uuid NOT NULL,
    "attachment_id"          uuid,
    "type"                   varchar(255),
    "relation_attachment_id" uuid,
    CONSTRAINT "attachment_relation_pkey" PRIMARY KEY ("id")
)
;