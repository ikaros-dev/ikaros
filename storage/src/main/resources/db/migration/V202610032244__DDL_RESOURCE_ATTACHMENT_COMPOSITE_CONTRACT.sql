alter table resource_attachment
    drop constraint resource_attachment_resource_attachment_uq,
    drop column id,
    add constraint resource_attachment_pkey primary key (resource_id, attachment_id);
