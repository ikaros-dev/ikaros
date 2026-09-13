INSERT INTO permission_registry (
    permission_key, owner_subsystem, description, risk_level, minimum_svl, fresh_verification_required
)
VALUES
    ('dashboard.read', 'console', '查看 Overview', 'LOW', 0, FALSE),
    ('ingestion.read', 'ingestion', '查看 Add Content', 'LOW', 0, FALSE),
    ('activity.read', 'activity', '查看 Activity', 'LOW', 0, FALSE),
    ('storage.read', 'storage', '查看 Storage', 'LOW', 0, FALSE),
    ('app.read', 'console', '查看 Apps', 'LOW', 0, FALSE),
    ('system.read', 'identity', '查看 System', 'LOW', 0, FALSE)
ON CONFLICT (permission_key) DO UPDATE SET
    owner_subsystem = EXCLUDED.owner_subsystem,
    description = EXCLUDED.description,
    risk_level = EXCLUDED.risk_level,
    minimum_svl = EXCLUDED.minimum_svl,
    fresh_verification_required = EXCLUDED.fresh_verification_required,
    deprecated = EXCLUDED.deprecated,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO role_permission (role_id, permission_key, created_at)
SELECT role.id, permission.permission_key, CURRENT_TIMESTAMP
FROM platform_role role
JOIN permission_registry permission ON permission.permission_key IN (
    'dashboard.read', 'ingestion.read', 'activity.read',
    'storage.read', 'app.read', 'system.read'
)
WHERE role.role_code = 'admin'
ON CONFLICT (role_id, permission_key) DO NOTHING;
