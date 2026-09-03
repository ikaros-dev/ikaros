alter table user_role add constraint user_role_user_fk
    foreign key (user_id) references platform_user (id) on delete cascade;

alter table user_role add constraint user_role_role_fk
    foreign key (role_id) references platform_role (id) on delete cascade;

alter table role_permission add constraint role_permission_role_fk
    foreign key (role_id) references platform_role (id) on delete cascade;

alter table security_session add constraint security_session_user_fk
    foreign key (user_id) references platform_user (id) on delete cascade;
