package com.schoolms.grading;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GradingService {
    private final GradeScaleRepository repository;

    public String resolveGrade(double score) {
        return repository.findAllByOrderByMinScoreDesc().stream()
                .filter(g -> score >= g.getMinScore() && score <= g.getMaxScore())
                .map(GradeScale::getGrade)
                .findFirst().orElse("N/A");
    }

    public List<GradeScale> list() { return repository.findAllByOrderByMinScoreDesc(); }
    public GradeScale save(GradeScale scale) { return repository.save(scale); }
}
