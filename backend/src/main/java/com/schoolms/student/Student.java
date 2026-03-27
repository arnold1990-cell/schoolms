package com.schoolms.student;

import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Student extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String admissionNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private String guardianName;
    private String guardianContact;
    private String status;

    @ManyToOne
    private SchoolClass schoolClass;
}
