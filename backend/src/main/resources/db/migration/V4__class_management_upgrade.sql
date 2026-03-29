ALTER TABLE school_class ADD COLUMN IF NOT EXISTS code VARCHAR(255);
ALTER TABLE school_class ADD COLUMN IF NOT EXISTS level VARCHAR(255);
ALTER TABLE school_class ADD COLUMN IF NOT EXISTS academic_year VARCHAR(255);
ALTER TABLE school_class ADD COLUMN IF NOT EXISTS capacity INTEGER;
ALTER TABLE school_class ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE school_class ADD COLUMN IF NOT EXISTS class_teacher_id BIGINT;

ALTER TABLE school_class
    ADD CONSTRAINT IF NOT EXISTS uk_school_class_code UNIQUE (code);

ALTER TABLE school_class
    ADD CONSTRAINT IF NOT EXISTS fk_school_class_teacher FOREIGN KEY (class_teacher_id) REFERENCES teacher(id);

UPDATE school_class
SET code = upper(replace(trim(name), ' ', '-'))
WHERE (code IS NULL OR trim(code) = '') AND name IS NOT NULL;

CREATE TABLE IF NOT EXISTS class_subjects (
    class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (class_id, subject_id),
    CONSTRAINT fk_class_subjects_class FOREIGN KEY (class_id) REFERENCES school_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_subjects_subject FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE CASCADE
);

ALTER TABLE student ALTER COLUMN school_class_id DROP NOT NULL;
