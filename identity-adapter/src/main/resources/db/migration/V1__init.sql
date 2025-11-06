-- =============================================
-- Identity Schema - Flyway Migration V1
-- =============================================
-- Description: Initial schema creation for Identity Adapter Service
-- Author: Car Sharing Development Team
-- Date: 2025-11-05
-- =============================================

-- Ensure UTC timezone (informational; set at cluster/instance level is preferred)
-- SET TIME ZONE 'UTC';

-- 1. Create schema
CREATE SCHEMA IF NOT EXISTS identity;

-- 2. Install required extensions
CREATE EXTENSION IF NOT EXISTS citext;  -- Case-insensitive text type for username/email

-- 3. Create audit trigger function (if not exists from other services)
CREATE OR REPLACE FUNCTION public.set_audit_fields() RETURNS trigger AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    NEW.created_date := COALESCE(NEW.created_date, now());
    NEW.created_by   := COALESCE(NEW.created_by, current_setting('app.current_account_id', true), 'system');
  END IF;
  NEW.last_modified_date := now();
  NEW.last_modified_by   := COALESCE(current_setting('app.current_account_id', true), NEW.last_modified_by, NEW.created_by, 'system');
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- 4. Create accounts table
CREATE TABLE IF NOT EXISTS identity.accounts (
  id VARCHAR(255) PRIMARY KEY,               -- Stores external subject/ID (e.g., Keycloak sub)
  username CITEXT NOT NULL UNIQUE,           -- Case-insensitive unique username
  email CITEXT UNIQUE,                       -- Case-insensitive unique email (NULLs allowed)
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  phone_number VARCHAR(50),
  image_url TEXT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,     -- Soft delete flag

  -- Audit fields
  created_date TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_modified_date TIMESTAMPTZ,
  created_by VARCHAR(255) NOT NULL DEFAULT 'system',
  last_modified_by VARCHAR(255)
);

-- 5. Create audit trigger
CREATE TRIGGER trg_accounts_audit
BEFORE INSERT OR UPDATE ON identity.accounts
FOR EACH ROW EXECUTE FUNCTION public.set_audit_fields();

-- 6. Create indexes
-- Email unique index (PostgreSQL UNIQUE allows multiple NULLs)
CREATE UNIQUE INDEX IF NOT EXISTS uq_identity_accounts_email ON identity.accounts(email);

-- Username index (already covered by UNIQUE constraint, but explicit for clarity)
CREATE UNIQUE INDEX IF NOT EXISTS uq_identity_accounts_username ON identity.accounts(username);

-- 7. Enable Row Level Security (optional, configured per deployment)
-- Uncomment if RLS is required for multi-tenant scenarios
-- ALTER TABLE identity.accounts ENABLE ROW LEVEL SECURITY;

-- Example RLS policy (users can only see/update their own account)
-- CREATE POLICY accounts_select_own ON identity.accounts
--   FOR SELECT USING (id = current_setting('app.current_account_id', true));
-- CREATE POLICY accounts_update_own ON identity.accounts
--   FOR UPDATE USING (id = current_setting('app.current_account_id', true));

-- 8. Create service role and grant permissions
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'identity_service') THEN
    CREATE ROLE identity_service NOLOGIN;
  END IF;
END $$;

GRANT USAGE ON SCHEMA identity TO identity_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA identity TO identity_service;

-- 9. Default privileges for future tables (if schema owner differs from migration user)
-- ALTER DEFAULT PRIVILEGES IN SCHEMA identity GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO identity_service;

-- 10. Add helpful comments
COMMENT ON SCHEMA identity IS 'Identity management schema for user accounts and authentication';
COMMENT ON TABLE identity.accounts IS 'User accounts synchronized with OIDC provider (Keycloak)';
COMMENT ON COLUMN identity.accounts.id IS 'External subject ID from OIDC provider (e.g., Keycloak sub claim)';
COMMENT ON COLUMN identity.accounts.username IS 'Unique username (case-insensitive via citext)';
COMMENT ON COLUMN identity.accounts.email IS 'Email address (case-insensitive, unique when not NULL)';
COMMENT ON COLUMN identity.accounts.enabled IS 'Account enabled status (soft delete support)';

-- =============================================
-- End of V1__init.sql
-- =============================================
