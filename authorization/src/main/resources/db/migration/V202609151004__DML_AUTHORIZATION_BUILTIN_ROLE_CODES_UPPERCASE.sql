UPDATE platform_role
SET role_code = CASE role_code
    WHEN 'admin' THEN 'ADMIN'
    WHEN 'operator' THEN 'OPERATOR'
    ELSE role_code
END,
updated_at = CURRENT_TIMESTAMP
WHERE built_in = TRUE
  AND role_code IN ('admin', 'operator');
