create table storage_restore_budget
(
    id                              uuid primary key,
    max_bytes_per_request           bigint not null,
    max_items_per_request           integer not null,
    max_concurrent_operations      integer not null,
    max_concurrent_bytes            bigint not null,
    daily_requested_bytes           bigint not null,
    daily_provider_restore_bytes   bigint not null,
    over_budget_action              varchar(32) not null default 'REJECT',
    updated_at                      timestamptz not null default current_timestamp,
    version                         bigint not null default 0,
    check (max_bytes_per_request > 0),
    check (max_items_per_request > 0),
    check (max_concurrent_operations > 0),
    check (max_concurrent_bytes > 0),
    check (daily_requested_bytes > 0),
    check (daily_provider_restore_bytes > 0),
    check (over_budget_action in ('REJECT', 'REQUIRE_CONFIRMATION', 'QUEUE_AFTER_BUDGET_RESET', 'PARTIAL_ACCEPT'))
);
insert into storage_restore_budget (id, max_bytes_per_request, max_items_per_request,
    max_concurrent_operations, max_concurrent_bytes, daily_requested_bytes, daily_provider_restore_bytes)
values ('00000000-0000-0000-0000-000000000001', 53687091200, 1000, 4,
    107374182400, 536870912000, 536870912000)
on conflict (id) do nothing;
