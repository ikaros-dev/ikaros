INSERT INTO permission_registry (
    permission_key, owner_subsystem, description, risk_level, minimum_svl, fresh_verification_required
)
VALUES (
    'storage.attachment.manage', 'storage', '管理所有用户的附件', 'MEDIUM', 0, FALSE
)
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
JOIN permission_registry permission ON permission.permission_key = 'storage.attachment.manage'
WHERE role.role_code = 'ADMIN'
ON CONFLICT (role_id, permission_key) DO NOTHING;
