package com.schoolms.subject;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByAssignedTeacherId(Long teacherId);
}
