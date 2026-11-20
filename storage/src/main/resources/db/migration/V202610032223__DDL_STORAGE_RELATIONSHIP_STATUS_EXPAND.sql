alter table blob
    add column updated_at timestamptz;

update blob
set updated_at = created_at;

alter table blob
    alter column updated_at set default current_timestamp,
    alter column updated_at set not null;

alter table attachment
    add column name varchar(512),
    add column status smallint not null default 1,
    add column created_by uuid,
    add column request_fingerprint char(64),
    add column updated_at timestamptz;

update attachment a
set name = a.file_name,
    status = case
        when a.deleted_at is not null then 2
        when a.archived_at is not null then 3
        else 1
    end,
    created_by = r.owner_id,
    updated_at = greatest(a.created_at, coalesce(a.deleted_at, a.created_at), coalesce(a.archived_at, a.created_at))
from resource r
where r.id = a.resource_id;

alter table attachment
    alter column name set not null,
    alter column created_by set not null,
    alter column updated_at set default current_timestamp,
    alter column updated_at set not null,
    add constraint attachment_status_ck check (status between 0 and 5),
    add constraint attachment_created_by_fk foreign key (created_by) references platform_user (id) on delete restrict;

create unique index attachment_creator_idempotency_uq
    on attachment (created_by, idempotency_key)
    where idempotency_key is not null;

create index attachment_status_created_idx on attachment (status, created_at desc, id);

create table resource_attachment
(
    id            uuid        primary key default uuid_v7(),
    resource_id   uuid        not null,
    attachment_id uuid        not null,
    created_at    timestamptz not null default current_timestamp,
    version       bigint      not null default 0,
    constraint resource_attachment_resource_attachment_uq unique (resource_id, attachment_id),
    constraint resource_attachment_resource_fk foreign key (resource_id) references resource (id) on delete restrict,
    constraint resource_attachment_attachment_fk foreign key (attachment_id) references attachment (id) on delete cascade,
    constraint resource_attachment_version_ck check (version >= 0)
);

insert into resource_attachment (resource_id, attachment_id, created_at, version)
select resource_id, id, created_at, version
from attachment;

create index resource_attachment_attachment_resource_idx on resource_attachment (attachment_id, resource_id);

create table attachment_blob
(
    id            uuid        primary key default uuid_v7(),
    attachment_id uuid        not null,
    blob_id       uuid        not null,
    role          varchar(16) not null,
    created_at    timestamptz not null default current_timestamp,
    version       bigint      not null default 0,
    constraint attachment_blob_attachment_blob_uq unique (attachment_id, blob_id),
    constraint attachment_blob_attachment_fk foreign key (attachment_id) references attachment (id) on delete cascade,
    constraint attachment_blob_blob_fk foreign key (blob_id) references blob (id) on delete restrict,
    constraint attachment_blob_role_ck check (role in ('ORIGINAL', 'DERIVED')),
    constraint attachment_blob_version_ck check (version >= 0)
);

insert into attachment_blob (attachment_id, blob_id, role, created_at, version)
select id, blob_id, 'ORIGINAL', created_at, version
from attachment;

create unique index attachment_blob_one_original_uq
    on attachment_blob (attachment_id)
    where role = 'ORIGINAL';

create index attachment_blob_blob_attachment_idx on attachment_blob (blob_id, attachment_id);
