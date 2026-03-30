package com.schoolms.student;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByAdmissionNumberIgnoreCase(String admissionNumber);

    boolean existsByAdmissionNumberIgnoreCaseAndIdNot(String admissionNumber, Long id);

    List<Student> findBySchoolClassId(Long classId);

    List<Student> findAllByOrderByCreatedAtDesc();
}
