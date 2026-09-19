CREATE SCHEMA IF NOT EXISTS app_runtime;

CREATE TABLE app_runtime.app_definition (
    app_id VARCHAR(160) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    publisher VARCHAR(160) NOT NULL,
    manifest_version VARCHAR(64) NOT NULL,
    supported_api_majors INTEGER[] NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_app_definition_api_majors_nonempty
        CHECK (cardinality(supported_api_majors) > 0)
);

CREATE TABLE app_runtime.app_installation (
    app_id VARCHAR(160) PRIMARY KEY,
    package_version VARCHAR(64) NOT NULL,
    lifecycle_state VARCHAR(32) NOT NULL,
    platform_api_min VARCHAR(64) NOT NULL,
    platform_api_max VARCHAR(64),
    configuration_state VARCHAR(32) NOT NULL,
    failure_code VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0,
    installed_at TIMESTAMPTZ NOT NULL,
    enabled_at TIMESTAMPTZ,
    disabled_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT app_installation_definition_fk
        FOREIGN KEY (app_id) REFERENCES app_runtime.app_definition(app_id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_installation_state CHECK (lifecycle_state IN (
        'DISCOVERED', 'INSTALLING', 'INSTALLED', 'ENABLING', 'ENABLED',
        'DISABLING', 'DISABLED', 'UPGRADING', 'FAILED',
        'UNINSTALLING', 'UNINSTALLED', 'INCOMPATIBLE'
    )),
    CONSTRAINT ck_app_installation_version CHECK (version >= 0)
);

CREATE INDEX idx_app_installation_state
    ON app_runtime.app_installation(lifecycle_state, updated_at);

CREATE TABLE app_runtime.app_scope_definition (
    app_id VARCHAR(160) NOT NULL,
    scope_key VARCHAR(160) NOT NULL,
    description VARCHAR(256) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (app_id, scope_key),
    CONSTRAINT app_scope_definition_app_fk
        FOREIGN KEY (app_id) REFERENCES app_runtime.app_definition(app_id) ON DELETE CASCADE
);

CREATE TABLE app_runtime.app_permission_grant (
    app_id VARCHAR(160) NOT NULL,
    permission_key VARCHAR(160) NOT NULL,
    grant_status VARCHAR(16) NOT NULL,
    granted_by_user_id UUID,
    granted_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (app_id, permission_key),
    CONSTRAINT app_permission_grant_app_fk
        FOREIGN KEY (app_id) REFERENCES app_runtime.app_definition(app_id) ON DELETE CASCADE,
    CONSTRAINT ck_app_permission_grant_status
        CHECK (grant_status IN ('GRANTED', 'DENIED', 'REVOKED'))
);

CREATE TABLE app_runtime.app_client_registration (
    client_id VARCHAR(200) PRIMARY KEY,
    app_id VARCHAR(160) NOT NULL,
    name VARCHAR(160) NOT NULL,
    client_type VARCHAR(32) NOT NULL,
    publisher VARCHAR(160) NOT NULL,
    official BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT app_client_registration_app_fk
        FOREIGN KEY (app_id) REFERENCES app_runtime.app_definition(app_id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_client_type
        CHECK (client_type IN ('PUBLIC_NATIVE', 'PUBLIC_BROWSER', 'CONFIDENTIAL_SERVER')),
    CONSTRAINT ck_app_client_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'REVOKED'))
);

CREATE INDEX idx_app_client_registration_app
    ON app_runtime.app_client_registration(app_id, status, client_id);

CREATE TABLE app_runtime.app_client_redirect_uri (
    client_id VARCHAR(200) NOT NULL,
    redirect_uri VARCHAR(2048) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (client_id, redirect_uri),
    CONSTRAINT app_client_redirect_uri_client_fk
        FOREIGN KEY (client_id) REFERENCES app_runtime.app_client_registration(client_id) ON DELETE CASCADE
);

CREATE TABLE app_runtime.app_dependency (
    app_id VARCHAR(160) NOT NULL,
    dependency_app_id VARCHAR(160) NOT NULL,
    api_requirement VARCHAR(128),
    required BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (app_id, dependency_app_id),
    CONSTRAINT app_dependency_app_fk
        FOREIGN KEY (app_id) REFERENCES app_runtime.app_definition(app_id) ON DELETE CASCADE,
    CONSTRAINT ck_app_dependency_not_self CHECK (app_id <> dependency_app_id)
);

CREATE TABLE app_runtime.app_migration_history (
    app_id VARCHAR(160) NOT NULL,
    migration_version VARCHAR(128) NOT NULL,
    checksum VARCHAR(256) NOT NULL,
    status VARCHAR(16) NOT NULL,
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (app_id, migration_version),
    CONSTRAINT app_migration_history_app_fk
        FOREIGN KEY (app_id) REFERENCES app_runtime.app_definition(app_id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_migration_history_status CHECK (status IN ('PENDING', 'APPLIED', 'FAILED'))
);
