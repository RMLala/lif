CREATE TABLE IF NOT EXISTS users (
  user_sub TEXT PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  password_salt TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS organizations (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 100),
  created_by TEXT NOT NULL REFERENCES users(user_sub),
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS memberships (
  org_id TEXT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  user_sub TEXT NOT NULL REFERENCES users(user_sub) ON DELETE CASCADE,
  role TEXT NOT NULL CHECK(role IN ('owner','admin','editor','viewer')),
  status TEXT NOT NULL CHECK(status IN ('active','suspended')),
  created_at TEXT NOT NULL,
  PRIMARY KEY(org_id,user_sub)
);
CREATE INDEX IF NOT EXISTS idx_memberships_user ON memberships(user_sub,status);
CREATE TABLE IF NOT EXISTS invitations (
  id TEXT PRIMARY KEY,
  org_id TEXT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  email TEXT NOT NULL,
  role TEXT NOT NULL CHECK(role IN ('admin','editor','viewer')),
  token_hash TEXT NOT NULL UNIQUE,
  created_by TEXT NOT NULL REFERENCES users(user_sub),
  expires_at TEXT NOT NULL,
  created_at TEXT NOT NULL,
  accepted_at TEXT,
  accepted_by TEXT REFERENCES users(user_sub)
);
CREATE INDEX IF NOT EXISTS idx_invites_org ON invitations(org_id,created_at DESC);
CREATE TABLE IF NOT EXISTS sessions (
  token_hash TEXT PRIMARY KEY,
  user_sub TEXT NOT NULL REFERENCES users(user_sub) ON DELETE CASCADE,
  expires_at TEXT NOT NULL,
  created_at TEXT NOT NULL,
  revoked_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_sub,expires_at);
CREATE TABLE IF NOT EXISTS auth_attempts (
  key_hash TEXT PRIMARY KEY,
  window_started INTEGER NOT NULL,
  failures INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS password_resets (
  id TEXT PRIMARY KEY,
  user_sub TEXT NOT NULL REFERENCES users(user_sub) ON DELETE CASCADE,
  token_hash TEXT NOT NULL UNIQUE,
  expires_at TEXT NOT NULL,
  created_by TEXT NOT NULL REFERENCES users(user_sub),
  created_at TEXT NOT NULL,
  used_at TEXT
);
CREATE TABLE IF NOT EXISTS app_snapshots (
  org_id TEXT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  app_id TEXT NOT NULL,
  payload TEXT NOT NULL,
  revision INTEGER NOT NULL CHECK(revision >= 1),
  updated_by TEXT NOT NULL REFERENCES users(user_sub),
  updated_at TEXT NOT NULL,
  PRIMARY KEY(org_id,app_id)
);
