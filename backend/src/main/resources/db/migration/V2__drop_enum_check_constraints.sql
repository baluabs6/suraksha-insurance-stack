-- Hibernate generated CHECK constraints on policies.type and policies.status listing the enum values that
-- existed at the time. ddl-auto=update never alters them, so new policy types (TWO_WHEELER, TRAVEL, ...)
-- and the PENDING_PAYMENT status would be rejected on an existing database.
-- The Policy entity now maps both columns as plain varchar (no CHECK), so new databases never get them.
-- Guarded so it is a no-op on a brand-new database where Hibernate has not created the table yet.
DO $$
BEGIN
    IF to_regclass('policies') IS NOT NULL THEN
        ALTER TABLE policies DROP CONSTRAINT IF EXISTS policies_type_check;
        ALTER TABLE policies DROP CONSTRAINT IF EXISTS policies_status_check;
    END IF;
END $$;
