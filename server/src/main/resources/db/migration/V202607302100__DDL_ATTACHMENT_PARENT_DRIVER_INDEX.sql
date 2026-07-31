create index if not exists "idx_attachment_parent_driver"
    on "attachment" ("parent_id", "driver_id");

comment on index "idx_attachment_parent_driver" is '加速附件驱动按父目录加载已有附件';
