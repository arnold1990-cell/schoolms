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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;
    private String middleName;
    @Column(nullable = false)
    private String lastName;
    private String preferredName;

    @Column(nullable = false, unique = true)
    private String admissionNumber;

    @Column(nullable = false)
    private String gender;

    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private LocalDate enrollmentDate;

    private String guardianName;

    private String guardianRelationship;

    private String guardianPhone;

    @Column(length = 1000)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status;

    private String nationality;
    private String nationalId;
    private String passportNumber;
    private String previousSchool;

    private String phoneNumber;
    private String alternativePhoneNumber;
    private String email;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String postalCode;
    private String country;

    private String guardianAltPhone;
    private String guardianEmail;
    private String guardianOccupation;
    private String guardianAddress;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    private String bloodGroup;

    @Column(length = 1000)
    private String allergies;

    @Column(length = 1000)
    private String medicalConditions;

    @Column(length = 1000)
    private String disabilities;

    @Column(length = 1000)
    private String medication;

    private String hospitalName;
    private String doctorName;
    private String doctorPhone;

    private Boolean usesTransport;
    private String pickupPoint;
    private String routeName;
    private String driverAssignment;

    private String religion;
    private String homeLanguage;
    private String residencyType;
    private String sponsorshipStatus;
    private String feeCategory;

    @Column(length = 1000)
    private String notes;

    @ManyToOne
    private SchoolClass schoolClass;
}
