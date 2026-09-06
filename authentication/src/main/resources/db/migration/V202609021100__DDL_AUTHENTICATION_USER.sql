CREATE TABLE platform_user (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    email VARCHAR(320),
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_platform_user_username UNIQUE (username),
    CONSTRAINT uk_platform_user_email UNIQUE (email)
);

CREATE INDEX idx_platform_user_status_username ON platform_user (status, username);
