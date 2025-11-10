-- Test schema initialization for feedback-service
-- This script creates the feedback schema before Hibernate DDL execution

CREATE SCHEMA IF NOT EXISTS feedback;

-- Create audit trigger function (shared across all services)
-- This function auto-populates created_date, created_by, last_modified_date, last_modified_by
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

