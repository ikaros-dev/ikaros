alter table media_delivery_binding
    drop column if exists origin_type,
    drop column if exists auth_mode;
