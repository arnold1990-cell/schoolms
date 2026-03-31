ALTER TABLE IF EXISTS student
    ALTER COLUMN date_of_birth DROP NOT NULL,
    ALTER COLUMN guardian_name DROP NOT NULL,
    ALTER COLUMN guardian_relationship DROP NOT NULL,
    ALTER COLUMN guardian_phone DROP NOT NULL,
    ALTER COLUMN address DROP NOT NULL;
