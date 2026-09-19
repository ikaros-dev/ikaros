alter table audit_event drop constraint audit_event_actor_constraint;

alter table audit_event
    add constraint audit_event_actor_constraint check (
        (actor_type in ('SYSTEM', 'ANONYMOUS') and actor_id is null)
        or (actor_type in ('USER', 'ADMIN') and actor_id is not null)
    );
