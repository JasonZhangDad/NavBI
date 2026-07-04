CREATE TABLE IF NOT EXISTS nav_category (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(64) NOT NULL UNIQUE,
  sort INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nav_category_sort ON nav_category (sort, id);

CREATE TABLE IF NOT EXISTS nav_item (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(128) NOT NULL,
  url VARCHAR(512) NOT NULL,
  category VARCHAR(64) NOT NULL DEFAULT '默认',
  icon VARCHAR(512),
  sort INT NOT NULL DEFAULT 0,
  click_count BIGINT NOT NULL DEFAULT 0,
  uv_count BIGINT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO nav_category (name, sort)
SELECT '默认', 0
WHERE NOT EXISTS (SELECT 1 FROM nav_category WHERE name = '默认');

INSERT INTO nav_category (name, sort)
SELECT ni.category, MIN(ni.sort)
FROM nav_item ni
LEFT JOIN nav_category c ON c.name = ni.category
WHERE ni.category IS NOT NULL
  AND ni.category <> ''
  AND c.id IS NULL
GROUP BY ni.category;

CREATE TABLE IF NOT EXISTS visit_log (
  id BIGSERIAL PRIMARY KEY,
  ip VARCHAR(64),
  country VARCHAR(64),
  province VARCHAR(64),
  city VARCHAR(64),
  device VARCHAR(32),
  os VARCHAR(64),
  browser VARCHAR(64),
  url VARCHAR(512),
  referer VARCHAR(512),
  user_agent VARCHAR(1024),
  session_id VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visit_log_created_at ON visit_log (created_at);
CREATE INDEX IF NOT EXISTS idx_visit_log_url ON visit_log (url);
CREATE INDEX IF NOT EXISTS idx_visit_log_ip ON visit_log (ip);
CREATE INDEX IF NOT EXISTS idx_visit_log_session ON visit_log (session_id);

CREATE TABLE IF NOT EXISTS app_user (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(128) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  register_ip VARCHAR(64),
  country VARCHAR(64),
  province VARCHAR(64),
  city VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 安全迁移：已有表补列
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS register_ip VARCHAR(64);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS country VARCHAR(64);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS province VARCHAR(64);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS city VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_app_user_created_at ON app_user (created_at);
CREATE INDEX IF NOT EXISTS idx_app_user_region ON app_user (country, province);

CREATE TABLE IF NOT EXISTS email_code (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(128) NOT NULL,
  code VARCHAR(8) NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_code_email ON email_code (email, id);

CREATE TABLE IF NOT EXISTS api_log (
  id BIGSERIAL PRIMARY KEY,
  method VARCHAR(8),
  path VARCHAR(256),
  ip VARCHAR(64),
  username VARCHAR(128),
  status INT,
  cost_ms INT,
  user_agent VARCHAR(512),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_log_created_at ON api_log (created_at);

CREATE TABLE IF NOT EXISTS user_session (
  id BIGSERIAL PRIMARY KEY,
  session_id VARCHAR(64) NOT NULL UNIQUE,
  ip VARCHAR(64),
  start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  end_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  page_count INT NOT NULL DEFAULT 0
);
