create table search_rebuild_generation
(
    id         varchar(64) primary key,
    generation bigint not null,
    updated_at timestamptz not null default current_timestamp,
    constraint search_rebuild_generation_ck check (generation >= 0)
);

insert into search_rebuild_generation (id, generation)
values ('global', 0)
on conflict (id) do nothing;
