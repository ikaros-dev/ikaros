INSERT INTO permission_registry (
    permission_key,
    owner_subsystem,
    description,
    risk_level,
    minimum_svl,
    fresh_verification_required
)
VALUES
    ('system.app.read', 'app-runtime', '查看 Server App 与 Client Registration', 'LOW', 0, FALSE),
    ('system.app.manage', 'app-runtime', '管理 Server App 安装、启停与权限', 'HIGH', 2, TRUE),
    ('system.app.client.manage', 'app-runtime', '管理 Client Registration', 'HIGH', 2, TRUE)
ON CONFLICT (permission_key) DO UPDATE SET
    owner_subsystem = EXCLUDED.owner_subsystem,
    description = EXCLUDED.description,
    risk_level = EXCLUDED.risk_level,
    minimum_svl = EXCLUDED.minimum_svl,
    fresh_verification_required = EXCLUDED.fresh_verification_required,
    deprecated = FALSE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO role_permission (role_id, permission_key, created_at)
SELECT role.id, permission.permission_key, CURRENT_TIMESTAMP
FROM platform_role role
JOIN permission_registry permission ON permission.permission_key IN (
    'system.app.read',
    'system.app.manage',
    'system.app.client.manage'
)
WHERE role.role_code = 'ADMIN'
ON CONFLICT (role_id, permission_key) DO NOTHING;

INSERT INTO role_permission (role_id, permission_key, created_at)
SELECT role.id, permission.permission_key, CURRENT_TIMESTAMP
FROM platform_role role
JOIN permission_registry permission ON permission.permission_key = 'system.app.read'
WHERE role.role_code = 'OPERATOR'
ON CONFLICT (role_id, permission_key) DO NOTHING;
