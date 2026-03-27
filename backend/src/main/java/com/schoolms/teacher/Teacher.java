package com.schoolms.teacher;

import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.common.BaseEntity;
import com.schoolms.user.User;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Teacher extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String staffCode;
    private String phone;

    @OneToOne(optional = false)
    private User user;

    @ManyToMany
    @JoinTable(name = "teacher_classes", joinColumns = @JoinColumn(name = "teacher_id"), inverseJoinColumns = @JoinColumn(name = "class_id"))
    private Set<SchoolClass> assignedClasses = new HashSet<>();
}
