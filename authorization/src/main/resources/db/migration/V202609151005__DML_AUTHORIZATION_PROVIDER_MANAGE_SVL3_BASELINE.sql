UPDATE permission_registry
SET minimum_svl = 3,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_key = 'storage.provider.manage';
