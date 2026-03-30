package com.schoolms.classmanagement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    boolean existsByLevelIgnoreCaseAndStreamIgnoreCaseAndAcademicYearIgnoreCase(String level, String stream, String academicYear);

    boolean existsByLevelIgnoreCaseAndStreamIgnoreCaseAndAcademicYearIgnoreCaseAndIdNot(
            String level,
            String stream,
            String academicYear,
            Long id
    );

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<SchoolClass> findByClassTeacherId(Long teacherId);
}
