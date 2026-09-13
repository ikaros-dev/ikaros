INSERT INTO permission_registry (
    permission_key, owner_subsystem, description, risk_level, minimum_svl, fresh_verification_required
)
VALUES
    ('account.self.read', 'identity', '查看个人资料', 'LOW', 0, FALSE),
    ('account.preference.read', 'identity', '查看个人偏好', 'LOW', 0, FALSE),
    ('account.notification.read', 'identity', '查看个人通知设置', 'LOW', 0, FALSE),
    ('account.security.read', 'identity', '查看账户安全', 'MEDIUM', 0, FALSE),
    ('user.read', 'identity', '查看用户管理', 'LOW', 0, FALSE),
    ('role.read', 'identity', '查看角色与权限', 'LOW', 0, FALSE),
    ('security.authentication.read', 'identity', '查看身份认证配置', 'MEDIUM', 0, FALSE),
    ('integration.read', 'integration', '查看平台集成', 'LOW', 0, FALSE),
    ('notification.read', 'notification', '查看通知中心', 'LOW', 0, FALSE),
    ('audit.read', 'audit', '查看审计日志', 'MEDIUM', 0, FALSE),
    ('platform.read', 'platform', '查看系统参数', 'LOW', 0, FALSE),
    ('system.health.read', 'system', '查看系统健康', 'LOW', 0, FALSE),
    ('system.diagnostics.read', 'system', '查看系统诊断', 'MEDIUM', 0, FALSE),
    ('collection.read', 'resource', '查看资源集合', 'LOW', 0, FALSE),
    ('search.use', 'resource', '使用资源搜索', 'LOW', 0, FALSE),
    ('storage.policy.read', 'storage', '查看存储策略', 'LOW', 0, FALSE),
    ('storage.archive.read', 'storage', '查看归档管理', 'LOW', 0, FALSE),
    ('storage.maintenance.read', 'storage', '查看存储维护', 'MEDIUM', 0, FALSE),
    ('backup.read', 'storage', '查看备份管理', 'MEDIUM', 0, FALSE),
    ('drive.space.read', 'drive', '查看云盘空间', 'LOW', 0, FALSE),
    ('drive.file.read', 'drive', '查看云盘文件', 'LOW', 0, FALSE),
    ('drive.transfer.read', 'drive', '查看云盘传输', 'LOW', 0, FALSE),
    ('drive.sync.read', 'drive', '查看云盘同步', 'LOW', 0, FALSE),
    ('drive.conflict.read', 'drive', '查看云盘冲突', 'LOW', 0, FALSE),
    ('drive.trash.read', 'drive', '查看云盘回收站', 'LOW', 0, FALSE),
    ('drive.settings.read', 'drive', '查看云盘设置', 'LOW', 0, FALSE),
    ('document.read', 'document', '查看文档', 'LOW', 0, FALSE),
    ('media.read', 'media', '查看媒体', 'LOW', 0, FALSE),
    ('planning.read', 'planning', '查看计划', 'LOW', 0, FALSE),
    ('finance.read', 'finance', '查看财务', 'MEDIUM', 0, FALSE),
    ('private_note.read', 'private-note', '查看私密笔记', 'HIGH', 1, TRUE),
    ('password.read', 'password', '查看密码库', 'CRITICAL', 2, TRUE),
    ('ai.read', 'ai', '查看 AI 应用', 'LOW', 0, FALSE),
    ('share.read', 'sharing', '查看分享', 'LOW', 0, FALSE),
    ('analytics.read', 'analytics', '查看数据分析', 'LOW', 0, FALSE),
    ('automation.read', 'automation', '查看自动化', 'MEDIUM', 0, FALSE)
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
    'account.self.read', 'account.preference.read', 'account.notification.read', 'account.security.read',
    'user.read', 'role.read', 'security.authentication.read', 'integration.read', 'notification.read',
    'audit.read', 'platform.read', 'system.health.read', 'system.diagnostics.read', 'collection.read',
    'search.use', 'storage.policy.read', 'storage.archive.read', 'storage.maintenance.read', 'backup.read',
    'drive.space.read', 'drive.file.read', 'drive.transfer.read', 'drive.sync.read', 'drive.conflict.read',
    'drive.trash.read', 'drive.settings.read', 'document.read', 'media.read', 'planning.read', 'finance.read',
    'private_note.read', 'password.read', 'ai.read', 'share.read', 'analytics.read', 'automation.read'
)
WHERE role.role_code = 'admin'
ON CONFLICT (role_id, permission_key) DO NOTHING;
