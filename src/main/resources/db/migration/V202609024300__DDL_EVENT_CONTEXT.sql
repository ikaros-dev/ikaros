alter table event_outbox add column request_id varchar(128);
alter table event_outbox add column correlation_id varchar(128);
alter table event_outbox add column causation_id varchar(128);
alter table event_outbox add column actor_id uuid;

create index event_outbox_correlation_idx on event_outbox (correlation_id, occurred_at desc);
