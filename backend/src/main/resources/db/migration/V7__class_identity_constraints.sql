ALTER TABLE school_class DROP CONSTRAINT IF EXISTS school_class_name_key;
ALTER TABLE school_class DROP CONSTRAINT IF EXISTS uk_school_class_name;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_school_class_level_stream_year'
    ) THEN
        ALTER TABLE school_class
            ADD CONSTRAINT uk_school_class_level_stream_year UNIQUE (level, stream, academic_year);
    END IF;
END
$$;
