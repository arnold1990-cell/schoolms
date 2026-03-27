package com.schoolms.grading;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class GradeScale extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String grade;
    private Double minScore;
    private Double maxScore;
    private boolean passing;
}
