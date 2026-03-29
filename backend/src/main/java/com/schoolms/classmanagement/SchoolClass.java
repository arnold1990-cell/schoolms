package com.schoolms.classmanagement;

import com.schoolms.common.BaseEntity;
import com.schoolms.subject.Subject;
import com.schoolms.teacher.Teacher;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "school_class")
public class SchoolClass extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true)
    private String code;

    private String level;

    private String academicYear;

    private String stream;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchoolClassStatus status = SchoolClassStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "class_teacher_id")
    private Teacher classTeacher;

    @ManyToMany
    @JoinTable(
            name = "class_subjects",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private Set<Subject> subjects = new HashSet<>();
}
