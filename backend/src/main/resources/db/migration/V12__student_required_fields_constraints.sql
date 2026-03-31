ALTER TABLE IF EXISTS student
    ALTER COLUMN date_of_birth SET NOT NULL,
    ALTER COLUMN guardian_name SET NOT NULL,
    ALTER COLUMN guardian_relationship SET NOT NULL,
    ALTER COLUMN guardian_phone SET NOT NULL,
    ALTER COLUMN address SET NOT NULL,
    ALTER COLUMN school_class_id SET NOT NULL;
