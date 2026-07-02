INSERT INTO nav_category (name, sort)
SELECT '开发', 1 WHERE NOT EXISTS (SELECT 1 FROM nav_category WHERE name = '开发');
INSERT INTO nav_category (name, sort)
SELECT 'AI', 2 WHERE NOT EXISTS (SELECT 1 FROM nav_category WHERE name = 'AI');
INSERT INTO nav_category (name, sort)
SELECT '运维', 3 WHERE NOT EXISTS (SELECT 1 FROM nav_category WHERE name = '运维');

INSERT INTO nav_item (title, url, category, icon, sort) VALUES
  ('GitHub', 'https://github.com', '开发', '🐙', 1),
  ('Stack Overflow', 'https://stackoverflow.com', '开发', '📚', 2),
  ('Claude', 'https://claude.ai', 'AI', '🤖', 1),
  ('ChatGPT', 'https://chat.openai.com', 'AI', '💬', 2),
  ('Uptime Kuma', 'https://monitor.magies.top', '运维', '📈', 1);
