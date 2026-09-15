ALTER TABLE platform_user
    ADD COLUMN is_del SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE platform_user
    ADD CONSTRAINT ck_platform_user_is_del CHECK (is_del IN (0, 1));

CREATE INDEX idx_platform_user_visible_status_username
    ON platform_user (status, username)
    WHERE is_del = 0;
