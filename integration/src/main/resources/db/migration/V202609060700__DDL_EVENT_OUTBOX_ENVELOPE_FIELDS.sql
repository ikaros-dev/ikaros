alter table event_outbox add column producer_subsystem varchar(128);
alter table event_outbox add column subject_type varchar(128);
alter table event_outbox add column subject_id uuid;

update event_outbox
set producer_subsystem = split_part(event_type, '.', 1),
    subject_type = aggregate_type,
    subject_id = aggregate_id
where producer_subsystem is null or subject_type is null;

alter table event_outbox alter column producer_subsystem set not null;
alter table event_outbox alter column subject_type set not null;

alter table event_outbox add constraint event_outbox_producer_subsystem_ck
    check (producer_subsystem ~ '^[a-z][a-z0-9-]*$');
