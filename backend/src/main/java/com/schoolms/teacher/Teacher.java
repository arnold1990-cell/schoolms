package com.schoolms.teacher;

import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.common.BaseEntity;
import com.schoolms.user.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "teacher")
public class Teacher extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String employeeNumber;

    @Column
    private String firstName;

    private String middleName;

    @Column
    private String lastName;

    @Enumerated(EnumType.STRING)
    private TeacherTitle title;

    @Enumerated(EnumType.STRING)
    private TeacherGender gender;

    private LocalDate dateOfBirth;

    @Column
    private String phoneNumber;

    private String alternativePhoneNumber;

    @Column
    private String email;

    @Column(unique = true)
    private String nationalId;

    private String passportNumber;

    @Column
    private String department;

    @Column
    private String specialization;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    private TeacherStatus status;

    @Column(length = 600)
    private String address;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String qualification;
    private String highestEducationLevel;
    private Integer yearsOfExperience;
    private String staffRole;
    private String salaryGrade;

    @Column(length = 1500)
    private String notes;

    private String profilePhotoUrl;

    @OneToOne(optional = false)
    private User user;

    @ManyToMany
    @JoinTable(name = "teacher_classes", joinColumns = @JoinColumn(name = "teacher_id"), inverseJoinColumns = @JoinColumn(name = "class_id"))
    private Set<SchoolClass> assignedClasses = new HashSet<>();

    /**
     * Backward-compatibility for existing code/tests using staffCode.
     */
    public String getStaffCode() {
        return employeeNumber;
    }

    public void setStaffCode(String staffCode) {
        this.employeeNumber = staffCode;
    }

    /**
     * Backward-compatibility for existing code/tests using phone.
     */
    public String getPhone() {
        return phoneNumber;
    }

    public void setPhone(String phone) {
        this.phoneNumber = phone;
    }
}
