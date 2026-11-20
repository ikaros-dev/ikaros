alter table resource
    add column status smallint not null default 1;

update resource
set status = case lifecycle
    when 'PURGED' then 0
    when 'TRASHED' then 2
    when 'ARCHIVED' then 3
    else 1
end;

alter table resource
    add constraint resource_status_ck check (status between 0 and 5);

create index resource_type_status_idx on resource (resource_type, status, updated_at desc, id);
create index resource_status_updated_idx on resource (status, updated_at desc, id);

create table user_resource
(
    id          uuid        primary key default uuid_v7(),
    user_id     uuid        not null,
    resource_id uuid        not null,
    role        varchar(16) not null,
    created_at  timestamptz not null default current_timestamp,
    updated_at  timestamptz not null default current_timestamp,
    version     bigint      not null default 0,
    constraint user_resource_user_resource_uq unique (user_id, resource_id),
    constraint user_resource_user_fk foreign key (user_id) references platform_user (id) on delete restrict,
    constraint user_resource_resource_fk foreign key (resource_id) references resource (id) on delete cascade,
    constraint user_resource_role_ck check (role in ('OWNER', 'EDITOR', 'VIEWER')),
    constraint user_resource_version_ck check (version >= 0)
);

insert into user_resource (user_id, resource_id, role, created_at, updated_at)
select owner_id, id, 'OWNER', created_at, updated_at
from resource;

create unique index user_resource_one_owner_uq
    on user_resource (resource_id)
    where role = 'OWNER';

create index user_resource_resource_role_idx
    on user_resource (resource_id, role, user_id);
