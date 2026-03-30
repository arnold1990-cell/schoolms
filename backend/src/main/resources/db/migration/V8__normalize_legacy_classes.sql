WITH derived AS (
    SELECT
        sc.id,
        COALESCE(
            NULLIF(BTRIM(sc.level), ''),
            NULLIF(SUBSTRING(COALESCE(sc.stream, '') FROM '(?i)(?:standard|grade|class)\s*([0-9]{1,2})'), ''),
            NULLIF(SUBSTRING(COALESCE(sc.name, '') FROM '(?i)(?:standard|grade|class)\s*([0-9]{1,2})'), ''),
            NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(?i)(?:standard|grade|class)-?([0-9]{1,2})'), ''),
            NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(?i)grade-([0-9]{1,2})-'), '')
        ) AS derived_level,
        UPPER(
            COALESCE(
                NULLIF(
                    CASE
                        WHEN sc.stream ~* '^[A-Z0-9]{1,3}$' THEN BTRIM(sc.stream)
                        ELSE NULL
                    END,
                    ''
                ),
                NULLIF(
                    CASE
                        WHEN sc.name ~* '^[A-Z]{1,3}$' THEN BTRIM(sc.name)
                        ELSE NULL
                    END,
                    ''
                ),
                NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(?i)^([A-Z]{1,3})-'), ''),
                NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(?i)grade-[0-9]{1,2}-([A-Z0-9]{1,3})(?:-|$)'), '')
            )
        ) AS derived_stream,
        COALESCE(
            NULLIF(BTRIM(sc.academic_year), ''),
            NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(20[0-9]{2})'), ''),
            NULLIF(SUBSTRING(COALESCE(sc.name, '') FROM '(20[0-9]{2})'), '')
        ) AS derived_year,
        sc.status,
        ROW_NUMBER() OVER (
            PARTITION BY
                COALESCE(
                    NULLIF(BTRIM(sc.level), ''),
                    NULLIF(SUBSTRING(COALESCE(sc.stream, '') FROM '(?i)(?:standard|grade|class)\s*([0-9]{1,2})'), ''),
                    NULLIF(SUBSTRING(COALESCE(sc.name, '') FROM '(?i)(?:standard|grade|class)\s*([0-9]{1,2})'), ''),
                    NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(?i)(?:standard|grade|class)-?([0-9]{1,2})'), ''),
                    NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(?i)grade-([0-9]{1,2})-'), '')
                ),
                UPPER(
                    COALESCE(
                        NULLIF(CASE WHEN sc.stream ~* '^[A-Z0-9]{1,3}$' THEN BTRIM(sc.stream) ELSE NULL END, ''),
                        NULLIF(CASE WHEN sc.name ~* '^[A-Z]{1,3}$' THEN BTRIM(sc.name) ELSE NULL END, ''),
                        NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(?i)^([A-Z]{1,3})-'), ''),
                        NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(?i)grade-[0-9]{1,2}-([A-Z0-9]{1,3})(?:-|$)'), '')
                    )
                ),
                COALESCE(
                    NULLIF(BTRIM(sc.academic_year), ''),
                    NULLIF(SUBSTRING(COALESCE(sc.code, '') FROM '(20[0-9]{2})'), ''),
                    NULLIF(SUBSTRING(COALESCE(sc.name, '') FROM '(20[0-9]{2})'), '')
                )
            ORDER BY sc.id
        ) AS identity_rank
    FROM school_class sc
), normalized AS (
    SELECT
        id,
        NULLIF(BTRIM(derived_level), '') AS level,
        NULLIF(BTRIM(derived_stream), '') AS stream,
        CASE WHEN derived_year ~ '^[0-9]{4}$' THEN derived_year ELSE NULL END AS academic_year,
        status,
        identity_rank
    FROM derived
)
UPDATE school_class sc
SET
    level = n.level,
    stream = n.stream,
    academic_year = CASE WHEN n.identity_rank = 1 THEN n.academic_year ELSE NULL END,
    name = CASE
        WHEN n.level IS NOT NULL AND n.stream IS NOT NULL THEN CONCAT('Grade ', n.level, n.stream)
        ELSE COALESCE(NULLIF(BTRIM(sc.name), ''), CONCAT('Legacy Class ', sc.id))
    END,
    code = CASE
        WHEN n.level IS NOT NULL AND n.stream IS NOT NULL AND n.academic_year IS NOT NULL AND n.identity_rank = 1
            THEN UPPER(REPLACE(CONCAT('GRADE-', n.level, '-', n.stream, '-', n.academic_year), ' ', '-'))
        ELSE CONCAT('LEGACY-CLASS-', sc.id)
    END,
    status = CASE
        WHEN n.level IS NULL OR n.stream IS NULL OR n.academic_year IS NULL OR n.identity_rank > 1 THEN 'INACTIVE'
        ELSE COALESCE(sc.status, 'ACTIVE')
    END
FROM normalized n
WHERE sc.id = n.id;
