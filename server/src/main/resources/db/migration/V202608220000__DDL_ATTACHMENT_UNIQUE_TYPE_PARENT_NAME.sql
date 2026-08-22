-- 清理 attachment 表中 (type, parent_id, name) 重复的记录，保留每组中 id 最小的一条
delete from attachment a using attachment b
where a.id > b.id
  and a.type = b.type
  and a.parent_id = b.parent_id
  and a.name = b.name;

-- 为 attachment 表添加 (type, parent_id, name) 唯一约束，防止并发刷新目录导致重复插入
alter table attachment
    add constraint uk_attachment_type_parent_name unique (type, parent_id, name);
