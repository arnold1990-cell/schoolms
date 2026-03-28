package com.schoolms.teacher;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUserId(Long userId);

    boolean existsByEmployeeNumberIgnoreCase(String employeeNumber);

    boolean existsByEmployeeNumberIgnoreCaseAndIdNot(String employeeNumber, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByNationalIdIgnoreCase(String nationalId);

    boolean existsByNationalIdIgnoreCaseAndIdNot(String nationalId, Long id);

    @Query("""
            select t from Teacher t
            where lower(t.employeeNumber) like lower(concat('%', :keyword, '%'))
               or lower(concat(t.firstName, ' ', t.lastName)) like lower(concat('%', :keyword, '%'))
               or lower(t.department) like lower(concat('%', :keyword, '%'))
               or lower(t.specialization) like lower(concat('%', :keyword, '%'))
               or lower(cast(t.status as string)) like lower(concat('%', :keyword, '%'))
            """)
    List<Teacher> search(String keyword);
}
