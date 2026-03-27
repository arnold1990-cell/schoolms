package com.schoolms.grading;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeScaleRepository extends JpaRepository<GradeScale, Long> {
    List<GradeScale> findAllByOrderByMinScoreDesc();
}
