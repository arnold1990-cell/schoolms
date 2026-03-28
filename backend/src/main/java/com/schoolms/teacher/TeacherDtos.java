package com.schoolms.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public class TeacherDtos {
    public record UpsertTeacherRequest(
            @NotBlank(message = "Employee number is required") String employeeNumber,
            @NotBlank(message = "First name is required") String firstName,
            String middleName,
            @NotBlank(message = "Last name is required") String lastName,
            @NotNull(message = "Gender is required") TeacherGender gender,
            LocalDate dateOfBirth,
            @NotBlank(message = "Phone number is required") String phoneNumber,
            String alternativePhoneNumber,
            @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
            String nationalId,
            String passportNumber,
            @NotBlank(message = "Department is required") String department,
            @NotBlank(message = "Specialization is required") String specialization,
            @NotNull(message = "Employment type is required") EmploymentType employmentType,
            @NotNull(message = "Hire date is required") LocalDate hireDate,
            @NotNull(message = "Status is required") TeacherStatus status,
            @NotBlank(message = "Address is required") String address,
            TeacherTitle title,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            String qualification,
            String highestEducationLevel,
            @PositiveOrZero(message = "Years of experience must be 0 or greater") Integer yearsOfExperience,
            String staffRole,
            String salaryGrade,
            String notes,
            String profilePhotoUrl,
            String password,
            Boolean enabled
    ) {}

    public record TeacherResponse(
            Long id,
            String employeeNumber,
            String staffCode,
            String firstName,
            String middleName,
            String lastName,
            String fullName,
            TeacherGender gender,
            LocalDate dateOfBirth,
            String phoneNumber,
            String phone,
            String alternativePhoneNumber,
            String email,
            String nationalId,
            String passportNumber,
            String department,
            String specialization,
            EmploymentType employmentType,
            LocalDate hireDate,
            TeacherStatus status,
            String address,
            TeacherTitle title,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            String qualification,
            String highestEducationLevel,
            Integer yearsOfExperience,
            String staffRole,
            String salaryGrade,
            String notes,
            String profilePhotoUrl,
            boolean enabled
    ) {}
}
