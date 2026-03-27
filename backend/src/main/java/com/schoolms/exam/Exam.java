package com.schoolms.exam;

import com.schoolms.academicsession.AcademicSession;
import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.common.BaseEntity;
import com.schoolms.subject.Subject;
import com.schoolms.term.Term;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Exam extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(unique = true)
    private String examCode;
    private LocalDate examDate;
    private Integer durationMinutes;
    private Double totalMarks;
    @Enumerated(EnumType.STRING)
    private ExamStatus status;

    @ManyToOne private SchoolClass schoolClass;
    @ManyToOne private Subject subject;
    @ManyToOne private Term term;
    @ManyToOne private AcademicSession academicSession;
}
