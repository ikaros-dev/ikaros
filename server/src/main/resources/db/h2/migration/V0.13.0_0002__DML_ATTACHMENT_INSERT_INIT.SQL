-- Insert Root directory
INSERT INTO attachment (id, parent_id, type, path, name, update_time)
SELECT 0, -1, 'Directory', '/', '/', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM attachment
                  WHERE id = 0);
-- Insert Covers directory
INSERT INTO attachment (id, parent_id, type, path, name, update_time)
SELECT 1, 0, 'Directory', '/Covers', 'Covers', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM attachment
                  WHERE id = 1);
-- Insert Downloads directory
INSERT INTO attachment (id, parent_id, type, path, name, update_time)
SELECT 2, 0, 'Directory', '/Downloads', 'Downloads', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM attachment
                  WHERE id = 2);