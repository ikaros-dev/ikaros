alter table "attachment"
    add column if not exists "modified_time" timestamp(6);

comment on column "attachment"."modified_time" is '驱动附件对应文件在文件系统中的最后修改时间，用于增量扫描判断';
