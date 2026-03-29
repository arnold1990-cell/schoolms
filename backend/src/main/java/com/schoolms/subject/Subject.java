package com.schoolms.subject;

import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.common.BaseEntity;
import com.schoolms.teacher.Teacher;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Subject extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    private Teacher assignedTeacher;

    @ManyToMany(mappedBy = "subjects")
    private Set<SchoolClass> classes = new HashSet<>();
}
