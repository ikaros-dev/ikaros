alter table audit_event
    add column result varchar(16) not null default 'UNKNOWN',
    add column risk_level varchar(16) not null default 'UNKNOWN',
    add column details_schema_version integer not null default 1;

alter table audit_event
    add constraint audit_event_actor_constraint check (
        (actor_type = 'SYSTEM' and actor_id is null)
        or (actor_type in ('USER', 'ADMIN') and actor_id is not null)
    ),
    add constraint audit_event_result_constraint check (result in ('SUCCESS', 'FAILURE', 'DENIED', 'UNKNOWN')),
    add constraint audit_event_risk_level_constraint check (risk_level in ('NORMAL', 'SENSITIVE', 'HIGH', 'UNKNOWN')),
    add constraint audit_event_details_schema_version_constraint check (details_schema_version > 0),
    add constraint audit_event_action_format_constraint check (action ~ '^[a-z][a-z0-9]*(\.[a-z][a-z0-9-]*)+$'),
    add constraint audit_event_target_type_format_constraint check (target_type ~ '^[A-Z][A-Z0-9_]*$'),
    add constraint audit_event_details_object_constraint check (jsonb_typeof(details::jsonb) = 'object');

create index audit_event_query_idx
    on audit_event (result, risk_level, occurred_at desc, id desc);
