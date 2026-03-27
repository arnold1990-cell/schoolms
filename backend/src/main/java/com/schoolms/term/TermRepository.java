package com.schoolms.term;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {
    Optional<Term> findByActiveTrue();
}
