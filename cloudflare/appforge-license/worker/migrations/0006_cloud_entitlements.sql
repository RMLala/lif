ALTER TABLE licenses ADD COLUMN cloud_enabled INTEGER NOT NULL DEFAULT 0 CHECK(cloud_enabled IN (0,1));
ALTER TABLE licenses ADD COLUMN suspended_at TEXT;
ALTER TABLE licenses ADD COLUMN cloud_daily_limit INTEGER NOT NULL DEFAULT 5000 CHECK(cloud_daily_limit BETWEEN 1 AND 100000);
ALTER TABLE trials ADD COLUMN cloud_enabled INTEGER NOT NULL DEFAULT 1 CHECK(cloud_enabled IN (0,1));
UPDATE licenses SET cloud_enabled=1 WHERE status='active' AND paid_confirmed=1 AND license_type IN ('monthly','annual','lifetime','owner');
