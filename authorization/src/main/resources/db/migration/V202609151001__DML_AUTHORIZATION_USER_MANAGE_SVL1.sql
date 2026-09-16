UPDATE permission_registry
SET minimum_svl = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_key = 'system.user.manage';
