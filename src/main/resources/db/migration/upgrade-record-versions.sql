-- Safe to repeat. Existing positive versions are preserved.
-- Hibernate uses the same default when adding version columns on future upgrades.
DO $$
DECLARE
    record_table text;
BEGIN
    FOREACH record_table IN ARRAY ARRAY['saving_goals', 'debts', 'bill'] LOOP
        IF to_regclass(record_table) IS NOT NULL THEN
            EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0', record_table);
            EXECUTE format('ALTER TABLE %I ALTER COLUMN version SET DEFAULT 0', record_table);
            EXECUTE format('UPDATE %I SET version = 0 WHERE version IS NULL', record_table);
            EXECUTE format('ALTER TABLE %I ALTER COLUMN version SET NOT NULL', record_table);
        END IF;
    END LOOP;
END $$;
