create table user_resource_state
(
    user_id           uuid not null,
    resource_id       uuid not null,
    favorite          boolean not null default false,
    rating            numeric(4,2),
    status_code       varchar(64),
    progress_value    numeric,
    progress_unit     varchar(64),
    last_accessed_at  timestamptz,
    version           bigint not null default 0,
    updated_at        timestamptz not null default current_timestamp,
    primary key (user_id, resource_id),
    constraint user_resource_state_rating_ck check (rating is null or (rating >= 0 and rating <= 10)),
    constraint user_resource_state_progress_ck check (progress_value is null or progress_value >= 0),
    constraint user_resource_state_resource_fk foreign key (resource_id)
        references resource (id) on delete restrict
);
