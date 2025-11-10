-- Trigger setup for audit fields in tests
-- This script is executed after Hibernate DDL generation

CREATE TRIGGER trg_feedback_audit
BEFORE INSERT OR UPDATE ON feedback.cars_feedback
FOR EACH ROW EXECUTE FUNCTION public.set_audit_fields();

