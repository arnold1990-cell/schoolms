package com.schoolms.marks;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkRepository extends JpaRepository<Mark, Long> {
    List<Mark> findByExamId(Long examId);
    Optional<Mark> findByExamIdAndStudentId(Long examId, Long studentId);
    List<Mark> findByStudentId(Long studentId);

    boolean existsByStudentSchoolClassId(Long classId);
}
