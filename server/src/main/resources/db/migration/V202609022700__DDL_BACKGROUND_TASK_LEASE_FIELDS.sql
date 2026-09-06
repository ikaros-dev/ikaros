alter table background_task add column lease_owner varchar(256);
alter table background_task add column lease_token uuid;
alter table background_task add column lease_expires_at timestamptz;
alter table background_task add column progress jsonb not null default '{}'::jsonb;
