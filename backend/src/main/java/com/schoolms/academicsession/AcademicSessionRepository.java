package com.schoolms.academicsession;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
    Optional<AcademicSession> findByActiveTrue();
}
