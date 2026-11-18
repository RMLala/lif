CREATE TABLE IF NOT EXISTS license_snapshots (
  license_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  payload TEXT NOT NULL,
  revision INTEGER NOT NULL CHECK(revision >= 1),
  updated_by_install TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  PRIMARY KEY(license_id, product_id)
);
CREATE TABLE IF NOT EXISTS license_sync_usage (
  license_id TEXT NOT NULL,
  day TEXT NOT NULL,
  requests INTEGER NOT NULL DEFAULT 0 CHECK(requests >= 0),
  updated_at TEXT NOT NULL,
  PRIMARY KEY(license_id, day)
);
CREATE INDEX IF NOT EXISTS idx_license_sync_usage_day ON license_sync_usage(day);
