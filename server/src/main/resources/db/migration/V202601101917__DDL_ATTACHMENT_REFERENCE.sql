create table if not exists "attachment_reference"
(
    "id"            uuid NOT NULL,
    "type"          varchar(255),
    "attachment_id" uuid,
    "reference_id"  uuid,
    CONSTRAINT "attachment_reference_pkey" PRIMARY KEY ("id")
)
;