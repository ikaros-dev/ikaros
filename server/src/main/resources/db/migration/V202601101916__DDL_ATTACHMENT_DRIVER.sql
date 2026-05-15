create table if not exists "attachment_driver"
(
    "id"             uuid primary key DEFAULT uuid_generate_v7(),
    "enable"         boolean,
    "driver_type"    varchar(100) NOT NULL,
    "driver_name"    varchar(100),
    "mount_name"     varchar(255),
    "remote_path"    varchar(1000),
    "driver_order"   bigint,
    "driver_comment" varchar(255),
    "refresh_token"  varchar(255),
    "access_token"   varchar(255),
    "expire_time"    timestamp(6),
    "list_page_size" bigint,
    "root_dir_id"    varchar(255),
    "request_limit"  bigint,
    "user_id"        varchar(255),
    "user_name"      varchar(255),
    "avatar"         varchar(1000),
    "space_total"    bigint,
    "space_use"      bigint
)
;