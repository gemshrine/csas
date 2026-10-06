UPDATE groups
SET curator_id = (SELECT id FROM users WHERE username = 'demo-dean')
WHERE name = 'DEMO-301'
  AND EXISTS (SELECT 1 FROM users WHERE username = 'demo-dean');

UPDATE groups
SET curator_id = (SELECT id FROM users WHERE username = 'lab1-test-dean')
WHERE name LIKE 'TEST-%'
  AND EXISTS (SELECT 1 FROM users WHERE username = 'lab1-test-dean');
