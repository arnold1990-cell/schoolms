package com.schoolms.student;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByAdmissionNumberIgnoreCase(String admissionNumber);

    boolean existsByAdmissionNumberIgnoreCaseAndIdNot(String admissionNumber, Long id);

    Optional<Student> findByAdmissionNumberIgnoreCase(String admissionNumber);

    List<Student> findBySchoolClassId(Long classId);

    @Query("""
            select s from Student s
            left join s.schoolClass c
            where (:keyword is null or :keyword = ''
                or lower(s.firstName) like lower(concat('%', :keyword, '%'))
                or lower(s.lastName) like lower(concat('%', :keyword, '%'))
                or lower(s.admissionNumber) like lower(concat('%', :keyword, '%'))
                or lower(s.grade) like lower(concat('%', :keyword, '%'))
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(c.stream) like lower(concat('%', :keyword, '%'))
                or lower(cast(s.status as string)) like lower(concat('%', :keyword, '%')))
              and (:grade is null or :grade = '' or lower(s.grade) = lower(:grade))
              and (:classId is null or c.id = :classId)
              and (:status is null or s.status = :status)
            order by s.createdAt desc
            """)
    List<Student> search(
            @Param("keyword") String keyword,
            @Param("grade") String grade,
            @Param("classId") Long classId,
            @Param("status") StudentStatus status
    );
}
