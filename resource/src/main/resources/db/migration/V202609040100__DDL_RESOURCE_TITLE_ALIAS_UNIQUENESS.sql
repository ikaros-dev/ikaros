-- 同一语言允许多个别名；标题和完全相同的别名仍保持唯一。
alter table resource_title
    drop constraint if exists resource_title_resource_id_locale_key;

alter table resource_title
    add constraint resource_title_value_unique
    unique (resource_id, locale, title_kind, title);
