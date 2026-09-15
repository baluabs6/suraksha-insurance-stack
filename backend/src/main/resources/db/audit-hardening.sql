-- Run this ONCE by a DBA using elevated/superuser credentials — NOT using the
-- application's own DB role (suraksha_app or whatever it's named in your
-- deployment). The point is that even a fully compromised application
-- process, using its own normal DB credentials, should not be able to
-- rewrite or erase its own audit trail.
--
-- Adjust the role name below to match your actual application DB user.

REVOKE UPDATE, DELETE, TRUNCATE ON audit_log FROM suraksha_app;
GRANT INSERT, SELECT ON audit_log TO suraksha_app;

-- Optional, stronger guarantee: also forbid the app role from altering the
-- table structure or dropping it outright.
REVOKE ALTER, DROP ON audit_log FROM suraksha_app;
