alter table media_delivery_lease add column binding_id uuid references media_delivery_binding(id);
alter table media_delivery_lease add column selection_epoch bigint not null default 1;
alter table media_delivery_lease add column selected_at timestamptz;
alter table media_delivery_lease add column selection_reason varchar(64) not null default 'PRIMARY';
alter table media_delivery_lease add column fallback_index integer not null default 0;
alter table media_delivery_lease add column health_snapshot_version varchar(128);
create index idx_media_delivery_lease_binding on media_delivery_lease(binding_id, selection_epoch);
