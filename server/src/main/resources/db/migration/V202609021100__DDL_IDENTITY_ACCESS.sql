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

CREATE TABLE platform_role (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    role_code VARCHAR(96) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    built_in BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_platform_role_code UNIQUE (role_code)
);

CREATE TABLE role_permission (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    role_id UUID NOT NULL,
    permission_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_key)
);

CREATE TABLE user_role (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE security_session (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    user_id UUID NOT NULL,
    login_method VARCHAR(32) NOT NULL,
    current_svl INTEGER NOT NULL DEFAULT 0,
    verified_at TIMESTAMPTZ,
    verification_expires_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_active_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT ck_security_session_svl CHECK (current_svl BETWEEN 0 AND 4)
);

CREATE INDEX idx_platform_user_status_username ON platform_user (status, username);
CREATE INDEX idx_user_role_user_id ON user_role (user_id);
CREATE INDEX idx_role_permission_role_id ON role_permission (role_id);
CREATE INDEX idx_security_session_user_active ON security_session (user_id, expires_at);
