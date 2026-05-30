create table if not exists "episode_sequence_regular" (
    "id"            uuid primary key default uuidv7(),
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
    "ol_version"    bigint,
    "name"          varchar(255) not null,
    "regex"         varchar(500) not null,
    "ep_group"      varchar(50),
    "sequence"      real,
    "priority"      integer not null default 0,
    "description"   varchar(1000),
    "enabled"       boolean not null default true
);

comment on table "episode_sequence_regular" is '剧集序号正则配置表 — 通过责任链模式根据附件文件名匹配剧集序号或类型';
comment on column "episode_sequence_regular"."name" is '规则名称，如 "NCED匹配"';
comment on column "episode_sequence_regular"."regex" is '正则表达式，用于匹配附件文件名';
comment on column "episode_sequence_regular"."ep_group" is '匹配成功后设置的剧集分组，如 OPENING_SONG';
comment on column "episode_sequence_regular"."sequence" is '匹配成功后设置的剧集序号，为空则从文件名解析';
comment on column "episode_sequence_regular"."priority" is '优先级，越大越优先尝试';
comment on column "episode_sequence_regular"."description" is '规则描述';
comment on column "episode_sequence_regular"."enabled" is '是否启用';
