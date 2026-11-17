CREATE TABLE IF NOT EXISTS api_usage (
  user_sub TEXT NOT NULL REFERENCES users(user_sub) ON DELETE CASCADE,
  day TEXT NOT NULL,
  requests INTEGER NOT NULL DEFAULT 0 CHECK(requests >= 0),
  updated_at TEXT NOT NULL,
  PRIMARY KEY(user_sub, day)
);
CREATE INDEX IF NOT EXISTS idx_api_usage_day ON api_usage(day);
