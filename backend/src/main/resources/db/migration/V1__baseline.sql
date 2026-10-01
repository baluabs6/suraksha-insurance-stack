-- Baseline. Until Flyway was introduced the schema was created and updated by Hibernate (ddl-auto=update),
-- so a database that already exists is treated as version 1 (spring.flyway.baseline-on-migrate=true).
SELECT 1;
