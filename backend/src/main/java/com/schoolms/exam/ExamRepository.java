package com.schoolms.exam;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findBySchoolClassId(Long classId);
    List<Exam> findBySubjectId(Long subjectId);
}
