create table if not exists "attachment_reference"
(
    "id"            uuid primary key DEFAULT uuid_generate_v7(),
    "type"          varchar(255),
    "attachment_id" uuid,
    "reference_id"  uuid
)
;