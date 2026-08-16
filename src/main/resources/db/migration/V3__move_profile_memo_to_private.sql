-- The profile memo lives under /private so public listings skip it by prefix.
UPDATE memos
SET name = '/private/profile'
WHERE name = '/profile'
  AND NOT EXISTS (SELECT 1 FROM memos m WHERE m.name = '/private/profile');
