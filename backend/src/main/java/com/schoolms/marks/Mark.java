package com.schoolms.marks;

import com.schoolms.common.BaseEntity;
import com.schoolms.exam.Exam;
import com.schoolms.student.Student;
import com.schoolms.teacher.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "student_id"}))
public class Mark extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Exam exam;
    @ManyToOne(optional = false) private Student student;
    @ManyToOne(optional = false) private Teacher teacher;
    private Double score;
    private String grade;
}
