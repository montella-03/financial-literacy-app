-- Run once against an existing PostgreSQL database after backing it up.
-- Hibernate schema updates do not reliably replace an existing enum CHECK constraint.
BEGIN;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_type_check;
ALTER TABLE transactions
    ADD CONSTRAINT transactions_type_check
        CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER'));
COMMIT;
