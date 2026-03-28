CREATE TABLE IF NOT EXISTS teacher (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE teacher ADD COLUMN IF NOT EXISTS employee_number VARCHAR(100);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS middle_name VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS title VARCHAR(50);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS gender VARCHAR(50);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS phone_number VARCHAR(100);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS alternative_phone_number VARCHAR(100);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS national_id VARCHAR(150);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS passport_number VARCHAR(150);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS department VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS employment_type VARCHAR(50);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS hire_date DATE;
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS status VARCHAR(50);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS address VARCHAR(600);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(100);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS emergency_contact_relationship VARCHAR(100);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS qualification VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS highest_education_level VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS years_of_experience INTEGER;
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS staff_role VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS salary_grade VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS notes VARCHAR(1500);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(500);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE teacher ADD COLUMN IF NOT EXISTS staff_code VARCHAR(255);
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS phone VARCHAR(255);

UPDATE teacher
SET employee_number = COALESCE(NULLIF(employee_number, ''), NULLIF(staff_code, ''), CONCAT('EMP-', id))
WHERE employee_number IS NULL OR employee_number = '';

UPDATE teacher
SET phone_number = COALESCE(NULLIF(phone_number, ''), NULLIF(phone, ''), 'N/A')
WHERE phone_number IS NULL OR phone_number = '';

UPDATE teacher
SET title = COALESCE(NULLIF(title, ''), 'MR'),
    gender = COALESCE(NULLIF(gender, ''), 'OTHER'),
    department = COALESCE(NULLIF(department, ''), 'General'),
    specialization = COALESCE(NULLIF(specialization, ''), 'General Studies'),
    employment_type = COALESCE(NULLIF(employment_type, ''), 'FULL_TIME'),
    status = COALESCE(NULLIF(status, ''), 'ACTIVE'),
    address = COALESCE(NULLIF(address, ''), 'Not provided'),
    hire_date = COALESCE(hire_date, CURRENT_DATE),
    email = COALESCE(NULLIF(email, ''), CONCAT('teacher-', id, '@schoolms.local')),
    first_name = COALESCE(NULLIF(first_name, ''), 'Teacher'),
    last_name = COALESCE(NULLIF(last_name, ''), CONCAT('No', id::TEXT));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_teacher_user'
    ) THEN
        ALTER TABLE teacher
            ADD CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_teacher_employee_number ON teacher (lower(employee_number));
CREATE UNIQUE INDEX IF NOT EXISTS uk_teacher_email ON teacher (lower(email));
CREATE UNIQUE INDEX IF NOT EXISTS uk_teacher_national_id ON teacher (lower(national_id)) WHERE national_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_teacher_user_id ON teacher (user_id);

ALTER TABLE teacher ALTER COLUMN employee_number SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN last_name SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN title SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN gender SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN phone_number SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN email SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN department SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN specialization SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN employment_type SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN hire_date SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN status SET NOT NULL;
ALTER TABLE teacher ALTER COLUMN address SET NOT NULL;

CREATE TABLE IF NOT EXISTS teacher_classes (
    teacher_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (teacher_id, class_id)
);
