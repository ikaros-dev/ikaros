alter table user_resource_state add constraint user_resource_state_user_fk
    foreign key (user_id) references platform_user (id) on delete cascade;
