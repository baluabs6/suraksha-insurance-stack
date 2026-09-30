-- Run ONCE on an EXISTING database before deploying the multi-insurance-type release.
--
-- Hibernate created a CHECK constraint on policies.type listing only the original enum values
-- (HEALTH, MOTOR, LIFE). ddl-auto=update never alters existing constraints, so inserting a policy
-- of a new type (TWO_WHEELER, TRAVEL, HOME, ...) would fail. Fresh databases don't need this.
--
-- The application also adds two nullable jsonb columns (policies.attributes, claims.details) by itself.

ALTER TABLE policies DROP CONSTRAINT IF EXISTS policies_type_check;
