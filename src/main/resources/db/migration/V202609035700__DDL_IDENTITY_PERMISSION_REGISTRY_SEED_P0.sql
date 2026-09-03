CREATE TABLE permission_registry (
    permission_key VARCHAR(128) PRIMARY KEY,
    owner_subsystem VARCHAR(64) NOT NULL,
    description VARCHAR(256) NOT NULL,
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW',
    minimum_svl INTEGER NOT NULL DEFAULT 0,
    fresh_verification_required BOOLEAN NOT NULL DEFAULT FALSE,
    deprecated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_permission_registry_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_permission_registry_svl CHECK (minimum_svl >= 0)
);

INSERT INTO permission_registry (
    permission_key, owner_subsystem, description, risk_level, minimum_svl, fresh_verification_required
)
VALUES
    ('system.user.read', 'identity', '查看用户', 'LOW', 0, FALSE),
    ('system.user.manage', 'identity', '管理用户', 'HIGH', 2, TRUE),
    ('system.role.read', 'identity', '查看角色', 'LOW', 0, FALSE),
    ('system.role.manage', 'identity', '管理角色', 'HIGH', 2, TRUE),
    ('system.session.read', 'identity', '查看会话', 'MEDIUM', 0, FALSE),
    ('system.session.manage', 'identity', '管理会话', 'HIGH', 2, TRUE),
    ('system.audit.read', 'audit', '查看审计记录', 'MEDIUM', 0, FALSE),
    ('resource.read', 'resource', '读取资源', 'LOW', 0, FALSE),
    ('resource.write', 'resource', '编辑资源', 'MEDIUM', 0, FALSE),
    ('resource.delete', 'resource', '删除资源', 'HIGH', 1, TRUE),
    ('resource.download', 'resource', '下载资源', 'MEDIUM', 0, FALSE),
    ('resource.share', 'resource', '分享资源', 'MEDIUM', 0, FALSE),
    ('storage.provider.read', 'storage', '查看存储 Provider', 'LOW', 0, FALSE),
    ('storage.provider.manage', 'storage', '管理存储 Provider', 'CRITICAL', 3, TRUE),
    ('storage.delivery.read', 'storage', '查看 Delivery Provider 与 Binding', 'LOW', 0, FALSE),
    ('storage.delivery.manage', 'storage', '管理 Delivery Provider 与 Binding', 'HIGH', 2, TRUE),
    ('storage.tiering.manage', 'storage', '管理存储分层与恢复预算', 'HIGH', 2, TRUE),
    ('storage.restore.request', 'storage', '请求媒体恢复', 'MEDIUM', 0, FALSE),
    ('storage.restore.read', 'storage', '查看媒体恢复状态', 'LOW', 0, FALSE),
    ('storage.restore.manage', 'storage', '管理媒体恢复任务', 'HIGH', 2, TRUE),
    ('ingestion.source.manage', 'ingestion', '管理导入来源', 'HIGH', 2, TRUE)
ON CONFLICT (permission_key) DO UPDATE SET
    owner_subsystem = EXCLUDED.owner_subsystem,
    description = EXCLUDED.description,
    risk_level = EXCLUDED.risk_level,
    minimum_svl = EXCLUDED.minimum_svl,
    fresh_verification_required = EXCLUDED.fresh_verification_required,
    deprecated = EXCLUDED.deprecated,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO platform_role (role_code, name, description, built_in, created_at, updated_at)
VALUES
    ('admin', 'Administrator', 'Full platform administration role.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('operator', 'Operator', 'Operational storage and restore management role.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (role_code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    built_in = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO role_permission (role_id, permission_key, created_at)
SELECT role.id, permission.permission_key, CURRENT_TIMESTAMP
FROM platform_role role
JOIN permission_registry permission ON TRUE
WHERE role.role_code = 'admin'
ON CONFLICT (role_id, permission_key) DO NOTHING;

INSERT INTO role_permission (role_id, permission_key, created_at)
SELECT role.id, permission.permission_key, CURRENT_TIMESTAMP
FROM platform_role role
JOIN permission_registry permission
  ON permission.permission_key IN (
      'system.user.read', 'system.role.read', 'system.session.read', 'system.audit.read',
      'resource.read', 'resource.download', 'storage.provider.read', 'storage.delivery.read',
      'storage.restore.read', 'storage.restore.manage', 'storage.tiering.manage'
  )
WHERE role.role_code = 'operator'
ON CONFLICT (role_id, permission_key) DO NOTHING;

ALTER TABLE role_permission
    ADD CONSTRAINT role_permission_permission_registry_fk
    FOREIGN KEY (permission_key) REFERENCES permission_registry (permission_key)
    ON DELETE RESTRICT;

CREATE INDEX idx_permission_registry_owner ON permission_registry (owner_subsystem, deprecated);
