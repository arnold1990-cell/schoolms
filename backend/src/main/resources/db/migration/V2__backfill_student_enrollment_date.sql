ALTER TABLE IF EXISTS student
    ADD COLUMN IF NOT EXISTS enrollment_date DATE;

UPDATE student
SET enrollment_date = CURRENT_DATE
WHERE enrollment_date IS NULL;

ALTER TABLE IF EXISTS student
    ALTER COLUMN enrollment_date SET NOT NULL;
