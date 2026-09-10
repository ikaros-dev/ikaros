alter table event_outbox add column if not exists request_id varchar(128);
alter table event_outbox add column if not exists correlation_id varchar(128);
alter table event_outbox add column if not exists causation_id varchar(128);
alter table event_outbox add column if not exists actor_id uuid;

create index if not exists event_outbox_correlation_idx on event_outbox (correlation_id, occurred_at desc);
