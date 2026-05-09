create table if not exists "migrations"
(
    "id"        int8 NOT NULL,
    description text NULL,
    CONSTRAINT "migrations_pkey" PRIMARY KEY ("id")
)
;