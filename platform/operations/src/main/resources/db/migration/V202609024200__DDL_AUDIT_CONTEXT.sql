alter table audit_event add column request_id varchar(128);
alter table audit_event add column correlation_id varchar(128);

create index audit_event_correlation_idx on audit_event (correlation_id, occurred_at desc);
