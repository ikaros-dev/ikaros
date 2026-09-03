ALTER TABLE platform_user
    ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE security_session
    ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE platform_user
    ADD CONSTRAINT ck_platform_user_security_version CHECK (security_version >= 0);

ALTER TABLE security_session
    ADD CONSTRAINT ck_security_session_security_version CHECK (security_version >= 0);
