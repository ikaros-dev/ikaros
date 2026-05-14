create table if not exists "attachment_reference"
(
    "id"            bigint primary key,
    "type"          varchar(255),
    "attachment_id" bigint,
    "reference_id"  bigint
)
;