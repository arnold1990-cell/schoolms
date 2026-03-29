ALTER TABLE school_class ADD COLUMN IF NOT EXISTS status VARCHAR(20);
UPDATE school_class SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE school_class ALTER COLUMN status SET NOT NULL;
